package pl.edu.pk.mech.model;

import org.opencv.core.Point3;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Model {

    private static volatile Model model;
    private final Point3 topPoint;
    private final Point3 leftPoint;
    private final Point3 rightPoint;
    private final Point3 dummyPoint;
    private final List<Point3> pointsList;
    private double halfWidth;
    private double depth;
    private double height;

    private Model() {
        if (model != null) {
            throw new RuntimeException("Not allowed. Please use getInstance() method");
        }

        halfWidth = 80.;
        depth = 165.;
        height = 145.;

        topPoint = new Point3(0., 0., height); // #1
        leftPoint = new Point3(depth, -halfWidth, 0.); // #2
        rightPoint = new Point3(depth, halfWidth, 0.); // #3
        dummyPoint = new Point3(getDummyPointCoords(topPoint, leftPoint, rightPoint)); // #4

        pointsList = List.of(topPoint, leftPoint, rightPoint, dummyPoint);
    }

    public static Model getInstance() {
        if (model == null) {
            model = new Model();
            return model;
        }

        return model;
    }

    public void setModelDims(final double halfWidth, final double height, final double depth) {
        this.halfWidth = halfWidth;
        this.height = height;
        this.depth = depth;

        topPoint.set(new double[]{0.f, 0.f, height});
        leftPoint.set(new double[]{depth, -halfWidth, 0.f});
        rightPoint.set(new double[]{depth, halfWidth, 0.f});
        dummyPoint.set(getDummyPointCoords());
    }

    public double getHalfWidth() {
        return halfWidth;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public Point3[] getPoints() {
        return pointsList.toArray(Point3[]::new);
    }

    private double[] getDummyPointCoords(final Point3... points) {
        final double averageX = Arrays.stream(points).collect(Collectors.averagingDouble(p -> p.x));
        final double averageY = Arrays.stream(points).collect(Collectors.averagingDouble(p -> p.y));
        final double averageZ = Arrays.stream(points).collect(Collectors.averagingDouble(p -> p.z));

        return new double[]{averageX, averageY, averageZ};
    }
}
