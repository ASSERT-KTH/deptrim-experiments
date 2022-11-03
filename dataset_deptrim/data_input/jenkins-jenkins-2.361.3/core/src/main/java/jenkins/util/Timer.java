package jenkins.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.security.ACL;
import hudson.util.ClassLoaderSanityThreadFactory;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import java.util.concurrent.ScheduledExecutorService;
import jenkins.security.ImpersonatingScheduledExecutorService;

/**
 * Holds the {@link ScheduledExecutorService} for running all background tasks in Jenkins.
 * This ExecutorService will create additional threads to execute due (enabled) tasks.
 *
 * Provides a minimal abstraction for locating the ScheduledExecutorService so that we
 * can modify it's behavior going forward. For instance, to add manageability/monitoring.
 *
 * This is not an @Extension because it must be available before any extensions are loaded.
 *
 * Plugins should probably use one of the following as they provide higher level abstractions:
 *   {@link hudson.model.AperiodicWork}, {@link hudson.model.PeriodicWork},
 *   {@link hudson.model.AsyncAperiodicWork}, {@link hudson.model.AsyncPeriodicWork}.
 *
 * @author Ryan Campbell
 * @since 1.541
 */
public class Timer {

    /**
     * The scheduled executor thread pool. This is initialized lazily since it may be created/shutdown many times
     * when running the test suite.
     */
    static ScheduledExecutorService executorService;


    /**
     * Returns the scheduled executor service used by all timed tasks in Jenkins.
     *
     * @return the single {@link ScheduledExecutorService}.
     */
    @NonNull
    public static synchronized ScheduledExecutorService get() {
        if (executorService == null) {
            // corePoolSize is set to 10, but will only be created if needed.
            // ScheduledThreadPoolExecutor "acts as a fixed-sized pool using corePoolSize threads"
            // TODO consider also wrapping in ContextResettingExecutorService
            executorService = new ImpersonatingScheduledExecutorService(
                    new ErrorLoggingScheduledThreadPoolExecutor(10, new NamingThreadFactory(new ClassLoaderSanityThreadFactory(new DaemonThreadFactory()), "jenkins.util.Timer")),
                    ACL.SYSTEM2);
        }
        return executorService;
    }

    /**
     * Shutdown the timer and throw it away.
     */
    public static synchronized void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    /**
     * Do not create this.
     */
    private Timer() {}

}
