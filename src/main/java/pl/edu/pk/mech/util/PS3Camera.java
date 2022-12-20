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

/**
 * Helper class for PS3Eye.
 */
public class PS3Camera {

    private static final Map<String, PS3Eye> devicesMap = Arrays.stream(PS3Eye.getDevices())
            .collect(Collectors.toMap(device -> String.format("PS3 Eye USB%d", device.getUSBPortNumber()),
                    device -> device));


    static {
        for (PS3Eye ps3eye : PS3Eye.getDevices()) {
            ps3eye.init(60, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
        }
    }

    private final PS3Eye ps3eye;
    private PS3Camera.FrameRate framerate;
    private int frameWidth;
    private int frameHeight;
    private byte[] pixels;
    private FOV fov;

    public PS3Camera(final String name) {
        ps3eye = devicesMap.get(name);
        fov = FOV.RED_DOT;
    }

    public static void disposeAll() {
        PS3Eye.disposeAll();
    }

    public static String[] getDevicesNames() {
        return devicesMap.keySet().toArray(String[]::new);
    }

    public FOV getFov() {
        return fov;
    }

    public void setFov(final FOV fov) {
        this.fov = fov;
    }

    public void setFlipH(final boolean flip) {
        ps3eye.setFlip(flip, getVerticalFlip());
    }

    public void setFlipV(final boolean flip) {
        ps3eye.setFlip(getHorizontalFlip(), flip);
    }

    public int getGain() {
        return ps3eye.getGain();
    }

    public void setGain(final int value) {
        ps3eye.setGain(value);
    }

    public int getExposure() {
        return ps3eye.getExposure();
    }

    public void setExposure(final int value) {
        ps3eye.setExposure(value);
    }

    public int getSharpness() {
        return ps3eye.getSharpness();
    }

    public void setSharpness(final int value) {
        ps3eye.setSharpness(value);
    }

    public int getHue() {
        return ps3eye.getHue();
    }

    public void setHue(final int value) {
        ps3eye.setHue(value);
    }

    public int getBrightness() {
        return ps3eye.getBrightness();
    }

    public void setBrightness(final int value) {
        ps3eye.setBrightness(value);
    }

    public int getContrast() {
        return ps3eye.getContrast();
    }

    public void setContrast(final int value) {
        ps3eye.setContrast(value);
    }

    public int getRedBalance() {
        return ps3eye.getRedBalance();
    }

    public void setRedBalance(final int value) {
        ps3eye.setRedBalance(value);
    }

    public int getGreenBalance() {
        return ps3eye.getGreenBalance();
    }

    public void setGreenBalance(final int value) {
        ps3eye.setGreenBalance(value);
    }

    public int getBlueBalance() {
        return ps3eye.getBlueBalance();
    }

    public void setBlueBalance(final int value) {
        ps3eye.setBlueBalance(value);
    }

    public boolean getAutogain() {
        return ps3eye.getAutogain();
    }

    public void setAutogain(final boolean value) {
        ps3eye.setAutogain(value);
    }

    public boolean getAutoWhiteBalance() {
        return ps3eye.getAutoWhiteBalance();
    }

    public void setAutoWhiteBalance(final boolean value) {
        ps3eye.setAutoWhiteBalance(value);
    }

    public boolean getHorizontalFlip() {
        return ps3eye.getFlipH();
    }

    public boolean getVerticalFlip() {
        return ps3eye.getFlipV();
    }

    public PS3Camera.VideoMode getVideoMode() {
        return VideoMode.getVideoMode(ps3eye.getResolution(), ps3eye.getFramerate());
    }

    public void setVideoMode(final PS3Camera.VideoMode videomode) {
        ps3eye.init(videomode.getFramerate(), videomode.getResolution(), PS3Eye.Format.RGB);
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

    public double getFrameRate() {
        return framerate.fps();
    }

    public enum FOV {
        RED_DOT(56),
        BLUE_DOT(75);

        private final int fov;

        FOV(int fov) {
            this.fov = fov;
        }

        public int getValue() {
            return fov;
        }

        @Override
        public String toString() {
            final String format = "%d\u00B0 (%s)";
            return switch (this) {
                case RED_DOT -> format.formatted(fov, "red dot");
                case BLUE_DOT -> format.formatted(fov, "blue dot");
            };
        }
    }

    public enum VideoMode {
        VGA_30(PS3Eye.Resolution.VGA, 30),
        VGA_60(PS3Eye.Resolution.VGA, 60),
        VGA_75(PS3Eye.Resolution.VGA, 75),
        QVGA_30(PS3Eye.Resolution.QVGA, 30),
        QVGA_60(PS3Eye.Resolution.QVGA, 60),
        QVGA_187(PS3Eye.Resolution.QVGA, 187);

        private final PS3Eye.Resolution colorMode;
        private final int framerate;

        VideoMode(final PS3Eye.Resolution colorMode, final int framerate) {
            this.colorMode = colorMode;
            this.framerate = framerate;
        }

        static VideoMode getVideoMode(final PS3Eye.Resolution resolution, final int framerate) {
            final String format = String.format("%s_%d", resolution, framerate);
            return VideoMode.valueOf(format);
        }

        public int getFramerate() {
            return framerate;
        }

        public PS3Eye.Resolution getResolution() {
            return colorMode;
        }

        @Override
        public String toString() {
            return "%s %d fps".formatted(this.colorMode, this.framerate);
        }
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
