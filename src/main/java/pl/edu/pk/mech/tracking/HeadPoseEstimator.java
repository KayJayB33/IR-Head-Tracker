package pl.edu.pk.mech.tracking;

import org.bytedeco.javacv.Frame;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class HeadPoseEstimator {

    private static final float FOV = 56f;
    private static final MatOfPoint3f model = new MatOfPoint3f(
            new Point3(-165, 9, 145), // TOP #1
            new Point3(0, -80, 0), // LEFT #2
            new Point3(0, 80, 0), // RIGHT #3
            new Point3(0, 0, 0) // dummy between #2 and #3
    );

    private static Logger LOGGER = Logger.getLogger(HeadPoseEstimator.class.getName());

    public static void estimate(final Frame frame, final List<Point> points) {
        assert points.size() == 3;

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

    private static Mat cameraMatrix(final float f, final Size center) {
        final double[] data = {f, 0, center.width, 0, f, center.height, 0, 0, 1f};
        Mat m = new Mat(3, 3, CvType.CV_64F);
        m.put(0, 0, data);
        return m;
    }
}
