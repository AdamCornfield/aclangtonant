package com.aclangtonant;

import java.util.concurrent.ConcurrentLinkedQueue;


public class renderPipeline implements Runnable {
    public static ConcurrentLinkedQueue<cellValue> changes = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        while (true) { 
            cellValue change = changes.poll();

            if (change != null) {
                System.out.println(change);
            }
        }
    }
    
}
