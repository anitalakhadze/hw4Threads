// Bank.java

/*
 Creates a bunch of accounts and uses threads
 to post transactions to the accounts concurrently.
*/

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bank {
	public static final int ACCOUNTS = 20;	 // number of accounts
	Account[] accounts;
	Buffer buffer;
	Object lockFromAccount, lockToAccount;
	private Semaphore allDone;
	int numWorkers, limit;
	List<BadTransaction> badTransactions;

	public Bank(int numWorkers, int limit){
		accounts = IntStream.range(0, ACCOUNTS)
				.mapToObj(i -> new Account(this, i, 1000))
				.toArray(Account[]::new);
		buffer = new Buffer();
		lockFromAccount = new Object();
		lockToAccount = new Object();
		this.numWorkers = numWorkers;
		this.limit = limit;
		badTransactions = new ArrayList<>();
	}
	
	class Worker extends Thread {
		public void run(){
			while (true) {
				try {
					Transaction t = buffer.remove();
					if (t == null) break;
					processTransaction(t);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
//			System.out.println("done");
			allDone.release();
		}

		public void processTransaction(Transaction t){
//			System.out.println(t.toString());
			synchronized (lockFromAccount){
				accounts[t.from].change(-t.amount);
			}
			synchronized (lockToAccount){
				accounts[t.to].change(t.amount);
			}
			int postTransaction = accounts[t.from].getBalance();
			if(postTransaction < limit){
				badTransactions.add(new BadTransaction(t, postTransaction));
			}
		}
	}

	class BadTransaction {
		int from, to, amt, bal;

		public BadTransaction(Transaction t, int bal) {
			from = t.from;
			to = t.to;
			amt = t.amount;
			this.bal = bal;
		}

		@Override
		public String toString() {
			return "from:" + from
					+ " to:" + to
					+ " amt:" + amt
					+ " bal:" + bal;
		}
	}
	
	/*
	 Reads transaction data (from/to/amt) from a file for processing.
	 (provided code)
	 */
	public void readFile(String file) throws InterruptedException {
		try {
		BufferedReader reader = new BufferedReader(new FileReader(file));

		// Use stream tokenizer to get successive words from file
		StreamTokenizer tokenizer = new StreamTokenizer(reader);
			
			while (true) {
				int read = tokenizer.nextToken();
				if (read == StreamTokenizer.TT_EOF) break;  // detect EOF
				int from = (int)tokenizer.nval;
				
				tokenizer.nextToken();
				int to = (int)tokenizer.nval;
				
				tokenizer.nextToken();
				int amount = (int)tokenizer.nval;
				
				// Use the from/to/amount
				// YOUR CODE HERE
				Transaction t = new Transaction(from, to, amount);
				buffer.add(t);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		for (int i = 0; i < numWorkers; i++) {
			buffer.add(null);
		}
	}

	/*
	 Processes one file of transaction data
	 -fork off workers
	 -read file into the buffer
	 -wait for the workers to finish
	*/
	public void processFile(String file, int numWorkers) throws InterruptedException {
		allDone = new Semaphore(0);
		for (int i = 0; i < numWorkers; i++) {
			Worker worker = new Worker();
			worker.start();
		}
		readFile(file);
		allDone.acquire(numWorkers);
		for (Account a : accounts){
			System.out.println(a.toString());
		}
		if (limit != -1){
			System.out.println("There were " + badTransactions.size() + " Bad Accounting cases...");
			for (int i = 0; i < badTransactions.size(); i++) {
				System.out.println(badTransactions.get(i).toString());
			}
		}
	}


	/*
	 Looks at commandline args and calls Bank processing.
	*/
	public static void main(String[] args) throws InterruptedException {
//		// deal with command-lines args
//		if (args.length == 0) {
//			System.out.println("Args: transaction-file [num-workers [limit]]");
//			System.exit(1);
//		}
//
//		String file = args[0];
//
//		int numWorkers = 1;
//		int limit = 0;
//		boolean badAccounting = false;
//		if (args.length >= 2) {
//			numWorkers = Integer.parseInt(args[1]);
//			if (args.length > 2) {
//				limit = Integer.parseInt(args[2]);
//				badAccounting = true;
//			}
//		}

		String file = "5k.txt";
		int numWorkers = 4;
		boolean badAccounting = true;
		int limit = 0;

		// YOUR CODE HERE
		Bank bank = new Bank(numWorkers, limit);
		bank.processFile(file, numWorkers);
	}
}

