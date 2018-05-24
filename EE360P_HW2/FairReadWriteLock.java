public class FairReadWriteLock {
	int currentlyServing = 0;
	int sequence = 0;
	int readsBetweenWrites = 0;
	int finishedReads = 0;
	boolean fresh = true;
	boolean lockout;
                        
	public synchronized void beginRead() {
		int mySequence = sequence;
		sequence++;

		//System.out.println("Reader " + sequence);
		while(lockout) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		readsBetweenWrites++;
		while(mySequence != currentlyServing) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		currentlyServing++;
	}
	
	public synchronized void endRead(){
		finishedReads++;
		notifyAll();
	}
	
	public synchronized void beginWrite() {
		int mySequence = sequence;
		sequence++;
		while(lockout)
		{
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		while(mySequence != currentlyServing && finishedReads == readsBetweenWrites)
		{
			lockout = true;
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
	public synchronized void endWrite() {
		lockout = false;
		finishedReads = 0;
		readsBetweenWrites = 0;
		//System.out.println("Writing " + Thread.currentThread().getId());
		currentlyServing++;
	}
}

