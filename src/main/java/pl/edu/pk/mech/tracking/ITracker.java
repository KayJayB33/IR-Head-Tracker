package pl.edu.pk.mech.tracking;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import static org.opencv.imgproc.Imgproc.line;

public interface ITracker {

    Scalar BLUE_COLOR = new Scalar(255, 0, 0);
    Scalar GREEN_COLOR = new Scalar(0, 255, 0);
    Scalar RED_COLOR = new Scalar(0, 0, 255);
    OpenCVFrameConverter.ToOrgOpenCvCoreMat MAT_FRAME_CONVERTER
            = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

    Frame track(Frame frame, float threshold, float minRadius, float maxRadius);

    int getDetectedAmount();

    static void drawCross(final Mat image, final double cX, final double cY) {
        line(image, new Point(cX - 30 / 2., cY), new Point(cX + 30 / 2., cY), RED_COLOR, 2);
        line(image, new Point(cX, cY - 30 / 2.), new Point(cX, cY + 30 / 2.), RED_COLOR, 2);
    }
}
