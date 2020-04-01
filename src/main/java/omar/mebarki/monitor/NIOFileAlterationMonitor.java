package omar.mebarki.monitor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

/**
 * A runnable that spawns a monitoring thread triggering any
 * registered {@link NIOFileAlterationObserver} at a specified interval.
 *
 * @version $Id$
 * @see NIOFileAlterationObserver
 * @since 2.0
 */
public final class NIOFileAlterationMonitor implements Runnable {

    private final long interval;
    private final List<NIOFileAlterationObserver> observers = new CopyOnWriteArrayList<>();
    private Thread thread = null;
    private ThreadFactory threadFactory;
    private volatile boolean running = false;

    /**
     * Construct a monitor with a default interval of 10 seconds.
     */
    public NIOFileAlterationMonitor() {
        this(10000);
    }

    /**
     * Construct a monitor with the specified interval.
     *
     * @param interval The amount of time in milliseconds to wait between
     *                 checks of the file system
     */
    public NIOFileAlterationMonitor(final long interval) {
        this.interval = interval;
    }

    /**
     * Construct a monitor with the specified interval and set of observers.
     *
     * @param interval  The amount of time in milliseconds to wait between
     *                  checks of the file system
     * @param observers The set of observers to add to the monitor.
     */
    public NIOFileAlterationMonitor(final long interval, final NIOFileAlterationObserver... observers) {
        this(interval);
        if (observers != null) {
            for (final NIOFileAlterationObserver observer : observers) {
                addObserver(observer);
            }
        }
    }

    /**
     * Return the interval.
     *
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Set the thread factory.
     *
     * @param threadFactory the thread factory
     */
    public synchronized void setThreadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Add a file system observer to this monitor.
     *
     * @param observer The file system observer to add
     */
    public void addObserver(final NIOFileAlterationObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /**
     * Remove a file system observer from this monitor.
     *
     * @param observer The file system observer to remove
     */
    public void removeObserver(final NIOFileAlterationObserver observer) {
        if (observer != null) {
            while (observers.remove(observer)) {
            }
        }
    }

    /**
     * Returns the set of {@link NIOFileAlterationObserver} registered with
     * this monitor.
     *
     * @return The set of {@link NIOFileAlterationObserver}
     */
    public Iterable<NIOFileAlterationObserver> getObservers() {
        return observers;
    }

    /**
     * Start monitoring.
     *
     * @throws Exception if an error occurs initializing the observer
     */
    public synchronized void start() throws Exception {
        if (running) {
            throw new IllegalStateException("Monitor is already running");
        }
        for (final NIOFileAlterationObserver observer : observers) {
            observer.initialize();
        }
        running = true;
        if (threadFactory != null) {
            thread = threadFactory.newThread(this);
        } else {
            thread = new Thread(this);
        }
        thread.start();
    }

    /**
     * Stop monitoring.
     *
     * @throws Exception if an error occurs initializing the observer
     */
    public synchronized void stop() throws Exception {
        stop(interval);
    }

    /**
     * Stop monitoring.
     *
     * @param stopInterval the amount of time in milliseconds to wait for the thread to finish.
     *                     A value of zero will wait until the thread is finished (see {@link Thread#join(long)}).
     * @throws Exception if an error occurs initializing the observer
     * @since 2.1
     */
    public synchronized void stop(final long stopInterval) throws Exception {
        if (running == false) {
            throw new IllegalStateException("Monitor is not running");
        }
        running = false;
        try {
            thread.join(stopInterval);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final NIOFileAlterationObserver observer : observers) {
            observer.destroy();
        }
    }

    /**
     * Run.
     */
    @Override
    public void run() {
        while (running) {
            for (final NIOFileAlterationObserver observer : observers) {
                observer.checkAndNotify();
            }
            if (!running) {
                break;
            }
            try {
                Thread.sleep(interval);
            } catch (final InterruptedException ignored) {
            }
        }
    }
}
