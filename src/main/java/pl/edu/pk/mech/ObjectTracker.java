package pl.edu.pk.mech;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opencv.imgproc.Imgproc.*;

public class ObjectTracker {

    private static final Scalar RED_COLOR = new Scalar(0, 0, 255);
    private static final OpenCVFrameConverter.ToOrgOpenCvCoreMat converter
            = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();
    private final List<Point2f> points = Stream.generate(Point2f::new).limit(3).collect(Collectors.toList());

    public Frame track(final Frame frame, final float thresholdVal) {
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
        findContours(binary, contours, hierarchy, RETR_TREE,
                CHAIN_APPROX_SIMPLE);

        if (contours.size() == 3) {

            // Drawing cross in the centers of the contours
            for (int i = 0; i < 3; i++) {
                final MatOfPoint contour = contours.get(i);

                final Moments moments = moments(contour);
                final float cX = (float) (moments.get_m10() / moments.get_m00());
                final float cY = (float) (moments.get_m01() / moments.get_m00());

                points.get(i).x(cX).y(cY);
            }

            points.sort((p1, p2) -> Float.compare(p1.y(), p2.y()));
            points.subList(1, points.size()).sort((p1, p2) -> Float.compare(p1.x(), p2.x()));

            for (int i = 0; i < points.size(); i++) {
                final Point2f point = points.get(i);
                //circle(src, new Point(cX, cY), 7, color,-1);
                drawCross(src, point.x(), point.y());
                putText(src, String.format("#%d %.1fpx", i+1, contourArea(contours.get(i))), new Point(point.x() - 40, point.y() - 40),
                        FONT_HERSHEY_SIMPLEX, 0.8, RED_COLOR, 2);
            }
        }

        // Converting image to 3 channels for JavaFX
        cvtColor(binary, binary, COLOR_GRAY2BGR);
        drawContours(binary, contours, -1, RED_COLOR, 2, LINE_4,
                hierarchy, 2, new Point());

        return converter.convert(binary);
    }

    private static void drawCross(final Mat image, final double cX, final double cY) {
        line(image, new Point(cX - 30 / 2., cY), new Point(cX + 30 / 2., cY), RED_COLOR, 2);
        line(image, new Point(cX, cY - 30 / 2.), new Point(cX, cY + 30 / 2.), RED_COLOR, 2);
    }

}
