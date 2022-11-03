package jenkins.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.remoting.AtmostOneThreadExecutor;
import hudson.security.ACL;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.ImpersonatingExecutorService;

/**
 * {@link Executor}-like class that executes a single task repeatedly, in such a way that a single execution
 * can cover multiple pending queued requests.
 *
 * <p>
 * This is akin to doing laundry &mdash; when you put a dirty cloth into the laundry box, you mentally "schedule"
 * a laundry task, regardless of whether there already is some cloths in the box or not. When you later actually get around
 * doing laundry, you wash all the dirty cloths in the box, not just your cloths. And if someone brings
 * more dirty cloths while a washer and dryer are in operation, the person has to mentally "schedule" the task
 * and run the machines another time later, as the current batch is already in progress.
 *
 * <p>
 * Since this class collapses multiple submitted tasks into just one run, it only makes sense when everyone
 * submits the same task. Thus {@link #submit()} method does not take {@link Callable} as a parameter,
 * instead you pass that in the constructor.
 *
 *
 * <h2>Implementation</h2>
 * <p>
 * This instance has two independent states. One is {@link #pending}, which indicates that
 * the task execution is requested but not yet scheduled. The other is {@link #inprogress},
 * which indicates that the task execution is scheduled but not yet completed.
 *
 * <p>
 * All the internal state transition is guarded by the monitor of 'this'. {@link #pending}
 * is non-null only if {@link #inprogress} is non-null.
 *
 * @author Kohsuke Kawaguchi
 * @see AtmostOneThreadExecutor
 */
@SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", justification = "TODO needs triage")
public class AtmostOneTaskExecutor<V> {

    private static final Logger LOGGER = Logger.getLogger(AtmostOneTaskExecutor.class.getName());

    /**
     * The actual executor that executes {@link #task}
     */
    private final ExecutorService base;

    /**
     * Task to be executed.
     */
    private final Callable<V> task;

    /**
     * If a task is already submitted and pending execution, non-null.
     * Guarded by "synchronized(this)"
     */
    private CompletableFuture<V> pending;

    private CompletableFuture<V> inprogress;

    public AtmostOneTaskExecutor(ExecutorService base, Callable<V> task) {
        this.base = base;
        this.task = task;
    }

    public AtmostOneTaskExecutor(Callable<V> task) {
        this(new ImpersonatingExecutorService(new AtmostOneThreadExecutor(new NamingThreadFactory(
                        new DaemonThreadFactory(),
                        String.format("AtmostOneTaskExecutor[%s]", task)
                )), ACL.SYSTEM2),
                task
        );
    }

    public synchronized Future<V> submit() {
        if (pending == null) {
            pending = new CompletableFuture<>();
            maybeRun();
        }
        return pending;
    }

    /**
     * If {@link #pending} is non-null (meaning someone requested the task to be kicked),
     * but {@link #inprogress} is null (meaning none is executing right now),
     * get one going.
     */
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", justification = "method signature does not permit plumbing through the return value")
    private synchronized void maybeRun() {
        if (inprogress == null && pending != null) {
            base.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    synchronized (AtmostOneTaskExecutor.this) {
                        // everyone who submits after this should form a next batch
                        inprogress = pending;
                        pending = null;
                    }

                    try {
                        inprogress.complete(task.call());
                    } catch (Throwable t) {
                        LOGGER.log(Level.WARNING, null, t);
                        inprogress.completeExceptionally(t);
                    } finally {
                        synchronized (AtmostOneTaskExecutor.this) {
                            // if next one is pending, get that scheduled
                            inprogress = null;
                            maybeRun();
                        }
                    }
                    return null;
                }
            });
        }
    }
}
