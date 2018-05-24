/*
 * EID's of group members
 * 
 */

public class MonitorThreadSynch {
	private int part;
	private Object lock = new Object();
	private int remaining;
	private boolean accepting = true;
	public MonitorThreadSynch(int parties) {
		part = parties;
		remaining = parties;
	}
	
	public int await() throws InterruptedException {
		   /*while(!accepting)
			   Thread.sleep(1);
           int index = 0;
           synchronized(lock)
		   {
		   	remaining--;
		   	index = remaining;
		   }
		   if(index == 0)
		   {
		   	accepting = false;
		   	remaining++;
		   }
		   else
		   {
		   	while(accepting)
				Thread.sleep(1);
		   	synchronized(lock)
			{
				remaining++;
				if(remaining == part)
					accepting = true;
			}
		   }
          // you need to write this code*/
		   int index;
		synchronized(lock)
		{
			remaining--;
			index = remaining;
			if(index == 0)
			{
				//accepting = false;
				//remaining++;
				remaining = part;
				lock.notifyAll();
			}
			else
				lock.wait();
		}
		//System.out.println(index);
	    return index;
	}
}
