package pl.edu.pk.mech.tracking;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import static org.opencv.imgproc.Imgproc.circle;

public interface ITracker {

    Scalar BLUE_COLOR = new Scalar(255, 0, 0);
    Scalar GREEN_COLOR = new Scalar(0, 255, 0);
    Scalar RED_COLOR = new Scalar(0, 0, 255);
    OpenCVFrameConverter.ToOrgOpenCvCoreMat MAT_FRAME_CONVERTER
            = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

    Frame track(Frame frame, float threshold, float minRadius, float maxRadius, int fov);

    int getDetectedAmount();

    static void drawCircle(final Mat image, final double cX, final double cY, final double radius) {
        circle(image, new Point(cX, cY), (int) radius + 6, RED_COLOR, 2);
    }
}
