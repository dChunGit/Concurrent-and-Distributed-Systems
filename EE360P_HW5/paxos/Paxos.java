package paxos;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Paxos implements PaxosRMI, Runnable{

    ReentrantLock mutex;
    String[] peers; // hostname
    int[] ports; // host port
    int[] peerDone;
    int me; // index into peers[]


    Registry registry;
    PaxosRMI stub;

    AtomicBoolean dead;// for testing
    AtomicBoolean unreliable;// for testing

    // Your data here
    //boolean amLeader;
    int currentLeader;
   //int currentSequence;

   int lowestCompletedSequence;

   // int highestProposal;
    int highestAccepted;
    Object acceptedValue;
    int highestProposalSeen = 1;
    HashMap<Integer, Integer> acceptedProposal; //<sequence, proposal number>
    HashMap<Integer, Object> acceptedValues; //<sequence, value>
    HashMap<Integer, Integer> highestProposal; //<sequence, number>
    HashMap<Integer, Boolean> finishedSequence;


    int handlingSeq;
    Object proposedVal;
    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports){

        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);

        // Your initialization code here
        //amLeader = false;
        peerDone = new int[peers.length];
        for(int i = 0; i < peers.length; i++)
        {
            peerDone[i] = -1;
        }
        //currentSequence = -1;
        acceptedValues = new HashMap<>();
        acceptedProposal = new HashMap<>();
        highestProposal = new HashMap<>();
        acceptedValue = null;
        highestAccepted = -1;
        finishedSequence = new HashMap<>();
        lowestCompletedSequence = -1;


        // register peers, do not modify this part
        try{
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
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

        PaxosRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(PaxosRMI) registry.lookup("Paxos");
            if(rmi.equals("Prepare"))
                callReply = stub.Prepare(req);
            else if(rmi.equals("Accept"))
                callReply = stub.Accept(req);
            else if(rmi.equals("Decide"))
                callReply = stub.Decide(req);
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }


    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(int seq, Object value){
        // Your code here
        mutex.lock();
        handlingSeq = seq;
        proposedVal = value;
        Thread paxos = new Thread(this);
        paxos.run();
        mutex.unlock();
    }

    @Override
    public void run(){
        //Your code here



        //Phase 1. Proposer
        mutex.lock();
        //System.out.println("Starting " + handlingSeq);
        if(finishedSequence.get(handlingSeq) == null)
            finishedSequence.put(handlingSeq, false);
        while(finishedSequence.get(handlingSeq) != true) {
            if(me == 0)
                System.out.println("I am 0");
            Response[] responses = new Response[peers.length];
            int responseCount = 0;


            if(highestProposal.get(handlingSeq) == null)
                highestProposal.put(handlingSeq, 0);

            int myProposal = highestProposal.get(handlingSeq) + 1;
            Request r = new Request(handlingSeq, myProposal, proposedVal, me);
            for (int i = 0; i < peers.length; i++) {
                if(i == me)
                {
                    responses[i] = Prepare(r);
                }
                else
                    responses[i] = Call("Prepare", r, i);
            }

            for(int i = 0; i < peers.length; i++)
            {
                if(responses[i] != null)
                {
                    int high = responses[i].proposalNumber;
                    if(high > highestProposal.get(handlingSeq))
                        highestProposal.put(handlingSeq, high);
                }
                if(responses[i] != null && responses[i].ack)
                    responseCount++;
            }

            //Phase 2. Proposer
            Object sendVal = null;
            if (responseCount > peers.length / 2) {
                //majority received send accept
                int max = -1;
                //System.out.println("accept");
                boolean first = true;
                for (int i = 0; i < responses.length; i++) {
                    if (responses[i] != null && responses[i].ack) {
                        if (first) {
                            first = false;
                            max = i;
                        } else {
                            if (responses[i].proposalNumber > responses[max].proposalNumber) {
                                max = i;
                            }
                        }
                    }
                }
                if (max == -1 || responses[max].proposalNumber == -1)
                    sendVal = proposedVal;
                else
                    sendVal = responses[max].value;
                Request acceptRequest = new Request(handlingSeq, myProposal, sendVal, me);
                acceptRequest.setOther(peerDone[me]);

                Response[] responses2 = new Response[responses.length];
                for (int i = 0; i < responses.length; i++) {
                        if(i == me)
                        {
                            responses2[i] = Accept(acceptRequest);
                        }
                        else
                            responses2[i] = Call("Accept", acceptRequest, i);
                }

                //Check for majority accept
                int count = 0;
                for (int i = 0; i < responses.length; i++) {
                    if (responses2[i] != null && responses2[i].ack) {
                        count++;
                    }
                }
                if (count >= (peers.length / 2 + 1)) {
                    //Decide if majority accept
                    Request req = new Request(handlingSeq, myProposal, sendVal, me);
                    for (int i = 0; i < peers.length; i++)
                        if(i == me)
                        {
                            responses2[i] = Decide(req);
                        }
                        else
                            responses2[i] = Call("Decide", req, i);

                }


                System.out.println(handlingSeq + " finished");

                //return;
            }

        }
        mutex.unlock();

    }

    // RMI handler
    public Response Prepare(Request req){
        // your code here
        //System.out.println("prepare");
        int proposed = req.proposalNumber;
        int seq = req.seq;

        if(highestProposal.get(seq) == null || (highestProposal.get(seq) < proposed))
        {
            //if no other proposal or if our proposal is high numbered than send response
            highestProposal.put(seq, proposed);
            Response resp = new Response(true, -1, null);
            if(acceptedProposal.get(seq) != null)
            {
                resp = new Response(true, acceptedProposal.get(seq), acceptedValues.get(seq));
            }
            //System.out.println("test");
            return resp;

        }
        return new Response(false, highestProposal.get(seq), null);

    }

    public Response Accept(Request req){
        // your code here

        //update dones
        /*int tempMin = -1;
        boolean detectedNull = false;
        boolean first2 = true;
        for (int i = 0; i < responses.length; i++) {
            if(responses2[i] == null)
                detectedNull = true;
            if (responses2[i] != null && responses2[i].ack){
                if(first2)
                {
                    first2 = false;
                    tempMin = responses2[i].proposalNumber;
                }
                else if (responses2[i].proposalNumber < tempMin)
                    tempMin = responses[i].proposalNumber;
            }
        }

        if(!detectedNull)
            lowestCompletedSequence = tempMin;*/

        peerDone[req.id] = req.other;
        int tempMin = peerDone[0];
        for(int i = 0; i < peerDone.length; i++)
        {
            if(peerDone[i] < tempMin)
                tempMin = peerDone[i];
        }

        lowestCompletedSequence = tempMin;


        int seqProposal = 0;
        if(highestProposal.get(req.seq) != null)
            seqProposal = highestProposal.get(req.seq);
        //System.out.println(req.proposalNumber + ", " + seqProposal);
        if(req.proposalNumber >= seqProposal)
        {
            //System.out.println("accept");
            acceptedValues.put(req.seq, req.value);
            acceptedProposal.put(req.seq, req.proposalNumber);
            return new Response(true, req.proposalNumber, req.value);
        }
        return new Response(false, req.proposalNumber, req.value);
    }

    public Response Decide(Request req){
        // your code here

        //System.out.println("ID: " + me);
        //System.out.println("decide " + req.seq);
        acceptedValues.put(req.seq, req.value);
        acceptedProposal.put(req.seq, req.proposalNumber);
        finishedSequence.put(req.seq, true);
        //System.out.println(finishedSequence.get(req.seq) + " " + req.seq + " " + finishedSequence.size());

        //calculate last sequence finished
        int i = peerDone[me] + 1;
        boolean go = true;
        while(go)
        {
            if(finishedSequence.get(i) == null || finishedSequence.get(i) == false)
            {
                Done(i - 1);
                go = false;
            }
            else
                i++;
        }


        //don't know what response to send
        //System.out.println("Done "+  i);
        Response resp = new Response(true, peerDone[me], null);
        return resp;


    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        // Your code here
        peerDone[me] = seq;

    }


    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max(){
        // Your code here
        ArrayList<Integer> sequences = new ArrayList<Integer>(finishedSequence.keySet());
        sequences.sort(Integer::compareTo);
        return sequences.get(sequences.size() - 1);
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min(){
        // Your code here
        return lowestCompletedSequence + 1;


    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq){
        // Your code here
        if(seq < lowestCompletedSequence)
            return new retStatus(State.Forgotten, null);
        if(finishedSequence.get(seq) != null && finishedSequence.get(seq))
        {
            retStatus stat = new retStatus(State.Decided, acceptedValues.get(seq));
            return stat;
        }
        else
        {
            return new retStatus(State.Pending, null);
        }



    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public Object v;

        public retStatus(State state, Object v){
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill(){
        this.dead.getAndSet(true);
        if(this.registry != null){
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch(Exception e){
                System.out.println("None reference");
            }
        }
    }

    public boolean isDead(){
        return this.dead.get();
    }

    public void setUnreliable(){
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable(){
        return this.unreliable.get();
    }


}
