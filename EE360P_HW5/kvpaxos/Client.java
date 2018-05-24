package kvpaxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Client {
    String[] servers;
    int[] ports;

    // Your data here
    static int ID = 0;
    int my_id;
    ReentrantLock lock;
    ArrayList<Integer> sent_log;
    ArrayList<Integer> heard_log;

    public Client(String[] servers, int[] ports){
        this.servers = servers;
        this.ports = ports;
        // Your initialization code here
        lock = new ReentrantLock();
        heard_log = new ArrayList<>();
        sent_log = new ArrayList<>();
        lock.lock();
        my_id = ID;
        ID++;
        lock.unlock();
    }

    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id){
        Response callReply = null;
        KVPaxosRMI stub;
        try{
            Registry registry= LocateRegistry.getRegistry(this.ports[id]);
            stub=(KVPaxosRMI) registry.lookup("KVPaxos");
            if(rmi.equals("Get"))
                callReply = stub.Get(req);
            else if(rmi.equals("Put")){
                callReply = stub.Put(req);}
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }

    // RMI handlers
    public Integer Get(String key){
        // Your code here
        lock.lock();
        my_id = ID;
        ID++;
        lock.unlock();
        Op getOp = new Op("Get", my_id, key, 0);
        Request request = new Request(getOp, heard_log);
        heard_log.clear();
        Response response = null;
        sent_log.add(my_id);

        int id = 0;
        while(response == null && id < ports.length) {
            response = Call("Get", request, id);
            id++;
        }

        if(response != null) {
            heard_log.add(my_id);
            //ID = response.newSeq + 1;
            return response.value_agreed;
        } else return 0;

    }

    public boolean Put(String key, Integer value){
        // Your code here
        lock.lock();
        my_id = ID;
        ID++;
        lock.unlock();
        Op putOp = new Op("Put", my_id, key, value);
        Request request = new Request(putOp, heard_log);
        heard_log.clear();
        Response response = null;
        sent_log.add(my_id);

        int id = 0;
        while(response == null && id < ports.length) {
            response = Call("Put", request, id);
            id++;
        }

        if(response != null) {
            heard_log.add(my_id);
            //ID = response.newSeq + 1;
            return true;
        } else {
            return false;
        }

    }

}
