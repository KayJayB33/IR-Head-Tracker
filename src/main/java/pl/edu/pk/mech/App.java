package pl.edu.pk.mech;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;

public class App {
    private static final File VIDEO = new File("Example_Videos\\Example_video.mp4");

    public static void main(String[] args) throws FrameGrabber.Exception {
        FrameGrabber grabber = FFmpegFrameGrabber.createDefault(VIDEO);
        grabber.setFrameRate(60);
        grabber.setFormat("mp4");
        grabber.start();

        final CanvasFrame cFrame = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());

        Frame capturedFrame = null;

        // While we are capturing...
        while ((capturedFrame = grabber.grab()) != null)
        {
            if (cFrame.isVisible())
            {
                // Show our frame in the preview
                cFrame.showImage(capturedFrame);
            }
        }

        cFrame.dispose();
        grabber.stop();
    }
}