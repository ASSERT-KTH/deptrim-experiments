/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc., Stephen Connolly
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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.BulkChange;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.slaves.EphemeralNode;
import hudson.slaves.OfflineCause;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.SystemProperties;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Manages all the nodes for Jenkins.
 *
 * @since 1.607
 */
@Restricted(NoExternalUse.class) // for now, we may make it public later
@SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", justification = "TODO needs triage")
public class Nodes implements Saveable {

    /**
     * Determine if we need to enforce the name restrictions during node creation or replacement.
     * Should be enabled (default) to prevent SECURITY-2021.
     */
    @Restricted(NoExternalUse.class)
    private static final boolean ENFORCE_NAME_RESTRICTIONS = SystemProperties.getBoolean(Nodes.class.getName() + ".enforceNameRestrictions", true);

    /**
     * The {@link Jenkins} instance that we are tracking nodes for.
     */
    @NonNull
    private final Jenkins jenkins;

    /**
     * The map of nodes.
     */
    private final ConcurrentMap<String, Node> nodes = new ConcurrentSkipListMap<>();

    /**
     * Constructor, intended to be called only from {@link Jenkins}.
     *
     * @param jenkins A reference to the {@link Jenkins} that this instance is tracking nodes for, beware not to
     *                let this reference escape from a partially constructed {@link Nodes} as when we are passed the
     *                reference the {@link Jenkins} instance has not completed instantiation.
     */
    /*package*/ Nodes(@NonNull Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    /**
     * Returns the list of nodes.
     *
     * @return the list of nodes.
     */
    @NonNull
    public List<Node> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    /**
     * Sets the list of nodes.
     *
     * @param nodes the new list of nodes.
     * @throws IOException if the new list of nodes could not be persisted.
     */
    public void setNodes(final @NonNull Collection<? extends Node> nodes) throws IOException {
        Queue.withLock(new Runnable() {
            @Override
            public void run() {
                Set<String> toRemove = new HashSet<>(Nodes.this.nodes.keySet());
                for (Node n : nodes) {
                    final String name = n.getNodeName();
                    toRemove.remove(name);
                    Nodes.this.nodes.put(name, n);
                }
                Nodes.this.nodes.keySet().removeAll(toRemove); // directory clean up will be handled by save
                jenkins.updateComputerList();
                jenkins.trimLabels();
            }
        });
        save();
    }

    /**
     * Adds a node. If a node of the same name already exists then that node will be replaced.
     *
     * @param node the new node.
     * @throws IOException if the list of nodes could not be persisted.
     */
    public void addNode(final @NonNull Node node) throws IOException {
        if (ENFORCE_NAME_RESTRICTIONS) {
            Jenkins.checkGoodName(node.getNodeName());
        }

        Node oldNode = nodes.get(node.getNodeName());
        if (node != oldNode) {
            AtomicReference<Node> old = new AtomicReference<>();
            old.set(nodes.put(node.getNodeName(), node));
            jenkins.updateNewComputer(node);
            jenkins.trimLabels(node, oldNode);
            // TODO there is a theoretical race whereby the node instance is updated/removed after lock release
            try {
                persistNode(node);
            } catch (IOException | RuntimeException e) {
                // JENKINS-50599: If persisting the node throws an exception, we need to remove the node from
                // memory before propagating the exception.
                Queue.withLock(new Runnable() {
                    @Override
                    public void run() {
                        nodes.compute(node.getNodeName(), (ignoredNodeName, ignoredNode) -> oldNode);
                        jenkins.updateComputerList();
                        jenkins.trimLabels(node, oldNode);
                    }
                });
                throw e;
            }
            if (old.get() != null) {
                NodeListener.fireOnUpdated(old.get(), node);
            } else {
                NodeListener.fireOnCreated(node);
            }
        }
    }

    /**
     * Actually persists a node on disk.
     *
     * @param node the node to be persisted.
     * @throws IOException if the node could not be persisted.
     */
    private void persistNode(final @NonNull Node node)  throws IOException {
        // no need for a full save() so we just do the minimum
        if (node instanceof EphemeralNode) {
            Util.deleteRecursive(new File(getNodesDir(), node.getNodeName()));
        } else {
            XmlFile xmlFile = new XmlFile(Jenkins.XSTREAM,
                    new File(new File(getNodesDir(), node.getNodeName()), "config.xml"));
            xmlFile.write(node);
            SaveableListener.fireOnChange(this, xmlFile);
        }
        jenkins.getQueue().scheduleMaintenance();
    }

    /**
     * Updates an existing node on disk. If the node instance is not in the list of nodes, then this
     * will be a no-op, even if there is another instance with the same {@link Node#getNodeName()}.
     *
     * @param node the node to be updated.
     * @return {@code true}, if the node was updated. {@code false}, if the node was not in the list of nodes.
     * @throws IOException if the node could not be persisted.
     * @since 1.634
     */
    public boolean updateNode(final @NonNull Node node) throws IOException {
        boolean exists;
        try {
            exists = Queue.withLock(new Callable<>() {
                @Override
                public Boolean call() throws Exception {
                    if (node == nodes.get(node.getNodeName())) {
                        jenkins.trimLabels(node);
                        return true;
                    }
                    return false;
                }
            });
        } catch (RuntimeException e) {
            // should never happen, but if it does let's do the right thing
            throw e;
        } catch (Exception e) {
            // can never happen
            exists = false;
        }
        if (exists) {
            // TODO there is a theoretical race whereby the node instance is updated/removed after lock release
            persistNode(node);
            // TODO should this fireOnUpdated?
            return true;
        }
        return false;
    }

    /**
     * Replace node of given name.
     *
     * @return {@code true} if node was replaced.
     * @since 2.8
     */
    public boolean replaceNode(final Node oldOne, final @NonNull Node newOne) throws IOException {
        if (ENFORCE_NAME_RESTRICTIONS) {
            Jenkins.checkGoodName(newOne.getNodeName());
        }

        if (oldOne == nodes.get(oldOne.getNodeName())) {
            // use the queue lock until Nodes has a way of directly modifying a single node.
            Queue.withLock(new Runnable() {
                @Override
                public void run() {
                    Nodes.this.nodes.remove(oldOne.getNodeName());
                    Nodes.this.nodes.put(newOne.getNodeName(), newOne);
                    jenkins.updateComputerList();
                    jenkins.trimLabels(oldOne, newOne);
                }
            });
            updateNode(newOne);
            if (!newOne.getNodeName().equals(oldOne.getNodeName())) {
                Util.deleteRecursive(new File(getNodesDir(), oldOne.getNodeName()));
            }
            NodeListener.fireOnUpdated(oldOne, newOne);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes a node. If the node instance is not in the list of nodes, then this will be a no-op, even if
     * there is another instance with the same {@link Node#getNodeName()}.
     *
     * @param node the node instance to remove.
     * @throws IOException if the list of nodes could not be persisted.
     */
    public void removeNode(final @NonNull Node node) throws IOException {
        if (node == nodes.get(node.getNodeName())) {
            Queue.withLock(new Runnable() {
                @Override
                public void run() {
                    Computer c = node.toComputer();
                    if (c != null) {
                        c.recordTermination();
                        c.disconnect(OfflineCause.create(hudson.model.Messages._Hudson_NodeBeingRemoved()));
                    }
                    if (node == nodes.remove(node.getNodeName())) {
                        jenkins.updateComputerList();
                        jenkins.trimLabels(node);
                    }
                }
            });
            // no need for a full save() so we just do the minimum
            Util.deleteRecursive(new File(getNodesDir(), node.getNodeName()));

            NodeListener.fireOnDeleted(node);
        }
    }

    @Override
    public void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }
        final File nodesDir = getNodesDir();
        final Set<String> existing = new HashSet<>();
        for (Node n : nodes.values()) {
            if (n instanceof EphemeralNode) {
                continue;
            }
            existing.add(n.getNodeName());
            XmlFile xmlFile = new XmlFile(Jenkins.XSTREAM, new File(new File(nodesDir, n.getNodeName()), "config.xml"));
            xmlFile.write(n);
            SaveableListener.fireOnChange(this, xmlFile);
        }
        for (File forDeletion : nodesDir.listFiles(pathname ->
                pathname.isDirectory() && !existing.contains(pathname.getName()))) {
            Util.deleteRecursive(forDeletion);
        }
    }

    /**
     * Returns the named node.
     *
     * @param name the {@link Node#getNodeName()} of the node to retrieve.
     * @return the {@link Node} or {@code null} if the node could not be found.
     */
    @CheckForNull
    public Node getNode(String name) {
        return name == null ? null : nodes.get(name);
    }

    /**
     * Loads the nodes from disk.
     *
     * @throws IOException if the nodes could not be deserialized.
     */
    public void load() throws IOException {
        final File nodesDir = getNodesDir();
        final File[] subdirs = nodesDir.listFiles(File::isDirectory);
        final Map<String, Node> newNodes = new TreeMap<>();
        if (subdirs != null) {
            for (File subdir : subdirs) {
                try {
                    XmlFile xmlFile = new XmlFile(Jenkins.XSTREAM, new File(subdir, "config.xml"));
                    if (xmlFile.exists()) {
                        Node node = (Node) xmlFile.read();
                        newNodes.put(node.getNodeName(), node);
                    }
                } catch (IOException e) {
                    Logger.getLogger(Nodes.class.getName()).log(Level.WARNING, "could not load " + subdir, e);
                }
            }
        }
        Queue.withLock(new Runnable() {
            @Override
            public void run() {
                nodes.entrySet().removeIf(stringNodeEntry -> !(stringNodeEntry.getValue() instanceof EphemeralNode));
                nodes.putAll(newNodes);
                jenkins.updateComputerList();
                jenkins.trimLabels();
            }
        });
    }

    /**
     * Returns the directory that the nodes are stored in.
     *
     * @return the directory that the nodes are stored in.
     */
    private File getNodesDir() throws IOException {
        final File nodesDir = new File(jenkins.getRootDir(), "nodes");
        if (!nodesDir.isDirectory() && !nodesDir.mkdirs()) {
            throw new IOException(String.format("Could not mkdirs %s", nodesDir));
        }
        return nodesDir;
    }

    /**
     * Returns {@code true} if and only if the list of nodes is stored in the legacy location.
     *
     * @return {@code true} if and only if the list of nodes is stored in the legacy location.
     */
    public boolean isLegacy() {
        return !new File(jenkins.getRootDir(), "nodes").isDirectory();
    }
}
