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

    /**
     * Draws the grid for pixels to be placed on.
     * It is fully responsive and will first calculate how big each of the cells (1 pixel as far as the simulation is concerned) would actually be rendered as.
     * It will then redraw all of the pixels from the key store, it is nessecary to redraw all pixels due to how the zooming function works.
     * @param canvasWidth
     * @param canvasHeight
     */
    public void drawGrid(double canvasWidth, double canvasHeight) {
        gridController.canvasWidth = canvasWidth;
        gridController.canvasHeight = canvasHeight;
        baseCellSize = Math.min(canvasWidth / GRID_WIDTH, canvasHeight / GRID_HEIGHT);
        updateViewport();

        grid.clearRect(0, 0, canvasWidth, canvasHeight);

        for (long key : gridStore) {
            int xPos = encodePos.getXPos(key);
            int yPos = encodePos.getYPos(key);

            setCellColour(xPos, yPos, Color.BLACK);
        }
    }

    /**
     * Moves the viewport around the grid.
     * It takes the amount the user has dragged by and adds this onto the current pan values.
     * It will then redraw the grid in its new position.
     * @param xOffset
     * @param yOffset
     */
    public void pan(double xOffset, double yOffset) {
        panX += xOffset;
        panY += yOffset;

        drawGrid(canvasWidth, canvasHeight);
    }

    /**
     * Handles zooming in and out of the grid.
     * It first calculates what grid position the mouse is currently over, then changes the zoom value.
     * It then changes the pan values so that the same point stays under the mouse after the zoom.
     * @param xPos
     * @param yPos
     * @param zoomFactor
     */
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

    /**
     * The toggle cell function will initially check if cell locking is enabled, if not it will just trigger the change anyhow, this may cause race conditions.
     * If locking is enabled, it will get the lock for the region the cell is in and wait until it can access it.
     * It then records the time spent waiting for that lock as a metric, performs the toggle, and unlocks the region.
     * @param xPos
     * @param yPos
     * @return
     */
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

    /**
     * Performs the actual cell toggle without doing any locking itself.
     * It checks the encoded sparse store to see what the previous value was, then either adds or removes the cell.
     * It then adds the cell to the render pipeline so the visual state can be updated.
     * @param xPos
     * @param yPos
     * @return
     */
    private boolean toggleCellWithoutLock(int xPos, int yPos) {
        long key = encodePos.encode(xPos, yPos);
        boolean previousValue = gridStore.contains(key);

        if (previousValue) {
            gridStore.remove(key);
        } else {
            gridStore.add(key);
        }

        renderPipeline.addChange(xPos, yPos);
        return previousValue; 
    }

    /**
     * Checks the sparse store for that specific cell, it checks if it is currently black or white.
     * If it's black then it will set it so, if it's white then it will clear it.
     * @param xPos
     * @param yPos
     */
    public void drawCellFromStore(int xPos, int yPos) {
        if (getCellValue(xPos, yPos)) {
            setCellColour(xPos, yPos, Color.BLACK);
        } else {
            clearCell(xPos, yPos);
        }
    }

    /**
     * Calculates how much of the screen should actually have it's pixels changed
     * As 1 cell in the store does not nessecarily mean 1 pixel on the screen, we need a way to map it, this calculates where it should be and draws the square.
     * @param gridX
     * @param gridY
     * @param colour
     */
    private void setCellColour(int gridX, int gridY, Color colour) {
        double x = gridOffsetX + gridX * cellSize;
        double y = gridOffsetY + gridY * cellSize;

        grid.setFill(colour);
        grid.fillRect(x, y, cellSize, cellSize);
    }

    /**
     * Simply gets the position of where that pixel should be and clears the colour from that area.
     * @param gridX
     * @param gridY
     */
    private void clearCell(int gridX, int gridY) {
        double x = gridOffsetX + gridX * cellSize;
        double y = gridOffsetY + gridY * cellSize;

        grid.clearRect(x, y, cellSize, cellSize);
    }

    /**
     * Updates the viewport values used when rendering the grid.
     * It calculates the current cell size using the zoom, then works out where the grid should start drawing from.
     * The pan values are added onto this so the user can move around the grid.
     */
    private static void updateViewport() {
        cellSize = baseCellSize * zoom;
        gridOffsetX = ((canvasWidth - (GRID_WIDTH * cellSize)) / 2) + panX;
        gridOffsetY = ((canvasHeight - (GRID_HEIGHT * cellSize)) / 2) + panY;
    }

    /**
     * Gets the lock for the region that the selected cell is inside.
     * It converts the cell position into a lock region position, then converts this into a unique array index.
     * @param xPos
     * @param yPos
     * @return
     */
    private static ReentrantLock getLockForCell(int xPos, int yPos) {
        int lockX = xPos / lockRegionSize;
        int lockY = yPos / lockRegionSize;
        int lockIndex = lockY * lockColumns + lockX;

        return cellLocks[lockIndex];
    }

    /**
     * Publically exposed function to set if locking should be used and how large each lock region should be.
     * If locking is enabled it creates the locks, otherwise it clears the lock store.
     * @param enabled
     * @param regionSize
     */
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

    /**
     * Creates the lock store used by region locking.
     * It calculates how many lock regions are needed across and down the grid, then creates one lock for each region.
     */
    private static void createLocks() {
        lockColumns = (int) Math.ceil((double) GRID_WIDTH / lockRegionSize);
        int lockRows = (int) Math.ceil((double) GRID_HEIGHT / lockRegionSize);
        cellLocks = new ReentrantLock[lockColumns * lockRows];

        for (int i = 0; i < cellLocks.length; i++) {
            cellLocks[i] = new ReentrantLock();
        }
    }

    /**
     * Publically exposed function to set the grid size variables within this grid ojbect. Will also trigger creating the locks array when it starts.
     * @param width
     * @param height
     */
    public static void setGridSize(int width, int height) {
        GRID_WIDTH = width;
        GRID_HEIGHT = height;
        gridStore.clear();

        if (lockingEnabled) {
            createLocks();
        }
    }

    /**
     * Gets whether or not a cell is stored at athat location from the cell store, it first encodes the coordinates to get the index key to find that pixel.
     * @param xPos
     * @param yPos
     * @return
     */
    private static boolean getCellValue(int xPos, int yPos) {
        return gridStore.contains(encodePos.encode(xPos, yPos));
    } 

    /**
     * Publically accessible function to clear the gridStore of all it's entries.
     */
    public static void clearStore() {
        gridStore.clear();
    }
}
