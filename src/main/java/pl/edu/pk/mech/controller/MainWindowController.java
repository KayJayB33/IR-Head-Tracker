package pl.edu.pk.mech.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
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
    private Button startButton;
    @FXML
    private ImageView cameraImageView;
    @FXML
    private ImageView maskImageView;

    private volatile Thread playThread;
    private volatile boolean isPlaying = false;

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
            stopCapturing();
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
                        final Frame maskedFrame = tracker.track(frame);

                        imageExecutor.submit(() -> {
                            final Image image = converter.convert(imageFrame);
                            //final Image maskImage = converter.convert(maskedFrame); // doesn't work
                            final long timeStampDeltaMicros = imageFrame.timestamp - playbackTimer.elapsedMicros();
                            imageFrame.close();
                            maskedFrame.close();
                            if (timeStampDeltaMicros > 0) {
                                final long delayMillis = timeStampDeltaMicros / 1000L;
                                try {
                                    Thread.sleep(delayMillis);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            Platform.runLater(() -> {
                                LOGGER.info("Changing image views...");
                                cameraImageView.setImage(image);
                                maskImageView.setImage(image);
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
                isPlaying = false;
                LOGGER.info("Capturing ended.");
            }
        });

        playThread.start();
    }
}
