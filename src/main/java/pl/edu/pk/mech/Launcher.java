package pl.edu.pk.mech;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import pl.edu.pk.mech.gui.App;
import pl.edu.pk.mech.udp.UDPServer;

import java.net.SocketException;
import java.net.UnknownHostException;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_PANIC;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

public class Launcher {
    public static void main(String[] args) {
        Loader.load(opencv_java.class);
        av_log_set_level(AV_LOG_PANIC);
        UDPServer.init();
        App.main(args);
    }
}
