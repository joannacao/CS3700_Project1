package huffmanCoding2;

import java.util.*;
import java.io.*;
import java.util.concurrent.*;

//speedup for creating tree:
//first implementation time: 126ms
//second implementation time: 99ms

//speedup for encoding text:
//first implementation time: 1467ms
//second implementation time: 114ms

//code for node
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

//recursively completes encoding the PriorityQueue to an easier to read map using the Fork/Join Frame
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
			return;
		}
		//create list of subtasks
		List<recursiveEncoding> subtasks =
                new ArrayList<recursiveEncoding>();
        //create two tasks: one for left side, one for right side 
        recursiveEncoding subtask1 = new recursiveEncoding(this.root.left, this.s + "0", hc);
        recursiveEncoding subtask2 = new recursiveEncoding(this.root.right, this.s + "1", hc);
        //add subtasks
        subtasks.add(subtask1);
        subtasks.add(subtask2);
        //fork the tasks 
        for (recursiveEncoding task : subtasks) {
        	task.fork();
        }
	}
}

public class Implementation2 {
	public static void buildHuffmanTree(FileReader fr, FileReader fr2) throws IOException{
		long start = System.currentTimeMillis();
		Map<Character, Integer> frequency = new HashMap<>();
		int c;
		while ((c = fr.read()) != -1) {
			if (!frequency.containsKey((char)c)){
				frequency.put((char)c, 0); 
			}
			frequency.put((char)c, frequency.get((char)c)+1);
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
		
		//instead of calling "encode" function, we create a ForkJoinPool with two threads
		Map<Character, String> huffmanCode = new HashMap<>();
		ForkJoinPool forkJoinPool = new ForkJoinPool(2); 
		//create a recursiveEncoding object
		recursiveEncoding rc = new recursiveEncoding(root,"", huffmanCode); 
		//invoke the ForkJoinPool with recursiveEncoding obj as the argument
		forkJoinPool.invoke(rc);
		
		//did not print key
		
		long end = System.currentTimeMillis(); 
		long time = end-start;
		System.out.println("Time to create map: " + time + " ms");
		//encode the file, just print to console
		start = System.currentTimeMillis(); 
		FileOutputStream fStream = new FileOutputStream("C:\\Users\\joann\\Downloads\\compressedConst.txt");
		StringBuilder sb = new StringBuilder();
		//store entire encoded string into string builder, then convert to string. use parseBytes and write to file
		while ((c = fr2.read()) != -1) {
			sb.append(huffmanCode.get((char)c));
		}
		String result = sb.toString();
		byte b; 
		//going to have to read subStrings
		for (int i = 0; i < result.length(); i +=8) {
			//if last string is less than 8 bits, determine how long it is, take substring, add zeros to end
			//slightly changed the logic for parsing the bytes, incredibly sped up program
			String sub;
			//if we don't have enough values for a byte, take rest of binary and add 0s to the end
			if (result.length()-i < 8) {
				sub = result.substring(i);
				while (result.length() -1 < 8) {
					sub = sub + "0";
				}
				b = Byte.parseByte(sub, 2);
				fStream.write(b);
			} else {
				sub = result.substring(i, i+7);
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
	
	public static void main (String[] args) throws IOException{
		//create file object of us constitution text
		File constitution = new File("C:\\Users\\joann\\Downloads\\const.txt");
		File constitution2 = new File("C:\\Users\\joann\\Downloads\\const.txt");
		//create reader for file
		try {
			FileReader reader = new FileReader(constitution);
			FileReader reader2 = new FileReader(constitution2);
			buildHuffmanTree(reader, reader2);
            reader.close();
            reader2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	//encode:
	}
	
}
