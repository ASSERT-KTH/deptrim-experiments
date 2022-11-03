#!/usr/bin/perl
# The MIT License
#
# Copyright (c) 2004-, Kohsuke Kawaguchi, Sun Microsystems, Inc., and a number
# of other of contributors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

# Author: Manuel Carrasco
# Date:   20 Mar 2010

# Perl script to generate missing translation keys and missing properties files,
# to remove unused keys, and to convert utf8 properties files to iso or ascii.
#
# 1.- It recursively looks for files in a folder, and analyzes them to extract
#     the keys being used in the application.
# 2.- If --add=true, it generates the appropriate file for the desired language
#     and adds these keys to it, adding the english text as a reference. If the
#     properties file already exists the script update it with the new keys.
# 3.- When --remove=true and there are unused keys in our file, the script
#     removes them.
# 4.- If an editor is passed as argument, the script edits each modified file
#     after adding new keys.
# 5.- Finally, when --toiso=true or --toascii=true, the script is able to
#     convert utf-8 properties files to iso or unicode hex representation is
#     ascii.
#

# Note, while the migration to Jenkins this file will report the keys which
# should point to Jenkins instead of the old name.

use warnings;
use strict;
use File::Basename;
use File::Find;
use File::Path;
use Getopt::Long;

my (
    $lang, $editor, $dir, $toiso, $toascii, $add,
    $remove, $reuse, $counter, $target, $help, $debug
) = (undef, undef, "./", 0, 0, 0, 0, 0, 0, "./", 0, 0);

GetOptions(
    'help' => \$help,
    'lang=s' => \$lang,
    'editor' => \$editor,
    'dir=s' => \$dir,
    'to-iso' => \$toiso,
    'to-ascii' => \$toascii,
    'add' => \$add,
    'remove' => \$remove,
    'reuse' => \$reuse,
    'counter' => \$counter,
    'target=s' => \$target,
    'debug' => \$debug
) or die("Error in command line arguments\n");

if ($help) {
    usage();
    exit(0);
}

$add = 1 if ($editor);

if ($toiso && $toascii) {
    warn "You can't have --to-iso and --to-ascii options at the same time\n";
    usage();
    exit(1);
}

my ($tfiles, $tkeys, $tmissing, $tunused, $tempty, $tsame, $tnojenkins,
    $countervalue)
    = (0, 0, 0, 0, 0, 0, 0, 1);

## language parameter is mandatory and shouldn't be 'en'
unless (($lang) and ($lang ne 'en')) {
    usage();
    exit(1);
}

print "Searching for files ...\n";
## look for Message.properties and *.jelly files in the provided folder
my @files = findTranslatableFiles($dir);
print 'Found ', scalar(@files), ' files', "\n";

## load a cache with keys already translated to utilize in the case the same key
## is used
my %cache = loadAllTranslatedKeys($reuse, $lang) if ($reuse && -e $reuse);

## process each file
foreach (@files) {
    $tfiles++;
    processFile($_);
}

## print statistics
my $tdone = $tkeys - $tmissing - $tunused - $tempty - $tsame - $tnojenkins;

my $pdone = 100;
my $pmissing = 0;
my $punused = 0;
my $pempty = 0;
my $psame = 0;
my $pnojenkins = 0;

if ($tkeys != 0) {
    $pdone = $tdone / $tkeys * 100;
    $pmissing = $tmissing / $tkeys * 100;
    $punused = $tunused / $tkeys * 100;
    $pempty = $tempty / $tkeys * 100;
    $psame = $tsame / $tkeys * 100;
    $pnojenkins = $tnojenkins / $tkeys * 100;
}

my @formatParameters = (
    $tfiles, $tkeys, $tdone, $pdone, $tmissing,
    $pmissing, $tunused, $punused, $tempty, $pempty,
    $tsame, $psame, $tnojenkins, $pnojenkins
);
printf
    "\nTOTAL: Files: %d Keys: %d Done: %d(%.2f%%)\n       Missing: %d(%.2f%%) Orphan: %d(%.2f%%) Empty: %d(%.2f%%) Same: %d(%.2f%%) NoJenkins: %d(%.2f%%)\n\n",
    (@formatParameters);
## end
exit();

### This is the main method with is run for each file
sub processFile {

    #  ofile -> output file in the current language,
    #  efile -> english file
    my $file = shift;
    my ($ofile, $efile) = ($file, $file);
    $ofile =~ s/$dir/$target/;
    $ofile =~ s/(\.jelly)|(\.properties)/_$lang.properties/;
    $efile =~ s/(\.jelly)/.properties/;

    # keys  -> Hash of keys used in jelly or Message.properties files
    # ekeys -> Hash of key/values in English
    # okeys -> Hash of key/values in the desired language which are already
    #          present in the file
    my (%keys, %okeys, %ekeys);

    # Read .jelly or Message.properties files, and fill a hash with the keys
    # found
    if ($file =~ m/.jelly$/) {
        %keys = loadJellyFile($file);
        %ekeys = loadPropertiesFile($efile);
    } else {
        %keys = %ekeys = loadPropertiesFile($file);
    }

    # load keys already present in the desired locale
    %okeys = loadPropertiesFile($ofile);

    # calculate missing keys in the file
    my $missing = "";
    foreach (keys %keys) {
        $tkeys++;
        if (!defined($okeys{$_})) {
            $_ .= "=" . $cache{$_} if (defined($cache{$_}));
            $missing .= ($add ? "  Adding " : "  Missing") . " -> $_\n";
            $tmissing++;
        } elsif ($okeys{$_} eq '') {
            $missing .= "  Empty   -> $_\n";
            $tempty++;
        }
    }

    # calculate old keys in the file which are currently unused
    my $unused = "";
    foreach (keys %okeys) {
        if (!defined $keys{$_}) {
            $unused .= "  Unused  -> $_\n";
            $tunused++;
        }
    }

    # calculate keys which have the same value in English
    my $same = "";
    foreach (keys %okeys) {
        if ($okeys{$_} && $ekeys{$_} && $okeys{$_} eq $ekeys{$_}) {
            $same .= "  Same    -> $_\n";
            $tsame++;
        }
    }

    my $nj = "";
    foreach (keys %okeys) {
        if ($okeys{$_} && $okeys{$_} =~ /Hudson/) {
            $nj .= "  Non Jenkins    -> $_ -> $okeys{$_}\n";
            $tnojenkins++;
        }
    }

    # Show Alerts
    print "\nFile: $ofile\n$missing$unused$same$nj"
        if ($missing ne "" || $unused ne '' || $same ne '' || $nj ne '');

    # write new keys in our file adding the English translation as a reference
    if ($add && $missing ne "") {
        printLicense($ofile) unless (-f $ofile);
        open(F, ">>$ofile");
        foreach (keys %keys) {
            if (!$okeys{$_}) {
                if (!defined($okeys{$_})) {
                    print F "$_=";
                    if (defined($cache{$_})) {
                        print F $cache{$_} . "\n";
                    } else {
                        if ($counter) {
                            # add unique value for each added translation
                            print F "---TranslateMe " . $countervalue . "--- " .
                                ($ekeys{$_} ? $ekeys{$_} : $_) . "\n";
                        } else {
                            print F "\n";
                        }
                    }
                    $countervalue++;
                }
            }
        }
        close(F);
    }

    # open the editor if the user has specified it and there are changes to
    # manage
    system("$editor $ofile")
        if ($editor && $add && ($missing ne "" || $same ne "" || $nj ne ''));

    # write new keys in our file adding the English translation as a reference
    removeUnusedKeys($ofile, %keys) if ($remove && $unused ne "");

    # convert the language file to ISO or ASCII which are
    # the charsets which Jenkins supports right now
    convert($ofile, $toiso, $toascii) if (-f $ofile);
}

# Create a hash with all keys which exist and have an unique value
sub loadAllTranslatedKeys {
    my ($dir, $lang, %ret) = @_;
    my @files = findTranslatableFiles($dir);
    foreach (@files) {
        s/(\.jelly)|(\.properties)/_$lang.properties/;
        next unless (-f $_);
        my (%h, $k, $v) = loadPropertiesFile($_);
        while (($k, $v) = each(%h)) {
            $ret{$k} = "" if (defined($ret{$k}) && $v ne $ret{$k});
            $ret{$k} = $v unless defined($ret{$k});
        }
    }
    return %ret;
}

# Look for Message.properties and *.jelly files
sub findTranslatableFiles {
    my $dir = shift;
    die "Folder doesn't exist: $dir\n" unless (-e $dir);
    my @ret;
    find(
        sub {
            my $file = $File::Find::name;
            push(@ret, $file)
                if ($file !~ m#(/src/test/)|(/target/)|(\.svn)#
                && $file =~ /(Messages.properties)$|(.*\.jelly)$/);
        },
        $dir
    );
    return @ret;
}

# Fill a hash with key/1 pairs from a .jelly file
sub loadJellyFile {
    my $file = shift;
    my %ret;
    open(F, $file) || die $! . " " . $file;
    while (<F>) {
        next if (!/\$\{.*?\%([^\(]+?).*\}/);
        my $line = $_;
        while ($line =~ /^.*?\$\{\%([^\(\}]+)(.*)$/
            || $line =~ /^.*?\$\{.*?['"]\%([^\(\}\"\']+)(.*)$/)
        {
            $line = $2;
            my $word = $1;
            $word =~ s/\(.+$//g;
            $word =~ s/'+/''/g;
            $word =~ s/ /\\ /g;
            $word =~ s/\&gt;/>/g;
            $word =~ s/\&lt;/</g;
            $word =~ s/\&amp;/&/g;
            $word =~ s/([#:=])/\\$1/g;
            $ret{$word} = 1;
        }
    }
    close(F);
    return %ret;
}

# Fill a hash with key/value pairs from a .properties file
sub loadPropertiesFile {
    my $file = shift;
    my %ret;
    print "Trying to load $file... " if ($debug);

    unless (-f $file) {
        print "file does not exist, skipping.\n" if ($debug);
        return %ret;
    }

    print "done.\n" if ($debug);
    my $skip_comment = qr/^#/;
    open(my $in, '<', $file) or die "Cannot read $file: $!\n";
    my ($cont, $key, $val) = (0, undef, undef);

    while (<$in>) {
        chomp;
        next if $_ eq '';
        next if $_ =~ $skip_comment;
        print 'Line: ', $_, "\n" if ($debug);
        s/[\r\n]+//;
        $ret{$key} .= "\n$1" if ($cont && /\s*(.*)[\\\s]*$/);
        if (/^([^#\s].*?[^\\])=(.*)[\s\\]*$/) {
            ($key, $val) = (trim($1), trim($2));
            $ret{$key} = $val;
        }
        $cont = (/\\\s*$/) ? 1 : 0;
    }

    close($in);
    # TODO: Use of uninitialized value $_ in pattern match (m//) at
    # ./translation-tool.pl line 345.
    $ret{$key} .= "\n$1" if ($cont && /\s*(.*)[\\\s]*$/);
    return %ret;
}

# remove unused keys from a file
sub removeUnusedKeys {
    my ($ofile, %keys) = @_;
    print "Removing unused keys from: $ofile\n";
    my $back = $ofile . "~~";
    if (rename($ofile, $back) && open(FI, $back) && open(FO, ">$ofile")) {
        my $cont = 0;
        while (<FI>) {
            if (!$cont) {
                if (/^([^#\s].*?[^\\])=(.*)[\s\\]*$/) {
                    if (!$keys{$1}) {
                        $cont = (/\\\s*$/) ? 1 : 0;
                        next;
                    }
                }
                print FO $_;
            } elsif ($cont && !/\\\s*$/) {
                $cont = 0;
            }
        }
        close(FI);
        close(FO);
        unlink($back);
    }
}

# convert a UTF-8 file to either ISO-8859 or ASCII
sub convert {
    my ($ofile, $toiso, $toascii) = @_;
    if (isUtf8($ofile) && ($toiso || $toascii)) {
        print "\nConverting file $ofile to " .
            ($toiso ? "ISO-8859" : "ASCII") . "\n";
        my $back = $ofile . "~~";
        if (rename($ofile, $back) && open(FI, $back) && open(FO, ">$ofile")) {
            while (<FI>) {
                if ($toiso) {
                    s/([\xC2\xC3])([\x80-\xBF])/chr(ord($1)<<6&0xC0|ord($2)&0x3F)/eg;
                } else {
                    s/([\xC0-\xDF])([\x80-\xBF])/sprintf('\\u%04x',
                                         unpack("c",$1)<<6&0x07C0|unpack("c",$2)&0x003F)/ge;
                    s/([\xE0-\xEF])([\x80-\xBF])([\x80-\xBF])/sprintf('\\u%04x',
                                         unpack("c",$1)<<12&0xF000|unpack("c",$2)<<6&0x0FC0|unpack("c",$3)&0x003F)/ge;
                    s/([\xF0-\xF7])([\x80-\xBF])([\x80-\xBF])([\x80-\xBF])/sprintf('\\u%04x',
                                         unpack("c",$1)<<18&0x1C0000|unpack("c",$2)<<12&0x3F000|
                                         unpack("c",$3)<<6&0x0FC0|unpack("c",$4)&0x003F)/ge;
                }
                print FO "$_";
            }
            close(FI);
            close(FO);
            unlink($back);
        }
    }
}

# Return true if the file has any UTF-8 character
sub isUtf8 {
    my $file = shift;
    if (open(F, $file)) {
        while (<F>) {
            if (/([\xC2\xC3])([\x80-\xBF])/) {
                close(F);
                return 1;
            }
        }
        close(F);
    }
    return 0;
}

# print MIT license in new files
# Note: the license is read from the head of this file
my $license;

sub printLicense {
    my $file = shift;
    if (!$license && open(F, $0)) {
        $license = "";
        my $on = 0;
        while (<F>) {
            $on = 1 if (!$on && /The MIT/);
            last if ($on && (/^$/ || /^[^#]/));
            $license .= $_ if ($on);
        }
        close(F);
    }
    if ($license && $license ne "") {
        my $dirname = dirname($file);
        unless (-d $dirname) {
            mkpath($dirname);
        }
        open(F, ">" . $file) || die $!;
        print F "$license\n";
        close(F);
    }
}

# trim function to remove whitespace from the start and end of the string
sub trim($) {
    my $string = shift;
    $string =~ s/^\s+//;
    $string =~ s/\s+$//;
    return $string;
}

### Usage
sub usage {
    print "
Translation Tool for Jenkins

Usage: $0 --lang=xx [options] [dir]

   dir:               -> source folder for searching files (default is the current directory)
   options:
     --help           -> print this help message and terminates the program with exit code 0.
     --lang=xx        -> language code to use (it is mandatory and it has to be different to English)
     --to-iso         -> optional, enables files in UTF-8 convertion to ISO-8859 if present
     --to-ascii       -> optional, convert files in UTF-8 to ASCII using the native2ascii command if present
     --add            -> optional, generate new files and add new keys to existing files if present
     --remove         -> optional, remove unused key/value pair for existing files if present
     --editor=command -> command to run over each updated file, implies --add if present
     --reuse=folder   -> optional, load a cache with keys already translated in the folder provided in
                         order to utilize them when the same key appears if present
     --counter        -> optional, to each translated key, unique value is added to easily identify match missing translation
                         with value in source code if present
     --target=folder  -> optional, target folder for writing files
     --debug          -> optional, print debugging messages to STDOUT when they are available

   Examples:
     - Look for Spanish files with incomplete keys in the 'main' folder,
       edit them with gedit, and finally convert them to ISO-8859
        $0 --lang=es --editor=gedit --to-iso main
     - Convert all Japanese files in the current folder encoded with UTF-8 to ASCII
        $0 --lang=ja --to-ascii .
     - Remove all orphaned keys from German files which are in the current file
        $0 --lang=de --remove .

";
}
