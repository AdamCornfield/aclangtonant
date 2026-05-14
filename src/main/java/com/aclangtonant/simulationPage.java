package com.aclangtonant;

import java.util.ArrayList;

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
    public static int simSpeed = 200; // Gap between runs in ms
    public static int renderSpeed = 100; // Gap between render runs in ms
    private static final int METRICS_UPDATE_SPEED = 100;

    private static ArrayList<runner> runners = new ArrayList<>();
    private static ArrayList<Thread> runnerThreads = new ArrayList<>();
    private static volatile boolean running = false;
    private static Timeline renderTimeline;
    private static Thread metricsThread;

    private static int antsQty = 10;
    private static int threadsQty = 1;

    

    @FXML private StackPane canvasContainer;
    @FXML private Canvas canvas;

    @FXML private Slider renderSpeedInput;
    @FXML private Slider simSpeedInput;

    @FXML private Label renderQueueValue;
    @FXML private Label simSpeedValue;
    @FXML private Label renderSpeedValue;
    @FXML private Label renderTimeValue;
    @FXML private Label backPressureSleeps;

    private gridController grid;
    private renderPipeline renderer;
    private double lastPanX;
    private double lastPanY;

    @FXML
    private void initialize() {
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        grid = new gridController(graphicsContext);
        renderer = new renderPipeline(grid);

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

    @FXML
    private void runSim() {
        if (!running) {
            running = true;

            startRenderTimeline();
            startMetricsThread();

            for (int i = 0; i < threadsQty; i++) {
                runner newRunner = new runner(antsQty, grid);
                Thread runnerThread = new Thread(newRunner);

                runners.add(newRunner);
                runnerThreads.add(runnerThread);

                runnerThread.start();
            }
        }
    }

    @FXML
    public void stopSim() {
        stopSimulation();
    }

    private static void stopSimulation() {
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
                runner.stop();
            }

            for (Thread thread : runnerThreads) {
                thread.interrupt();
            }

            runners.clear();
            runnerThreads.clear();
        }
    }

    public static void shutdown() {
        stopSimulation();
    }

    @FXML
    public void setSimSpeed() {
        simSpeed = (int) simSpeedInput.getValue();
        System.out.println(simSpeed);
    }

    @FXML
    public void setRenderSpeed() {
        renderSpeed = (int) renderSpeedInput.getValue();

        if (running) {
            startRenderTimeline();
        }

        System.out.println(renderSpeed);
    }

    private void startRenderTimeline() {
        if (renderTimeline != null) {
            renderTimeline.stop();
        }

        renderTimeline = new Timeline(new KeyFrame(Duration.millis(renderSpeed), e -> renderer.run()));
        renderTimeline.setCycleCount(Animation.INDEFINITE);
        renderTimeline.play();
    }

    private void startMetricsThread() {
        if (metricsThread != null) {
            metricsThread.interrupt();
        }

        metricsThread = new Thread(() -> {
            while (running) {
                Platform.runLater(() -> {
                    renderQueueValue.setText("Current Render Queue: " + renderPipeline.changes.size());
                    renderTimeValue.setText(String.format("Current Render Time: %.3fms", renderPipeline.renderTime));
                    simSpeedValue.setText("Current Sim Speed: " + simSpeed + "ms");
                    renderSpeedValue.setText("Current Render Speed: " + renderSpeed + "ms");
                    backPressureSleeps.setText("Render Backpressure Sleep Cycles: : " + Math.round((renderPipeline.renderBackpressureSleeps.get() * 10) / threadsQty) + " ms of sleeping");
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

    public static void setAntsThreads(int ants, int threads) {
        antsQty = ants;
        threadsQty = threads;
    }
}
