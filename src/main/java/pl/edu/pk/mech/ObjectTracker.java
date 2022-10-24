package pl.edu.pk.mech;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.opencv.imgproc.Imgproc.*;

public class ObjectTracker {

    private static final float FOV = 56f;
    private static final Scalar RED_COLOR = new Scalar(0, 0, 255);
    private static final OpenCVFrameConverter.ToOrgOpenCvCoreMat converter
            = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

    private final List<Point> points = new ArrayList<>();
    private final MatOfPoint3f model = new MatOfPoint3f(
            new Point3(-165, 9, 145), // TOP #1
            new Point3(0, -80, 0), // LEFT #2
            new Point3(0, 80, 0), // RIGHT #3
            new Point3(0, 0, 0) // dummy between #2 and #3
    );

    private static final Logger LOGGER = Logger.getLogger(ObjectTracker.class.getName());


    public Frame track(final Frame frame, final float thresholdVal, final float minRadius, final float maxRadius) {
        // Clearing previous points
        points.clear();

        // Converting Frame to Matrix
        final Mat src = converter.convert(frame);

        // Converting the source image to binary
        final Mat gray = new Mat(src.rows(), src.cols(), src.type());
        cvtColor(src, gray, COLOR_BGR2GRAY);
        final Mat binary = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        threshold(gray, binary, thresholdVal, 255, THRESH_BINARY);

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

        if(points.size() == 3) {
            estimateHeadPose(frame);
        }

        for (int i = 0; i < points.size(); i++) {
            final Point point = points.get(i);
            final float radius = (float) Math.sqrt(contourArea(contours.get(i)) / Math.PI);
            drawCross(src, point.x, point.y);
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
        return converter.convert(binary);
    }

    public int getDetectedAmount() { return points.size(); }

    private static void drawCross(final Mat image, final double cX, final double cY) {
        line(image, new Point(cX - 30 / 2., cY), new Point(cX + 30 / 2., cY), RED_COLOR, 2);
        line(image, new Point(cX, cY - 30 / 2.), new Point(cX, cY + 30 / 2.), RED_COLOR, 2);
    }

    private void estimateHeadPose(final Frame frame) {
        // Sorting points (assuming there are only 3)
        points.sort(Comparator.comparingDouble(p -> p.y));
        points.subList(1, points.size()).sort(Comparator.comparingDouble(p -> p.x));

        final Point dummy2DPoint = new Point(
                (points.get(1).x + points.get(2).x) / 2,
                (points.get(1).y + points.get(2).y) / 2
        );

        final MatOfPoint2f matOfImagePoints = new MatOfPoint2f();
        matOfImagePoints.fromList(Stream.concat(points.stream(), Stream.of(dummy2DPoint)).toList());

        float fc = (float) (frame.imageWidth / 2. / Math.tan(FOV / 2.));
        final Mat camMat = cameraMatrix(fc, new Size(frame.imageWidth / 2., frame.imageHeight / 2.));
        final MatOfDouble coeff = new MatOfDouble(); // dummy

        final Mat rvec = new MatOfFloat();
        final Mat tvec = new MatOfFloat();

        Calib3d.solvePnP(model, matOfImagePoints, camMat, coeff, rvec, tvec);

        LOGGER.info(String.format("tvec:\n%s", tvec.dump()));
        LOGGER.info(String.format("rvec:\n%s", rvec.dump()));
    }

    private static Mat cameraMatrix(float f, Size center) {
        final double[] data = { f, 0, center.width, 0, f, center.height, 0, 0, 1f };
        Mat m = new Mat(3, 3, CvType.CV_64F);
        m.put(0, 0, data);
        return m;
    }
}
