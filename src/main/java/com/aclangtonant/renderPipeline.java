package com.aclangtonant;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class renderPipeline implements Runnable {
    public static ConcurrentLinkedQueue<cellValue> changes = new ConcurrentLinkedQueue<>();
    public static double renderTime = 0; 

    public static final int MAX_RENDER_QUEUE_SIZE = 1_000_000;
    public static final AtomicInteger queuedChanges = new AtomicInteger(0);
    public static final AtomicInteger renderBackpressureSleeps = new AtomicInteger(0);


    private gridController grid;

    public renderPipeline(gridController grid) {
        this.grid = grid;
    }

    public static void addChange(int x, int y) {
        changes.add(new cellValue(x, y));
        queuedChanges.incrementAndGet();
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

        cellValue change;
        while ((change = renderPipeline.changes.poll()) != null) { 
            grid.drawCellFromStore(change.x, change.y);
            queuedChanges.decrementAndGet();
        }

        grid.drawGridBorder();

        long end = System.nanoTime();
        renderTime = (end - start) / 1_000_000.0;
    }
}
