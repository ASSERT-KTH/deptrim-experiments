/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.cli;

import hudson.Extension;
import hudson.model.Run;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Deletes builds records in a bulk.
 *
 * @author Kohsuke Kawaguchi
 */
@Restricted(NoExternalUse.class) // command implementation only
@Extension
public class DeleteBuildsCommand extends RunRangeCommand {
    @Override
    public String getShortDescription() {
        return Messages.DeleteBuildsCommand_ShortDescription();
    }

    @Override
    protected void printUsageSummary(PrintStream stderr) {
        stderr.println(
            "Delete build records of a specified job, possibly in a bulk. "
        );
    }

    @Override
    protected int act(List<Run<?, ?>> builds) throws IOException {
        job.checkPermission(Run.DELETE);

        final HashSet<Integer> hsBuilds = new HashSet<>();

        for (Run<?, ?> build : builds) {
            if (!hsBuilds.contains(build.number)) {
                build.delete();
                hsBuilds.add(build.number);
            }
        }

        stdout.println("Deleted " + hsBuilds.size() + " builds");

        return 0;
    }

}
