/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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

package hudson.init;

import org.jvnet.hudson.reactor.Executable;
import org.jvnet.hudson.reactor.Milestone;
import org.jvnet.hudson.reactor.TaskBuilder;
import org.jvnet.hudson.reactor.TaskGraphBuilder;

/**
 * Various key milestone in the initialization process of Hudson.
 *
 * <p>
 * Plugins can use these milestones to execute their initialization at the right moment
 * (in addition to defining their own milestones by implementing {@link Milestone}.
 *
 * <p>
 * These milestones are achieve in this order:
 * <ol>
 *  <li>STARTED
 *  <li>PLUGINS_LISTED
 *  <li>PLUGINS_PREPARED
 *  <li>PLUGINS_STARTED
 *  <li>EXTENSIONS_AUGMENTED
 *  <li>SYSTEM_CONFIG_LOADED</li>
 *  <li>SYSTEM_CONFIG_ADAPTED</li>
 *  <li>JOB_LOADED
 *  <li>JOB_CONFIG_ADAPTED</li>
 *  <li>COMPLETED
 * </ol>
 *
 * @author Kohsuke Kawaguchi
 */
public enum InitMilestone implements Milestone {
    /**
     * The very first milestone that gets achieved without doing anything.
     *
     * This is used in {@link Initializer#after()} since annotations cannot have null as the default value.
     */
    STARTED("Started initialization"),

    /**
     * By this milestone, all plugins metadata are inspected and their dependencies figured out.
     */
    PLUGINS_LISTED("Listed all plugins"),

    /**
     * By this milestone, all plugin metadata are loaded and its classloader set up.
     */
    PLUGINS_PREPARED("Prepared all plugins"),

    /**
     * By this milestone, all plugins start executing, all extension points loaded, descriptors instantiated
     * and loaded.
     *
     * <p>
     * This is a separate milestone from {@link #PLUGINS_PREPARED} since the execution
     * of a plugin often involves finding extension point implementations, which in turn
     * require all the classes from all the plugins to be loadable.
     */
    PLUGINS_STARTED("Started all plugins"),

    /**
     * By this milestone, all programmatically constructed extension point implementations
     * should be added.
     */
    EXTENSIONS_AUGMENTED("Augmented all extensions"),

    /**
     * By this milestone, all the system configurations are loaded from file system
     * @since 2.220
     */
    SYSTEM_CONFIG_LOADED("System config loaded"),

    /**
     * By this milestone, the system configuration is adapted just in case any plugin (CasC might be an example) needs
     * to update configuration files
     * @since 2.220
     */
    SYSTEM_CONFIG_ADAPTED("System config adapted"),

    /**
     * By this milestone, all jobs and their build records are loaded from disk.
     */
    JOB_LOADED("Loaded all jobs"),

    /**
     * By this milestone, any job configuration is adapted or updated just in case any plugin needs to update former/old configurations.
     * It does not include {@link hudson.init.impl.GroovyInitScript}s which get executed later
     * @since 2.220
     */
    JOB_CONFIG_ADAPTED("Configuration for all jobs updated"),

    /**
     * The very last milestone.
     * All executions should be completed by it, including {@link hudson.init.impl.GroovyInitScript}s.
     * This is used in {@link Initializer#before()} since annotations cannot have null as the default value.
     */
    COMPLETED("Completed initialization");

    private final String message;

    InitMilestone(String message) {
        this.message = message;
    }

    /**
     * Creates a set of dummy tasks to enforce ordering among {@link InitMilestone}s.
     */
    public static TaskBuilder ordering() {
        TaskGraphBuilder b = new TaskGraphBuilder();
        InitMilestone[] v = values();
        for (int i = 0; i < v.length - 1; i++)
            b.add(null, Executable.NOOP).requires(v[i]).attains(v[i + 1]);
        return b;
    }


    @Override
    public String toString() {
        return message;
    }
}
