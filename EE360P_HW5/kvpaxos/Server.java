package kvpaxos;
import paxos.Paxos;
import paxos.State;
// You are allowed to call Paxos.Status to check if agreement was made.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements KVPaxosRMI {

    ReentrantLock mutex;
    Registry registry;
    Paxos px;
    int me;

    String[] servers;
    int[] ports;
    KVPaxosRMI stub;

    // Your definitions here
    HashMap<String, Integer> data;

    public Server(String[] servers, int[] ports, int me){
        this.me = me;
        this.servers = servers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.px = new Paxos(me, servers, ports);
        // Your initialization code here
        data = new HashMap<>();


        try{
            System.setProperty("java.rmi.server.hostname", this.servers[this.me]);
            registry = LocateRegistry.getRegistry(this.ports[this.me]);
            stub = (KVPaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("KVPaxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    // RMI handlers
    public Response Get(Request req){
        // Your code here
        if(!req.heard_rpc.contains(req.op.ClientSeq)) {
            //enter into paxos log
            req.op.value = data.get(req.op.key);
            px.Start(req.op.ClientSeq, req.op);
            Op op = wait(req.op.ClientSeq);
            //interpret response to reflect puts
            px.Done(req.op.ClientSeq);

            return new Response(op.key, op.value, req.op.ClientSeq);
        } else {
            return new Response(req.op.key, data.get(req.op.key), req.op.ClientSeq);
        }

    }

    public Response Put(Request req){
        // Your code here
        if(!req.heard_rpc.contains(req.op.ClientSeq)) {
            px.Start(req.op.ClientSeq, req.op);
            Op op = wait(req.op.ClientSeq);
            mutex.lock();
            data.put(op.key, op.value);
            mutex.unlock();
            px.Done(req.op.ClientSeq);

            return new Response(op.key, op.value, req.op.ClientSeq);
        } else {
            return new Response(req.op.key, data.get(req.op.key), req.op.ClientSeq);
        }
    }

    public Op wait(int seq) {
        int to = 10;
        while(true) {
            Paxos.retStatus ret = this.px.Status(seq);
            if(ret.state == State.Decided) {
                return Op.class.cast(ret.v);
            }
            try {
                Thread.sleep(to);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(to < 1000) {
                to = to * 2;
            }
        }
    }
}
