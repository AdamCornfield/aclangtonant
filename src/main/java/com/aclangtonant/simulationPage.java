package com.aclangtonant;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;

public class simulationPage {

    @FXML
    private StackPane canvasContainer;

    @FXML
    private Canvas canvas;
    private gridController grid;

    @FXML
    private void initialize() {
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        grid = new gridController(graphicsContext);

        // Constrains the canvas to the available space in the page.
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        canvas.widthProperty().addListener((observable, oldValue, newValue) -> drawGrid());
        canvas.heightProperty().addListener((observable, oldValue, newValue) -> drawGrid());

        drawGrid();
    }

    private void drawGrid() {
        grid.drawGrid(canvas.getWidth(), canvas.getHeight());
    }

    @FXML
    private void runAnt() {
        // Ant ant = new Ant(2, 2, "UP");
        
        for (int i = 0; i < 4; i++) {
            runner newRunner = new runner(20, grid);

            new Thread(newRunner).start();
        }
    }
}
