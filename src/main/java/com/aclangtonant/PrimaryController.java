package com.aclangtonant;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class PrimaryController {
    @FXML private TextField widthInput;
    @FXML private TextField heightInput;
    @FXML private TextField antsInput;
    @FXML private TextField threadsInput;
    @FXML private TextField stepsInput;
    @FXML private TextField simSpeedInput;
    @FXML private TextField renderSpeedInput;
    @FXML private CheckBox lockingEnabledInput;
    @FXML private TextField lockRegionSizeInput;
    

    @FXML
    private void startSimulation() throws IOException {
        int width = Integer.parseInt(widthInput.getText().trim());
        int height = Integer.parseInt(heightInput.getText().trim());
        int antsQty = Integer.parseInt(antsInput.getText().trim());
        int threadsQty = Integer.parseInt(threadsInput.getText().trim());
        int stepsQty = Integer.parseInt(stepsInput.getText().trim());
        int simSpeed = Integer.parseInt(simSpeedInput.getText().trim());
        int renderSpeed = Integer.parseInt(renderSpeedInput.getText().trim());
        int lockRegionSize = Integer.parseInt(lockRegionSizeInput.getText().trim());

        int antsQtySplit = Math.round(antsQty / threadsQty); 

        System.out.println("Starting simulation with width: " + width + " and height: " + height + " with " + antsQtySplit + " ants split over " + threadsQty + "threads.");
        
        gridController.setGridSize(width, height);
        gridController.setLocking(lockingEnabledInput.isSelected(), lockRegionSize);
        simulationPage.setAntsThreadsSteps(antsQtySplit, threadsQty, stepsQty);
        simulationPage.setSimulationSpeeds(simSpeed, renderSpeed);

        App.setRoot("simulationPage");

    }
}
