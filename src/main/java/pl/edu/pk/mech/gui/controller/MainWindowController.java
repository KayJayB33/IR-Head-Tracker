package pl.edu.pk.mech.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pl.edu.pk.mech.tracking.*;
import pl.edu.pk.mech.util.PS3Camera;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainWindowController implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());
    private final Map<String, ITracker> trackers = Map.of(
            ContourTracker.class.getSimpleName(),
            new ContourTracker(),
            BlobTracker.class.getSimpleName(),
            new BlobTracker()
    );
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
    @FXML
    private Menu cameraMenu;
    private List<RadioMenuItem> devicesInCameraMenu;
    private RadioMenuItem demoRadioMenuItem;
    private TrackingThread playThread;

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
        demoRadioMenuItem = new RadioMenuItem("Demo");
        demoRadioMenuItem.setToggleGroup(toggleGroup);
        demoRadioMenuItem.setSelected(true);

        devicesInCameraMenu = Arrays.stream(PS3Camera.getDevicesNames())
                .map(RadioMenuItem::new)
                .peek(item -> item.setToggleGroup(toggleGroup))
                .collect(Collectors.toList());
        devicesInCameraMenu.add(demoRadioMenuItem);

        cameraMenu.getItems().addAll(0, devicesInCameraMenu);
    }

    @Override
    public void close() {
        if (playThread != null) {
            playThread.stopCapturing();
        }

        PS3Camera.disposeAll();
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
    public void settingsOnAction() throws IOException {
        final FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/gui/SettingsWindow.fxml"));
        final VBox root = loader.load();
        final SettingsWindowController controller = loader.getController();

        final Stage settingsStage = new Stage();
        final Scene settingsScene = new Scene(root);

        settingsStage.setTitle("Settings");
        settingsStage.setScene(settingsScene);
        settingsStage.setResizable(false);

        controller.cameraSettingsTab.disableProperty().bind(demoRadioMenuItem.selectedProperty());
        controller.setStage(settingsStage);

        final RadioMenuItem selectedItem = devicesInCameraMenu.stream()
                .filter(RadioMenuItem::isSelected)
                .findAny()
                .orElseThrow();

        if (!selectedItem.getText().equals("Demo")) {
            controller.setCamera(new PS3Camera(selectedItem.getText()));
        }

        settingsStage.show();
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
        RadioMenuItem selectedItem = devicesInCameraMenu.stream()
                .filter(RadioMenuItem::isSelected)
                .findAny()
                .orElseThrow();

        if (selectedItem.equals(demoRadioMenuItem)) {
            playThread = new DemoTrackingThread(this, trackers.get(trackerComboBox.getValue()));
        } else {
            playThread = new CameraTrackingThread(this, trackers.get(trackerComboBox.getValue()), selectedItem.getText());
        }
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

    public void updateDetectedAmount(final int amount) {
        final String text = String.format("Detected objects: %d", amount);

        if (amount != 3) {
            detectedAmountLabel.setTextFill(Color.RED);
        } else {
            detectedAmountLabel.setTextFill(Color.BLACK);
        }

        detectedAmountLabel.setText(text);
    }

    public double getThresholdValue() {
        return thresholdSlider.getValue();
    }

    public double getMinRadiusValue() {
        return minRadiusSlider.getValue();
    }

    public double getMaxRadiusValue() {
        return maxRadiusSlider.getValue();
    }

    public void updateViews(final Image... images) {
        cameraView.setImage(images[0]);
        thresholdView.setImage(images[1]);
    }
}
