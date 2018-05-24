import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class testRWLock implements Runnable {
	private static int currentWriters = 0;
	private static int currentReaders = 0;

	private final FairReadWriteLock rwLock;
	private static AtomicInteger wrongValues = new AtomicInteger(0);
	private static ReentrantLock concurrentLock = new ReentrantLock();
	private static Condition empty = concurrentLock.newCondition();
	private static AtomicInteger currentSize = new AtomicInteger(0);
	public testRWLock(FairReadWriteLock rwLock) {
		this.rwLock = rwLock;
		
	}
	
	public static void main(String argc[]) {
		ExecutorService pool = Executors.newCachedThreadPool();
		FairReadWriteLock rwLock = new FairReadWriteLock();
		
		for (int i = 0; i < 200; ++i) {
			pool.submit(new testRWLock(rwLock));
		}
		pool.shutdown();
		while (!pool.isTerminated()){}
	}

	public void run() {
		Random generator = new Random();
		Random rwRandom = new Random();
		int readOrWrite = rwRandom.nextInt(100);
		
		int number = generator.nextInt(20);
		try{
			// Reader
			if (readOrWrite % 2 == 0){
				for (int i = 0; i < number; ++i){
					System.out.println("Attempting Reading: ");
					rwLock.beginRead();
					System.out.println("Reading: ");
					Thread.sleep(new Random().nextInt(100));
					System.out.println("End Read: ");
					rwLock.endRead();
					
				}
					
			}
			//Writer
			else {
					System.out.println("Attempting Write: ");
					rwLock.beginWrite();
					System.out.println("Writing: ");
					Thread.sleep(new Random().nextInt(100));
					System.out.println("End Write: ");
					rwLock.endWrite();
					
			}
			
		}
		catch (Exception e){
			e.printStackTrace(System.out);
		}
			
		
	}
}
