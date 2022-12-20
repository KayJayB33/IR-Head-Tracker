package pl.edu.pk.mech.util;

import com.thomasdiewald.ps3eye.PS3Eye;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class CameraPreviewThread extends Thread {
    private final BufferedImage frame;
    private final PS3Eye ps3eye;
    private final ImageView preview;
    private boolean isPlaying = false;

    public CameraPreviewThread(final PS3Camera camera, final ImageView imageView) {
        ps3eye = camera.ps3eye;
        preview = imageView;
        int frameWidth = ps3eye.getResolution().w;
        int frameHeight = ps3eye.getResolution().h;

        frame = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void run() {
        int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
        isPlaying = true;
        ps3eye.start();
        while (isPlaying) {
            ps3eye.getFrame(pixels);
            preview.setImage(SwingFXUtils.toFXImage(frame, null));
        }
    }

    public void stopCapturing() {
        isPlaying = false;
        ps3eye.stop();
    }
}
