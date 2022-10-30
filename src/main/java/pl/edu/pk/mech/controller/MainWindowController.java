package pl.edu.pk.mech.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import pl.edu.pk.mech.VideoTrackingThread;
import pl.edu.pk.mech.tracking.BlobTracker;
import pl.edu.pk.mech.tracking.ContourTracker;
import pl.edu.pk.mech.tracking.ITracker;

import java.io.Closeable;
import java.util.Map;
import java.util.logging.Logger;

public class MainWindowController implements Closeable {

    @FXML
    private Button startButton;
    @FXML
    private ComboBox<String> trackerComboBox;
    @FXML
    private Slider thresholdSlider;
    @FXML
    private Label thresholdValue;
    @FXML
    private Slider minRadiusSlider;
    @FXML
    private Label minRadiusValue;
    @FXML
    private Slider maxRadiusSlider;
    @FXML
    private Label maxRadiusValue;
    @FXML
    private Label detectedAmountLabel;
    @FXML
    private ImageView cameraView;
    @FXML
    private ImageView thresholdView;

    private volatile VideoTrackingThread playThread;

    public Map<String, ITracker> trackers;

    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());

    @FXML
    public void initialize() {
        Loader.load(opencv_java.class);
        trackers = Map.of(
                ContourTracker.class.getSimpleName(),
                new ContourTracker(),
                BlobTracker.class.getSimpleName(),
                new BlobTracker()
        );

        thresholdValue.textProperty().bind(Bindings.format("%.0f", thresholdSlider.valueProperty()));
        minRadiusValue.textProperty().bind(Bindings.format("%.1f px", minRadiusSlider.valueProperty()));
        maxRadiusValue.textProperty().bind(Bindings.format("%.1f px", maxRadiusSlider.valueProperty()));

        minRadiusSlider.maxProperty().bind(maxRadiusSlider.valueProperty());
        maxRadiusSlider.minProperty().bind(minRadiusSlider.valueProperty());

        trackerComboBox.getItems().addAll(trackers.keySet());
        trackerComboBox.getSelectionModel().selectFirst();
    }

    @Override
    public void close() {
        if (playThread != null) {
            playThread.stopCapturing();
        }
    }

    @FXML
    public void buttonOnAction() {
        if (playThread != null && playThread.isPlaying()) {
            stopCapturing();
            return;
        }

        startCapturing();
    }

    public void stopCapturing() {
        LOGGER.info("Stopping capturing...");

        detectedAmountLabel.setText("");
        playThread.stopCapturing();
    }

    public void startCapturing() {
        LOGGER.info("Starting capturing...");

        playThread = new VideoTrackingThread(this, trackers.get(trackerComboBox.getValue()));
        playThread.start();
    }

    public void updateInterface() {
        if (startButton.getText().equals("Start")) {
            startButton.setText("Stop");
            trackerComboBox.setDisable(true);
            return;
        }

        startButton.setText("Start");
        trackerComboBox.setDisable(false);
    }

    public void updateDetectedAmount(int amount) {
        final String text = String.format("Detected objects: %d", amount);

        if(amount != 3) {
            detectedAmountLabel.setTextFill(Color.RED);
        } else {
            detectedAmountLabel.setTextFill(Color.BLACK);
        }

        detectedAmountLabel.setText(text);
    }

    public double getThresholdValue()
    {
        return thresholdSlider.getValue();
    }

    public double getMinRadiusValue()
    {
        return minRadiusSlider.getValue();
    }

    public double getMaxRadiusValue()
    {
        return maxRadiusSlider.getValue();
    }

    public void updateViews(final Image ...images)
    {
        cameraView.setImage(images[0]);
        thresholdView.setImage(images[1]);
    }
}
