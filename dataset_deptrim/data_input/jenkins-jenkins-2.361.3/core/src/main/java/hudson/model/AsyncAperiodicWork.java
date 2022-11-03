/*
 * The MIT License
 *
 * Copyright (c) 2011, Vojtech Juranek
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

package hudson.model;

import hudson.Functions;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import jenkins.util.SystemProperties;

/**
 * {@link AperiodicWork} that takes a long time to run. Similar to {@link AsyncPeriodicWork}, see {@link AsyncPeriodicWork} for
 * details and {@link AperiodicWork} for differences between {@link AperiodicWork} and {@link PeriodicWork}.
 *
 * @author vjuranek
 * @since 1.410
 */
public abstract class AsyncAperiodicWork extends AperiodicWork {
    /**
     * The default number of minutes after which to try and rotate the log file used by {@link #createListener()}.
     * This value is controlled by the system property {@code hudson.model.AsyncAperiodicWork.logRotateMinutes}.
     * Each individual AsyncAperiodicWork can also have a per-extension override using the system property
     * based on their fully qualified class name with {@code .logRotateMinutes} appended.
     *
     * @since 1.651
     */
    private static final long LOG_ROTATE_MINUTES = SystemProperties.getLong(AsyncAperiodicWork.class.getName() +
            ".logRotateMinutes", TimeUnit.DAYS.toMinutes(1));
    /**
     * The default file size after which to try and rotate the log file used by {@link #createListener()}.
     * A value of {@code -1L} disables rotation based on file size.
     * This value is controlled by the system property {@code hudson.model.AsyncAperiodicWork.logRotateSize}.
     * Each individual AsyncAperiodicWork can also have a per-extension override using the system property
     * based on their fully qualified class name with {@code .logRotateSize} appended.
     *
     * @since 1.651
     */
    private static final long LOG_ROTATE_SIZE = SystemProperties.getLong(AsyncAperiodicWork.class.getName() + ".logRotateSize",
            -1L);
    /**
     * The number of milliseconds (since startup or previous rotation) after which to try and rotate the log file.
     *
     * @since 1.651
     */
    private final long logRotateMillis;
    /**
     * {@code -1L} disabled file size based log rotation, otherwise when starting an {@link #execute(TaskListener)},
     * if the log file size is above this number of bytes then the log file will be rotated beforehand.
     *
     * @since 1.651
     */
    private final long logRotateSize;
    /**
     * The last time the log files were rotated. On start-up this will be {@link Long#MIN_VALUE} to ensure that the
     * logs are always rotated every time Jenkins starts up to make it easier to correlate events with the main log.
     *
     * @since 1.651
     */
    private long lastRotateMillis = Long.MIN_VALUE;
    /**
     * Name of the work.
     */
    public final String name;

    private Thread thread;

    protected AsyncAperiodicWork(String name) {
        this.name = name;
        this.logRotateMillis = TimeUnit.MINUTES.toMillis(
                SystemProperties.getLong(getClass().getName() + ".logRotateMinutes", LOG_ROTATE_MINUTES));
        this.logRotateSize = SystemProperties.getLong(getClass().getName() + ".logRotateSize", LOG_ROTATE_SIZE);
    }

    /**
     * Schedules this periodic work now in a new thread, if one isn't already running.
     */
    @Override
    public final void doAperiodicRun() {
        try {
            if (thread != null && thread.isAlive()) {
                logger.log(getSlowLoggingLevel(), "{0} thread is still running. Execution aborted.", name);
                return;
            }
            thread = new Thread(() -> {
                logger.log(getNormalLoggingLevel(), "Started {0}", name);
                long startTime = System.currentTimeMillis();
                long stopTime;

                AsyncPeriodicWork.LazyTaskListener l = new AsyncPeriodicWork.LazyTaskListener(() -> createListener(), String.format("Started at %tc", new Date(startTime)));
                try (ACLContext ctx = ACL.as2(ACL.SYSTEM2)) {
                    execute(l);
                } catch (IOException e) {
                    Functions.printStackTrace(e, l.fatalError(e.getMessage()));
                } catch (InterruptedException e) {
                    Functions.printStackTrace(e, l.fatalError("aborted"));
                } finally {
                    stopTime = System.currentTimeMillis();
                    l.close(String.format("Finished at %tc. %dms", new Date(stopTime), stopTime - startTime));
                }

                logger.log(getNormalLoggingLevel(), "Finished {0}. {1,number} ms",
                        new Object[]{name, stopTime - startTime});
            }, name + " thread");
            thread.start();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, name + " thread failed with error", t);
        }
    }

    protected StreamTaskListener createListener() {
        File f = getLogFile();
        if (!f.getParentFile().isDirectory()) {
            if (!f.getParentFile().mkdirs()) {
                logger.log(getErrorLoggingLevel(), "Could not create directory {0}", f.getParentFile());
            }
        }
        if (f.isFile()) {
            if (lastRotateMillis + logRotateMillis < System.currentTimeMillis()
                    || (logRotateSize > 0 && f.length() > logRotateSize)) {
                lastRotateMillis = System.currentTimeMillis();
                File prev = null;
                for (int i = 5; i >= 0; i--) {
                    File curr = i == 0 ? f : new File(f.getParentFile(), f.getName() + "." + i);
                    if (curr.isFile()) {
                        if (prev != null && !prev.exists()) {
                            if (!curr.renameTo(prev)) {
                                logger.log(getErrorLoggingLevel(), "Could not rotate log files {0} to {1}",
                                        new Object[]{curr, prev});
                            }
                        } else {
                            if (!curr.delete()) {
                                logger.log(getErrorLoggingLevel(), "Could not delete log file {0} to enable rotation",
                                        curr);
                            }
                        }
                    }
                    prev = curr;
                }
            }
        } else {
            lastRotateMillis = System.currentTimeMillis();
            // migrate old log files the first time we start-up
            File oldFile = new File(Jenkins.get().getRootDir(), f.getName());
            if (oldFile.isFile()) {
                File newFile = new File(f.getParentFile(), f.getName() + ".1");
                if (!newFile.isFile()) {
                    // if there has never been rotation then this is the first time
                    if (oldFile.renameTo(newFile)) {
                        logger.log(getNormalLoggingLevel(), "Moved {0} to {1}", new Object[]{oldFile, newFile});
                    } else {
                        logger.log(getErrorLoggingLevel(), "Could not move {0} to {1}",
                                new Object[]{oldFile, newFile});
                    }
                }
            }
        }
        try {
            return new StreamTaskListener(f, true, null);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Determines the log file that records the result of this task.
     */
    protected File getLogFile() {
        return new File(getLogsRoot(), "/tasks/" + name + ".log");
    }

    /**
     * Returns the logging level at which normal messages are displayed.
     *
     * @return The logging level.
     * @since 1.651
     */
    protected Level getNormalLoggingLevel() {
        return Level.INFO;
    }

    /**
     * Returns the logging level at which previous task still executing messages is displayed.
     *
     * @return The logging level.
     * @since 1.651
     */
    protected Level getSlowLoggingLevel() {
        return getNormalLoggingLevel();
    }

    /**
     * Returns the logging level at which error messages are displayed.
     *
     * @return The logging level.
     * @since 1.651
     */
    protected Level getErrorLoggingLevel() {
        return Level.SEVERE;
    }

    /**
     * Executes the task.
     *
     * @param listener
     *      Output sent will be reported to the users. (this work is TBD.)
     * @throws InterruptedException
     *      The caller will record the exception and moves on.
     * @throws IOException
     *      The caller will record the exception and moves on.
     */
    protected abstract void execute(TaskListener listener) throws IOException, InterruptedException;

}
