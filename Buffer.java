// Buffer.java

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/*
 Holds the transactions for the worker
 threads.
*/
public class Buffer {
	public static final int SIZE = 64;
	List<Transaction> transactions;
	Semaphore canAdd, canRemove;
	private Object lock;

	public Buffer(int size){
		transactions = new ArrayList<>();
		canAdd = new Semaphore(size);
		canRemove = new Semaphore(0);
		lock = new Object();
	}

	public Buffer(){
		transactions = new ArrayList<>();
		canAdd = new Semaphore(SIZE);
		canRemove = new Semaphore(0);
		lock = new Object();
	}

	public void add (Transaction t) throws InterruptedException {
		canAdd.acquire();
//		System.out.println("canAdd acquired");
		synchronized (lock) {
			transactions.add(t);
		}
		canRemove.release();
//		System.out.println("canRemove released");
	}

	public Transaction remove() throws InterruptedException {
		Transaction result;
		canRemove.acquire();
//		System.out.println("canRemove acquired");
		synchronized (lock) {
			result = transactions.remove(0);
			//System.out.println(result);
		}
		canAdd.release();
//		System.out.println("canAdd released");
		return result;
	}
}
