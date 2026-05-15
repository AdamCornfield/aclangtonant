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

    /**
     * Pulls in the initial grid object so it is able to make changes like toggling cells
     */
    public runner(gridController grid) {
        this.grid = grid;
    }

    /**
     * Creates new ants based on how many this runner is tasked to manage. each ant has it's location and direction randomised.
     * New Ant objects are created from the Ant class and saved in the ants array list
     * @param antsToMake
     */
    public void createNewAnts(int antsToMake) {
        antsQty += antsToMake;
        // Create new ants
        for (int i = 0; i < antsToMake; i++)   {
            int randomX = (int)(Math.random() * gridController.GRID_WIDTH);
            int randomY = (int)(Math.random() * gridController.GRID_HEIGHT);
            String randomDirection = directions[(int)(Math.random() * 4)];

            Ant newAnt = new Ant(randomX, randomY, randomDirection);
            ants.add(newAnt);
        }
    }

    /**
     * Entry point to begin running this object
     * Will always run if the running value is true.
     * 
     * If the paused value is set to true then it will keep sleeping until it is unpaused, this however prevents the thread from being closed so the ants stay in their original position.
     * It will then loop through the following for each ant:
     * Check if it is the 1000th ant in the list, if it is check to ensure the render queue is not full as if it is then it should wait to avoid overflowing the renderer, this is the backpressure system.
     * 
     * If it is good then it will get the ant's current position and then tell the grid to toggle that cell.
     * It will then rotate and move the ant before moving on.
     * 
     * 
     * At the end it will check that if the number of steps limit is active, i.e. it is over 0, and it has completed all of it's steps, it will pause itself and complete increment the counter once on the simulation main page.
     */
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

    /**
     * Public function exposed to stop this runner object
     */
    public void stop() {
        running = false;
    }

    /**
     * Public function exposed to pause this runner object
     */
    public void pause() {
        paused = true;
    }

    /**
     * Public function exposed to resume this runner object
     */
    public void resume() {
        paused = false;
    }

    /**
     * Public function exposed to set how many steps it needs to complete
     */
    public void setStepsToComplete(int steps) {
        stepsToComplete = steps;
    }

    /**
     * Public function exposed to so the metrics thread can see an update on progress.
     */
    public int stepsCompleted() {
        return completedSteps;
    }
}
