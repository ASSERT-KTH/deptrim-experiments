/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Stephen Connolly
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

package hudson.slaves;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.model.identity.InstanceIdentityProvider;
import jenkins.slaves.RemotingWorkDirSettings;
import jenkins.util.SystemProperties;
import jenkins.websocket.WebSockets;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * {@link ComputerLauncher} via inbound connections.
 *
 * @author Stephen Connolly
 * @author Kohsuke Kawaguchi
*/
public class JNLPLauncher extends ComputerLauncher {
    /**
     * If the agent needs to tunnel the connection to the controller,
     * specify the "host:port" here. This can include the special
     * syntax "host:" and ":port" to indicate the default host/port
     * shall be used.
     *
     * <p>
     * Null if no tunneling is necessary.
     *
     * @since 1.250
     */
    @CheckForNull
    public final String tunnel;

    /**
     * @deprecated No longer used.
     */
    @Deprecated
    public final transient String vmargs = null;

    @NonNull
    private RemotingWorkDirSettings workDirSettings = RemotingWorkDirSettings.getEnabledDefaults();

    private boolean webSocket;

    /**
     * @see #getInboundAgentUrl()
     */
    @NonNull
    @Restricted(NoExternalUse.class)
    public static final String CUSTOM_INBOUND_URL_PROPERTY = "jenkins.agent.inboundUrl";

    /**
     * Constructor.
     * @param tunnel Tunnel settings
     * @param vmargs JVM arguments
     * @param workDirSettings Settings for Work Directory management in Remoting.
     *                        If {@code null}, {@link RemotingWorkDirSettings#getEnabledDefaults()}
     *                        will be used to enable work directories by default in new agents.
     * @since 2.68
     * @deprecated use {@link #JNLPLauncher(String, String)} and {@link #setWorkDirSettings(RemotingWorkDirSettings)}
     */
    @Deprecated
    public JNLPLauncher(@CheckForNull String tunnel, @CheckForNull String vmargs, @CheckForNull RemotingWorkDirSettings workDirSettings) {
        this(tunnel, vmargs);
        if (workDirSettings != null) {
            setWorkDirSettings(workDirSettings);
        }
    }

    // TODO cannot easily make tunnel into a @DataBoundSetter because then the @DataBoundConstructor would be on a no-arg constructor
    // which is already defined and deprecated. Could retroactively let no-arg constructor use default for workDirSettings,
    // which would be a behavioral change only for callers of the Java constructor (unlikely).
    @DataBoundConstructor
    public JNLPLauncher(@CheckForNull String tunnel) {
        this.tunnel = Util.fixEmptyAndTrim(tunnel);
    }

    /**
     * @deprecated use {@link JNLPLauncher#JNLPLauncher(String)}
     */
    @Deprecated
    public JNLPLauncher(@CheckForNull String tunnel, @CheckForNull String vmargs) {
        this.tunnel = Util.fixEmptyAndTrim(tunnel);
    }

    /**
     * @deprecated This Launcher does not enable the work directory.
     *             It is recommended to use {@link #JNLPLauncher(boolean)}
     */
    @Deprecated
    public JNLPLauncher() {
        this(false);
    }

    /**
     * Constructor with default options.
     *
     * @param enableWorkDir If {@code true}, the work directory will be enabled with default settings.
     */
    public JNLPLauncher(boolean enableWorkDir) {
        this(null, null, enableWorkDir
                ? RemotingWorkDirSettings.getEnabledDefaults()
                : RemotingWorkDirSettings.getDisabledDefaults());
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "workDirSettings in readResolve is needed for data migration.")
    protected Object readResolve() {
        if (workDirSettings == null) {
            // For the migrated code agents are always disabled
            workDirSettings = RemotingWorkDirSettings.getDisabledDefaults();
        }
        return this;
    }

    /**
     * Returns work directory settings.
     *
     * @since 2.72
     */
    @NonNull
    public RemotingWorkDirSettings getWorkDirSettings() {
        return workDirSettings;
    }

    @DataBoundSetter
    public final void setWorkDirSettings(@NonNull RemotingWorkDirSettings workDirSettings) {
        this.workDirSettings = workDirSettings;
    }

    @Override
    public boolean isLaunchSupported() {
        return false;
    }

    /**
     * @since 2.216
     */
    public boolean isWebSocket() {
        return webSocket;
    }

    /**
     * @since 2.216
     */
    @DataBoundSetter
    public void setWebSocket(boolean webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) {
        // do nothing as we cannot self start
    }

    /**
     * @deprecated as of 1.XXX
     *      Use {@link Jenkins#getDescriptor(Class)}
     */
    @Deprecated
    @Restricted(NoExternalUse.class)
    public static /*almost final*/ Descriptor<ComputerLauncher> DESCRIPTOR;

    /**
     * Gets work directory options as a String.
     *
     * In public API {@code getWorkDirSettings().toCommandLineArgs(computer)} should be used instead
     * @param computer Computer
     * @return Command line options for launching with the WorkDir
     */
    @NonNull
    @Restricted(NoExternalUse.class)
    public String getWorkDirOptions(@NonNull Computer computer) {
        if (!(computer instanceof SlaveComputer)) {
            return "";
        }
        return workDirSettings.toCommandLineString((SlaveComputer) computer);
    }

    @Extension @Symbol("jnlp")
    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {
        @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "for backward compatibility")
        public DescriptorImpl() {
            DESCRIPTOR = this;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.JNLPLauncher_displayName();
        }

        /**
         * Checks if Work Dir settings should be displayed.
         *
         * This flag is checked in {@code config.jelly} before displaying the
         * {@link JNLPLauncher#workDirSettings} property.
         * By default the configuration is displayed only for {@link JNLPLauncher},
         * but the implementation can be overridden.
         * @return {@code true} if work directories are supported by the launcher type.
         * @since 2.73
         */
        public boolean isWorkDirSupported() {
            // This property is included only for JNLPLauncher by default.
            // Causes JENKINS-45895 in the case of includes otherwise
            return DescriptorImpl.class.equals(getClass());
        }

        public FormValidation doCheckWebSocket(@QueryParameter boolean webSocket, @QueryParameter String tunnel) {
            if (webSocket) {
                if (!WebSockets.isSupported()) {
                    return FormValidation.error("WebSocket support is not enabled in this Jenkins installation");
                }
                if (Util.fixEmptyAndTrim(tunnel) != null) {
                    return FormValidation.error("Tunneling is not supported in WebSocket mode");
                }
            } else {
                if (Jenkins.get().getTcpSlaveAgentListener() == null) {
                    return FormValidation.error("Either WebSocket mode is selected, or the TCP port for inbound agents must be enabled");
                }
                if (InstanceIdentityProvider.RSA.getCertificate() == null || InstanceIdentityProvider.RSA.getPrivateKey() == null) {
                    return FormValidation.error("You must install the instance-identity plugin to use inbound agents in TCP mode");
                }
            }
            return FormValidation.ok();
        }

    }

    /**
     * Overrides the url that inbound TCP agents should connect to
     * as advertised in the agent.jnlp file. If not set, the default
     * behavior is unchanged and returns the root URL.
     *
     * This enables using a private address for inbound tcp agents,
     * separate from Jenkins root URL.
     *
     * @see <a href="https://issues.jenkins.io/browse/JENKINS-63222">JENKINS-63222</a>
     */
    @Restricted(NoExternalUse.class)
    public static String getInboundAgentUrl() {
        String url = SystemProperties.getString(CUSTOM_INBOUND_URL_PROPERTY);
        if (url == null || url.isEmpty()) {
            return Jenkins.get().getRootUrl();
        }
        return url;
    }
}
