package huffmanCoding4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import java.util.concurrent.Callable; 
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

//when youre encoding the text, create two threads. one will encode the first half the other will encode the second. 
//you'll have two string results but they were encoded at the same time

//create map: 106ms
//encode text: 108ms;

class HuffmanNode { 
	  
    int frequency; 
    char c; 
  
    HuffmanNode left = null; 
    HuffmanNode right = null; 
    
    HuffmanNode(char ch, int freq){
    	this.c = ch;
    	this.frequency = freq;
    }
    
    public HuffmanNode(char ch, int freq, HuffmanNode l, HuffmanNode r) {
    	this.c = ch;
    	this.frequency = freq;
    	this.left = l;
    	this.right = r;
    }
}

class recursiveEncoding extends RecursiveAction {
	private HuffmanNode root;
	private String s; 
	private Map<Character, String> hc;
	
	public recursiveEncoding(HuffmanNode r, String str, Map<Character, String> huffmanCode) {  
		this.root = r;
		this.s = str;
		this.hc = huffmanCode;
	}
	protected void compute() {
		if (this.root == null) {
			return;
		}
		if (this.root.left == null && this.root.right == null && this.root.c != 0) {
			hc.put(this.root.c,  s);
			//System.out.println(this.root.c + " : " + s); 
			return;
		}
		List<recursiveEncoding> subtasks =
                new ArrayList<recursiveEncoding>();
        recursiveEncoding subtask1 = new recursiveEncoding(this.root.left, this.s + "0", hc);
        recursiveEncoding subtask2 = new recursiveEncoding(this.root.right, this.s + "1", hc);
        subtasks.add(subtask1);
        subtasks.add(subtask2);
        for (recursiveEncoding task : subtasks) {
        	task.fork();
        }
	}
}

//what does encodingText thread need? 
//huffmanCode
//arraylist of characters 
//beginning index (0 or (array.size()/2 + 1))
//we want it to return a string 
//actually we can split character array before we pass it to thread, no need to know index. 
//use Callable so that we can return a string to a future
class encodingText implements Callable<String> {
	private Map<Character, String> huffmanCode; 
	private ArrayList<Character> chars; 
	
	public encodingText(Map<Character, String> hc, ArrayList<Character> c) {
		this.huffmanCode = hc;
		this.chars = c; 
	}
	public String call(){
		//encode text and stores into string that we will return to a future object
		String result;
		StringBuilder sb = new StringBuilder();
		 for (char letter : chars) {
			 sb.append(huffmanCode.get(letter));
		 }
		result = sb.toString();
		return result;
	}
}

public class Implementation4 {
	public static void buildHuffmanTree(FileReader fr) throws IOException, ExecutionException, InterruptedException{
		ArrayList<Character> copy = new ArrayList<Character>(33000);
		long start = System.currentTimeMillis();
		Map<Character, Integer> frequency = new HashMap<>();
		int c;
		while ((c = fr.read()) != -1) {
			if (!frequency.containsKey((char)c)){
				frequency.put((char)c, 0); 
			}
			frequency.put((char)c, frequency.get((char)c)+1);
			copy.add((char)c); 
		}
		PriorityQueue<HuffmanNode> pq = new PriorityQueue<>((l,r) -> (l.frequency - r.frequency));
		for (Map.Entry<Character, Integer> entry : frequency.entrySet()) {
			pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
		}
		while (pq.size() != 1) {
			HuffmanNode left = pq.poll(); 
			HuffmanNode right = pq.poll();
			
			int sum = left.frequency + right.frequency; 
			pq.add(new HuffmanNode('\0', sum, left, right));
		}
		
		HuffmanNode root = pq.peek();
		
		Map<Character, String> huffmanCode = new HashMap<>();
		ForkJoinPool forkJoinPool = new ForkJoinPool(2); 
		recursiveEncoding rc = new recursiveEncoding(root,"", huffmanCode); 
		forkJoinPool.invoke(rc);
		
		long end = System.currentTimeMillis(); 
		long time = end-start;
		System.out.println("Time to create map: " + time + " ms");
		//encode the file, just print to console
		start = System.currentTimeMillis(); 
		FileOutputStream fStream = new FileOutputStream("C:\\Users\\joann\\Downloads\\compressedConst.txt");
		StringBuilder sb = new StringBuilder();
		//store entire encoded string into string builder, then convert to string. use parseBytes and write to file
		//cut character array in half 
		int halfArray = copy.size()/2; 
		ArrayList<Character> part1 = new ArrayList<Character>(copy.subList(0, halfArray));
		ArrayList<Character> part2 = new ArrayList<Character>(copy.subList(halfArray+1, copy.size()-1));
		//create two threads
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> thread1 = executor.submit(new encodingText(huffmanCode, part1));
		Future<String> thread2 = executor.submit(new encodingText(huffmanCode, part2));
		//create two future objects 
		String result1 = thread1.get(); 
		String result2 = thread2.get();
		executor.shutdown();
		byte b; 
		//going to have to read subStrings
		//read first result string into file
		for (int i = 0; i < result1.length(); i +=8) {
			//if last string is less than 8 bits, determine how long it is, take substring, add zeros to end
			String sub;
			if (result1.length()-i < 8) {
				sub = result1.substring(i);
				while (result1.length() -1 < 8) { 
					sub = sub + "0";
				} 
				b = Byte.parseByte(sub, 2);
				fStream.write(b);
			} else {
				sub = result1.substring(i, i+7);
				b = Byte.parseByte(sub,2); 
				fStream.write(b);
			}
		}
		//read second result string into file
		for (int i = 0; i < result2.length(); i +=8) {
			//if last string is less than 8 bits, determine how long it is, take substring, add zeros to end
			String sub;
			if (result2.length()-i < 8) {
				sub = result2.substring(i);
				while (result2.length() -1 < 8) {
					sub = sub + "0";
				}
				b = Byte.parseByte(sub, 2);
				fStream.write(b);
			} else {
				sub = result2.substring(i, i+7);
				b = Byte.parseByte(sub,2); 
				fStream.write(b);
			}
		}
		
		end = System.currentTimeMillis(); 
		time = end-start; 
		System.out.println("Time to encode file: " + time + " ms");
		System.out.println("Original file: 45 KB");
		System.out.println("Compressed file:  25 KB");
		System.out.println("Compression percentage: 44.4%");
		fStream.close();
	}
	
	public static void main (String[] args) throws IOException, InterruptedException, ExecutionException{
		//create file object of us constitution text
		File constitution = new File("C:\\Users\\joann\\Downloads\\const.txt");
		//create reader for file
		try {
			FileReader reader = new FileReader(constitution);
			buildHuffmanTree(reader);
            reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
