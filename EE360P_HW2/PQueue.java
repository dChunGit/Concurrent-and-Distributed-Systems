import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashSet;


public class PQueue {
	private final static int MAX_PRIORITY = 9;
	private final static int MIN_PRIORITY = 0;
	private final Node head;
	private final int maxSize;
	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();
	private final Condition notFull = lock.newCondition();
	private int size = 0;
	private HashSet<String> inList = new HashSet<String>();
	
	public PQueue(int maxSize) {
		head = new Node("__HEAD__", MAX_PRIORITY + 1);
		head.next = new Node("__TAIL__", MIN_PRIORITY - 1);
		this.maxSize = maxSize;
	}
	
	public int insert(String name, int priority) throws IllegalArgumentException {
		if (priority < MIN_PRIORITY || priority > MAX_PRIORITY)
			throw new IllegalArgumentException();
		if (inList.contains(name))
			return -1;
		
		while (true) {
			// Block INSERT until there is space
			lock.lock();
			try {
				while (size >= maxSize)
					notFull.await();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
			
			int index = 0;
			Node pred = head;
			Node curr = pred.next;
			while (curr.priority >= priority) {
				if (curr.name.equals(name) && !curr.marked) return -1;
				++index;
				pred = curr;
				curr = curr.next;
			}
			pred.lock();
			try {
				curr.lock();
				try {
					if (validate(pred, curr)) {
						lock.lock();
						try {
							// Check size again before inserting
							if (size >= maxSize) continue;
							else {
								++size;
								Node node = new Node(name, priority);
								node.next = curr;
								pred.next = node;
								inList.add(name);
								notEmpty.signalAll();
							}
						} finally {
							lock.unlock();
						}

						return index;
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}
	
	public int search(String name) {
		int index = -1;
		if (!inList.contains(name))
			return index;
		Node curr = head;
		while (curr != null && !curr.name.equals(name)) {
			++index;
			curr = curr.next;
		}
		if (curr == null)
			return -1;
		if (!curr.marked) {
			return index;
		} else {
			return -1;
		}
	}
	
	public String getFirst() {
		while (true) {
			// Block getFirst until there is an element
			lock.lock();
			try {
				while (size <= 0)
					notEmpty.await();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
			
			Node pred = head;
			Node curr = pred.next;
			
			pred.lock();
			try {
				curr.lock();
				try {
					if (validate(pred, curr)) {
						curr.marked = true;
						lock.lock();
						try {
							--size;
							notFull.signalAll();
						} finally {
							lock.unlock();
						}
						pred.next = curr.next;
						inList.remove(curr.name);
						return curr.name;
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}

	private boolean validate(Node pred, Node curr) {
		return !pred.marked && !curr.marked && pred.next == curr;
	}
	
	class Node extends ReentrantLock {
		public String name;
		public int priority;
		public Node next;
		public boolean marked = false;
		
		public Node(String name, int priority) {
			this(name, priority, null);
		}
		
		public Node(String name, int priority, Node next) {
			this.name = name;
			this.priority = priority;
			this.next = next;
		}
	}
	@Override
	public String toString() {
		String print = "[";
		Node temp = head;
		while(temp != null) {
			print += temp.name + " (" + temp.priority + "), ";
			temp = temp.next;
		}
		print += "]";

		return print;
	}
}
