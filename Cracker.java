// Cracker.java
/*
 Generates SHA hashes of short strings in parallel.
*/

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class Cracker {
	// Array of chars used to produce strings
	public static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.,-!".toCharArray();
	public static List<String> allPermutations = new ArrayList<>();
	private static CountDownLatch latch;
	private static String result;
	
	/*
	 Given a byte[] array, produces a hex String,
	 such as "234a6f". with 2 chars for each byte in the array.
	 (provided code)
	*/
	public static String hexToString(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (int i=0; i<bytes.length; i++) {
			int val = bytes[i];
			val = val & 0xff;  // remove higher bits, sign
			if (val<16) buff.append('0'); // leading 0
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

	public static void makeKLengthPerms(int k){
		int n = CHARS.length;
		makeKLengthPermsRec("", n, k);
	}

	public static void makeKLengthPermsRec(String prefix, int n, int k){
		if (k == 0){
			allPermutations.add(prefix);
			return;
		}
		for (int i = 0; i < n; i++) {
			String newPrefix = prefix + CHARS[i];
			makeKLengthPermsRec(newPrefix, n, k-1);
		}
	}

	static class Worker extends Thread{
		String hash;
		int start, end;
		boolean foundResult;
		List<String> allPermutations;

		Worker (String hash, int start, int end, List<String> allPerms){
			this.hash = hash;
			this.start = start;
			this.end = end;
			this.allPermutations = allPerms;
			foundResult = false;
		}

		public void run(){
			while (start != end - 1){
				try {
					String currentString = allPermutations.get(start);
					String currentHash = Cracker.hexToString(generateHash(currentString));
					if(currentHash.equals(hash)){
						result = currentString;
						break;
					}
					start++;
				} catch (NoSuchAlgorithmException e) {
					System.out.println(e.getMessage());
				}
			}
			latch.countDown();
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

		String targ = "34800e15707fae815d7c90d49de44aca97e2d759";
		int len = 2;
		int numWorkers = 4;

		// YOUR CODE HERE
		System.out.println("The original word is " + crack(targ, len, numWorkers));
	}

	private static String crack(String hash, int len, int numWorkers) throws InterruptedException {
		latch = new CountDownLatch(numWorkers);
		List<Worker> workers = new ArrayList<>();
		System.out.println("Going to make all permutations");
		makeKLengthPerms(len);
		System.out.println("Permutations are complete");
		int lenOneWorker = allPermutations.size() / numWorkers;
		for (int i = 0; i < numWorkers; i++) {
			int start = i * lenOneWorker;
			int end = (i + 1) * lenOneWorker;
			if (i == numWorkers - 1) end = allPermutations.size();
			Worker worker = new Worker(hash, start, end, allPermutations);
			workers.add(worker);
		}
		for (Worker worker : workers){
			worker.start();
		}
		latch.await();
		return result;
	}
}
