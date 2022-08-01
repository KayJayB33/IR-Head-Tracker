package pl.edu.pk.mech;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.swing.WindowConstants;
import java.io.File;

public class App {
    private static final File VIDEO = new File("Example_Videos\\Example_video.mp4");

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(VIDEO);
        grabber.start();

        CanvasFrame canvas = new CanvasFrame("Preview Video", 1);
        canvas.setCanvasSize(grabber.getImageWidth(), grabber.getImageHeight());
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        long delay = Math.round(1000d / grabber.getFrameRate());

        // Read frame by frame, stop early if the display window is closed
        Frame frame;
        while ((frame = grabber.grabImage()) != null && canvas.isVisible()) {
            // Capture and show the frame
            canvas.showImage(frame);
            // Delay
            Thread.sleep(delay);
        }

        canvas.dispose();
        grabber.release();
    }
}