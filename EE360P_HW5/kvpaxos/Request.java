package kvpaxos;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Please fill in the data structure you use to represent the request message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 * Hint: Easier to make each variable public
 */
public class Request implements Serializable {
    static final long serialVersionUID=11L;
    // Your data here
    Op op;
    ArrayList<Integer> heard_rpc;

    public ArrayList<Integer> getHeard_rpc() {
        return heard_rpc;
    }

    public void setHeard_rpc(ArrayList<Integer> heard_rpc) {
        this.heard_rpc = heard_rpc;
    }

    public Op getOp() {
        return op;
    }

    public void setOp(Op op) {
        this.op = op;
    }

    public Request(Op op, ArrayList<Integer> heard_rpc) {
        this.op = op;
        this.heard_rpc = heard_rpc;
    }
// Your constructor and methods here

}
