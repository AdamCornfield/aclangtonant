package com.aclangtonant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class simulationPage {
    public static int simSpeed = 25; // Gap between runs in ms
    public static int renderSpeed = 10; // Gap between render runs in ms
    private static final int METRICS_UPDATE_SPEED = 50;

    private static final ArrayList<runner> runners = new ArrayList<>();
    private static final ArrayList<Thread> runnerThreads = new ArrayList<>();
    private static volatile boolean running = false;
    private static Timeline renderTimeline;
    private static Thread metricsThread;

    private static int antsQty = 10;
    private static int threadsQty = 1;
    private static int stepsQty = 0;
    private static int totalAntsQty = 10;
    private static int gridWidth = 32;
    private static int gridHeight = 32;
    private static boolean lockingEnabled = false;
    private static int lockRegionSize = 1;
    private static final AtomicInteger completedRunnerCount = new AtomicInteger(0);

    private static long start = 0;

    @FXML private StackPane canvasContainer;
    @FXML private Canvas canvas;

    @FXML private Slider renderSpeedInput;
    @FXML private Slider simSpeedInput;

    @FXML private Label renderQueueValue;
    @FXML private Label simSpeedValue;
    @FXML private Label renderSpeedValue;
    @FXML private Label renderTimeValue;
    @FXML private Label backPressureSleeps;
    @FXML private Label lockWaitTimeValue;
    @FXML private Label stepsCompletedValue;
    @FXML private Label currentTimerValue;
    @FXML private Label settingsGridSizeValue;
    @FXML private Label settingsAntsValue;
    @FXML private Label settingsThreadsValue;
    @FXML private Label settingsStepsValue;
    @FXML private Label settingsSimSpeedValue;
    @FXML private Label settingsRenderSpeedValue;
    @FXML private Label settingsLockingValue;
    @FXML private Label settingsLockRegionSizeValue;
    

    private gridController grid;
    private renderPipeline renderer;
    private double lastPanX;
    private double lastPanY;

    /**
     * Start of the simulation page. Will initially create the rendering pipeline and grid, setting the simSpeed and render speed sliders.
     * 
     * Handles the logic for zooming,
     */
    @FXML
    @SuppressWarnings("unused")
    private void initialize() {
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        grid = new gridController(graphicsContext);
        renderer = new renderPipeline(grid);
        simSpeedInput.setValue(simSpeed);
        renderSpeedInput.setValue(renderSpeed);
        setSettingsLabels();

        // Constrains the canvas to the available space in the page.
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        canvas.widthProperty().addListener((observable, oldValue, newValue) -> drawGrid());
        canvas.heightProperty().addListener((observable, oldValue, newValue) -> drawGrid());
        simSpeedInput.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            if (!isChanging) {
                setSimSpeed();
            }
        });
        renderSpeedInput.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            if (!isChanging) {
                setRenderSpeed();
            }
        });
        canvasContainer.setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.2 : 0.8;
            grid.zoom(event.getX(), event.getY(), zoomFactor);
            event.consume();
        });
        canvasContainer.setOnMousePressed(event -> {
            lastPanX = event.getX();
            lastPanY = event.getY();
        });
        canvasContainer.setOnMouseDragged(event -> {
            grid.pan(event.getX() - lastPanX, event.getY() - lastPanY);
            lastPanX = event.getX();
            lastPanY = event.getY();
            event.consume();
        });

        drawGrid();
    }

    private void drawGrid() {
        grid.drawGrid(canvas.getWidth(), canvas.getHeight());
    }

    private void setSettingsLabels() {
        settingsGridSizeValue.setText("Grid Size: " + gridWidth + " x " + gridHeight);
        settingsAntsValue.setText("Ants: " + totalAntsQty);
        settingsThreadsValue.setText("Threads: " + threadsQty);
        settingsStepsValue.setText("Steps: " + stepsQty);
        settingsSimSpeedValue.setText("Sim Speed: " + simSpeed + "ms");
        settingsRenderSpeedValue.setText("Render Speed: " + renderSpeed + "ms");
        settingsLockingValue.setText("Region Locking: " + lockingEnabled);
        settingsLockRegionSizeValue.setText("Lock Region Size: " + lockRegionSize);
    }

    /**
     * Runs the core simulation loop.
     * If it is already running it will not run a second isntance.
     * 
     * If it is a fresh run it will set the runner count to 0 which tracks how many threads have completed their tasks.
     * 
     * Then it creates all of the new runner classes with a set quantity of ants each.
     * 
     * It will then create all of the threads and runs them all.
     * 
     * If they are only paused then it will just continue instead of creating new ants.
     */
    @FXML
    private void runSim() {
        if (!running) {
            running = true;

            if (runnerThreads.isEmpty()) {
                resetSimulationView();
                completedRunnerCount.set(0);

                start = System.nanoTime();

                for (int i = 0; i < threadsQty; i++) {
                    runner newRunner = new runner(grid);
                    newRunner.createNewAnts(antsQty);
                    newRunner.setStepsToComplete(stepsQty);

                    Thread runnerThread = new Thread(newRunner);

                    runners.add(newRunner);
                    runnerThreads.add(runnerThread);

                    runnerThread.start();
                }
            } else {
                for (runner runner : runners) {
                    runner.resume();
                }
            }

            startRenderTimeline();
            startMetricsThread();
        }
    }

    /* Clears the canvas, the grid store, and any pending render changes. */
    private void resetSimulationView() {
        gridController.clearStore();
        renderPipeline.clearChanges();
        drawGrid();
    }

    // Stop function exposed for javaFX to trigger
    @FXML
    public void stopSim() {
        stopSimulation();
    }

    // Pause function exposed for javaFX to trigger
    @FXML
    public void pauseSim() {
        pauseSimulation();
    }

    // javaFX return to the main settings page button
    @FXML
    public void returnToSettings() throws IOException {
        stopSimulation();
        App.setRoot("primary");
    }

    /**
     * Pauses the simulation by setting running to false, 
     * stopping the render timelines and triggering the pause function in each of the threads
     */
    private static void pauseSimulation() {
        if (running) {
            running = false;

            if (renderTimeline != null) {
                renderTimeline.stop();
                renderTimeline = null;
            }

            if (metricsThread != null) {
                metricsThread.interrupt();
                metricsThread = null;
            }

            for (runner runner : runners) {
                runner.pause();
            }
        }
    }

    /**
     * Executes a pause first to reuse similar code. Then it will formally stop all of the runners and threads to provide a clean exit avoiding any leftover running threads.
     * Then clears all of the threads and runners lists.
     */
    private static void stopSimulation() {
        pauseSimulation();
        System.out.println("Stopping, clearing runners out");

        for (runner runner : runners) {
            runner.stop();
        }

        for (Thread thread : runnerThreads) {
            thread.interrupt();
        }
            
        runners.clear();
        runnerThreads.clear();
    }

    /**
     * Static Function exposed for the app.java to trigger shutdown
     */
    public static void shutdown() {
        stopSimulation();
    }

    /**
     * Function to update simuation speed
     */
    @FXML
    public void setSimSpeed() {
        simSpeed = (int) simSpeedInput.getValue();
    }

    /**
     * Sets the new render speed from the slider
     * If it is running it will restart the render timeline
     */
    @FXML
    public void setRenderSpeed() {
        renderSpeed = (int) renderSpeedInput.getValue();

        if (running) {
            startRenderTimeline();
        }

        System.out.println(renderSpeed);
    }

    // Makes use of javaFX's timeline feature to trigger the render cycle at a variable referesh rate, if it's already running it will restart it
    private void startRenderTimeline() {
        if (renderTimeline != null) {
            renderTimeline.stop();
        }

        renderTimeline = new Timeline(new KeyFrame(Duration.millis(renderSpeed), e -> renderer.run()));
        renderTimeline.setCycleCount(Animation.INDEFINITE);
        renderTimeline.play();
    }

    /**
     * Separate thread that manages updating the metrics lables on the simulation page
     * Also contains the code to check if all threads have completed their steps to then terminate the whole simulation
     */
    private void startMetricsThread() {
        if (metricsThread != null) {
            metricsThread.interrupt();
        }

        metricsThread = new Thread(() -> {
            while (running) {
                Platform.runLater(() -> {
                    renderQueueValue.setText("Current Render Queue: " + renderPipeline.queuedChanges.get() + "/10,000,000 jobs");
                    renderTimeValue.setText(String.format("Current Render Time: %.3fms", renderPipeline.renderTime));
                    simSpeedValue.setText("Current Sim Speed: " + simSpeed + "ms");
                    renderSpeedValue.setText("Current Render Speed: " + renderSpeed + "ms");
                    backPressureSleeps.setText("Render Backpressure Sleep Cycles: " + Math.round((renderPipeline.renderBackpressureSleeps.get() * 10) / threadsQty) + " ms of sleeping");
                    lockWaitTimeValue.setText(String.format("Lock Wait Time: %.3fms", (gridController.lockWaitTime.get() / 1_000_000.0) / threadsQty));
                    currentTimerValue.setText(String.format("Current Time Elapsed: %.2fs", (System.nanoTime() - start) / 1_000_000_000.0));

                    int totalStepsCompleted = 0;

                    for (runner runner : runners) {
                        totalStepsCompleted += runner.stepsCompleted();
                    }

                    stepsCompletedValue.setText("Steps Completed: " + Math.round(totalStepsCompleted / threadsQty));
                    

                    if (completedRunnerCount.get() == threadsQty) {
                        stopSimulation();
                    }
                });

                try {
                    Thread.sleep(METRICS_UPDATE_SPEED);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        metricsThread.setDaemon(true);
        metricsThread.start();
    }

    /**
     * The following functions take in the values from the main data entry screen and saves them into this class.
     */
    public static void setAntsThreadsSteps(int ants, int threads, int steps) {
        antsQty = ants;
        threadsQty = threads;
        stepsQty = steps;
    }

    public static void setSimulationSpeeds(int simSpeedValue, int renderSpeedValue) {
        simSpeed = simSpeedValue;
        renderSpeed = renderSpeedValue;
    }

    public static void setSettingsValues(int width, int height, int ants, boolean locking, int regionSize) {
        gridWidth = width;
        gridHeight = height;
        totalAntsQty = ants;
        lockingEnabled = locking;
        lockRegionSize = regionSize;
    }

    /**
     * Public function exposed for runners to increment the completed runner count once it is finished
     */
    public static void incrementCompletedRunnerCount() {
        completedRunnerCount.incrementAndGet();
    }
}
