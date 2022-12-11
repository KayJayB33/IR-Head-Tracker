package pl.edu.pk.mech.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import pl.edu.pk.mech.model.Model;

public class SettingsWindowController {
    @FXML
    public Tab cameraSettingsTab;
    private static final Model model = Model.getInstance();
    @FXML
    public Spinner<Double> widthSpinner;
    @FXML
    public Spinner<Double> heightSpinner;
    @FXML
    public Spinner<Double> depthSpinner;
    private Stage stage;


    public void setStage(final Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        StringConverter<Double> doubleStringConverter = new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value.toString() + " mm";
            }

            @Override
            public Double fromString(String string) {
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
    }

    @FXML
    public void cancelOnAction() {
        stage.close();
    }

    @FXML
    public void saveOnAction() {
        final double width = widthSpinner.getValue();
        final double height = heightSpinner.getValue();
        final double depth = depthSpinner.getValue();

        model.setModelDims(width, height, depth);
        stage.close();
    }

}
