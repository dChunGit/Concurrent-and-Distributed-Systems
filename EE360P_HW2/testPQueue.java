import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class testPQueue implements Runnable {
	final static int QUE_SIZE = 250;
	private final PQueue myPQueue;
	private final PriorityQueue studentPQueue; 
	private static AtomicInteger wrongValues = new AtomicInteger(0);
	private static ReentrantLock concurrentLock = new ReentrantLock();
	private static Condition empty = concurrentLock.newCondition();
	private static AtomicInteger currentSize = new AtomicInteger(0);
	public testPQueue(PQueue myQ, PriorityQueue studentQ) {
		this.myPQueue = myQ;
		this.studentPQueue = studentQ;
	}
	
	public static void main(String argc[]) {
		ExecutorService pool = Executors.newCachedThreadPool();
		PQueue myQ = new PQueue(QUE_SIZE);
		PriorityQueue sQ = new PriorityQueue(QUE_SIZE);
		
		for (int i = 0; i < 150; ++i) {
			pool.submit(new testPQueue(myQ, sQ));
		}
		pool.shutdown();
		while (!pool.isTerminated()){}
		pool = Executors.newCachedThreadPool();

		for (int i = 0; i < 150; ++i) {
			pool.submit(new testPQueue(myQ, sQ));
		}
		pool.shutdown();
		while (!pool.isTerminated()){}

		System.out.println();
		System.out.println("Difference: " + wrongValues.get());
	}

	public void run() {
		Random generator = new Random();

		int act = generator.nextInt(100);
		if (act < 35) {
			String name = Integer.toString(generator.nextInt(1000));
			int priority = generator.nextInt(10);
			try{
				concurrentLock.lock();
				//System.out.println("Adding");
				if (currentSize.get() == QUE_SIZE){
					concurrentLock.unlock();
					return;
				}

				int myAdd = myPQueue.insert(name , priority);
				int studentAdd = studentPQueue.add(name , priority);
				//System.out.println("Added");
                System.out.println(myPQueue.toString());
                System.out.println(studentPQueue.toString());
				if (myAdd != studentAdd) {
                    System.out.println("Add " + name + " " + priority + " " + myAdd + " " + studentAdd);
                    wrongValues.getAndIncrement();
                }
				concurrentLock.unlock();
			}
			catch (Exception e){
				e.printStackTrace();
			}

		} else if (act < 60) {

			try{
				concurrentLock.lock();
				//System.out.println("getFirst");
				if (currentSize.get() == 0){
					concurrentLock.unlock();
					return;
				}

				String myName = myPQueue.getFirst();
				String sName = studentPQueue.getFirst();
				//System.out.println("getFirstDone");
                System.out.println("Name " + myName + " " + sName);
				if (!sName.equals(myName)) {
                    wrongValues.getAndIncrement();
                }

				concurrentLock.unlock();
			}
			catch (Exception e){
				e.printStackTrace();
			}

		} else {
			String name = Integer.toString(generator.nextInt(1000));
			try {
				concurrentLock.lock();
				//System.out.println("Searching");
				int myIndex = myPQueue.search(name);
				int sIndex = studentPQueue.search(name);
				//System.out.println("Searching Done");
				if (myIndex != sIndex) {
                    System.out.println("Search " + myIndex + " " + sIndex);
                    wrongValues.getAndIncrement();
                }
				concurrentLock.unlock();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			

		}
	}
}
