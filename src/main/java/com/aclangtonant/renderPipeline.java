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

    public static void addChange(int xPos, int yPos) {
        long key = encodePos.encode(xPos, yPos);

        changes.add(key);
        queuedChanges.incrementAndGet();
    }

    public static void clearChanges() {
        changes.clear();
        queuedChanges.set(0);
    }
    

    public static void waitIfRenderQueueIsFull() throws InterruptedException {
        while (queuedChanges.get() > MAX_RENDER_QUEUE_SIZE) {
            renderBackpressureSleeps.incrementAndGet();
            Thread.sleep(10);
        }
    }

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
