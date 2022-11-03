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

package hudson.slaves;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.Computer;
import hudson.model.DirectoryBrowserSupport;
import java.io.Closeable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.SystemProperties;

/**
 * Used by {@link Computer} to keep track of workspaces that are actively in use.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.319
 * @see Computer#getWorkspaceList()
 */
public final class WorkspaceList {
    private static final class AllocationAt extends Exception {
        @Override
        public String toString() {
            return "Allocation Point";
        }
    }
    /**
     * Book keeping for workspace allocation.
     */

    public static final class Entry {
        /**
         * Who acquired this workspace?
         */
        public final Thread holder = Thread.currentThread();

        /**
         * When?
         */
        public final long time = System.currentTimeMillis();

        /**
         * From where?
         */
        public final Exception source = new AllocationAt();

        /**
         * True makes the caller of {@link WorkspaceList#allocate(FilePath)} wait
         * for this workspace.
         */
        public final boolean quick;

        public final @NonNull FilePath path;

        /**
         * Multiple threads can acquire the same lock if they share the same context object.
         */
        public final Object context;

        public int lockCount = 1;

        private Entry(@NonNull FilePath path, boolean quick) {
            this(path, quick, new Object()); // unique context
        }

        private Entry(@NonNull FilePath path, boolean quick, Object context) {
            this.path = path;
            this.quick = quick;
            this.context = context;
        }

        @Override
        public String toString() {
            String s = path + " owned by " + holder.getName() + " from " + new Date(time);
            if (quick) s += " (quick)";
            s += "\n" + Functions.printThrowable(source);
            return s;
        }
    }

    /**
     * Represents a leased workspace that needs to be returned later.
     */
    public abstract static class Lease implements /*Auto*/Closeable {
        public final @NonNull FilePath path;

        protected Lease(@NonNull FilePath path) {
            if (path == null) { // Protection from old API
                throw new NullPointerException("The specified FilePath is null");
            }
            this.path = path;
        }

        /**
         * Releases this lease.
         */
        public abstract void release();

        /**
         * By default, calls {@link #release}, but should be idempotent.
         * @since 1.600
         */
        @Override public void close() {
            release();
        }

        /**
         * Creates a dummy {@link Lease} object that does no-op in the release.
         */
        public static Lease createDummyLease(@NonNull FilePath p) {
            return new Lease(p) {
                @Override
                public void release() {
                    // noop
                }
            };
        }

        /**
         * Creates a {@link Lease} object that points  to the specified path, but the lock
         * is controlled by the given parent lease object.
         */
        public static Lease createLinkedDummyLease(@NonNull FilePath p, final Lease parent) {
            return new Lease(p) {
                @Override
                public void release() {
                    parent.release();
                }
            };
        }
    }

    private final Map<String, Entry> inUse = new HashMap<>();

    public WorkspaceList() {
    }

    /**
     * Allocates a workspace by adding some variation to the given base to make it unique.
     *
     * <p>
     * This method doesn't block prolonged amount of time. Whenever a desired workspace
     * is in use, the unique variation is added.
     */
    public synchronized Lease allocate(@NonNull FilePath base) throws InterruptedException {
        return allocate(base, new Object());
    }

    /**
     * See {@link #allocate(FilePath)}
     *
     * @param context
     *      Threads that share the same context can re-acquire the same lock (which will just increment the lock count.)
     *      This allows related executors to share the same workspace.
     */
    public synchronized Lease allocate(@NonNull FilePath base, Object context) throws InterruptedException {
        for (int i = 1; ; i++) {
            FilePath candidate = i == 1 ? base : base.withSuffix(COMBINATOR + i);
            Entry e = inUse.get(candidate.getRemote());
            if (e != null && !e.quick && e.context != context)
                continue;
            return acquire(candidate, false, context);
        }
    }

    /**
     * Just record that this workspace is being used, without paying any attention to the synchronization support.
     */
    public synchronized Lease record(@NonNull FilePath p) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "recorded " + p, new Throwable("from " + this));
        }
        Entry old = inUse.put(p.getRemote(), new Entry(p, false));
        if (old != null)
            throw new AssertionError("Tried to record a workspace already owned: " + old);
        return lease(p);
    }

    /**
     * Releases an allocated or acquired workspace.
     */
    private synchronized void _release(@NonNull FilePath p) {
        Entry old = inUse.get(p.getRemote());
        if (old == null)
            throw new AssertionError("Releasing unallocated workspace " + p);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "releasing " + p + " with lock count " + old.lockCount, new Throwable("from " + this));
        }
        old.lockCount--;
        if (old.lockCount == 0)
            inUse.remove(p.getRemote());
        notifyAll();
    }

    /**
     * Acquires the given workspace. If necessary, this method blocks until it's made available.
     *
     * @return
     *      The same {@link FilePath} as given to this method.
     */
    public synchronized Lease acquire(@NonNull FilePath p) throws InterruptedException {
        return acquire(p, false);
    }

    /**
     * See {@link #acquire(FilePath)}
     *
     * @param quick
     *      If true, indicates that the acquired workspace will be returned quickly.
     *      This makes other calls to {@link #allocate(FilePath)} to wait for the release of this workspace.
     */
    public synchronized Lease acquire(@NonNull FilePath p, boolean quick) throws InterruptedException {
        return acquire(p, quick, new Object());
    }

    /**
     * See {@link #acquire(FilePath,boolean)}
     *
     * @param context
     *      Threads that share the same context can re-acquire the same lock (which will just increment the lock count.)
     *      This allows related executors to share the same workspace.
     */
    public synchronized Lease acquire(@NonNull FilePath p, boolean quick, Object context) throws InterruptedException {
        Entry e;

        Thread t = Thread.currentThread();
        String oldName = t.getName();
        t.setName("Waiting to acquire " + p + " : " + t.getName());
        try {
            while (true) {
                e = inUse.get(p.getRemote());
                if (e == null || e.context == context)
                    break;
                wait();
            }
        } finally {
            t.setName(oldName);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "acquired " + p + (e == null ? "" : " with lock count " + e.lockCount), new Throwable("from " + this));
        }

        if (e != null)    e.lockCount++;
        else            inUse.put(p.getRemote(), new Entry(p, quick, context));
        return lease(p);
    }

    /**
     * Wraps a path into a valid lease.
     */
    private Lease lease(@NonNull FilePath p) {
        return new Lease(p) {
            final AtomicBoolean released = new AtomicBoolean();
            @Override
            public void release() {
                _release(path);
            }

            @Override public void close() {
                if (released.compareAndSet(false, true)) {
                    release();
                }
            }
        };
    }

    /**
     * Locates a conventional temporary directory to be associated with a workspace.
     * <p>This directory is suitable for temporary files to be deleted later in the course of a build,
     * or caches and local repositories which should persist across builds done in the same workspace.
     * (If multiple workspaces are present for a single job built concurrently, via {@link #allocate(FilePath)}, each will get its own temporary directory.)
     * <p>It may also be used for security-sensitive files which {@link DirectoryBrowserSupport} ought not serve,
     * acknowledging that these will be readable by builds of other jobs done on the same node.
     * <p>Each plugin using this directory is responsible for specifying sufficiently unique subdirectory/file names.
     * {@link FilePath#createTempFile} may be used for this purpose if desired.
     * <p>The resulting directory may not exist; you may call {@link FilePath#mkdirs()} on it if you need it to.
     * It may be deleted alongside the workspace itself during cleanup actions.
     * @param ws a directory such as a build workspace
     * @return a sibling directory, for example {@code …/something@tmp} for {@code …/something}, or {@code null} if {@link FilePath#getParent} is null
     * @since 1.652
     */
    @CheckForNull
    public static FilePath tempDir(FilePath ws) {
        return ws.sibling(ws.getName() + COMBINATOR + "tmp");
    }

    private static final Logger LOGGER = Logger.getLogger(WorkspaceList.class.getName());

    /**
     * The token that combines the project name and unique number to create unique workspace directory.
     * @since 2.244
     */
    public static final String COMBINATOR = SystemProperties.getString(WorkspaceList.class.getName(), "@");
}
