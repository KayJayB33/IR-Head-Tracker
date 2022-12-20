package pl.edu.pk.mech.tracking;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import pl.edu.pk.mech.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opencv.imgproc.Imgproc.line;

public class HeadPoseEstimator {
    private static final MatOfPoint3f model = new MatOfPoint3f(Model.getInstance().getPoints());
    private static final MatOfPoint3f axis = new MatOfPoint3f(
            new Point3(0, 0, 0),
            new Point3(50, 0, 0),
            new Point3(0, 50, 0),
            new Point3(0, 0, 50));
    private static final Mat[] lastSolution = new Mat[2];
    private static final Logger LOGGER = Logger.getLogger(HeadPoseEstimator.class.getName());

    public static void estimate(final Mat src, final List<Point> points, final int fov) {
        assert points.size() == 3;

        points.sort(Comparator.comparingDouble(p -> p.y));
        points.subList(1, points.size()).sort(Comparator.comparingDouble(p -> p.x));

        final Point dummy2DPoint = new Point(
                (points.get(0).x + points.get(1).x + points.get(2).x) / 3,
                (points.get(0).y + points.get(1).y + points.get(2).y) / 3);

        final MatOfPoint2f matOfImagePoints = new MatOfPoint2f();
        matOfImagePoints.fromList(Stream.concat(
                points.stream(),
                Stream.of(dummy2DPoint)).collect(Collectors.toList()));

        float focalLength = (float) (src.cols() / 2. / Math.tan(fov / 2.));
        final Mat camMat = getCameraMatrix(focalLength, new Size(src.cols() / 2., src.rows() / 2.));
        final MatOfDouble distCoeff = new MatOfDouble();

        final List<Mat> rotationsList = new ArrayList<>();
        final List<Mat> translationsList = new ArrayList<>();

        // Solving PnP
        int solutionsCount = Calib3d.solveP3P(
                model,
                matOfImagePoints,
                camMat,
                distCoeff,
                rotationsList,
                translationsList,
                Calib3d.SOLVEPNP_P3P);

        if (solutionsCount == 0) {
            LOGGER.info("No solution found! Skipping...");
            return;
        }

        // Saving last found solution
        if (lastSolution[0] == null) {
            lastSolution[0] = rotationsList.get(0);
            lastSolution[1] = translationsList.get(0);
        } else {
            // Saving nearest solution to the last one
            final List<Double> errors = new ArrayList<>(rotationsList.size());
            for (final Mat mat : rotationsList) {
                for (int j = 0; j < lastSolution[0].get(0, 0).length; j++) {
                    errors.add(Math.abs(lastSolution[0].get(0, 0)[j] - mat.get(0, 0)[j]));
                }
            }

            int index = errors.indexOf(Collections.min(errors));

            lastSolution[0] = rotationsList.get(index);
            lastSolution[1] = translationsList.get(index);
        }

        // Projecting axis to image for line drawing
        final MatOfPoint2f projectedAxis = new MatOfPoint2f();
        Calib3d.projectPoints(axis, lastSolution[0], lastSolution[1], camMat, distCoeff, projectedAxis);

        drawAxis(src, projectedAxis.toList());
    }

    private static Mat getCameraMatrix(final float focalLength, final Size center) {
        final double[] data = {focalLength, 0, center.width, 0, focalLength, center.height, 0, 0, 1f};
        final Mat mat = new Mat(3, 3, CvType.CV_64F);
        mat.put(0, 0, data);
        return mat;
    }

    private static void drawAxis(final Mat src, final List<Point> endPoints) {
        final Point startPoint = endPoints.get(0);
        line(src, startPoint, endPoints.get(3), ITracker.BLUE_COLOR, 3);
        line(src, startPoint, endPoints.get(2), ITracker.GREEN_COLOR, 3);
        line(src, startPoint, endPoints.get(1), ITracker.RED_COLOR, 3);
    }
}
