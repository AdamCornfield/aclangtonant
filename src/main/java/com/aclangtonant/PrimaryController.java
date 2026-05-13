package com.aclangtonant;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class PrimaryController {
    @FXML private TextField widthInput;
    @FXML private TextField heightInput;

    @FXML
    private void startSimulation() throws IOException {
        int width = Integer.parseInt(widthInput.getText().trim());
        int height = Integer.parseInt(heightInput.getText().trim());

        System.out.println("Starting simulation with width: " + width + " and height: " + height);
        
        gridController.setGridSize(width, height);

        App.setRoot("simulationPage");

    }
}
