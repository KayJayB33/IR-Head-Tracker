package pl.edu.pk.mech.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import pl.edu.pk.mech.gui.controller.MainWindowController;

import java.io.IOException;
import java.util.logging.Logger;

public class App extends Application {

    private MainWindowController controller;

    public static void main(String[] args) {
        launch(args);
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/gui/MainWindow.fxml"));
        BorderPane root = loader.load();

        controller = loader.getController();

        Scene scene = new Scene(root);

        primaryStage.setTitle("IR Head Tracker");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            LOGGER.info("Closing app...");
            controller.close();
            Platform.exit();
        });
    }
}
