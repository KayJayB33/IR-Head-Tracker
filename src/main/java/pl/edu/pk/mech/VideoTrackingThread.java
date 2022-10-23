package pl.edu.pk.mech;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import pl.edu.pk.mech.controller.MainWindowController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoTrackingThread extends Thread {

    private final MainWindowController controller;
    private volatile boolean isPlaying = false;

    private static final Logger LOGGER = Logger.getLogger(VideoTrackingThread.class.getName());

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

    public VideoTrackingThread(final MainWindowController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try (final FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(App.VIDEO_FILE);
             final JavaFXFrameConverter converter = new JavaFXFrameConverter()) {

            grabber.start();

            LOGGER.info("Capturing started...");
            isPlaying = true;
            Platform.runLater(controller::updateButtonText);

            final PlaybackTimer playbackTimer;

            playbackTimer = new PlaybackTimer();

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
                    final Frame binaryFrame = tracker.track(
                            imageFrame,
                            (float) controller.getThresholdValue(),
                            (float) controller.getMinRadiusValue(),
                            (float) controller.getMaxRadiusValue());

                    final Image image = converter.convert(imageFrame);
                    final Image thresholdImage = converter.convert(binaryFrame);

                    Platform.runLater(() -> controller.updateDetectedAmount(tracker.getDetectedAmount()));

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

                        Platform.runLater(() -> controller.updateViews(image, thresholdImage));
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
            LOGGER.info("Stopping thread...");
            stopCapturing();
        }
    }

    public void stopCapturing() {
        Platform.runLater(controller::updateButtonText);
        isPlaying = false;
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
