// Cracker.java
/*
 Generates SHA hashes of short strings in parallel.
*/

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

public class Cracker {
	// Array of chars used to produce strings
	public static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.,-!".toCharArray();
	private static CountDownLatch latch;
	private static String result;
	private static BlockingQueue<String> permutations;
	
	/*
	 Given a byte[] array, produces a hex String,
	 such as "234a6f". with 2 chars for each byte in the array.
	 (provided code)
	*/
	public static String hexToString(byte[] bytes) {
		StringBuilder buff = new StringBuilder();
		for (int aByte : bytes) {
			int val = aByte;
			val = val & 0xff;  // remove higher bits, sign
			if (val < 16) buff.append('0'); // leading 0
			buff.append(Integer.toString(val, 16));
		}
		return buff.toString();
	}
	
	/*
	 Given a string of hex byte values such as "24a26f", creates
	 a byte[] array of those values, one byte value -128..127
	 for each 2 chars.
	 (provided code)
	*/
	public static byte[] hexToArray(String hex) {
		byte[] result = new byte[hex.length()/2];
		for (int i=0; i<hex.length(); i+=2) {
			result[i/2] = (byte) Integer.parseInt(hex.substring(i, i+2), 16);
		}
		return result;
	}

	public static byte[] generateHash (String input) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		return md.digest(input.getBytes(StandardCharsets.UTF_8));
	}

	static class Worker extends Thread{
		String hash;
		int start, end;
		BlockingQueue<String> allPermutations;

		Worker (String hash, int start, int end, BlockingQueue<String> allPerms){
			this.hash = hash;
			this.start = start;
			this.end = end;
			this.allPermutations = allPerms;
		}

		public void run(){
			while (start != end - 1){
				try {
					String currentString = allPermutations.take();
					String currentHash = Cracker.hexToString(generateHash(currentString));
					if(currentHash.equals(hash)){
						result = currentString;
						break;
					}
					start++;
				} catch (NoSuchAlgorithmException | InterruptedException e) {
					System.out.println(e.getMessage());
				}
			}
			latch.countDown();
		}
	}

	static class Producer extends Thread {
		private int sizeOfPermutations;

		public Producer(int len){
			sizeOfPermutations = len;
		}

		public void run(){
			int n = CHARS.length;
			makeKLengthPermsRec("", n, sizeOfPermutations);
		}

		void makeKLengthPermsRec(String prefix, int n, int k){
			if (k == 0){
				permutations.add(prefix);
				return;
			}
			for (int i = 0; i < n; i++) {
				String newPrefix = prefix + CHARS[i];
				makeKLengthPermsRec(newPrefix, n, k-1);
			}
		}

	}

	public static void main(String[] args) throws InterruptedException {
//		if (args.length < 2) {
//			System.out.println("Args: target length [workers]");
//			System.exit(1);
//		}
//		// args: targ len [numWorkers]
//		String targ = args[0];
//		int len = Integer.parseInt(args[1]);
//		int numWorkers = 1;
//		if (args.length>2) {
//			numWorkers = Integer.parseInt(args[2]);
//		}
		// a! 34800e15707fae815d7c90d49de44aca97e2d759
		// xyz 66b27417d37e024c46526c2f6d358a754fc552f3

		String targ = "66b27417d37e024c46526c2f6d358a754fc552f3";
		int len = 3;
		int numWorkers = 4;

		// YOUR CODE HERE
		System.out.println("The original word is " + crack(targ, len, numWorkers));
	}

	private static String crack(String hash, int len, int numWorkers) throws InterruptedException {
		latch = new CountDownLatch(numWorkers);
		permutations = new LinkedBlockingDeque<>();
		List<Worker> workers = new ArrayList<>();
		int permutationsSize = (int)Math.pow(CHARS.length, len);
		int lenOneWorker = permutationsSize / numWorkers;
		for (int i = 0; i < numWorkers; i++) {
			int start = i * lenOneWorker;
			int end = (i + 1) * lenOneWorker;
			if (i == numWorkers - 1) end = permutationsSize;
			Worker worker = new Worker(hash, start, end, permutations);
			workers.add(worker);
		}
		Producer producer = new Producer(len);
		producer.start();

		for (Worker worker : workers){
			worker.start();
		}
		latch.await();
		return result;
	}
}
