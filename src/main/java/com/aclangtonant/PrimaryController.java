package com.aclangtonant;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class PrimaryController {
    @FXML private TextField widthInput;
    @FXML private TextField heightInput;
    @FXML private TextField antsInput;
    @FXML private TextField threadsInput;
    

    @FXML
    private void startSimulation() throws IOException {
        int width = Integer.parseInt(widthInput.getText().trim());
        int height = Integer.parseInt(heightInput.getText().trim());
        int antsQty = Integer.parseInt(antsInput.getText().trim());
        int threadsQty = Integer.parseInt(threadsInput.getText().trim());

        int antsQtySplit = Math.round(antsQty / threadsQty); 

        System.out.println("Starting simulation with width: " + width + " and height: " + height + " with " + antsQtySplit + " ants split over " + threadsQty + "threads.");
        
        gridController.setGridSize(width, height);
        simulationPage.setAntsThreads(antsQtySplit, threadsQty);

        App.setRoot("simulationPage");

    }
}
