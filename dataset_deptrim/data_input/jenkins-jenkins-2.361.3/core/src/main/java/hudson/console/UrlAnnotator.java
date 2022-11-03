package hudson.console;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.MarkupText;
import hudson.MarkupText.SubText;
import java.util.regex.Pattern;
import org.jenkinsci.Symbol;

/**
 * Annotates URLs in the console output to hyperlink.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension @Symbol("url")
public class UrlAnnotator extends ConsoleAnnotatorFactory<Object> {
    @Override
    public ConsoleAnnotator newInstance(Object context) {
        return new UrlConsoleAnnotator();
    }

    private static class UrlConsoleAnnotator extends ConsoleAnnotator {
        @Override
        public ConsoleAnnotator annotate(@NonNull Object context, @NonNull MarkupText text) {
            for (SubText t : text.findTokens(URL)) {
                int prev = t.start() - 1;
                char ch = prev >= 0 ? text.charAt(prev) : ' ';
                int idx = OPEN.indexOf(ch);
                if (idx >= 0) { // if inside a bracket, exclude the end bracket.
                    t = t.subText(0, t.getText().indexOf(CLOSE.charAt(idx)));
                }
                t.href(t.getText());
            }
            return this;
        }

        private static final long serialVersionUID = 1L;

        /**
         * Starts with a word boundary and protocol identifier,
         * don't include any whitespace, '&lt;', nor '>'.
         * In addition, the last character shouldn't be ',' ':', '"', etc, as often those things show up right next
         * to URL in plain text (e.g., test="http://www.example.com/")
         */
        private static final Pattern URL = Pattern.compile("\\b(http|https|file|ftp)://[^\\s<>]+[^\\s<>,\\.:\"'()\\[\\]=]");

        private static final String OPEN = "'\"()[]<>";
        private static final String CLOSE = "'\")(][><";
    }
}
