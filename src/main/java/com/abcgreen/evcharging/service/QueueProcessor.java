package com.abcgreen.evcharging.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

import com.abcgreen.evcharging.util.AppLogger;

/**
 * Background worker thread (Module III - multithreading) that serves the charging queue.
 * It sleeps until either the poll interval passes or another thread calls {@link #wakeUp()}
 * (e.g. when a session stops and a charger becomes free), then allocates chargers.
 */
public final class QueueProcessor implements Runnable {

    private static final Object SIGNAL = new Object();
    private static boolean pendingSignal;
    private static Thread thread;
    private static volatile boolean running;
    private static final List<IntConsumer> LISTENERS = new CopyOnWriteArrayList<>();

    private final QueueService queueService = new QueueService();
    private final ConfigService config = new ConfigService();

    private QueueProcessor() {
    }

    public static synchronized void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }
        running = true;
        thread = new Thread(new QueueProcessor(), "queue-processor");
        thread.setDaemon(true);
        thread.start();
        AppLogger.info("Queue processor thread started");
    }

    public static synchronized void stop() {
        running = false;
        wakeUp();
    }

    /** Signals the worker to process the queue immediately. */
    public static void wakeUp() {
        synchronized (SIGNAL) {
            pendingSignal = true;
            SIGNAL.notifyAll();
        }
    }

    /** Listener is called with the number of allocations after every run that allocated something. */
    public static void addListener(IntConsumer listener) {
        LISTENERS.add(listener);
    }

    @Override
    public void run() {
        while (running) {
            try {
                int allocated = queueService.processQueue();
                if (allocated > 0) {
                    AppLogger.info("Queue processor allocated " + allocated + " charger(s)");
                    LISTENERS.forEach(l -> l.accept(allocated));
                }
            } catch (RuntimeException e) {
                AppLogger.error("Queue processor error: " + e.getMessage(), null);
            }
            long waitMs = Math.max(2, config.getInt(ConfigService.QUEUE_POLL_SECONDS, 10)) * 1000L;
            synchronized (SIGNAL) {
                try {
                    if (!pendingSignal) {
                        SIGNAL.wait(waitMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                pendingSignal = false;
            }
        }
    }
}
