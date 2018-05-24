import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPRunnable implements Runnable{
    int port;
    String ipAddress;

    public TCPRunnable(int port, String ipAddress) {
        this.port = port;
        this.ipAddress = ipAddress;
    }

    @Override
    public void run() {
        try {
            while(true) {
                ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getByName(ipAddress));
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String query = in.readLine();
                System.out.println(query);
                String result = Utils.processRequest(query);
                out.println(result);
                out.flush();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
