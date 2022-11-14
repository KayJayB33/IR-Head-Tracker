package pl.edu.pk.mech;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

public class Launcher {
    public static void main(String[] args) {
        Loader.load(opencv_java.class);
        App.main(args);
    }
}
