/*
 * The MIT License
 *
 * Copyright 2014 Jesse Glick.
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

package jenkins.tasks;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.InvisibleAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jenkins.model.DependencyDeclarer;
import jenkins.model.RunAction2;
import jenkins.model.TransientActionFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * A build step (like a {@link Builder} or {@link Publisher}) which may be called at an arbitrary time during a build (or multiple times), run, and be done.
 * <p>Such a build step would typically be written according to some guidelines that ensure it makes few assumptions about how it is being used:
 * <ul>
 * <li>Do not implement {@link BuildStep#prebuild}, since this presupposes a particular execution order.
 * <li>Do not implement {@link BuildStep#getProjectActions}, since this might never be called
 *     if the step is not part of the static configuration of a project; instead, add a {@link LastBuildAction} to a build when run.
 * <li>Implement {@link BuildStep#getRequiredMonitorService} to be {@link BuildStepMonitor#NONE}, since this facility
 *     only makes sense for a step called exactly once per build.
 * <li>Do not implement {@link DependencyDeclarer} since this would be limited to use in {@link AbstractProject}.
 * <li>Return true unconditionally from {@link BuildStepDescriptor#isApplicable} (there is currently no filtering for other {@link Job} types).
 * <li>Do not expect {@link Executor#currentExecutor} to be non-null, and by extension do not use {@link Computer#currentComputer}.
 * </ul>
 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(AbstractBuild, Launcher, BuildListener)
 * @since 1.577
 */
public interface SimpleBuildStep extends BuildStep {

    /**
     * Run this step.
     * @param run a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher a way to start processes
     * @param listener a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong; use {@link AbortException} for a polite error
     * @deprecated Use {@link #perform(Run, FilePath, EnvVars, Launcher, TaskListener)} instead.
     */
    @Deprecated
    default void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
                         @NonNull TaskListener listener) throws InterruptedException, IOException {
        // No additional environment available; just use that from the Run.
        this.perform(run, workspace, run.getEnvironment(listener), launcher, listener);
    }

    /**
     * Run this step.
     * <p>
     * This method <strong>must</strong> be overridden when this step requires a workspace context. If such a context is
     * <em>not</em> required, it does not need to be overridden; it will then forward to
     * {@link #perform(Run, EnvVars, TaskListener)}.
     *
     * @param run a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param env environment variables applicable to this step
     * @param launcher a way to start processes
     * @param listener a place to send output
     * @throws AbstractMethodError if this step requires a workspace context and neither this method nor {@link #perform(Run, FilePath, Launcher, TaskListener)} is overridden
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong; use {@link AbortException} for a polite error
     * @since 2.241
     */
    default void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher,
                         @NonNull TaskListener listener) throws InterruptedException, IOException {
        // If this does not require a workspace, defer to the version that does not take a workspace and launcher.
        if (!this.requiresWorkspace()) {
            this.perform(run, env, listener);
            return;
        }
        // If we get here, this must be an implementer of the previous API, in which case we call that, discarding
        // the environment we were given.
        // But for that to work, that API method must have been implemented.
        if (Util.isOverridden(SimpleBuildStep.class, this.getClass(),
                "perform", Run.class, FilePath.class, Launcher.class, TaskListener.class)) {
            this.perform(run, workspace, launcher, listener);
        } else {
            throw new AbstractMethodError("Unless a build step is marked as not requiring a workspace context, you must implement the overload of the perform() method that takes both a workspace and a launcher.");
        }
    }

    /**
     * Run this step, without a workspace context.
     * <p>
     * This method <strong>must</strong> be overridden when this step does not require a workspace context, and will not
     * be called when such a context <em>is</em> required.
     *
     * @param run a build this is running as a part of
     * @param env environment variables applicable to this step
     * @param listener a place to send output
     * @throws AbstractMethodError if this method is not overridden
     * @throws IllegalStateException if this step requires a workspace context
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong; use {@link AbortException} for a polite error
     * @since 2.258
     */
    default void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener) throws InterruptedException, IOException {
        // If this step requires a workspace, this is the wrong method to call.
        if (this.requiresWorkspace()) {
            throw new IllegalStateException("This build step requires a workspace context, but none was provided.");
        }
        // Otherwise, this method must have an implementation.
        throw new AbstractMethodError("When a build step is marked as not requiring a workspace context, you must implement the overload of the perform() method that does not take a workspace or launcher.");
    }

    /**
     * Determines whether or not this step requires a workspace context (working directory and launcher).
     * <p>
     * When such a context is required (the default), {@link #perform(Run, FilePath, EnvVars, Launcher, TaskListener)}
     * applies. Otherwise, {@link #perform(Run, EnvVars, TaskListener)} applies.
     *
     * @return {@code true} if this step requires a workspace context; {@code false} otherwise.
     * @since 2.258
     */
    default boolean requiresWorkspace() {
        return true;
    }

    /**
     * Marker for explicitly added build actions (as {@link Run#addAction}) which should imply a transient project
     * action ({@link Job#getActions}) when present on the {@link Job#getLastSuccessfulBuild}.
     * This can serve as a substitute for {@link BuildStep#getProjectActions} which does not assume that the project
     * can enumerate the steps it would run before they are actually run.
     * (Use {@link InvisibleAction} as a base class if you do not need to show anything in the build itself.)
     */
    interface LastBuildAction extends Action {

        /**
         * Optionally add some actions to the project owning this build.
         * @return zero or more transient actions;
         * if you need to know the {@link Job}, implement {@link RunAction2} and use {@link Run#getParent}
         */
        Collection<? extends Action> getProjectActions();

    }

    @SuppressWarnings("rawtypes")
    @Restricted(NoExternalUse.class)
    @Extension
    final class LastBuildActionFactory extends TransientActionFactory<Job> {

        @Override
        public Class<Job> type() {
            return Job.class;
        }

        @NonNull
        @Override
        public Collection<? extends Action> createFor(@NonNull Job j) {
            List<Action> actions = new ArrayList<>();
            Run r = j.getLastSuccessfulBuild();
            if (r != null) {
                for (LastBuildAction a : r.getActions(LastBuildAction.class)) {
                    actions.addAll(a.getProjectActions());
                }
            }
            // TODO should there be an option to check lastCompletedBuild even if it failed?
            // Not useful for, say, TestResultAction, since if you have a build that fails before recording test
            // results, the job would then have no TestResultProjectAction.
            return actions;
        }
    }

}
