package pl.edu.pk.mech;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ObjectTracker {

    private static final Logger LOGGER = Logger.getLogger(ObjectTracker.class.getName());

    private static final OpenCVFrameConverter.ToOrgOpenCvCoreMat converter = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

    public void track(Frame frame, double thresholdVal) {
        final Mat src = converter.convert(frame);
        //Converting the source image to binary
        final Mat gray = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        final Mat binary = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        Imgproc.threshold(gray, binary, thresholdVal, 255, Imgproc.THRESH_BINARY_INV);
        //Finding Contours
        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE);
        //Drawing the Contours
        final Scalar color = new Scalar(0, 0, 255);
        Imgproc.drawContours(src, contours, -1, color, 2, Imgproc.LINE_8,
                hierarchy, 2, new Point());
    }

}
