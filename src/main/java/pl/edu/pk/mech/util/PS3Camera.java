package pl.edu.pk.mech.util;

import com.thomasdiewald.ps3eye.PS3Eye;
import org.bytedeco.javacv.Frame;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.opencv.core.CvType.CV_8UC3;
import static pl.edu.pk.mech.tracking.ITracker.MAT_FRAME_CONVERTER;

public class PS3Camera {
    private static final Logger LOGGER = Logger.getLogger(PS3Camera.class.getName());
    private final PS3Eye ps3eye;
    private PS3Camera.FrameRate framerate;

    private static final Map<String, PS3Eye> devicesMap = Arrays.stream(PS3Eye.getDevices())
            .collect(Collectors.toMap(device -> String.format("PS3 Eye USB%d", device.getUSBPortNumber()),
                    device -> device));

    public PS3Camera() {
        ps3eye = PS3Eye.getDevice();
        ps3eye.init(60, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
    }

    public PS3Camera(final int index) {
        ps3eye = PS3Eye.getDevice(index);
        ps3eye.init(60, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
    }

    public PS3Camera(final int index, final int framerate) {
        ps3eye = PS3Eye.getDevice(index);
        ps3eye.init(framerate, PS3Eye.Resolution.VGA, PS3Eye.Format.RGB);
    }

    public void start() {
        ps3eye.start();
        framerate = new FrameRate();
    }

    public Frame grab() {
        int frame_w = ps3eye.getResolution().w;
        int frame_h = ps3eye.getResolution().h;
        BufferedImage frame = new BufferedImage(frame_w, frame_h, BufferedImage.TYPE_3BYTE_BGR);
        byte[] pixels = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
        ps3eye.getFrame(pixels);
        framerate.update();
        Mat finalImage = new Mat(frame_h, frame_w, CV_8UC3);
        finalImage.put(0, 0, pixels);

        return MAT_FRAME_CONVERTER.convert(finalImage);
    }

    public void stop() {
        ps3eye.stop();
    }

    public void release() {
        ps3eye.release();
    }

    public static String[] getDevicesNames() {
        return devicesMap.keySet().toArray(String[]::new);
    }

    private Mat BufferedImageToMat(final BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        Mat out = new Mat(height, width, CV_8UC3);
        byte[] data = new byte[height * width * (int) out.elemSize()];
        int[] dataBuff = image.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < dataBuff.length; i++) {
            data[i * 3 + 2] = (byte) ((dataBuff[i] >> 16) & 0xFF);
            data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
            data[i * 3] = (byte) ((dataBuff[i]) & 0xFF);
        }

        return out;
    }

    public double getFrameRate() {
        return framerate.fps();
    }

    static class FrameRate {
        private final int numTimers = 30;
        private final long[] timerHistory = new long[numTimers];
        float smooth = 0.95f; // [0,1]
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
            framerate = framerate * smooth + framerateCur * (1.0f - smooth);

            return this;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "%5.2f", framerate);
        }

    }
}
