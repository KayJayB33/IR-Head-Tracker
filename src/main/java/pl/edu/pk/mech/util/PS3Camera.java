package pl.edu.pk.mech.util;

import com.thomasdiewald.ps3eye.PS3Eye;
import org.bytedeco.javacv.Frame;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opencv.core.CvType.CV_8UC3;
import static pl.edu.pk.mech.tracking.ITracker.MAT_FRAME_CONVERTER;

public class PS3Camera {

    private final PS3Eye ps3eye;
    private PS3Camera.FrameRate framerate;

    private static final Map<String, PS3Eye> devicesMap = Arrays.stream(PS3Eye.getDevices())
            .collect(Collectors.toMap(device -> String.format("PS3 Eye USB%d", device.getUSBPortNumber()),
                    device -> device));
    private int frameWidth;
    private int frameHeight;
    private byte[] pixels;

    public PS3Camera() {
        ps3eye = PS3Eye.getDevice();
        ps3eye.init(60, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
    }

    public PS3Camera(final String name) {
        ps3eye = devicesMap.get(name);
        ps3eye.init(60, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
    }

    public PS3Camera(final String name, final int framerate) {
        ps3eye = devicesMap.get(name);
        ps3eye.init(framerate, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
    }

    public static void disposeAll() {
        PS3Eye.disposeAll();
    }

    public void start() {
        ps3eye.start();
        frameWidth = ps3eye.getResolution().w;
        frameHeight = ps3eye.getResolution().h;
        BufferedImage frame = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_3BYTE_BGR);
        pixels = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
        framerate = new FrameRate();
    }

    public void stop() {
        ps3eye.stop();
    }

    public Frame grab() {
        ps3eye.getFrame(pixels);
        framerate.update();
        Mat finalImage = new Mat(frameHeight, frameWidth, CV_8UC3);
        finalImage.put(0, 0, pixels);

        return MAT_FRAME_CONVERTER.convert(finalImage);
    }

    public static String[] getDevicesNames() {
        return devicesMap.keySet().toArray(String[]::new);
    }

    public double getFrameRate() {
        return framerate.fps();
    }

    static class FrameRate {
        private final int numTimers = 30;
        private final long[] timerHistory = new long[numTimers];
        private long count;
        private float framerate = 0;
        private int timerIdx = 0;

        public FrameRate() {
        }

        public float fps() {
            return framerate;
        }

        public long counter() {
            return count;
        }

        public FrameRate update() {
            int idxCur = timerIdx % numTimers;
            int idxOld = (timerIdx + numTimers + 1) % numTimers;

            timerHistory[idxCur] = System.nanoTime();
            timerIdx++;
            count++;

            long duration = timerHistory[idxCur] - timerHistory[idxOld];

            float framerateCur = numTimers / (duration / 1E09f);
            // [0,1]
            float smooth = 0.95f;
            framerate = framerate * smooth + framerateCur * (1.0f - smooth);

            return this;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "%5.2f", framerate);
        }

    }
}
