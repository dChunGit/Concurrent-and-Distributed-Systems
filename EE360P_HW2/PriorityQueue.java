import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityQueue {

	private class StringPrio{
		String s;
		int pri;
		StringPrio next = null;
		ReentrantLock indLock;
		Condition indCond;
		public StringPrio(String s, int pri)
		{
			this.s = s;
			this.pri = pri;
			indLock = new ReentrantLock();
			indCond = indLock.newCondition();
		}
	}
	int currentSize = 0;
	int maxSize;
	StringPrio head;
	ReentrantLock lock;
	Condition cond;
	int lockedIndex = 0;
	public PriorityQueue(int maxSize) {
		// Creates a Priority queue with maximum allowed size as capacity
		this.maxSize = maxSize;
		//ll = new LinkedList<>();
		lock = new ReentrantLock();
		cond = lock.newCondition();
	}

	public int add(String name, int priority) {
		// Adds the name with its priority to this queue.
		// Returns the current position in the list where the name was inserted;
		// otherwise, returns -1 if the name is already present in the list.
		// This method blocks when the list is full.
		StringPrio sp = new StringPrio(name, priority);
		int index = 0;
		try {
			// this lock is only used to block thread when the list is full
			// but can be replaced with wait and notify thread where Condition is used
            lock.lock();
            while (currentSize > maxSize) {
                try {
                    cond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    cond.signalAll();
                    lock.unlock();
                }
            }

            if(head == null || currentSize == 0) {

                head = sp;
                index = 0;

                currentSize++;

                cond.signalAll();
                lock.unlock();

            } else {
                cond.signalAll();
                lock.unlock();

                StringPrio current = head;
                StringPrio prev = null;
                ReentrantLock cLock = current.indLock, nLock;
                cLock.lock();
                int i = 0;
                boolean found = false;

                while(current != null) {

                    if(current.s.equals(sp.s)) {
                        index = -1;
                        found = true;
                    }

                    if(current.next != null) {
                        nLock = current.next.indLock;
                        nLock.lock();
                        cLock.unlock();
                        cLock = nLock;
                    }
                    current = current.next;
                }
                cLock.unlock();

                current = head;
                cLock = current.indLock;
                cLock.lock();

                while(current != null && !found) {

                    if (prev == null) {
                        if (sp.pri > current.pri) {
                            sp.next = head;
                            head = sp;
                            index = 0;
                            break;
                        } else if(current.next == null) {
                            current.next = sp;
                            index = i + 1;
                            break;
                        }
                    }else if (sp.pri <= prev.pri && sp.pri > current.pri) {
                        sp.next = current;
                        prev.next = sp;
                        index = i;
                        break;
                    }  else if (current.next == null) {
                        current.next = sp;
                        index = i + 1;
                        break;
                    }

                    i++;
                    prev = current;
                    nLock = current.next.indLock;
                    nLock.lock();
                    cLock.unlock();
                    cLock = nLock;
                    current = current.next;
                }

                if(index != -1) {
                    currentSize++;
                }
                cLock.unlock();
            }
		} catch (Exception e) {
		    e.printStackTrace();
        } finally {
		    ////System.out.println(Thread.currentThread().getId() + " wrote " + "Test" + Thread.currentThread().getId());
            System.out.println(toString());
            return index;
		}
	}

	public int search(String name) {
		// Returns the position of the name in the list;
		// otherwise, returns -1 if the name is not found.
		ReentrantLock cLock, nLock;

		StringPrio current = head;
		if(current == null) {
			return -1;
		}

		cLock = nLock = current.indLock;
		cLock.lock();
		int index = 0;
		while(current != null) {
			if(current.s.equals(name)) {
                cLock.unlock();
                return index;
            }
			index++;
			if(current.next != null) {
			    nLock = current.next.indLock;
			    nLock.lock();
            }

            cLock.unlock();
            cLock = nLock;

			current = current.next;
		}
		return -1;
	}

	public String getFirst() {
		// Retrieves and removes the name with the highest priority in the list,
		// or blocks the thread if the list is empty.
		String returnVal = "";
		ReentrantLock cLock;

		// this lock is only used to block thread when the list is empty
		// but can be replaced with wait and notify thread where Condition is used
		lock.lock();

		try{
			while(currentSize == 0 || head == null) {
				try {
					cond.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

            cLock = head.indLock;
            cLock.lock();

            cond.signalAll();
            lock.unlock();

			returnVal = head.s;
			head = head.next;

			currentSize--;

			cLock.unlock();

		} catch (Exception e) {
		    ////System.out.println(Thread.currentThread().getId() + " " + e.toString());
		    e.printStackTrace();
        } finally{
		    //System.out.println(Thread.currentThread().getId() + " read " + returnVal);
            return returnVal;
		}
	}

	@Override
	public String toString() {
		String print = "[";
		StringPrio temp = head;
		while(temp != null) {
			print += temp.s + " (" + temp.pri + "), ";
			temp = temp.next;
		}
        print += "]";

		return print;
	}
}

