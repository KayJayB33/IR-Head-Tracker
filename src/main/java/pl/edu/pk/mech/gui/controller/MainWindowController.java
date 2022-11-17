package pl.edu.pk.mech.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pl.edu.pk.mech.tracking.BlobTracker;
import pl.edu.pk.mech.tracking.ContourTracker;
import pl.edu.pk.mech.tracking.DemoTrackingThread;
import pl.edu.pk.mech.tracking.ITracker;
import pl.edu.pk.mech.util.PS3Camera;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainWindowController implements Closeable {

    private static final File VIDEO_FILE = new File("./res/ExampleVideo.mp4");
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
    private final Map<String, ITracker> trackers = Map.of(
            ContourTracker.class.getSimpleName(),
            new ContourTracker(),
            BlobTracker.class.getSimpleName(),
            new BlobTracker()
    );
    @FXML
    private Menu cameraMenu;
    @FXML
    private MenuItem cameraSettingsMenuItem;

    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());
    private DemoTrackingThread playThread;

    @FXML
    public void initialize() {
        thresholdValue.textProperty().bind(Bindings.format("%.0f", thresholdSlider.valueProperty()));
        minRadiusValue.textProperty().bind(Bindings.format("%.1f px", minRadiusSlider.valueProperty()));
        maxRadiusValue.textProperty().bind(Bindings.format("%.1f px", maxRadiusSlider.valueProperty()));

        minRadiusSlider.maxProperty().bind(maxRadiusSlider.valueProperty());
        maxRadiusSlider.minProperty().bind(minRadiusSlider.valueProperty());

        trackerComboBox.getItems().addAll(trackers.keySet().stream().sorted().toList());
        trackerComboBox.getSelectionModel().selectFirst();

        final ToggleGroup toggleGroup = new ToggleGroup();
        final RadioMenuItem demoRadioMenuItem = new RadioMenuItem("Demo");
        demoRadioMenuItem.setToggleGroup(toggleGroup);
        demoRadioMenuItem.setSelected(true);

        List<MenuItem> menuItems = Arrays.stream(PS3Camera.getDevicesNames())
                .map(RadioMenuItem::new)
                .peek(item -> item.setToggleGroup(toggleGroup))
                .collect(Collectors.toList());
        menuItems.add(demoRadioMenuItem);

        cameraMenu.getItems().addAll(0, menuItems);

        cameraSettingsMenuItem.disableProperty().bind(demoRadioMenuItem.selectedProperty());
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

    @FXML
    public void aboutOnAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/gui/AboutWindow.fxml"));
        SplitPane root = loader.load();

        Stage aboutStage = new Stage();
        Scene aboutScene = new Scene(root);

        aboutStage.setTitle("About");
        aboutStage.setScene(aboutScene);
        aboutStage.setResizable(false);
        aboutStage.show();
    }

    public void stopCapturing() {
        LOGGER.info("Stopping capturing...");

        detectedAmountLabel.setText("");
        playThread.stopCapturing();
    }

    public void startCapturing() {
        LOGGER.info("Starting capturing...");

        playThread = new DemoTrackingThread(this, trackers.get(trackerComboBox.getValue()), VIDEO_FILE);
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
