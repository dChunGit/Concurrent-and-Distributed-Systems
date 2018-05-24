public class PQThread implements Runnable {

    String write;
    boolean read;
    PriorityQueue pq;
    public PQThread(String s, boolean r, PriorityQueue p)
    {
        write = s;
        read = r;
        pq = p;
    }

    @Override
    public void run() {
        write = "Test" + Thread.currentThread().getId();
        int wait = (int) (Math.random() * 100);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(read)
        {
            System.out.println(Thread.currentThread().getId() + " wants to read");
            String read = pq.getFirst();
            //System.out.println(Thread.currentThread().getId() + " read " + read);
        }
        else {
            System.out.println(Thread.currentThread().getId() + " wants to write");
            pq.add(write, (int) Thread.currentThread().getId());
            //System.out.println(Thread.currentThread().getId() + " wrote " + write);
        }

    }
    public static void main (String[] args)
    {
        int SIZE = 20;
        FairReadWriteLock lock = new FairReadWriteLock();
        PriorityQueue pq = new PriorityQueue(SIZE);
        Thread[] t = new Thread[SIZE];

        for (int i = 0; i < SIZE; ++i) {
            if(i % 2 == 0)
                t[i] = new Thread(new PQThread("Test" + i,false, pq));
            else
                t[i] = new Thread(new PQThread("Test" + i,true, pq));
        }

        for (int i = 0; i < SIZE; ++i) {
            t[i].start();
        }

    }
}
