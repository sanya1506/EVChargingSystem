package com.abcgreen.evcharging.service;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * In-memory multithreading demonstration (Module III): a producer thread lets vehicles
 * arrive at a station, several charger threads (consumers) take vehicles from a shared FIFO queue.
 * Synchronisation uses synchronized blocks with wait() / notifyAll().
 */
public class QueueSimulator {

    private final Queue<String> waiting = new LinkedList<>();
    private final AtomicInteger served = new AtomicInteger();
    private final AtomicInteger maxQueue = new AtomicInteger();
    private volatile boolean arrivalsFinished;
    private final Random random = new Random();

    /**
     * Runs the simulation and blocks until all vehicles are charged.
     *
     * @param chargers   number of charger threads
     * @param vehicles   number of arriving vehicles
     * @param arrivalMs  average time between arrivals (ms)
     * @param chargeMs   average charging time (ms)
     * @param log        receives log lines (called from worker threads)
     */
    public void run(int chargers, int vehicles, int arrivalMs, int chargeMs, Consumer<String> log) throws InterruptedException {
        long start = System.currentTimeMillis();
        log.accept("Simulation started: " + chargers + " chargers, " + vehicles + " vehicles");

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= vehicles; i++) {
                sleep(arrivalMs / 2 + random.nextInt(Math.max(1, arrivalMs)));
                String vehicle = "EV-" + String.format("%03d", i);
                synchronized (waiting) {
                    waiting.add(vehicle);
                    maxQueue.set(Math.max(maxQueue.get(), waiting.size()));
                    log.accept(t(start) + vehicle + " arrived  -> queue size " + waiting.size());
                    waiting.notifyAll(); // wake a free charger
                }
            }
            arrivalsFinished = true;
            synchronized (waiting) {
                waiting.notifyAll();
            }
        }, "arrivals");

        Thread[] workers = new Thread[chargers];
        for (int c = 0; c < chargers; c++) {
            String name = "Charger-" + (c + 1);
            workers[c] = new Thread(() -> {
                while (true) {
                    String vehicle;
                    synchronized (waiting) {
                        while (waiting.isEmpty() && !arrivalsFinished) {
                            try {
                                waiting.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        if (waiting.isEmpty()) {
                            return; // no more vehicles
                        }
                        vehicle = waiting.poll();
                    }
                    log.accept(t(start) + name + " starts charging " + vehicle);
                    sleep(chargeMs / 2 + random.nextInt(Math.max(1, chargeMs)));
                    served.incrementAndGet();
                    log.accept(t(start) + name + " finished " + vehicle + " (" + served.get() + "/" + vehicles + " done)");
                }
            }, name);
        }
        producer.start();
        for (Thread w : workers) {
            w.start();
        }
        producer.join();
        for (Thread w : workers) {
            w.join();
        }
        log.accept(String.format("Simulation finished in %.1f s. Served %d vehicles, longest queue %d.",
                (System.currentTimeMillis() - start) / 1000.0, served.get(), maxQueue.get()));
    }

    private static String t(long start) {
        return String.format("[%5.1fs] ", (System.currentTimeMillis() - start) / 1000.0);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
