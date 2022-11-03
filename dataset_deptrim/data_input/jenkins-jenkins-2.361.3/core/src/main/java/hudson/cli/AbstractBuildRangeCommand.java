package hudson.cli;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Fingerprint.RangeSet;
import java.io.IOException;
import java.util.List;
import org.kohsuke.args4j.Argument;

/**
 * {@link CLICommand} that acts on a series of {@link AbstractBuild}s.
 *
 * @author Kohsuke Kawaguchi
 * @deprecated rather use {@link RunRangeCommand}
 */
@Deprecated
public abstract class AbstractBuildRangeCommand extends CLICommand {
    @Argument(metaVar = "JOB", usage = "Name of the job to build", required = true, index = 0)
    public AbstractProject<?, ?> job;

    @Argument(metaVar = "RANGE", usage = "Range of the build records to delete. 'N-M', 'N,M', or 'N'", required = true, index = 1)
    public String range;

    @Override
    protected int run() throws Exception {
        RangeSet rs = RangeSet.fromString(range, false);

        return act((List) job.getBuilds(rs));
    }

    protected abstract int act(List<AbstractBuild<?, ?>> builds) throws IOException;
}
