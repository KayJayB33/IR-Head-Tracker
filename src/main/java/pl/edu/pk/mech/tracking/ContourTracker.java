package pl.edu.pk.mech.tracking;

import org.bytedeco.javacv.Frame;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.opencv.imgproc.Imgproc.*;

public class ContourTracker implements ITracker {

    private List<Point> points = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(ContourTracker.class.getName());

    @Override
    public Frame track(final Frame frame, final float threshold, final float minRadius, final float maxRadius) {
        // Clearing previous points
        points.clear();

        // Converting Frame to Matrix
        final Mat src = CONVERTER.convert(frame);

        // Converting the source image to grayscale and thresholding
        final Mat gray = new Mat(src.rows(), src.cols(), src.type());
        cvtColor(src, gray, COLOR_BGR2GRAY);
        final Mat binary = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        threshold(gray, binary, threshold, 255, THRESH_BINARY);

        // Finding Contours
        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        findContours(binary, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);

        // Filtering contours
        contours.removeIf(contour -> {
            final float radius = (float) Math.sqrt(contourArea(contour) / Math.PI);
            return radius < minRadius || radius > maxRadius;
        });

        // Drawing cross in the centers of the contours
        for (final MatOfPoint contour : contours) {

            // Finding centroids of each contour
            final Moments moments = moments(contour);
            final float cX = (float) (moments.get_m10() / moments.get_m00());
            final float cY = (float) (moments.get_m01() / moments.get_m00());

            points.add(new Point(cX, cY));
        }

        if (points.size() == 3) {
            HeadPoseEstimator.estimate(frame, points);
        }

        for (int i = 0; i < points.size(); i++) {
            final Point point = points.get(i);
            final float radius = (float) Math.sqrt(contourArea(contours.get(i)) / Math.PI);
            ITracker.drawCross(src, point.x, point.y);
            putText(src,
                    String.format("#%d %.1fpx", i + 1, radius),
                    new Point(point.x - 40, point.y - 40),
                    FONT_HERSHEY_SIMPLEX,
                    0.8,
                    RED_COLOR,
                    2);
        }

        // Converting image to 3 channels for JavaFX
        cvtColor(binary, binary, COLOR_GRAY2BGR);
        return CONVERTER.convert(binary);
    }

    @Override
    public int getDetectedAmount() { return points.size(); }
}
