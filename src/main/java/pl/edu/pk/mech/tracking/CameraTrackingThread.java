package pl.edu.pk.mech.tracking;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import pl.edu.pk.mech.gui.controller.MainWindowController;
import pl.edu.pk.mech.util.PS3Camera;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CameraTrackingThread extends TrackingThread {

    private static final Logger LOGGER = Logger.getLogger(CameraTrackingThread.class.getName());
    private final String cameraName;

    public CameraTrackingThread(final MainWindowController controller, final ITracker tracker, final String cameraName) {
        super(controller, tracker);
        this.cameraName = cameraName;
    }

    @Override
    public void run() {
        Platform.runLater(controller::updateInterface);
        try (final JavaFXFrameConverter converter = new JavaFXFrameConverter()) {
            PS3Camera camera = new PS3Camera(cameraName);
            camera.start();

            LOGGER.info("Capturing started...");
            isPlaying = true;

            final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

            while (isPlaying && !Thread.interrupted()) {
                final Frame frame = camera.grab();
                if (frame == null) {
                    break;
                }

                if (frame.image != null) {
                    final Frame imageFrame = frame.clone();
                    final Frame binaryFrame = tracker.track(
                            imageFrame,
                            (float) controller.getThresholdValue(),
                            (float) controller.getMinRadiusValue(),
                            (float) controller.getMaxRadiusValue(),
                            camera.getFov().getValue());

                    final Image image = converter.convert(imageFrame);
                    final Image thresholdImage = converter.convert(binaryFrame);

                    imageExecutor.submit(() -> {
                        imageFrame.close();
                        binaryFrame.close();

                        Platform.runLater(() -> {
                            controller.updateViews(image, thresholdImage);
                            controller.updateDetectedAmount(tracker.getDetectedAmount());
                        });
                    });
                }
            }

            camera.stop();
            // grabber.release() - not releasing USB device

            imageExecutor.shutdownNow();
            imageExecutor.awaitTermination(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Exception occurred: %s", e.getMessage()), e);
            stopCapturing();
        }
        LOGGER.info("Capturing ended...");
    }

    @Override
    public void stopCapturing() {
        LOGGER.info("Stopping thread...");
        super.stopCapturing();
    }
}
