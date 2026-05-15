package com.aclangtonant;

import java.util.ArrayList;

public class runner implements Runnable {
    private ArrayList<Ant> ants = new ArrayList<Ant>();
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private boolean completed = false;


    private int antsQty = 0;
    private int stepsToComplete = 0;
    private int completedSteps = 0;
    private final String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

    private final gridController grid;

    public runner(gridController grid) {
        this.grid = grid;
    }

    private void addAnt(Ant newAnt) {
        ants.add(newAnt);
    }

    public void createNewAnts(int antsToMake) {
        antsQty += antsToMake;
        // Create new ants
        for (int i = 0; i < antsToMake; i++)   {
            int randomX = (int)(Math.random() * gridController.GRID_WIDTH);
            int randomY = (int)(Math.random() * gridController.GRID_HEIGHT);
            String randomDirection = directions[(int)(Math.random() * 4)];

            Ant newAnt = new Ant(randomX, randomY, randomDirection);
            addAnt(newAnt);
        }
    }

    @Override
    public void run() {
        while (running) { 
            try {
                if (paused) {
                    Thread.sleep(10);
                    continue;
                }

                int antCounter = 0;

                for (Ant ant : ants) {
                    if (antCounter % 1000 == 0) {
                        renderPipeline.waitIfRenderQueueIsFull();
                    }

                    antCounter++;

                    int xPos = ant.getXPos();
                    int yPos = ant.getYPos();
                    boolean previousCellValue = grid.toggleCellAndGetPrevious(xPos, yPos);

                    if (previousCellValue) {
                        ant.rotateAnt("CCW");
                    } else {
                        ant.rotateAnt("CW");
                    }

                    ant.moveAnt(1);
                }

                completedSteps++;

                if (stepsToComplete > 0 && completedSteps >= stepsToComplete && !completed) {
                    completed = true;
                    paused = true;
                    simulationPage.incrementCompletedRunnerCount();
                }

                try {
                    Thread.sleep(simulationPage.simSpeed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {
        running = false;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void setStepsToComplete(int steps) {
        stepsToComplete = steps;
    }

    public int stepsCompleted() {
        return completedSteps;
    }
}
