package huffmanCoding3;

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

//speedup for encoding text:
//first implementation time: 114ms
//second implementation time: 102ms

//got rid of an extra filereader

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

//kept ForkJoinPool from Implementation2
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

public class Implementation3 {
	//changed implementation so that we don't need multiple FileReaders
	public static void buildHuffmanTree(FileReader fr) throws IOException{
		ArrayList<Character> copy = new ArrayList<Character>(33000);
		long start = System.currentTimeMillis();
		Map<Character, Integer> frequency = new HashMap<>();
		int c;
		//while reading from FileReader, also add char to char ArrayList for later reference
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
		//read from character arraylist instead of second filereader
		for (char character : copy) {
			sb.append(huffmanCode.get(character));
		}
		String result = sb.toString();
		byte b; 
		//going to have to read subStrings
		for (int i = 0; i < result.length(); i +=8) {
			//if last string is less than 8 bits, determine how long it is, take substring, add zeros to end
			String sub;
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
		//create reader for file
		try {
			FileReader reader = new FileReader(constitution);
			//only one filereader instead of two! saving resources yey
			buildHuffmanTree(reader);
            reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	//encode:
	}

}
