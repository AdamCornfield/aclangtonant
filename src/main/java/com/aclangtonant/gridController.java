package com.aclangtonant;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class gridController {

    public static int GRID_WIDTH = 32;
    public static int GRID_HEIGHT = 32;
    public static boolean[][] gridStore = new boolean[GRID_WIDTH][GRID_HEIGHT];

    private static double cellSize = 0;
    private static double gridOffsetX = 0;
    private static double gridOffsetY = 0;

    private GraphicsContext grid;

    public gridController(GraphicsContext grid) {
        this.grid = grid;
    }

    public void drawGrid(double canvasWidth, double canvasHeight) {
        cellSize = Math.min(canvasWidth / GRID_WIDTH, canvasHeight / GRID_HEIGHT);
        gridOffsetX = (canvasWidth - (GRID_WIDTH * cellSize)) / 2;
        gridOffsetY = (canvasHeight - (GRID_HEIGHT * cellSize)) / 2;

        grid.clearRect(0, 0, canvasWidth, canvasHeight);

        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (gridStore[x][y]) {
                    setCellColour(x, y, Color.BLACK);
                }
            }
        }

        drawGridBorder();
    }

    public void toggleCell(int gridX, int gridY) {
        if (gridStore[gridX][gridY]) {
            gridStore[gridX][gridY] = false;
            drawGrid(grid.getCanvas().getWidth(), grid.getCanvas().getHeight());
        } else {
            gridStore[gridX][gridY] = true;
            setCellColour(gridX, gridY, Color.BLACK);
            drawGridBorder();
        }
    }

    private void setCellColour(int gridX, int gridY, Color colour) {
        double x = gridOffsetX + gridX * cellSize;
        double y = gridOffsetY + gridY * cellSize;

        grid.setFill(colour);
        grid.fillRect(x, y, cellSize, cellSize);
    }

    private void drawGridBorder() {
        grid.setStroke(Color.DARKGRAY);
        grid.setLineWidth(2);
        grid.strokeRect(gridOffsetX, gridOffsetY, GRID_WIDTH * cellSize, GRID_HEIGHT * cellSize);
    }

    public static void setGridSize(int width, int height) {
        GRID_WIDTH = width;
        GRID_HEIGHT = height;
        gridStore = new boolean[width][height];
    }
}
