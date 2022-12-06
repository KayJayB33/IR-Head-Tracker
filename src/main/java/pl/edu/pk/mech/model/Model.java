package pl.edu.pk.mech.model;

import org.opencv.core.Point3;

import java.util.List;

public class Model {

    private static volatile Model model;
    private final Point3 topPoint = new Point3(-165.f, 0.f, 145.f); // #1
    private final Point3 leftPoint = new Point3(0.f, -80.f, 0.f); // #2
    private final Point3 rightPoint = new Point3(0.f, 80.f, 0.f); // #3
    private final Point3 dummyPoint = new Point3(-165.f / 3, 0.f / 3, 145.f / 3); // #4
    private final List<Point3> pointsList = List.of(topPoint, leftPoint, rightPoint, dummyPoint);

    private Model() {
        if(model != null) {
            throw new RuntimeException("Not allowed. Please use getInstance() method");
        }
    }

    public static Model getInstance() {
        if (model == null) {
            model = new Model();
            return model;
        }

        return model;
    }

    public void setModelDims(final double halfWidth, final double height, final double depth) {
        topPoint.set(new double[]{depth, 0.f, height});
        leftPoint.set(new double[]{0.f, -halfWidth, 0.f});
        rightPoint.set(new double[]{0.f, halfWidth, 0.f});
    }

    public List<Point3> getPointsList() {
        return pointsList;
    }
}
