package pl.edu.pk.mech.tracking;

import org.opencv.core.*;
import pl.edu.pk.mech.model.Model;
import pl.edu.pk.mech.udp.UDPServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opencv.calib3d.Calib3d.*;
import static org.opencv.imgproc.Imgproc.line;

public class HeadPoseEstimator {
    private static final MatOfPoint3f modelMatrix = new MatOfPoint3f(Model.getInstance().getPoints());
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

        final MatOfPoint2f imagePointsMatrix = new MatOfPoint2f();
        imagePointsMatrix.fromList(Stream.concat(
                points.stream(),
                Stream.of(dummy2DPoint)).collect(Collectors.toList())
        );

        float focalLengthX = (float) (0.5 * src.cols() / Math.tan(0.5 * fov * Math.PI / 180));
        float focalLengthY = (float) (0.5 * src.rows() / Math.tan(0.5 * fov * src.rows() / src.cols() * Math.PI / 180));
        final Mat cameraMatrix = getCameraMatrix(focalLengthX, focalLengthY, src.cols() / 2f, src.rows() / 2f);
        final MatOfDouble distortionCoefficientMatrix = new MatOfDouble();

        final List<Mat> rotations = new ArrayList<>();
        final List<Mat> translations = new ArrayList<>();

        // Solving PnP
        int solutionsCount = solveP3P(
                modelMatrix,
                imagePointsMatrix,
                cameraMatrix,
                distortionCoefficientMatrix,
                rotations,
                translations,
                SOLVEPNP_P3P);

        if (solutionsCount == 0) {
            LOGGER.info("No solution found! Skipping...");
            return;
        }

        // Saving last found solution
        if (lastSolution[0] == null) {
            lastSolution[0] = rotations.get(0);
            lastSolution[1] = translations.get(0);
        } else {
            // Saving nearest solution to the last one
            final List<Double> errors = new ArrayList<>(rotations.size());
            for (final Mat mat : rotations) {
                for (int j = 0; j < lastSolution[0].get(0, 0).length; j++) {
                    errors.add(Math.abs(lastSolution[0].get(0, 0)[j] - mat.get(0, 0)[j]));
                }
            }

            int index = errors.indexOf(Collections.min(errors));

            lastSolution[0] = rotations.get(index);
            lastSolution[1] = translations.get(index);
        }

        UDPServer.sendVector(
                (float)lastSolution[1].get(2, 0)[0],
                (float)lastSolution[1].get(1, 0)[0],
                (float)lastSolution[1].get(0, 0)[0]
        );

        // Projecting axis to image for line drawing
        final MatOfPoint2f projectedAxis = new MatOfPoint2f();
        projectPoints(axis, lastSolution[0], lastSolution[1], cameraMatrix, distortionCoefficientMatrix, projectedAxis);

        drawAxis(src, projectedAxis.toList());
    }

    private static Mat getCameraMatrix(final float focalLengthX, final float focalLengthY, final float width,
                                       final float height) {
        final double[] data = {
                focalLengthX, 0f, width,
                0f, focalLengthY, height,
                0f, 0f, 1f
        };
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
