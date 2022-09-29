package pl.edu.pk.mech;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ObjectTracker {

    private static final Logger LOGGER = Logger.getLogger(ObjectTracker.class.getName());

    private static final OpenCVFrameConverter.ToOrgOpenCvCoreMat converter = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

    public Frame track(final Frame frame, final double thresholdVal) {
        final Mat src = converter.convert(frame);
        //Converting the source image to binary
        final Mat gray = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        final Mat binary = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        Imgproc.threshold(gray, binary, thresholdVal, 255, Imgproc.THRESH_BINARY);
        //Finding Contours
        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE);
        //Drawing the Contours
        final Scalar color = new Scalar(0, 0, 255);
//        Imgproc.drawContours(src, contours, -1, color, 2, Imgproc.LINE_8,
//                hierarchy, 2, new Point());

        // Drawing cross in the centers of the contours
        for (final MatOfPoint contour : contours) {

            final Moments moments = Imgproc.moments(contour);
            final double cX = moments.get_m10() / moments.get_m00();
            final double cY = moments.get_m01() / moments.get_m00();

            //Imgproc.circle(src, new Point(cX, cY), 7, color,-1);
            drawCross(src, cX, cY, color);

            Imgproc.putText(src, "center", new Point(cX - 40, cY - 40),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
        }

        // Converting image to 3 channels for JavaFX
        Imgproc.cvtColor(binary, binary, Imgproc.COLOR_GRAY2BGR);

        return converter.convert(binary);
    }

    private void drawCross(final Mat image, final double cX, final double cY, final Scalar color) {
        Imgproc.line(image, new Point(cX - 30 / 2., cY), new Point(cX + 30 / 2., cY), color, 2);
        Imgproc.line(image, new Point(cX, cY - 30 / 2.), new Point(cX, cY + 30 / 2.), color, 2);
    }

}
