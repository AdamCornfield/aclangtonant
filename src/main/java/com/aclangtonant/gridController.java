package com.aclangtonant;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class gridController {

    public static int GRID_WIDTH = 32;
    public static int GRID_HEIGHT = 32;
    public static boolean[][] gridStore = new boolean[GRID_WIDTH][GRID_HEIGHT];
    
    public static boolean lockingEnabled = false;
public static int lockRegionSize = 1;

    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 80.0;

    private static double canvasWidth = 0;
    private static double canvasHeight = 0;
    private static double baseCellSize = 0;
    private static double cellSize = 0;
    private static double gridOffsetX = 0;
    private static double gridOffsetY = 0;
    private static double panX = 0;
    private static double panY = 0;
    private static double zoom = 1;

    private GraphicsContext grid;

    public gridController(GraphicsContext grid) {
        this.grid = grid;
    }

    public void drawGrid(double canvasWidth, double canvasHeight) {
        gridController.canvasWidth = canvasWidth;
        gridController.canvasHeight = canvasHeight;
        baseCellSize = Math.min(canvasWidth / GRID_WIDTH, canvasHeight / GRID_HEIGHT);
        updateViewport();

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

    public void pan(double xOffset, double yOffset) {
        panX += xOffset;
        panY += yOffset;

        drawGrid(canvasWidth, canvasHeight);
    }

    public void zoom(double xPos, double yPos, double zoomFactor) {
        double gridX = (xPos - gridOffsetX) / cellSize;
        double gridY = (yPos - gridOffsetY) / cellSize;

        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * zoomFactor));
        cellSize = baseCellSize * zoom;

        double centeredOffsetX = (canvasWidth - (GRID_WIDTH * cellSize)) / 2;
        double centeredOffsetY = (canvasHeight - (GRID_HEIGHT * cellSize)) / 2;
        panX = xPos - centeredOffsetX - (gridX * cellSize);
        panY = yPos - centeredOffsetY - (gridY * cellSize);

        drawGrid(canvasWidth, canvasHeight);
    }

    public void toggleCell(int xPos, int yPos) {
        gridStore[xPos][yPos] = !gridStore[xPos][yPos];

        renderPipeline.addChange(xPos, yPos);
    }

    public void drawCellFromStore(int xPos, int yPos) {
        if (gridStore[xPos][yPos]) {
            setCellColour(xPos, yPos, Color.BLACK);
        } else {
            clearCell(xPos, yPos);
        }
    }

    private void setCellColour(int gridX, int gridY, Color colour) {
        double x = gridOffsetX + gridX * cellSize;
        double y = gridOffsetY + gridY * cellSize;

        grid.setFill(colour);
        grid.fillRect(x, y, cellSize, cellSize);
    }

    private void clearCell(int gridX, int gridY) {
        double x = gridOffsetX + gridX * cellSize;
        double y = gridOffsetY + gridY * cellSize;

        grid.clearRect(x, y, cellSize, cellSize);
    }

    public void drawGridBorder() {
        grid.setStroke(Color.DARKGRAY);
        grid.setLineWidth(2);
        grid.strokeRect(gridOffsetX, gridOffsetY, GRID_WIDTH * cellSize, GRID_HEIGHT * cellSize);
    }

    private static void updateViewport() {
        cellSize = baseCellSize * zoom;
        gridOffsetX = ((canvasWidth - (GRID_WIDTH * cellSize)) / 2) + panX;
        gridOffsetY = ((canvasHeight - (GRID_HEIGHT * cellSize)) / 2) + panY;
    }

    public static void setGridSize(int width, int height) {
        GRID_WIDTH = width;
        GRID_HEIGHT = height;
        gridStore = new boolean[width][height];
    }
}
