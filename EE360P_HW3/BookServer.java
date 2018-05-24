import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BookServer {
    //Holds current inventory -> <Book name, # in inventory>
    static ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();
    //Holds current checked out records, <RecordId, {book, student}>
    static ConcurrentHashMap<Integer, String[]> checkedRecords = new ConcurrentHashMap<>();
    static ArrayList<String> bookOrder = new ArrayList<>();

    static AtomicInteger recordID = new AtomicInteger(0);

    static HashMap<String, Integer> assignedPorts = new HashMap<String, Integer>();

    static HashMap<String, ClientHandler> assignedHandler = new HashMap<>();
    public static void main (String[] args) {
        String hostAddress;
        int tcpPort;
        int udpPort;

        if (args.length != 1) {
            System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
            System.exit(-1);
        }
        String fileName = args[0];
        hostAddress = "localhost";
        tcpPort = 7000;
        udpPort = 8000;

        // parse the inventory file
        try {
            Scanner sc = new Scanner(new FileReader(fileName));

            while (sc.hasNextLine()) {
                String cmd = sc.nextLine();
                String[] book = cmd.split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                bookOrder.add(book[0]);
                inventory.put(book[0], Integer.valueOf(book[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BookServer bs = new BookServer();
        bs.SetUpNetworking(tcpPort, udpPort);


    }

    private void SetUpNetworking(int tcpPort, int udpPort) {
        try {
            DatagramSocket ds = new DatagramSocket(udpPort);
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while(true)
            {
                //try to read from ds
                try {

                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    ds.receive(receivePacket);
                    String sentence = new String(receivePacket.getData());
                    String[] tokens = sentence.split(" ");
                    String clientID = tokens[0];
                    String prot = tokens[1];
                    System.out.println("Debug: " + clientID + " wants to connect.");

                    boolean tcp = false;
                    if(prot.contains("TCP"))
                        tcp = true;
                    //if it isn't assigned create a new handler thread, otherwise only send the response
                    if(!assignedPorts.containsKey(clientID)) {
                        assignedHandler.put(clientID, new ClientHandler(clientID, getRandomPort(clientID), tcp));
                        Thread client = new Thread(assignedHandler.get(clientID));
                        client.start();
                    }
                    else if(assignedHandler.get(clientID).getTCP() != tcp)
                    {
                            int port = assignedHandler.get(clientID).agreedPort;
                            if(assignedHandler.get(clientID).TCP) {
                                assignedHandler.get(clientID).clientSocket.close();
                                assignedHandler.get(clientID).ss.close();
                            }
                            else
                                assignedHandler.get(clientID).ds.close();

                            assignedHandler.get(clientID).stop();
                            assignedHandler.put(clientID, new ClientHandler(clientID, port, tcp));
                            Thread client = new Thread(assignedHandler.get(clientID));
                            client.start();

                    }
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();
                    String response = "" + clientID + " configured " + assignedPorts.get(clientID);
                    sendData = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                    ds.send(sendPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    //assigns a random port between 7001 and 7999
    private static int getRandomPort(String clientID) {
        boolean uniqueYet = true;
        int port = 0;
        while(uniqueYet) {
            port = (int) (Math.random() * 998) + 7001;
            if(!assignedPorts.containsValue(port) && isPortAvailable(port)) {
                uniqueYet = false;
                assignedPorts.put(clientID, port);
            }
        }
        return port;
    }

    protected static boolean isPortAvailable(int port) {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
            socket.close();
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    public static int getRecordID()
    {
        return recordID.addAndGet(1);
    }

    //handles the client
    private class ClientHandler implements Runnable{
        String clientID;
        int agreedPort;
        boolean TCP = false; //true for TCP
        boolean oldTCP;
        boolean first = true;
        volatile boolean running = true;
        DatagramSocket ds;
        Socket clientSocket;
        ServerSocket ss;
        public ClientHandler(String cID, int port, boolean tcp)
        {
            clientID = cID;
            agreedPort = port;
            TCP = tcp;
            if(!tcp) {
                    try {
                    ds = new DatagramSocket(port);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            while(running) {
                if (TCP)
                    handleTCP();
                else
                    handleUDP();
            }
        }

        public void handleTCP()
        {
            if(first)
            {
                first = false;
                try {
                    ss = new ServerSocket(agreedPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            BufferedReader in;
            PrintWriter out;
            try {
                if(ss.isClosed() || ss == null) {
                    ss = new ServerSocket(agreedPort);
                }
                clientSocket = ss.accept();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String message;
                if ((message = in.readLine()) != null) {
                    String[] tokens = message.split(" ");
                    System.out.println(message);
                    String response = "";

                    if(tokens[0].equals("s")) {
                        if (tokens[1].equalsIgnoreCase("T")) {
                            response = "configured";
                            oldTCP = TCP;
                            TCP = true;
                        } else {
                            response = "configured";
                            oldTCP = TCP;
                            TCP = false;
                        }
                    } else {
                        response = Utils.processRequest(message);
                    }
                   // String result = Utils.processRequest(message);
                    out.println(response);
                    out.flush();
                    System.out.println(response);

                }
                //clientSocket.close();
            } catch(IOException e){
                e.printStackTrace();
            }


        }

        public void handleUDP()
        {

                //DatagramSocket ds = new DatagramSocket(agreedPort);
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
                    //try to read from ds
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        ds.setSoTimeout(10000);
                        ds.receive(receivePacket);
                        String cmd = new String(receivePacket.getData()).trim();
                        System.out.println(cmd);
                        String[] tokens = cmd.split(" ");
                        String response = "";

                        if(tokens[0].equals("s")) {
                            if (tokens[1].equalsIgnoreCase("T")) {
                                response = "configured";
                                oldTCP = TCP;
                                TCP = true;
                                first = true;
                            } else {
                                response = "configured";
                                oldTCP = TCP;
                                TCP = false;
                            }
                        } else {
                            response = Utils.processRequest(cmd);
                        }
                        InetAddress IPAddress = receivePacket.getAddress();
                        int port = receivePacket.getPort();
                        sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                        ds.send(sendPacket);
                        System.out.println(response);
                    } catch (IOException e) {
                        running = false;
                    }
        }

        public boolean getTCP()
        {
            return TCP;
        }

        public void stop()
        {
            running = false;
        }


    }
}
