package pl.edu.pk.mech.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import pl.edu.pk.mech.VideoTrackingThread;

import java.io.Closeable;
import java.util.logging.Logger;

public class MainWindowController implements Closeable {

    @FXML
    private BorderPane borderPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Button startButton;
    @FXML
    private Slider thresholdSlider;
    @FXML
    private Label sliderValueLabel;
    @FXML
    private ImageView cameraView;
    @FXML
    private ImageView thresholdView;

    private volatile VideoTrackingThread playThread;

    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());

    @FXML
    public void initialize() {
        Loader.load(opencv_java.class);
        sliderValueLabel.textProperty().bind(Bindings.format("%.0f", thresholdSlider.valueProperty()));
    }

    @Override
    public void close() {
        if (playThread != null) {
            playThread.interrupt();
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

        playThread.stopCapturing();
    }

    public void startCapturing() {
        LOGGER.info("Starting capturing...");

        playThread = new VideoTrackingThread(this);
        playThread.start();
    }

    public void updateButtonText()
    {
        if(startButton.getText().equals("Start"))
        {
            startButton.setText("Stop");
            return;
        }

        startButton.setText("Start");
    }

    public double getSliderValue()
    {
        return thresholdSlider.getValue();
    }

    public void updateViews(final Image ...images)
    {
        cameraView.setImage(images[0]);
        thresholdView.setImage(images[1]);
    }
}
