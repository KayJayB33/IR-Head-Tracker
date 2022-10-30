package pl.edu.pk.mech.tracking;

import org.bytedeco.javacv.Frame;
import org.opencv.core.*;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.features2d.SimpleBlobDetector_Params;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;

public class BlobTracker implements ITracker {

    private static final SimpleBlobDetector_Params params = new SimpleBlobDetector_Params();

    static {
        params.set_filterByArea(true);
        params.set_maxThreshold(256); // maxThreshold is exclusive value
        params.set_filterByColor(true);
    }

    private SimpleBlobDetector detector = SimpleBlobDetector.create(params);
    private List<KeyPoint> keypointsList;

    private static final Logger LOGGER = Logger.getLogger(BlobTracker.class.getName());

    @Override
    public Frame track(final Frame frame, final float threshold, final float minRadius, final float maxRadius) {
        if (newParamsDetected(threshold, minRadius, maxRadius)) {
            detector = SimpleBlobDetector.create(params);
        }

        // Converting Frame to Matrix
        final Mat src = CONVERTER.convert(frame);

        // Converting source image to grayscale
        final Mat gray = new Mat(src.rows(), src.cols(), src.type());
        cvtColor(src, gray, COLOR_BGR2GRAY);
        Core.bitwise_not(gray, gray);

        // Binary image for visualisation
        final Mat binary = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        threshold(gray, binary, threshold, 255, THRESH_BINARY);

        // Detecting keypoints
        final MatOfKeyPoint keypoints = new MatOfKeyPoint();
        detector.detect(gray, keypoints);
        keypointsList = keypoints.toList();

        // Passing points from keypoints to HeadPoseEstimator
        final List<Point> detectedPoints = keypointsList
                .stream()
                .map(keyPoint -> keyPoint.pt)
                .collect(Collectors.toList());

        if (keypointsList.size() == 3) {
            HeadPoseEstimator.estimate(src, detectedPoints);
            // Sorting keypoints (assuming there are only 3)
            keypointsList.sort(Comparator.comparingDouble(kp -> kp.pt.y));
            keypointsList.subList(1, keypointsList.size()).sort(Comparator.comparingDouble(kp -> kp.pt.x));
        }

        for (int i = 0; i < keypointsList.size(); i++) {
            final KeyPoint keypoint = keypointsList.get(i);
            final Point point = keypoint.pt;
            final float radius = keypoint.size / 2;
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
    public int getDetectedAmount() {
        return keypointsList.size();
    }

    private boolean newParamsDetected(final float threshold, final float minRadius, final float maxRadius) {
        boolean updateDetector = false;

        if (Float.compare(params.get_minThreshold(), threshold) != 0) {
            LOGGER.info(String.format("minThreshold - old %.2f : new %.2f", params.get_minThreshold(), threshold));
            params.set_minThreshold(threshold);
            updateDetector = true;
        }

        final float minArea = (float) (Math.PI * Math.pow(minRadius, 2));
        if (Float.compare(params.get_minArea(), minArea) != 0) {
            LOGGER.info(String.format("minArea - old %.2f : new %.2f", params.get_minArea(), minArea));
            params.set_minArea(minArea);
            updateDetector = true;
        }

        final float maxArea = (float) (Math.PI * Math.pow(maxRadius, 2) + 0.01); // Area filter is in range [minArea, maxArea)
        if (Float.compare(params.get_maxArea(), maxArea) != 0) {
            LOGGER.info(String.format("maxArea - old %.2f : new %.2f", params.get_maxArea(), maxArea));
            params.set_maxArea(maxArea);
            updateDetector = true;
        }

        return updateDetector;
    }
}
