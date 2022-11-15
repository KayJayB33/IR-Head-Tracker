package pl.edu.pk.mech.util;

import com.thomasdiewald.ps3eye.PS3Eye;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PS3Camera {

    private static final Map<String, PS3Eye> devicesMap = Arrays.stream(PS3Eye.getDevices())
            .collect(Collectors.toMap(device -> String.format("PS3 Eye USB%d", device.getUSBPortNumber()),
                    device -> device));

    public PS3Camera() {
    }

    public static String[] getDevicesNames() {
        return devicesMap.keySet().toArray(String[]::new);
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
