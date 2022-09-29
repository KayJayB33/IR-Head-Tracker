package pl.edu.pk.mech.controller;

import javafx.application.Platform;
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
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.opencv.opencv_java;
import pl.edu.pk.mech.App;
import pl.edu.pk.mech.ObjectTracker;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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

    private volatile Thread playThread;
    private volatile boolean isPlaying = false;

    @FXML
    public void initialize() {
        Loader.load(opencv_java.class);
        sliderValueLabel.textProperty().bind(Bindings.format("%.0f", thresholdSlider.valueProperty()));
    }

    @Override
    public void close() {
        if (playThread != null) {
            playThread.interrupt();
        }

        if (isPlaying) {
            stopCapturing();
        }
    }

    private static class PlaybackTimer {
        private Long startTime = -1L;

        public void start() {
            startTime = System.nanoTime();
        }

        public long elapsedMicros() {
            if (startTime < 0) {
                throw new IllegalStateException("PlaybackTimer not initialized.");
            }

            return (System.nanoTime() - startTime) / 1000;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());

    @FXML
    public void buttonOnAction() {
        if (isPlaying) {
            isPlaying = false;
            return;
        }

        startCapturing();
    }

    private void stopCapturing() {
        LOGGER.info("Stopping capturing...");

        isPlaying = false;
        startButton.setText("Start");
    }

    private void startCapturing() {
        startButton.setText("Stop");
        playThread = new Thread(() -> {
            try {
                final FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(App.VIDEO_FILE);
                grabber.start();

                LOGGER.info("Capturing started...");
                isPlaying = true;

                final PlaybackTimer playbackTimer;

                playbackTimer = new PlaybackTimer();
                final JavaFXFrameConverter converter = new JavaFXFrameConverter();
                final ObjectTracker tracker = new ObjectTracker();

                final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

                final long maxReadAheadBufferMicros = 1000 * 1000L;

                long lastTimeStamp = -1L;
                while (isPlaying && !Thread.interrupted()) {
                    final Frame frame = grabber.grab();
                    if (frame == null) {
                        break;
                    }

                    if (lastTimeStamp < 0) {
                        playbackTimer.start();
                    }
                    lastTimeStamp = frame.timestamp;

                    if (frame.image != null) {
                        final Frame imageFrame = frame.clone();
                        final Frame binaryFrame = tracker.track(imageFrame, thresholdSlider.getValue());

                        final Image image = converter.convert(imageFrame);
                        final Image thresholdImage = converter.convert(binaryFrame);

                        imageExecutor.submit(() -> {
                            final long timeStampDeltaMicros = imageFrame.timestamp - playbackTimer.elapsedMicros();

                            imageFrame.close();
                            binaryFrame.close();

                            if (timeStampDeltaMicros > 0) {
                                final long delayMillis = timeStampDeltaMicros / 1000L;
                                try {
                                    Thread.sleep(delayMillis);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            Platform.runLater(() -> {
                                cameraView.setImage(image);
                                thresholdView.setImage(thresholdImage);
                            });
                        });
                    }

                    final long timeStampDeltaMicros = frame.timestamp - playbackTimer.elapsedMicros();
                    if (timeStampDeltaMicros > maxReadAheadBufferMicros) {
                        Thread.sleep((timeStampDeltaMicros - maxReadAheadBufferMicros) / 1000);
                    }
                }

                if (!Thread.interrupted()) {
                    long delay = (lastTimeStamp - playbackTimer.elapsedMicros()) / 1000 +
                            Math.round(1 / grabber.getFrameRate() * 1000);
                    Thread.sleep(Math.max(0, delay));
                }
                grabber.stop();
                grabber.release();

                imageExecutor.shutdownNow();
                imageExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, String.format("Exception occured: %s", e.getMessage()), e);
            } finally {
                Platform.runLater(this::stopCapturing);
            }
        });

        playThread.start();
    }
}
