package com.aclangtonant;

import java.util.ArrayList;

public class runner implements Runnable {
    private ArrayList<Ant> ants = new ArrayList<Ant>();
    private volatile boolean running = true;


    private final int antsQty;
    private final String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

    private final gridController grid;

    public runner(int antsQty, gridController grid) {
        this.antsQty = antsQty;
        this.grid = grid;
    }

    private void addAnt(Ant newAnt) {
        ants.add(newAnt);
    }

    @Override
    public void run() {
        // Create new ants
        for (int i = 0; i < antsQty; i++)   {
            int randomX = (int)(Math.random() * gridController.GRID_WIDTH);
            int randomY = (int)(Math.random() * gridController.GRID_HEIGHT);
            String randomDirection = directions[(int)(Math.random() * 4)];

            Ant newAnt = new Ant(randomX, randomY, randomDirection);
            addAnt(newAnt);
        }

        while (running) { 
            try {
                renderPipeline.waitIfRenderQueueIsFull();

                for (Ant ant : ants) {
                    int xPos = ant.getXPos();
                    int yPos = ant.getYPos();

                    if (gridController.gridStore[xPos][yPos]) {
                        grid.toggleCell(xPos, yPos);
                        ant.rotateAnt("CCW");
                    } else {
                        grid.toggleCell(xPos, yPos);
                        ant.rotateAnt("CW");
                    }

                    ant.moveAnt(1);
                }

                try {
                    Thread.sleep(simulationPage.simSpeed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException ex) {
                System.getLogger(runner.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }

    public void stop() {
        running = false;
    }
}
