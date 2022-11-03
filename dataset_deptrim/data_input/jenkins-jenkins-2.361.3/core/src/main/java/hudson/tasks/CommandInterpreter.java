/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Tom Huybrechts
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
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.ChannelClosedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.tasks.filters.EnvVarsFilterException;
import jenkins.tasks.filters.EnvVarsFilterLocalRule;
import jenkins.tasks.filters.EnvVarsFilterableBuilder;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Common part between {@link Shell} and {@link BatchFile}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CommandInterpreter extends Builder implements EnvVarsFilterableBuilder {
    /**
     * Command to execute. The format depends on the actual {@link CommandInterpreter} implementation.
     */
    protected final String command;

    /**
     * List of configured environment filter rules
     */
    @Restricted(Beta.class)
    protected List<EnvVarsFilterLocalRule> configuredLocalRules = new ArrayList<>();

    protected CommandInterpreter(String command) {
        this.command = command;
    }

    public final String getCommand() {
        return command;
    }

    @Override
    public @NonNull List<EnvVarsFilterLocalRule> buildEnvVarsFilterRules() {
        return configuredLocalRules == null ? Collections.emptyList() : new ArrayList<>(configuredLocalRules);
    }

    // used by Jelly view
    @Restricted(NoExternalUse.class)
    public List<EnvVarsFilterLocalRule> getConfiguredLocalRules() {
        return configuredLocalRules == null ? Collections.emptyList() : configuredLocalRules;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        return perform(build, launcher, (TaskListener) listener);
    }

    /**
     * Determines whether a non-zero exit code from the process should change the build
     * status to {@link Result#UNSTABLE} instead of default {@link Result#FAILURE}.
     *
     * Changing to {@link Result#UNSTABLE} does not abort the build, next steps are continued.
     *
     * @since 2.26
     */
    protected boolean isErrorlevelForUnstableBuild(int exitCode) {
        return false;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
        FilePath ws = build.getWorkspace();
        if (ws == null) {
            Node node = build.getBuiltOn();
            if (node == null) {
                throw new NullPointerException("no such build node: " + build.getBuiltOnStr());
            }
            throw new NullPointerException("no workspace from node " + node + " which is computer " + node.toComputer() + " and has channel " + node.getChannel());
        }
        FilePath script = null;
        int r = -1;
        try {
            try {
                script = createScriptFile(ws);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                Functions.printStackTrace(e, listener.fatalError(Messages.CommandInterpreter_UnableToProduceScript()));
                return false;
            }

            try {
                EnvVars envVars = build.getEnvironment(listener);
                // on Windows environment variables are converted to all upper case,
                // but no such conversions are done on Unix, so to make this cross-platform,
                // convert variables to all upper cases.
                for (Map.Entry<String, String> e : build.getBuildVariables().entrySet())
                    envVars.put(e.getKey(), e.getValue());

                launcher.prepareFilterRules(build, this);

                Launcher.ProcStarter procStarter = launcher.launch();
                procStarter.cmds(buildCommandLine(script))
                        .envs(envVars)
                        .stdout(listener)
                        .pwd(ws);

                try {
                    Proc proc = procStarter.start();
                    r = join(proc);
                } catch (EnvVarsFilterException se) {
                    LOGGER.log(Level.FINE, "Environment variable filtering failed", se);
                    return false;
                }

                if (isErrorlevelForUnstableBuild(r)) {
                    build.setResult(Result.UNSTABLE);
                    r = 0;
                }
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                Functions.printStackTrace(e, listener.fatalError(Messages.CommandInterpreter_CommandFailed()));
            }
            return r == 0;
        } finally {
            try {
                if (script != null)
                    script.delete();
            } catch (IOException e) {
                if (r == -1 && e.getCause() instanceof ChannelClosedException) {
                    // JENKINS-5073
                    // r==-1 only when the execution of the command resulted in IOException,
                    // and we've already reported that error. A common error there is channel
                    // losing a connection, and in that case we don't want to confuse users
                    // by reporting the 2nd problem. Technically the 1st exception may not be
                    // a channel closed error, but that's rare enough, and JENKINS-5073 is common enough
                    // that this suppressing of the error would be justified
                    LOGGER.log(Level.FINE, "Script deletion failed", e);
                } else {
                    Util.displayIOException(e, listener);
                    Functions.printStackTrace(e, listener.fatalError(Messages.CommandInterpreter_UnableToDelete(script)));
                }
            } catch (Exception e) {
                Functions.printStackTrace(e, listener.fatalError(Messages.CommandInterpreter_UnableToDelete(script)));
            }
        }
    }

    /**
     * Reports the exit code from the process.
     *
     * This allows subtypes to treat the exit code differently (for example by treating non-zero exit code
     * as if it's zero, or to set the status to {@link Result#UNSTABLE}). Any non-zero exit code will cause
     * the build step to fail. Use {@link #isErrorlevelForUnstableBuild(int exitCode)} to redefine the default
     * behaviour.
     *
     * @since 1.549
     */
    protected int join(Proc p) throws IOException, InterruptedException {
        return p.join();
    }

    /**
     * Creates a script file in a temporary name in the specified directory.
     */
    public FilePath createScriptFile(@NonNull FilePath dir) throws IOException, InterruptedException {
        return dir.createTextTempFile("jenkins", getFileExtension(), getContents(), false);
    }

    public abstract String[] buildCommandLine(FilePath script);

    protected abstract String getContents();

    protected abstract String getFileExtension();

    private static final Logger LOGGER = Logger.getLogger(CommandInterpreter.class.getName());
}
