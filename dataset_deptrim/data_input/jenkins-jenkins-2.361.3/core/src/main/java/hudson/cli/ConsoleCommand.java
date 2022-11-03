package hudson.cli;

import hudson.Extension;
import hudson.console.AnnotatedLargeText;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Run;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * cat/tail/head of the console output.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ConsoleCommand extends CLICommand {
    @Override
    public String getShortDescription() {
        return Messages.ConsoleCommand_ShortDescription();
    }

    @Argument(metaVar = "JOB", usage = "Name of the job", required = true)
    public Job<?, ?> job;

    @Argument(metaVar = "BUILD", usage = "Build number or permalink to point to the build. Defaults to the last build", required = false, index = 1)
    public String build = "lastBuild";

    @Option(name = "-f", usage = "If the build is in progress, stay around and append console output as it comes, like 'tail -f'")
    public boolean follow = false;

    @Option(name = "-n", metaVar = "N", usage = "Display the last N lines")
    public int n = -1;

    @Override
    protected int run() throws Exception {
        job.checkPermission(Item.READ);

        Run<?, ?> run;

        try {
            int n = Integer.parseInt(build);
            run = job.getBuildByNumber(n);
            if (run == null)
                throw new IllegalArgumentException("No such build #" + n);
        } catch (NumberFormatException e) {
            // maybe a permalink?
            Permalink p = job.getPermalinks().get(build);
            if (p != null) {
                run = p.resolve(job);
                if (run == null)
                    throw new IllegalStateException("Permalink " + build + " produced no build", e);
            } else {
                Permalink nearest = job.getPermalinks().findNearest(build);
                throw new IllegalArgumentException(nearest == null ?
                        String.format("Not sure what you meant by \"%s\".", build) :
                        String.format("Not sure what you meant by \"%s\". Did you mean \"%s\"?",
                                build, nearest.getId()), e);
            }
        }

        OutputStreamWriter w = new OutputStreamWriter(stdout, getClientCharset());
        try {
            long pos = n >= 0 ? seek(run) : 0;

            if (follow) {
                AnnotatedLargeText logText;
                do {
                    logText = run.getLogText();
                    pos = logText.writeLogTo(pos, w);
                    // TODO should sleep as in Run.writeWholeLogTo
                } while (!logText.isComplete());
            } else {
                try (InputStream logInputStream = run.getLogInputStream()) {
                    IOUtils.skip(logInputStream, pos);
                    IOUtils.copy(new InputStreamReader(logInputStream, run.getCharset()), w);
                }
            }
        } finally {
            w.flush(); // this pointless flush needed to work around SSHD-154
            w.close();
        }

        return 0;
    }

    /**
     * Find the byte offset in the log input stream that marks "last N lines".
     */
    private long seek(Run<?, ?> run) throws IOException {
        class RingBuffer {
            long[] lastNlines = new long[n];
            int ptr = 0;

            RingBuffer() {
                for (int i = 0; i < n; i++)
                    lastNlines[i] = -1;
            }

            void add(long pos) {
                lastNlines[ptr] = pos;
                ptr = (ptr + 1) % lastNlines.length;
            }

            long get() {
                long v = lastNlines[ptr];
                if (v < 0)    return lastNlines[0];   // didn't even wrap around
                return v;
            }
        }

        RingBuffer rb = new RingBuffer();

        try (InputStream in = run.getLogInputStream()) {
            byte[] buf = new byte[4096];
            int len;
            byte prev = 0;
            long pos = 0;
            boolean prevIsNL = false;
            while ((len = in.read(buf)) >= 0) {
                for (int i = 0; i < len; i++) {
                    byte ch = buf[i];
                    boolean isNL = ch == '\r' || ch == '\n';
                    if (!isNL && prevIsNL)  rb.add(pos);
                    if (isNL && prevIsNL && !(prev == '\r' && ch == '\n'))  rb.add(pos);
                    pos++;
                    prev = ch;
                    prevIsNL = isNL;
                }
            }

            return rb.get();
        }
    }

    @Override
    protected void printUsageSummary(PrintStream stderr) {
        stderr.println(
            "Produces the console output of a specific build to stdout, as if you are doing 'cat build.log'"
        );
    }
}
