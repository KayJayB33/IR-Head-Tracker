package pl.edu.pk.mech.tracking;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;
import pl.edu.pk.mech.gui.controller.MainWindowController;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DemoTrackingThread extends TrackingThread {

    private static final Logger LOGGER = Logger.getLogger(DemoTrackingThread.class.getName());
    private static final File DEMO_VIDEO = new File("./res/ExampleVideo.mp4");
    private static final int FOV = 56;
    private final File videoFile;

    public DemoTrackingThread(final MainWindowController controller, final ITracker tracker, final File videoFile) {
        super(controller, tracker);
        this.videoFile = videoFile;
    }

    public DemoTrackingThread(final MainWindowController controller, final ITracker tracker) {
        super(controller, tracker);
        this.videoFile = DEMO_VIDEO;
    }

    @Override
    public void run() {
        Platform.runLater(controller::updateInterface);
        // Playing video in a loop
        do {
            try (final FrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
                 final JavaFXFrameConverter converter = new JavaFXFrameConverter()) {

                grabber.start();

                LOGGER.info("Capturing started...");
                isPlaying = true;

                final PlaybackTimer playbackTimer;

                playbackTimer = new PlaybackTimer();

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
                                (float) controller.getMaxRadiusValue(),
                                FOV);

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
                                controller.updateViews(image, thresholdImage);
                                controller.updateDetectedAmount(tracker.getDetectedAmount());
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
                LOGGER.log(Level.SEVERE, String.format("Exception occurred: %s", e.getMessage()), e);
                stopCapturing();
            }
            LOGGER.info("Capturing ended...");

            if (isPlaying) {
                LOGGER.info("Playing in a loop...");
            }
        } while (isPlaying);
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
}
