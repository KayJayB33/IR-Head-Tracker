package pl.edu.pk.mech.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import pl.edu.pk.mech.model.Model;
import pl.edu.pk.mech.util.CameraPreviewThread;
import pl.edu.pk.mech.util.PS3Camera;

public class SettingsWindowController {
    private static final Model model = Model.getInstance();
    @FXML
    public Tab cameraSettingsTab;
    @FXML
    public Spinner<Double> widthSpinner;
    @FXML
    public Spinner<Double> heightSpinner;
    @FXML
    public Spinner<Double> depthSpinner;
    @FXML
    public Slider gainSlider;
    @FXML
    public Slider exposureSlider;
    @FXML
    public Slider sharpnessSlider;
    @FXML
    public Slider hueSlider;
    @FXML
    public Slider brightnessSlider;
    @FXML
    public Slider contrastSlider;
    @FXML
    public Slider redSlider;
    @FXML
    public Slider greenSlider;
    @FXML
    public Slider blueSlider;
    @FXML
    public CheckBox autogainCheckBox;
    @FXML
    public CheckBox autoWhiteCheckBox;
    @FXML
    public CheckBox flipHCheckBox;
    @FXML
    public CheckBox flipVCheckBox;
    @FXML
    public ImageView cameraPreview;
    @FXML
    public ComboBox<PS3Camera.FOV> fovComboBox;
    @FXML
    public ComboBox<PS3Camera.VideoMode> videoModeComboBox;

    private Stage stage;
    private PS3Camera camera;
    private CameraSettings initialSettings;
    private CameraPreviewThread previewThread;


    public void setStage(final Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(event -> cancelOnAction());
    }

    public void setCamera(final PS3Camera camera) {
        this.camera = camera;
        this.previewThread = new CameraPreviewThread(camera, cameraPreview);

        initialSettings = new CameraSettings(camera);
        gainSlider.setValue(initialSettings.gain);
        exposureSlider.setValue(initialSettings.exposure);
        sharpnessSlider.setValue(initialSettings.sharpness);
        hueSlider.setValue(initialSettings.hue);
        brightnessSlider.setValue(initialSettings.brightness);
        contrastSlider.setValue(initialSettings.contrast);
        redSlider.setValue(initialSettings.redBalance);
        greenSlider.setValue(initialSettings.greenBalance);
        blueSlider.setValue(initialSettings.blueBalance);
        autogainCheckBox.setSelected(initialSettings.autoGain);
        autoWhiteCheckBox.setSelected(initialSettings.autoWhiteBalance);
        flipHCheckBox.setSelected(initialSettings.flipH);
        flipVCheckBox.setSelected(initialSettings.flipV);

        fovComboBox.getSelectionModel().select(camera.getFov());
        videoModeComboBox.getSelectionModel().select(camera.getVideoMode());
    }

    @FXML
    public void initialize() {
        StringConverter<Double> doubleStringConverter = new StringConverter<>() {
            @Override
            public String toString(final Double value) {
                return value.toString() + " mm";
            }

            @Override
            public Double fromString(final String string) {
                String valueWithoutUnits = string.replaceAll("mm", "").trim();
                if (valueWithoutUnits.isEmpty()) {
                    return 0.;
                } else {
                    return Double.valueOf(valueWithoutUnits);
                }
            }
        };

        SpinnerValueFactory<Double> widthValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, model.getHalfWidth(), 0.1);
        SpinnerValueFactory<Double> heightValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, model.getHeight(), 0.1);
        SpinnerValueFactory<Double> depthValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, model.getDepth(), 0.1);

        widthValueFactory.setConverter(doubleStringConverter);
        heightValueFactory.setConverter(doubleStringConverter);
        depthValueFactory.setConverter(doubleStringConverter);

        widthSpinner.setValueFactory(widthValueFactory);
        heightSpinner.setValueFactory(heightValueFactory);
        depthSpinner.setValueFactory(depthValueFactory);

        gainSlider.valueProperty()
                .addListener((o, old, value) -> camera.setGain(value.intValue()));
        exposureSlider.valueProperty()
                .addListener((o, old, value) -> camera.setExposure(value.intValue()));
        sharpnessSlider.valueProperty()
                .addListener((o, old, value) -> camera.setSharpness(value.intValue()));
        hueSlider.valueProperty()
                .addListener((o, old, value) -> camera.setHue(value.intValue()));
        brightnessSlider.valueProperty()
                .addListener((o, old, value) -> camera.setBrightness(value.intValue()));
        contrastSlider.valueProperty()
                .addListener((o, old, value) -> camera.setContrast(value.intValue()));
        redSlider.valueProperty()
                .addListener((o, old, value) -> camera.setRedBalance(value.intValue()));
        greenSlider.valueProperty()
                .addListener((o, old, value) -> camera.setGreenBalance(value.intValue()));
        blueSlider.valueProperty()
                .addListener((o, old, value) -> camera.setBlueBalance(value.intValue()));
        autogainCheckBox.selectedProperty()
                .addListener(((o, old, value) -> camera.setAutogain(value)));
        autoWhiteCheckBox.selectedProperty()
                .addListener(((o, old, value) -> camera.setAutoWhiteBalance(value)));
        flipHCheckBox.selectedProperty()
                .addListener(((o, old, value) -> camera.setFlipH(value)));
        flipVCheckBox.selectedProperty()
                .addListener(((o, old, value) -> camera.setFlipV(value)));


        fovComboBox.getItems().add(PS3Camera.FOV.RED_DOT);
        fovComboBox.getItems().add(PS3Camera.FOV.BLUE_DOT);
        fovComboBox.valueProperty()
                .addListener((o, old, value) -> camera.setFov(value));

        videoModeComboBox.getItems().addAll(PS3Camera.VideoMode.class.getEnumConstants());
        videoModeComboBox.valueProperty()
                .addListener((o, old, value) -> camera.setVideoMode(value));

        cameraSettingsTab.selectedProperty().addListener((o, old, value) -> {
            if (value) {
                previewThread.start();
                return;
            }
            previewThread.stopCapturing();
        });
    }

    @FXML
    public void cancelOnAction() {
        if (initialSettings != null) {
            initialSettings.restoreCameraSettings(camera);
            previewThread.stopCapturing();
        }
        stage.close();
    }

    @FXML
    public void saveOnAction() {
        final double width = widthSpinner.getValue();
        final double height = heightSpinner.getValue();
        final double depth = depthSpinner.getValue();

        model.setModelDims(width, height, depth);

        if (initialSettings != null) {
            previewThread.stopCapturing();
        }

        stage.close();
    }

    /**
     * Helper class to save initial camera settings.
     */
    static class CameraSettings {
        int gain;
        int exposure;
        int sharpness;
        int hue;
        int brightness;
        int contrast;
        int redBalance;
        int greenBalance;
        int blueBalance;
        boolean autoGain;
        boolean autoWhiteBalance;
        boolean flipH;
        boolean flipV;
        PS3Camera.VideoMode videoMode;
        PS3Camera.FOV fov;

        CameraSettings(final PS3Camera camera) {
            gain = camera.getGain();
            exposure = camera.getExposure();
            sharpness = camera.getSharpness();
            hue = camera.getHue();
            brightness = camera.getBrightness();
            contrast = camera.getContrast();
            redBalance = camera.getRedBalance();
            greenBalance = camera.getGreenBalance();
            blueBalance = camera.getBlueBalance();
            autoGain = camera.getAutogain();
            autoWhiteBalance = camera.getAutoWhiteBalance();
            flipH = camera.getHorizontalFlip();
            flipV = camera.getVerticalFlip();
            videoMode = camera.getVideoMode();
            fov = camera.getFov();
        }

        void restoreCameraSettings(final PS3Camera camera) {
            camera.setGain(gain);
            camera.setExposure(exposure);
            camera.setSharpness(sharpness);
            camera.setHue(hue);
            camera.setBrightness(brightness);
            camera.setContrast(contrast);
            camera.setRedBalance(redBalance);
            camera.setGreenBalance(greenBalance);
            camera.setBlueBalance(blueBalance);
            camera.setAutogain(autoGain);
            camera.setAutoWhiteBalance(autoWhiteBalance);
            camera.setFlipH(flipH);
            camera.setFlipV(flipV);
            camera.setVideoMode(videoMode);
            camera.setFov(fov);
        }
    }
}
