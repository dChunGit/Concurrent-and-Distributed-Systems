
    public class WriterThread implements Runnable{

        boolean run;
        String toWrite;
        String[] current;
        StringRegister sr;
        FairReadWriteLock l;
        public static class StringRegister{
            String s = "";
            public StringRegister(){

            }
            public void write(String s)
            {
                this.s = s;
            }
            public String read()
            {
                return s;
            }
        }
        public WriterThread(String w, boolean run, StringRegister sr, FairReadWriteLock l)
        {
            toWrite = "Thread" + Thread.currentThread().getId();
            this.run = run;
            this.sr = sr;
            this.l = l;
        }
        @Override
        public void run() {
            toWrite = "Thread" + Thread.currentThread().getId();
            int randomWait = (int) (Math.random() * 100);
            try {
                Thread.sleep(randomWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(run)
            {
                //System.out.println("I am reading " + Thread.currentThread().getId());
                l.beginRead();
                System.out.println(sr.read());
                l.endRead();
            }
            else
            {
                //System.out.println("I am writing " + toWrite + " " + Thread.currentThread().getId());
                l.beginWrite();
                sr.write(toWrite);
                l.endWrite();
            }

        }
        public static void main (String[] args)
        {
            int SIZE = 20;
            FairReadWriteLock lock = new FairReadWriteLock();
            StringRegister sr = new StringRegister();
            Thread[] t = new Thread[SIZE];

            for (int i = 0; i < SIZE; ++i) {
                if(i % 2 == 0)
                 t[i] = new Thread(new WriterThread("Test" + i,false, sr, lock));
                else
                    t[i] = new Thread(new WriterThread("Test" + i,true, sr, lock));
            }

            for (int i = 0; i < SIZE; ++i) {
                t[i].start();
            }

        }
    }

