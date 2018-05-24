import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class UDPRunnable implements Runnable {
    int port;

    public UDPRunnable(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        DatagramPacket datapacket, returnpacket;

        try {
            DatagramSocket datasocket = new DatagramSocket(port);
            byte[] buf = new byte[1024];
            while (true) {
                datapacket = new DatagramPacket(buf, buf.length);
                datasocket.receive(datapacket);
                String request = new String(datapacket.getData(), 0, datapacket.getLength());

                String parsed = Utils.processRequest(request);

                returnpacket = new DatagramPacket(
                        parsed.getBytes(),
                        parsed.getBytes().length,
                        datapacket.getAddress(),
                        datapacket.getPort());
                datasocket.send(returnpacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
