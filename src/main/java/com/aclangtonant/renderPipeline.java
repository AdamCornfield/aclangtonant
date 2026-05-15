package com.aclangtonant;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class renderPipeline implements Runnable {
    public static Set<Long> changes = ConcurrentHashMap.newKeySet();
    public static double renderTime = 0; 

    public static final int MAX_RENDER_QUEUE_SIZE = 10_000_000;
    public static final AtomicInteger queuedChanges = new AtomicInteger(0);
    public static final AtomicInteger renderBackpressureSleeps = new AtomicInteger(0);


    private gridController grid;

    public renderPipeline(gridController grid) {
        this.grid = grid;
    }

    /**
     * Uses the encode utilities and enters the fact that the pixel has had an update and should be re-rendered.
     * Adds it to a thread safe set backed by a concurrentHashMap.
     * It then increments its counter used by the backpressure metric.
     * @param xPos
     * @param yPos
     */
    public static void addChange(int xPos, int yPos) {
        long key = encodePos.encode(xPos, yPos);

        changes.add(key);
        queuedChanges.incrementAndGet();
    }

    /**
     * Publically exposed function to clear all of the changes that have been made and reset the counter
     */
    public static void clearChanges() {
        changes.clear();
        queuedChanges.set(0);
    }
    

    /**
     * Function to be called by the runners making them wait while the queue is full.
     * It increments the central backpressure sleeps cycle counter, this is rendered on the page as a metric of how much time is spent by threads waiting for the render changes to empty to assist in optimisation.
     * @throws InterruptedException
     */
    public static void waitIfRenderQueueIsFull() throws InterruptedException {
        while (queuedChanges.get() > MAX_RENDER_QUEUE_SIZE) {
            renderBackpressureSleeps.incrementAndGet();
            Thread.sleep(10);
        }
    }

    /**
     * Performs one full rendering sweep.
     * It works by going through each of the encoded changes, decoding its coordinates, and then triggers the grid to redraw that cell from the store of pixels.
     * 
     * Each one of these full passes has it's time taken recorded to track performance.
     */
    @Override
    public void run() {
        long start = System.nanoTime();

        for (long key : changes) {
            changes.remove(key);

            queuedChanges.decrementAndGet();

            int xPos = encodePos.getXPos(key);
            int yPos = encodePos.getYPos(key);

            grid.drawCellFromStore(xPos, yPos);
        }

        long end = System.nanoTime();
        renderTime = (end - start) / 1_000_000.0;
    }
}
