import java.net.*;
import java.util.Scanner;
import java.io.*;
import java.util.*;
public class BookClient {
    static Socket clientSocket;
    static boolean firstTCP = false;
    public static void main(String[] args) {
        String hostAddress;
        int tcpPort;
        int udpPort;
        int clientId;
        int mode = 1;
        int port;
        if (args.length != 2) {
            System.out.println("ERROR: Provide 2 arguments: commandFile, clientId");
            System.out.println("\t(1) <command-file>: file with commands to the server");
            System.out.println("\t(2) client id: an integer between 1..9");
            System.exit(-1);
        }

        String commandFile = args[0];
        clientId = Integer.parseInt(args[1]);
        hostAddress = "localhost";
        tcpPort = 7000;// hardcoded -- must match the server's tcp port
        udpPort = 8000;// hardcoded -- must match the server's udp port
        port = udpPort;
        String response = "";

        response = sendUDPPacket(hostAddress, udpPort, clientId + " " + "UDP");
        //System.out.println(response);
        port = Integer.valueOf(response.split(" ")[2]);

        try {
            Scanner sc = new Scanner(new FileReader(commandFile));

            while (sc.hasNextLine()) {
                String cmd = sc.nextLine();
                System.out.println(cmd);
                String[] tokens = cmd.split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

                if(tokens[0].equals("setmode")) {
                    if (tokens[1].equalsIgnoreCase("T")) {
                        mode = 0;
                    } else {
                        mode = 1;
                    }

                    if(mode == 0)
                    {
                        response = sendUDPPacket(hostAddress, udpPort, clientId + " " + "TCP");
                        firstTCP = true;
                    }
                    else {
                        response = sendUDPPacket(hostAddress, udpPort, clientId + " " + "UDP");
                    }

                } else if (tokens[0].equals("borrow")) {

                    response = sendMessage(mode, hostAddress, port, "b:" + tokens[1] + ":" + tokens[2] + ":" + clientId);

                } else if (tokens[0].equals("return")) {
                    response = sendMessage(mode, hostAddress, port, "r:" + tokens[1] + ":" + clientId);

                } else if (tokens[0].equals("inventory")) {
                    response = sendMessage(mode, hostAddress, port, "i:" + clientId);
                    StringBuilder sb = new StringBuilder();
                    String[] splitInventory = response.split(":");
                    for(int a = 0; a < splitInventory.length - 1; a++) {
                        sb.append(splitInventory[a] + "\n");
                    }
                    sb.append(splitInventory[splitInventory.length - 1]);
                    response = sb.toString();

                } else if (tokens[0].equals("list")) {
                    response = sendMessage(mode, hostAddress, port, "l:" + tokens[1] + ":" + clientId);

                } else if (tokens[0].equals("exit")) {
                    response = sendMessage(mode, hostAddress, port, "exit:" + clientId);

                } else {
                    System.out.println("ERROR: No such command");
                }

                try {
                    if(!response.equals("ERROR") && !response.equals("") && !response.contains("configured")) {
                        PrintWriter bw = new PrintWriter(new FileWriter("out_" + clientId + ".txt", true));
                        bw.println(response);
                        bw.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * sendMessage toggles between UDP and TCP based on client specifications
     * @param mode TCP or UDP current setting
     * @param hostAddress address for TCP lookup
     * @param port tcp/udp port to send on
     * @param message string request to send to server
     * @return String response from server
     */
    private static String sendMessage(int mode, String hostAddress, int port, String message) {
        switch(mode) {
            case 0: {
                return sendTCPPacket(hostAddress, port, message);
            }
            case 1: {
                return sendUDPPacket(hostAddress, port, message);
            }
            default: return "ERROR";
        }
    }

    /**
     * sendUDPPacket sends over UDP socket
     * @param hostAddress
     * @param port
     * @param message
     * @return String response
     */
    private static String sendUDPPacket(String hostAddress, int port, String message) {
        DatagramPacket sPacket, rPacket;
        byte[] rbuffer = new byte[1024];
        byte[] buffer;

        try {
            InetAddress ia = InetAddress.getByName(hostAddress);
            DatagramSocket datasocket = new DatagramSocket();

            buffer = message.getBytes();

            sPacket = new DatagramPacket(buffer, buffer.length, ia, port);
            rPacket = new DatagramPacket(rbuffer, rbuffer.length);
            datasocket.send(sPacket);
            datasocket.receive(rPacket);

            String retstring = new String(rPacket.getData(), 0,
                    rPacket.getLength());
            datasocket.close();
            return retstring;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "NORESP";
    }

    /**
     * sendTCPPacket sends over TCP socket
     * @param hostAddress
     * @param port
     * @param message
     * @return String response
     */
    private static String sendTCPPacket(String hostAddress, int port, String message) {
        try {
            if(firstTCP) {
                //System.out.println(port);
                boolean keepTrying = true;
                clientSocket = new Socket();
                while (keepTrying) {
                    try {
                        clientSocket = new Socket(hostAddress, port);
                        keepTrying = false;
                       // firstTCP = false;
                    } catch (ConnectException ce) {
                        int stall = 3000;
                        while (stall > 0)
                            stall--;
                    }
                }
            }
            PrintWriter outToServer = new PrintWriter(clientSocket.getOutputStream());
            outToServer.println(message);
            outToServer.flush();

            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String response = inFromServer.readLine();

            clientSocket.close();

            return response;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "NORESP";

    }
}
