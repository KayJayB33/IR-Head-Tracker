package pl.edu.pk.mech;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import pl.edu.pk.mech.controller.MainWindowController;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class App extends Application {

    public static final File VIDEO_FILE = new File(
            (Objects.requireNonNull(App.class.getClassLoader().getResource("ExampleVideo.mp4"))).getFile());

    private MainWindowController controller;

    public static void main(String[] args) {
        launch(args);
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/MainWindow.fxml"));
        BorderPane root = loader.load();

        controller = loader.getController();

        Scene scene = new Scene(root);

        primaryStage.setTitle("IR Head Tracker");
        primaryStage.setScene(scene);
        primaryStage.show();

        LOGGER.info(String.format("Video resource: %s", VIDEO_FILE));
    }

    @Override
    public void stop() {
        LOGGER.info("Closing app...");
        controller.close();
        Platform.exit();
    }
}
