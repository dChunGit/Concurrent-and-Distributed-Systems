/*
 * EID's of group members
 *
 */
import java.util.concurrent.Semaphore; // for implementation using Semaphores

public class ThreadSynch {
	private Semaphore sem;
	private boolean ready;
	private int part;
	public static Object lock = new Object();
	public ThreadSynch(int parties) {
		sem = new Semaphore(parties);
		ready = true;
		part = parties;
	}

	public int await() throws InterruptedException {

		// you need to write this code
		while(!ready){Thread.sleep(1);}
		int index;
		sem.acquire();
		index = sem.availablePermits();
		if(index == 0)
		{
			//System.out.println("No more permits");
			sem.release();
			ready = false;
		}
		else {
			Thread.sleep(1);
			while (ready == true)
				Thread.sleep(1);
			sem.release();
			if(sem.availablePermits() == part)
				ready = true;
		}
		return index;
	}
}
