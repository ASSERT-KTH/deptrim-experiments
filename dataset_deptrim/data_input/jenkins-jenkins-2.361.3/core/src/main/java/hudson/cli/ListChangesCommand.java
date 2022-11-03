package hudson.cli;

import hudson.Extension;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.util.QuotedStringTokenizer;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import jenkins.scm.RunWithSCM;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.args4j.Option;
import org.kohsuke.stapler.export.Flavor;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;

/**
 * Retrieves a change list for the specified builds.
 *
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class) // command implementation only
@Extension
public class ListChangesCommand extends RunRangeCommand {
    @Override
    public String getShortDescription() {
        return Messages.ListChangesCommand_ShortDescription();
    }

//    @Override
//    protected void printUsageSummary(PrintStream stderr) {
//        TODO
//    }

    enum Format {
        XML, CSV, PLAIN
    }

    @Option(name = "-format", usage = "Controls how the output from this command is printed.")
    public Format format = Format.PLAIN;

    @Override
    protected int act(List<Run<?, ?>> builds) throws IOException {
        // Loading job for this CLI command requires Item.READ permission.
        // No other permission check needed.
        switch (format) {
        case XML:
            Charset charset;
            try {
                charset = getClientCharset();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            PrintWriter w = new PrintWriter(new OutputStreamWriter(stdout, charset));
            w.println("<changes>");
            for (Run<?, ?> build : builds) {
                if (build instanceof RunWithSCM) {
                    w.println("<build number='" + build.getNumber() + "'>");
                    for (ChangeLogSet<?> cs : ((RunWithSCM<?, ?>) build).getChangeSets()) {
                        Model p = new ModelBuilder().get(cs.getClass());
                        p.writeTo(cs, Flavor.XML.createDataWriter(cs, w));
                    }
                    w.println("</build>");
                }
            }
            w.println("</changes>");
            w.flush();
            break;
        case CSV:
            for (Run<?, ?> build : builds) {
                if (build instanceof RunWithSCM) {
                    for (ChangeLogSet<?> cs : ((RunWithSCM<?, ?>) build).getChangeSets()) {
                        for (ChangeLogSet.Entry e : cs) {
                            stdout.printf("%s,%s%n",
                                    QuotedStringTokenizer.quote(e.getAuthor().getId()),
                                    QuotedStringTokenizer.quote(e.getMsg()));
                        }
                    }
                }
            }
            break;
        case PLAIN:
            for (Run<?, ?> build : builds) {
                if (build instanceof RunWithSCM) {
                    for (ChangeLogSet<?> cs : ((RunWithSCM<?, ?>) build).getChangeSets()) {
                        for (ChangeLogSet.Entry e : cs) {
                            stdout.printf("%s\t%s%n", e.getAuthor(), e.getMsg());
                            for (String p : e.getAffectedPaths()) {
                                stdout.println("  " + p);
                            }
                        }
                    }
                }
            }
            break;
        default:
            throw new AssertionError("Unknown format: " + format);
        }

        return 0;
    }

}
