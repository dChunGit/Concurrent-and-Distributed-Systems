package kvpaxos;

import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=22L;
    // your data here
    String key_agreed;
    Integer value_agreed;
    Integer newSeq;

    public Response(String key_agreed, Integer value_agreed, Integer newSeq) {
        this.key_agreed = key_agreed;
        this.value_agreed = value_agreed;
        this.newSeq = newSeq;
    }

    public Integer getNewSeq() {
        return newSeq;
    }

    public void setNewSeq(Integer newSeq) {
        this.newSeq = newSeq;
    }

    public String getKey_agreed() {
        return key_agreed;
    }

    public void setKey_agreed(String key_agreed) {
        this.key_agreed = key_agreed;
    }

    public Integer getValue_agreed() {
        return value_agreed;
    }

    public void setValue_agreed(Integer value_agreed) {
        this.value_agreed = value_agreed;
    }

    // Your constructor and methods here
}
