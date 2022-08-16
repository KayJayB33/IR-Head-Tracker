package pl.edu.pk.mech.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

import java.util.logging.Logger;

public class MainWindowController {

    @FXML
    private Button startButton;
    @FXML
    private ImageView imageView;

    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());

    @FXML
    protected void startCapturing(ActionEvent e) {
        LOGGER.info("Pressed START button");
    }
}
