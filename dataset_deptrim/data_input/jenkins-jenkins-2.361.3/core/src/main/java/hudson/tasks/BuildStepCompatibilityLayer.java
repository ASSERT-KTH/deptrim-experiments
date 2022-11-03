/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

package hudson.tasks;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jenkins.tasks.SimpleBuildStep;

/**
 * Provides compatibility with {@link BuildStep} before 1.150
 * so that old plugin binaries can continue to function with new Hudson.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.150
 * @deprecated since 1.150
 */
@Deprecated
public abstract class BuildStepCompatibilityLayer implements BuildStep {
//
// new definitions >= 1.150
//
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (build instanceof Build)
            return prebuild((Build) build, listener);
        else
            return true;
    }

    /**
     * @return Delegates to {@link SimpleBuildStep#perform(Run, FilePath, Launcher, TaskListener)} if possible, always returning true or throwing an error.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (this instanceof SimpleBuildStep) {
            // delegate to the overloaded version defined in SimpleBuildStep
            final SimpleBuildStep step = (SimpleBuildStep) this;
            final FilePath workspace = build.getWorkspace();
            if (step.requiresWorkspace() && workspace == null) {
                throw new AbortException("no workspace for " + build);
            }
            if (workspace != null) { // if we have one, provide it regardless of whether it's _required_
                step.perform(build, workspace, build.getEnvironment(listener), launcher, listener);
            } else {
                step.perform(build, build.getEnvironment(listener), listener);
            }
            return true;
        } else if (build instanceof Build) {
            // delegate to the legacy signature deprecated in 1.312
            return perform((Build) build, launcher, listener);
        } else {
            return true;
        }
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        if (project instanceof Project)
            return getProjectAction((Project) project);
        else
            return null;
    }

    @Override
    @NonNull
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        // delegate to getJobAction (singular) for backward compatible behavior
        Action a = getProjectAction(project);
        if (a == null)    return Collections.emptyList();
        return List.of(a);
    }


//
// old definitions < 1.150
//
    /**
     * @deprecated
     *      Use {@link #prebuild(AbstractBuild, BuildListener)} instead.
     */
    @Deprecated
    public boolean prebuild(Build<?, ?> build, BuildListener listener) {
        return true;
    }

    /**
     * @deprecated
     *      Use {@link #perform(AbstractBuild, Launcher, BuildListener)} instead.
     */
    @Deprecated
    public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (build != null && Util.isOverridden(BuildStepCompatibilityLayer.class, this.getClass(),
                "perform", AbstractBuild.class, Launcher.class, BuildListener.class)) {
            return perform((AbstractBuild<?, ?>) build, launcher, listener);
        }
        throw new AbstractMethodError();
    }

    /**
     * @deprecated
     *      Use {@link #getProjectAction(AbstractProject)} instead.
     */
    @Deprecated
    public Action getProjectAction(Project<?, ?> project) {
        return null;
    }
}
