package pl.edu.pk.mech;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Main {
    public static void main(String[] args) {
        nu.pattern.OpenCV.loadLocally();
        Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println("mat = " + mat.dump());
    }
}