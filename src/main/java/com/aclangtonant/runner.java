package com.aclangtonant;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class runner implements Runnable {
    private ArrayList<Ant> ants = new ArrayList<Ant>();


    private int antsQty;
    private final String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

    private gridController grid;
    private Timeline timeline;

    public runner(int antsQty, gridController grid) {
        this.antsQty = antsQty;
        this.grid = grid;
    }

    private void addAnt(Ant newAnt) {
        ants.add(newAnt);
    }

    @Override
    public void run() {
        for (int i = 0; i < antsQty; i++)   {
            int randomX = (int)(Math.random() * gridController.GRID_WIDTH);
            int randomY = (int)(Math.random() * gridController.GRID_HEIGHT);
            String randomDirection = directions[(int)(Math.random() * 4)];

            Ant newAnt = new Ant(randomX, randomY, randomDirection);
            addAnt(newAnt);
        }

        KeyFrame toggleEverySecond = new KeyFrame(Duration.millis(10), e -> {
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
        });

        timeline = new Timeline(toggleEverySecond);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}
