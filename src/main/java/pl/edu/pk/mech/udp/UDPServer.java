package pl.edu.pk.mech.udp;

import java.io.IOException;
import java.net.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class UDPServer {
    static final int PORT = 8080;
    static DatagramSocket socket;
    static InetAddress address;

    public static void init() {
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName("localhost");
            System.out.println("INIT TEST");
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void sendVector(float x, float y, float z) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(3 * Float.BYTES);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(z);
            byte[] data = buffer.array();

            DatagramPacket packet = new DatagramPacket(data, data.length, address, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
