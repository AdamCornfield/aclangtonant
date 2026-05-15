package com.aclangtonant;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class gridController {

    public static int GRID_WIDTH = 32;
    public static int GRID_HEIGHT = 32;
    public static Set<Long> gridStore = ConcurrentHashMap.newKeySet();


    private static boolean lockingEnabled = false;
    private static int lockRegionSize = 1;
    private static int lockColumns = 0;
    private static ReentrantLock[] cellLocks;
    public static final AtomicLong lockWaitCount = new AtomicLong(0);
    public static final AtomicLong lockWaitTime = new AtomicLong(0);

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

        for (long key : gridStore) {
            int xPos = (int) (key >> 32);
            int yPos = (int) key;

            setCellColour(xPos, yPos, Color.BLACK);
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

    public boolean toggleCellAndGetPrevious(int xPos, int yPos) {
        if (!lockingEnabled) {
            return toggleCellWithoutLock(xPos, yPos);
        }

        ReentrantLock cellLock = getLockForCell(xPos, yPos);
        long waitStart = System.nanoTime();
        cellLock.lock();
        lockWaitTime.addAndGet(System.nanoTime() - waitStart);
        lockWaitCount.incrementAndGet();

        try {
            return toggleCellWithoutLock(xPos, yPos);
        } finally {
            cellLock.unlock();
        }
    }

    private boolean toggleCellWithoutLock(int xPos, int yPos) {
        long key = encodePos(xPos, yPos);
        boolean previousValue = gridStore.contains(key);

        if (previousValue) {
            gridStore.remove(key);
        } else {
            gridStore.add(key);
        }

        renderPipeline.addChange(xPos, yPos);
        return previousValue; 
    }

    public void drawCellFromStore(int xPos, int yPos) {
        if (getCellValue(xPos, yPos)) {
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

    private static ReentrantLock getLockForCell(int xPos, int yPos) {
        int lockX = xPos / lockRegionSize;
        int lockY = yPos / lockRegionSize;
        int lockIndex = lockY * lockColumns + lockX;

        return cellLocks[lockIndex];
    }

    public static void setLocking(boolean enabled, int regionSize) {
        lockingEnabled = enabled;
        lockRegionSize = Math.max(1, regionSize);

        if (lockingEnabled) {
            createLocks();
        } else {
            cellLocks = null;
            lockColumns = 0;
        }
    }

    private static void createLocks() {
        lockColumns = (int) Math.ceil((double) GRID_WIDTH / lockRegionSize);
        int lockRows = (int) Math.ceil((double) GRID_HEIGHT / lockRegionSize);
        cellLocks = new ReentrantLock[lockColumns * lockRows];

        for (int i = 0; i < cellLocks.length; i++) {
            cellLocks[i] = new ReentrantLock();
        }
    }

    public static void setGridSize(int width, int height) {
        GRID_WIDTH = width;
        GRID_HEIGHT = height;
        gridStore.clear();

        if (lockingEnabled) {
            createLocks();
        }
    }

    private static long encodePos(int xPos, int yPos) {
        return (((long) xPos) << 32) | (yPos & 0xffffffffL);
    }

    private static boolean getCellValue(int xPos, int yPos) {
        return gridStore.contains(encodePos(xPos, yPos));
    } 

    public static void clearStore() {
        gridStore.clear();
    }
}
