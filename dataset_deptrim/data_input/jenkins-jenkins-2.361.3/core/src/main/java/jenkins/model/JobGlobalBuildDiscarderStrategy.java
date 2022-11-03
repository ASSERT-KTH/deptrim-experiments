/*
 * The MIT License
 *
 * Copyright 2019 Daniel Beck
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

package jenkins.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import java.io.IOException;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Periodically call a job's configured build discarder in the background.
 */
@Restricted(NoExternalUse.class)
public class JobGlobalBuildDiscarderStrategy extends GlobalBuildDiscarderStrategy {

    @DataBoundConstructor
    public JobGlobalBuildDiscarderStrategy() {
        // required for data binding
    }

    @Override
    public boolean isApplicable(Job<?, ?> job) {
        return job.getBuildDiscarder() != null;
    }

    @Override
    public void apply(Job<?, ?> job) throws IOException, InterruptedException {
        job.logRotate();
    }

    @Extension @Symbol("jobBuildDiscarder")
    public static class DescriptorImpl extends GlobalBuildDiscarderStrategyDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.JobGlobalBuildDiscarderStrategy_displayName();
        }
    }
}
