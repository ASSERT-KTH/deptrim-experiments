package jenkins;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.TcpSlaveAgentListener;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import jenkins.model.Jenkins;

/**
 * Pluggable Jenkins TCP agent protocol handler called from {@link TcpSlaveAgentListener}.
 *
 * <p>
 * To register your extension, put {@link Extension} annotation on your subtype.
 * Implementations of this extension point is singleton, and its {@link #handle(Socket)} method
 * gets invoked concurrently whenever a new connection comes in.
 *
 * <h2>Extending UI</h2>
 * <dl>
 *  <dt>description.jelly</dt>
 *  <dd>Optional protocol description</dd>
 *  <dt>deprecationCause.jelly</dt>
 *  <dd>Optional. If the protocol is marked as {@link #isDeprecated()},
 *      clarifies the deprecation reason and provides extra documentation links</dd>
 * </dl>
 *
 * @author Kohsuke Kawaguchi
 * @since 1.467
 * @see TcpSlaveAgentListener
 */
public abstract class AgentProtocol implements ExtensionPoint {
    /**
     * Allow experimental {@link AgentProtocol} implementations to declare being opt-in.
     * Note that {@link Jenkins#setAgentProtocols(Set)} only records the protocols where the admin has made a
     * conscious decision thus:
     * <ul>
     *     <li>if a protocol is opt-in, it records the admin enabling it</li>
     *     <li>if a protocol is opt-out, it records the admin disabling it</li>
     * </ul>
     * Implementations should not transition rapidly from {@code opt-in -> opt-out -> opt-in}.
     * Implementations should never flip-flop: {@code opt-in -> opt-out -> opt-in -> opt-out} as that will basically
     * clear any preference that an admin has set. This latter restriction should be ok as we only ever will be
     * adding new protocols and retiring old ones.
     *
     * @return {@code true} if the protocol requires explicit opt-in.
     * @since 2.16
     * @see Jenkins#setAgentProtocols(Set)
     */
    public boolean isOptIn() {
        return false;
    }
    /**
     * Allow essential {@link AgentProtocol} implementations (basically {@link TcpSlaveAgentListener.PingAgentProtocol})
     * to be always enabled.
     *
     * @return {@code true} if the protocol can never be disabled.
     * @since 2.16
     */

    public boolean isRequired() {
        return false;
    }

    /**
     * Checks if the protocol is deprecated.
     *
     * @since 2.75
     */
    public boolean isDeprecated() {
        return false;
    }

    /**
     * Protocol name.
     *
     * This is a short string that consists of printable ASCII chars. Sent by the client to select the protocol.
     *
     * @return
     *      null to be disabled. This is useful for avoiding getting used
     *      until the protocol is properly configured.
     */
    public abstract String getName();

    /**
     * Returns the human readable protocol display name.
     *
     * @return the human readable protocol display name.
     * @since 2.16
     */
    public String getDisplayName() {
        return getName();
    }

    /**
     * Called by the connection handling thread to execute the protocol.
     */
    public abstract void handle(Socket socket) throws IOException, InterruptedException;

    /**
     * Returns all the registered {@link AgentProtocol}s.
     */
    public static ExtensionList<AgentProtocol> all() {
        return ExtensionList.lookup(AgentProtocol.class);
    }

    @CheckForNull
    public static AgentProtocol of(String protocolName) {
        for (AgentProtocol p : all()) {
            String n = p.getName();
            if (n != null && n.equals(protocolName))
                return p;
        }
        return null;
    }
}
