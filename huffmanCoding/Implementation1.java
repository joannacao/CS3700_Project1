package huffmanCoding;

import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.*;
import java.io.*;

//original size: 45 kb
//how huffman coding works:
//make a list of how frequent each character is used and put it in order. (use a hashmap or some sort of dictionary) 
//take the two least used characters (at the end of the list) 
//create a parent node between the two, which will store the sum of their frequencies
//last element is left side, next element is right side
//add this back into the list, wherever it belongs based on its new frequency
//repeat: take bottom two off list and create parent node, store back into list
//parent nodes can be used as child nodes if they appear at the bottom of the list again
//you'll end up with a huffman tree
//compress bit format: if you take the right hand side, its a 1. if you take left hand side, its a 0
//must also store the tree itself to provide a translation table. if a character uses more than 8 bits its ok because its not used often
//decompress: take left fork if you see a 0, right if you see a 1
//in other words, build a min heap of all leaf nodes

//class to create node in priorityqueue
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

public class Implementation1 {
	
	public static void encode(HuffmanNode root, String str, Map<Character, String> huffmanCode) {
		if (root == null) {
			return; //if there is no root (reached a leaf), return
		}
		
		if (root.left == null && root.right == null) {
			huffmanCode.put(root.c, str); //if we reached a char leaf, store completed binary string in map
		}
		
		encode(root.left, str + "0", huffmanCode); //check left branch and add 0 to binary string
		encode(root.right, str + "1", huffmanCode); //check right branch and add 1 to binary string
	}
	
	public static void buildHuffmanTree(FileReader fr, FileReader fr2) throws IOException{
		//start recording how long it takes to build the tree
		long start = System.currentTimeMillis();
		//create map to map char to frequency in file
		Map<Character, Integer> frequency = new HashMap<>();
		int c;
		while ((c = fr.read()) != -1) {
			//if reader reads char from file and its not already in map, add it 
			if (!frequency.containsKey((char)c)){
				frequency.put((char)c, 0); 
			}
			//increment frequency value at that char
			frequency.put((char)c, frequency.get((char)c)+1);
		}
		//create priority queue to set up tree
		PriorityQueue<HuffmanNode> pq = new PriorityQueue<>((l,r) -> (l.frequency - r.frequency));
		for (Map.Entry<Character, Integer> entry : frequency.entrySet()) {
			//add every character + frequency
			pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
		}
		//keep merging nodes together until we have just one node, the root node
		while (pq.size() != 1) {
			HuffmanNode left = pq.poll(); 
			HuffmanNode right = pq.poll();
			
			int sum = left.frequency + right.frequency; 
			pq.add(new HuffmanNode('\0', sum, left, right));
		}
		//obtain value of root node
		HuffmanNode root = pq.peek();
		
		//create a map to make it easier to encode characters
		Map<Character, String> huffmanCode = new HashMap<>();
		encode(root, "", huffmanCode);
		
		//print out the key
		for (Map.Entry<Character, String> entry : huffmanCode.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		//mark end time
		long end = System.currentTimeMillis(); 
		long time = end-start;
		//print time taken to set up maps
		System.out.println("Time to create map: " + time + " ms");
		//encode the file, just print to console
		start = System.currentTimeMillis(); 
		FileOutputStream fStream = new FileOutputStream("C:\\Users\\joann\\Downloads\\compressedConst.txt");
		StringBuilder sb = new StringBuilder();
		//store entire encoded string into string builder, then convert to string. use parseBytes and write to file
		while ((c = fr2.read()) != -1) {
			//read each character from file, find its coded binary string and append to result string
			sb.append(huffmanCode.get((char)c));
		}
		String result = sb.toString();
		byte b; 
		//going to have to read subStrings
		for (int i = 0; i < result.length(); i +=8) {
			//if last string is less than 8 bits, determine how long it is, take substring, add zeros to end
			String sub;
			if (result.substring(i).length() < 8) {
				sub = result.substring(i); 
			} else {
				sub = result.substring(i, i+7); 
			}
			b = Byte.parseByte(sub,2); //parse the binary string into a byte and then write to the output file
			fStream.write(b);
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
			//build huffman tree and also encodes text
			buildHuffmanTree(reader, reader2);
            reader.close();
            reader2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
