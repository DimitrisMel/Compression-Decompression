/* On my honor, I have neither given nor received unauthorized aid on this assignment */

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;
import java.io.FileWriter;

public class SIM {
	File cout;
	File dout;
	FileWriter writer;
	
	ArrayList<String> original = new ArrayList<String>(); //Contents of the original.txt
	ArrayList<String> compressed = new ArrayList<String>(); //Contents of the compressed.txt
	String[] dictionary = new String[16]; //The Dictionary
	HashMap<Integer, String> dicthash = new HashMap<>(); //HashMap to map the dictionary instruction pointers to the string codes (0000, 0001, etc)
	HashMap<Integer, String> mishash = new HashMap<>(); //HashMap to map the mismatch location pointers to the string codes (00000, 00001, etc)
	HashMap<Integer, Boolean> reserved = new HashMap<>(); //HashMap to tell which input file instruction pointers are already encoded by a format
	HashMap<Integer, String> cHash = new HashMap<>(); //HashMap to map the original instructions to their encoded equivalents
	
	public static void main(String[] args) {
		SIM simulation = new SIM(args);
	}
	
	SIM(String[] args) { //Constructor
		if(args.length != 1) { //Check number of parameters
			throw new IllegalArgumentException("Exactly 1 parameters required!");
		}
		else {
			int argument;
			try { //Check if the argument is an integer
				argument = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException nbfe) {
				throw new IllegalArgumentException("The argument must be an integer!");
			}
			
			if(argument == 1) { //Parameter 1 means compress and parameter 2 means decompress
				Compress();
			}
			else if(argument == 2) {
				Decompress();
			}
			else {
				throw new IllegalArgumentException("Provide 1 for compression or 2 for decompression!");
			}
		}
	}
	
	public void Compress() { //Method for compression
		
		StringBuilder finalBuild = new StringBuilder();
		String strfinal = "";
		
		//Populate the HashMap to map the dictionary instruction pointers to the string codes
		dicthash.put(0, "0000");
		dicthash.put(1, "0001");
		dicthash.put(2, "0010");
		dicthash.put(3, "0011");
		dicthash.put(4, "0100");
		dicthash.put(5, "0101");
		dicthash.put(6, "0110");
		dicthash.put(7, "0111");
		dicthash.put(8, "1000");
		dicthash.put(9, "1001");
		dicthash.put(10, "1010");
		dicthash.put(11, "1011");
		dicthash.put(12, "1100");
		dicthash.put(13, "1101");
		dicthash.put(14, "1110");
		dicthash.put(15, "1111");
		
		//Populate the HashMap to map the mismatch location pointers to the string codes
		mishash.put(0, "00000");
		mishash.put(1, "00001");
		mishash.put(2, "00010");
		mishash.put(3, "00011");
		mishash.put(4, "00100");
		mishash.put(5, "00101");
		mishash.put(6, "00110");
		mishash.put(7, "00111");
		mishash.put(8, "01000");
		mishash.put(9, "01001");
		mishash.put(10, "01010");
		mishash.put(11, "01011");
		mishash.put(12, "01100");
		mishash.put(13, "01101");
		mishash.put(14, "01110");
		mishash.put(15, "01111");
		mishash.put(16, "10000");
		mishash.put(17, "10001");
		mishash.put(18, "10010");
		mishash.put(19, "10011");
		mishash.put(20, "10100");
		mishash.put(21, "10101");
		mishash.put(22, "10110");
		mishash.put(23, "10111");
		mishash.put(24, "11000");
		mishash.put(25, "11001");
		mishash.put(26, "11010");
		mishash.put(27, "11011");
		mishash.put(28, "11100");
		mishash.put(29, "11101");
		mishash.put(30, "11110");
		mishash.put(31, "11111");
		
		ReadOriginal(); //Read the original.txt
		
		for(int i=0; i<original.size(); i++) { //Initialize all original entries to not reserved
			reserved.put(i, false);
		}
		
		Dictionary(); //Create the dictionary based on frequency
		
		//The encoding methods are tried one by one, in the order of their encoding length, smallest to largest

		RLE(); //Call the RLE method because it gives the shortest encoding, even for repetition=3
		
		for(int i=0; i<original.size(); i++) { //Parse the whole original array to find best encoding methods
			if(reserved.get(i) == false) { //Check if the current instruction is already encoded (reserved = false), or if it's available for encoding
				DirectMatching(original.get(i), i); //Whatever is not encoded by RLE, try to encode it with Direct Matching
			}
			if(reserved.get(i) == false) { //If it still not reserved...
				OneBitMis(original.get(i), i); //Whatever is not encoded with the previous methods, try to encode it with this one
			}
			if(reserved.get(i) == false) {
				TwoBitConsecMis(original.get(i), i);
			}
			
			if(reserved.get(i) == false) {
				FourBitConsecMis(original.get(i), i);
			}

			if(reserved.get(i) == false) {
				Bitmask(original.get(i), i);
			}
			
			if(reserved.get(i) == false) {
				TwoBitAnywhereMis(original.get(i), i);
			}

			if(reserved.get(i) == false) {
				OriginalBinaries(original.get(i), i); //Whatever is not encoded with the previous methods, try to encode it with this one
			}
		}	
		
		cout = new File("cout.txt"); //Create the output file in order to write in it
		try {
			writer = new FileWriter(cout); //Open the output file
		} catch (IOException e) {
			System.out.println("Write error! " + e);
		}
		
		for(int i =0; i<original.size(); i++) {
			if(cHash.containsKey(i)) {
				finalBuild.append(cHash.get(i)); //Put the contents of the cHash into the finalBuild StringBuilder
			}
		}
		
		strfinal = finalBuild.toString(); //Transfer it to a String
		
		if(strfinal.length()%32 != 0) { //Check the length of the string and do zero padding in the end, if it's not divisible by 32
			for(int i=1; i<=32-strfinal.length()%32; i++) {
				finalBuild.append("0");
			}
			strfinal = finalBuild.toString();  //Transfer the padded StringBuilder to a String
		}
		
		finalBuild.setLength(0); //Empty the StringBuilder to use it again
		
		for(int i=1; i<=strfinal.length(); i++) { //Divide the encoded bits into 32-bit words, by adding new lines
			char c = strfinal.charAt(i-1);
			if(i%32 == 0) {
				finalBuild.append(c);
				finalBuild.append("\n");
			}
			else {
				finalBuild.append(c);
			}
		}
		
		strfinal = finalBuild.toString(); //Transfer the divided StringBuilder to a String
		
		WriteToFile(strfinal); //Write the divided string into the cout.txt
		
		WriteDictionary(); //Finally, call this method to write the Dictionary into the output file
		
		try {
			writer.close(); //Close the output file
		} catch (IOException e) {
			System.out.println("Write error! " + e);
		}
	}
	
	public void Decompress() { //Method for decompression
		int endFile = 0;
		StringBuilder compressedBuilder = new StringBuilder();
		StringBuilder decompressedBuilder = new StringBuilder();
		String compressedString = "";
		String formatMethod = "";
		int nexti = 0;
		boolean exit = false;
		
		ReadCompressed(); //Read the compressed.txt
		
		for(int i=compressed.size()-1; i>=0; i--) { //Iterate the compressed from the bottom
			if(compressed.get(i).equals("xxxx")) { //Find where the Dictionary starts
				endFile = i;
				int z = 0;
				for(int j=i+1; j<compressed.size(); j++) { //Iterate from the next position to the end of the ArrayList
					dictionary[z] = compressed.get(j); //Store the Dictionary inside the dictionary[]
					z++;
				}
				break;
			}
		}
		
		for(int i=0; i<endFile; i++) { //Iterate the compressed from the beginning to the point right before the xxxx
			compressedBuilder.append(compressed.get(i));
		}
		
		compressedString = compressedBuilder.toString();
		
		for(int i=0; i<compressedString.length()-3; i=nexti) { //Go from the beginning to three spots before the end, because every encoding method has at least 3 bits after the format
			formatMethod = compressedString.substring(i, i+3); //Read the next format method
			
			switch(formatMethod) {
			
			case "000":
				if(i+3+32 > compressedString.length()) { //If there is a string of 000 at least 3 spots before the end but there are not at least 32 bits afterwards, it's the zero padding
					exit = true; //So make the exit flag true
				}
				else {
					decompressedBuilder.append(compressedString.substring(i+3, i+3+32)); //Otherwise, it's just an uncompressed string, so get the next 32 bits
					nexti = i+3+32;
				}
				break;
				
			case "001":
				int repetition = 0;
				repetition = Integer.parseInt(compressedString.substring(i+3, i+6), 2); //The 3 bits after the format method are the amount of repetitions. Make the binary string an integer
				for(int j=0; j<repetition+1; j++) {
					decompressedBuilder.append(dictionary[Integer.parseInt(compressedString.substring(i-4, i), 2)]); //Go back to the previous string and get the dictionary word
				}
				nexti = i+3+3;
				break;
				
			case "010":
				int misLocation = 0;
				int dictIndex = 0;
				String pattern = "";
				String misdict = "";
				String fixed = "";
				
				misLocation = Integer.parseInt(compressedString.substring(i+3, i+8), 2); //Get the index of the beginning of the bitmask (first mismatch bit)
				pattern = compressedString.substring(i+8, i+12); //Get the bitmask pattern
				dictIndex = Integer.parseInt(compressedString.substring(i+12, i+16), 2); //Get the index of the dictionary word
				misdict = dictionary[dictIndex].substring(misLocation, misLocation+4); //Get the wrong part from the dictionary word
				for(int j=0; j<4; j++) {
					fixed += (pattern.charAt(j) ^ (misdict.charAt(j))); //XOR the 2 strings and append each character to the fixed variable
				}
				decompressedBuilder.append(dictionary[dictIndex].substring(0, misLocation)); //Append the part of the Dictionary word that is correct
				decompressedBuilder.append(fixed); //Append the fixed part
				decompressedBuilder.append(dictionary[dictIndex].substring(misLocation+4, 32)); //Append the rest of the correct Dictionary word
				nexti = i+3+5+4+4;
				break;
				
			case "011":
				misLocation = Integer.parseInt(compressedString.substring(i+3, i+8), 2); //Get the location of the string that is wrong
				dictIndex = Integer.parseInt(compressedString.substring(i+8, i+12), 2); //Get the index of the dictionary word
				pattern = dictionary[dictIndex].substring(misLocation, misLocation+1); //Get the bit that is wrong
				fixed = pattern.replace('0', '2').replace('1', '0').replace('2', '1'); //Flip the pattern
				decompressedBuilder.append(dictionary[dictIndex].substring(0, misLocation)); //Append the part of the Dictionary word that is correct
				decompressedBuilder.append(fixed); //Append the fixed part
				decompressedBuilder.append(dictionary[dictIndex].substring(misLocation+1, 32)); //Append the rest of the correct Dictionary word
				nexti = i+3+5+4;
				break;
				
			case "100":
				misLocation = Integer.parseInt(compressedString.substring(i+3, i+8), 2); //Get the starting location of the string that is wrong
				dictIndex = Integer.parseInt(compressedString.substring(i+8, i+12), 2); //Get the index of the dictionary word
				pattern = dictionary[dictIndex].substring(misLocation, misLocation+2); //Get the bits that are wrong
				fixed = pattern.replace('0', '2').replace('1', '0').replace('2', '1'); //Flip the pattern
				decompressedBuilder.append(dictionary[dictIndex].substring(0, misLocation)); //Append the part of the Dictionary word that is correct
				decompressedBuilder.append(fixed); //Append the fixed part
				decompressedBuilder.append(dictionary[dictIndex].substring(misLocation+2, 32)); //Append the rest of the correct Dictionary word
				nexti = i+3+5+4;
				break;
				
			case "101":
				misLocation = Integer.parseInt(compressedString.substring(i+3, i+8), 2); //Get the starting location of the string that is wrong
				dictIndex = Integer.parseInt(compressedString.substring(i+8, i+12), 2); //Get the index of the dictionary word
				pattern = dictionary[dictIndex].substring(misLocation, misLocation+4); //Get the bits that are wrong
				fixed = pattern.replace('0', '2').replace('1', '0').replace('2', '1'); //Flip the pattern
				decompressedBuilder.append(dictionary[dictIndex].substring(0, misLocation)); //Append the part of the Dictionary word that is correct
				decompressedBuilder.append(fixed); //Append the fixed part
				decompressedBuilder.append(dictionary[dictIndex].substring(misLocation+4, 32)); //Append the rest of the correct Dictionary word
				nexti = i+3+5+4;
				break;
				
			case "110":
				int misLocation1 = Integer.parseInt(compressedString.substring(i+3, i+8), 2); //Get the 1st location of the string that is wrong
				int misLocation2 = Integer.parseInt(compressedString.substring(i+8, i+13), 2); //Get the 2nd location of the string that is wrong
				dictIndex = Integer.parseInt(compressedString.substring(i+13, i+17), 2); //Get the index of the dictionary word
				String pattern1 = dictionary[dictIndex].substring(misLocation1, misLocation1+1); //Get the 1st bit that is wrong
				String fixed1 = pattern1.replace('0', '2').replace('1', '0').replace('2', '1'); //Flip the 1st pattern
				String pattern2 = dictionary[dictIndex].substring(misLocation2, misLocation2+1); //Get the 2nd bit that is wrong
				String fixed2 = pattern2.replace('0', '2').replace('1', '0').replace('2', '1'); //Flip the 2nd pattern
				decompressedBuilder.append(dictionary[dictIndex].substring(0, misLocation1)); //Append the part of the Dictionary word that is correct
				decompressedBuilder.append(fixed1); //Append the 1st fixed part
				decompressedBuilder.append(dictionary[dictIndex].substring(misLocation1+1, misLocation2)); //Append the next part of the Dictionary word that is correct
				decompressedBuilder.append(fixed2); //Append the 2nd fixed part
				decompressedBuilder.append(dictionary[dictIndex].substring(misLocation2+1, 32)); //Append the rest of the correct Dictionary word
				nexti = i+3+5+5+4;
				break;
				
			case "111":
				dictIndex = Integer.parseInt(compressedString.substring(i+3, i+7), 2); //Get the index of the dictionary word
				decompressedBuilder.append(dictionary[dictIndex]); //Append the Dictionary word
				nexti = i+3+4;
				break;
				
			default:
				exit = true;
				break;
			}
			if(exit == true) {
				break;
			}
		}
		
		String finalDecomp = decompressedBuilder.toString();
		
		
		dout = new File("dout.txt"); //Create the output file in order to write in it
		try {
			writer = new FileWriter(dout); //Open the output file
		} catch (IOException e) {
			System.out.println("Write error! " + e);
		}
		
		for(int i=0; i<finalDecomp.length()-31; i+=32) { //Check if it's near the end
			String finalDecompPiece = "";
			finalDecompPiece = finalDecomp.substring(i, i+32);
			WriteToFile(finalDecompPiece);
			if(i+33 < finalDecomp.length()) {
				WriteToFile("\n");
			}
		}
		
		try {
			writer.close(); //Close the output file
		} catch (IOException e) {
			System.out.println("Write error! " + e);
		}
	}
	
	public void ReadOriginal() {
		try {
			original = (ArrayList<String>) Files.readAllLines(Paths.get("original.txt"), Charset.defaultCharset()); //Put the contents of the original.txt into the original ArrayList
		} catch (IOException e) {
			System.out.println("Read error! " + e);
		}
	}
	
	public void ReadCompressed() {
		try {
			compressed = (ArrayList<String>) Files.readAllLines(Paths.get("compressed.txt"), Charset.defaultCharset()); //Put the contents of the compressed.txt into the compressed ArrayList
		} catch (IOException e) {
			System.out.println("Read error! " + e);
		}
	}
	
	public void Dictionary() { //Create the Dictionary
		String[][] freqmap = new String[original.size()][3];
		ArrayList<String> dict = new ArrayList<String>();
		String[] diction = new String[original.size()];
		
		for(int i=0; i<original.size(); i++) { //Store each instruction, its frequency and its pointer of the original List to the freqmap 2D array
			freqmap[i][0] = original.get(i);
			freqmap[i][1] = Integer.toString(Collections.frequency(original, original.get(i)));
			freqmap[i][2] = Integer.toString(i);
		}
		
		Collections.reverse(Arrays.asList(freqmap)); //Reverse the order of the 2D Array based on the frequency, in order to get the items that appear first in the document, on the bottom
		
		Arrays.sort(freqmap, new Comparator<String[]>() { //Order the 2D Array based on the frequency. Now we have arranged them firstly based on frequency, and then the order of appearance
            @Override
            public int compare(final String[] entry1, final String[] entry2) {
                final int freq1 = Integer.parseInt(entry1[1]);
                final int freq2 = Integer.parseInt(entry2[1]);
                return Integer.compare(freq1, freq2);
            }
        });
		
		for(int i=freqmap.length-1; i>=0; i--) { //Store the whole frequency based dictionary, not just 16 values, by parsing it from the bottom
			if(!dict.contains(freqmap[i][0])) {
				dict.add(freqmap[i][0]);
			}
		}
		
		diction = dict.stream().toArray(String[]::new); //Convert from ArrayList to Array
		
		if(diction.length > 15) { //Check the length of the overall dictionary
			for(int i=0; i<16; i++) { //Keep only the top 16 most frequent values
				dictionary[i] = diction[i];
			}
		}
		else {
			System.out.println("Provide at least 16 different instructions.");
		}
		
	}
	
	public void RLE() {
		int count = 1;
		boolean denied = false;
		
		for(int i=1; i<original.size(); i++) { //Parse the whole original arraylist starting from line 1
			if(original.get(i).equals(original.get(i-1))) { //Check if this line is the same as the previous
				count++; //If it is, raise the counter
				if(denied == false) {
					reserved.put(i, true); //Make it reserved, so other methods can't encode it
				}
				else {
					denied = false;
				}
				if(count == 9) {
					cHash.put(i, "001111");
					denied = true; //Deny the reservance for the 10th iteration, so 112 stays false
				}
			}
			else {
				if(count > 2) {
					switch(count) {
					case 3:
						cHash.put(i-1, "001001");
						break;
					case 4:
						cHash.put(i-1, "001010");
						break;
					case 5:
						cHash.put(i-1, "001011");
						break;
					case 6:
						cHash.put(i-1, "001100");
						break;
					case 7:
						cHash.put(i-1, "001101");
						break;
					case 8:
						cHash.put(i-1, "001110");
						break;
					case 9:
						cHash.put(i-1, "001111");
					}
				}
				count = 1; //If this line is not the same as the previous, reset the counter
			}
			
			if(count > 8) { //If there are 9 consecutive occurrences, reset the counter
				count = 0;
			}
		}
	}
	
	public void DirectMatching(String str, int pointer) { //Check the incoming String against the Dictionary. If it matches, put it in the cHash 
		String value = "";
		StringBuilder matchBuilder = new StringBuilder();
		
		for(int i=0; i<dictionary.length; i++) {
			if(str.equals(dictionary[i])) {
				value  = dicthash.get(i);
				reserved.put(pointer, true);
				matchBuilder.append("111");
				matchBuilder.append(value);
				
				cHash.put(pointer, matchBuilder.toString());
				break;
			}
		}
	}
	
	public void OneBitMis(String str, int pointer) { //Check the incoming String against the Dictionary
		int counter = 0;
		int strlocation = 0;
		int dictlocation = 0;
		StringBuilder OneBitMisBuilder = new StringBuilder();
		
		for(int j=0; j<16; j++) {
			for(int i=0; i<str.length(); i++) {
				char a = str.charAt(i);
				char b = dictionary[j].charAt(i);
				
				if(a != b) { //If there is a mismatch raise the counter and keep the location of the mismatch and the dictionary pointer
					counter++;
					strlocation = i;
					dictlocation = j;
				}
				if(counter > 1) { //If there is more than 1 mismatch with this Dictionary entry, break and try the next 
					counter = 0;
					break;
				}
			}
			if(counter == 1) { //If after checking the whole string, there is exactly 1 mismatch, stop searching the Dictionary
				break;
			}
		}
		
		if(counter == 1) { //If after checking the whole string, there is exactly 1 mismatch
			reserved.put(pointer, true); //Reserve it
			OneBitMisBuilder.append("011"); //Code for the OneBitMis format
			OneBitMisBuilder.append(mishash.get(strlocation)); //Write 5 bits for the mismatch location
			OneBitMisBuilder.append(dicthash.get(dictlocation)); //Write the Dictionary word that matched
			cHash.put(pointer, OneBitMisBuilder.toString()); //Write the whole String to the cHash
		}
	}
	
	public void TwoBitConsecMis(String str, int pointer) {
		int counter = 0;
		int strlocation = 0;
		int dictlocation = 0;
		StringBuilder TwoBitConsecMisBuilder = new StringBuilder();
		
		for(int j=0; j<16; j++) {
			for(int i=0; i<str.length()-1; i++) {
				char a = str.charAt(i);
				char b = dictionary[j].charAt(i);
				
				if(a != b) { //If there is a mismatch, check if the next char is a mismatch as well
					if(str.charAt(i+1) != dictionary[j].charAt(i+1)) { //If it is, raise the counter and keep the location of the first mismatch and the dictionary pointer
						counter++;
						strlocation = i;
						dictlocation = j;
						i+=1;
					}
					else { //If it's not, we have 1 mismatch, so we move to the next dictionary word
						counter = 0;
						break;
					}
				}
				if(counter > 1) { //If there is more than 1 2-bit mismatch with this Dictionary entry, break and try the next 
					counter = 0;
					break;
				}
			}
			if(counter == 1) { //If after checking the whole string, there is exactly 1 2-bit consecutive mismatch, stop searching the Dictionary
				break;
			}
		}
		
		if(counter == 1) { //If after checking the whole string, there is exactly 1 2-bit consecutive mismatch
			reserved.put(pointer, true); //Reserve it
			TwoBitConsecMisBuilder.append("100"); //Code for the TwoBitConsecMis format
			TwoBitConsecMisBuilder.append(mishash.get(strlocation)); //Write 5 bits for the leftmost mismatch location
			TwoBitConsecMisBuilder.append(dicthash.get(dictlocation)); //Write the Dictionary word that matched
			cHash.put(pointer, TwoBitConsecMisBuilder.toString()); //Write the whole String to the cHash
		}
	}
	
	public void FourBitConsecMis(String str, int pointer) {
		int counter = 0;
		int strlocation = 0;
		int dictlocation = 0;
		StringBuilder FourBitConsecMisBuilder = new StringBuilder();
		
		for(int j=0; j<16; j++) {
			for(int i=0; i<str.length()-3; i++) {
				char a = str.charAt(i);
				char b = dictionary[j].charAt(i);
				
				//If there is a mismatch, check if the next 3 chars are a mismatch as well. If they are, raise the counter and keep the location of the first mismatch and the dictionary pointer
				if(a != b) {
					if((str.charAt(i+1) != dictionary[j].charAt(i+1)) && (str.charAt(i+2) != dictionary[j].charAt(i+2)) && (str.charAt(i+3) != dictionary[j].charAt(i+3))) {
						counter++;
						strlocation = i;
						dictlocation = j;
						i+=3;
					}
					else { //If they are not, try the next dictionary entry
						counter = 0;
						break;
					}
				}
				if(counter > 1) { //If there is more than 1 4-bit mismatch with this Dictionary entry, break and try the next 
					counter = 0;
					break;
				}
			}
			if(counter == 1) { //If after checking the whole string, there is exactly 1 4-bit consecutive mismatch, stop searching the Dictionary
				break;
			}
		}
		
		if(counter == 1) { //If after checking the whole string, there is exactly 1 4-bit consecutive mismatch
			reserved.put(pointer, true); //Reserve it
			FourBitConsecMisBuilder.append("101"); //Code for the FourBitConsecMis format
			FourBitConsecMisBuilder.append(mishash.get(strlocation)); //Write 5 bits for the leftmost mismatch location
			FourBitConsecMisBuilder.append(dicthash.get(dictlocation)); //Write the Dictionary word that matched
			cHash.put(pointer, FourBitConsecMisBuilder.toString()); //Write the whole String to the cHash
		}
		
	}
	
	public void Bitmask(String str, int pointer) {
		int counter = 0;
		int strlocation1 = 0;
		int strlocation2 = 0;
		int dictlocation = 0;
		String bitmaskstr = "";
		StringBuilder BitmaskBuilder = new StringBuilder();
		
		for(int j=0; j<16; j++) {
			for(int i=0; i<str.length(); i++) {
				char a = str.charAt(i);
				char b = dictionary[j].charAt(i);
				
				if(a != b && counter > 0) { //Find the last mismatch of the string
					counter++;
					strlocation2 = i;
				}
				
				if(a != b && counter == 0 && i < str.length()-3) { //Find the first mismatch of the string as long as it's not in the last 3 digits
					counter++;
					strlocation1 = i;
					dictlocation = j;
				}
			}
			
			if(strlocation2 - strlocation1 > 3) { //If the first and last locations have distance longer than 4 (their pointers are numbered more than 3 apart), reset everything
				strlocation1 = 0;
				strlocation2 = 0;
				counter = 0;
			}
			else { //Create bitmask value
				if((str.charAt(strlocation1+1) == dictionary[j].charAt(strlocation1+1)) && (str.charAt(strlocation1+2) == dictionary[j].charAt(strlocation1+2)) && (str.charAt(strlocation1+3) != dictionary[j].charAt(strlocation1+3))) {
					bitmaskstr = "1001";
					break;
				}
				else if((str.charAt(strlocation1+1) == dictionary[j].charAt(strlocation1+1)) && (str.charAt(strlocation1+2) != dictionary[j].charAt(strlocation1+2)) && (str.charAt(strlocation1+3) == dictionary[j].charAt(strlocation1+3))) {
					bitmaskstr = "1010";
					break;
				}
				else if((str.charAt(strlocation1+1) == dictionary[j].charAt(strlocation1+1)) && (str.charAt(strlocation1+2) != dictionary[j].charAt(strlocation1+2)) && (str.charAt(strlocation1+3) != dictionary[j].charAt(strlocation1+3))) {
					bitmaskstr = "1011";
					break;
				}
				else if((str.charAt(strlocation1+1) != dictionary[j].charAt(strlocation1+1)) && (str.charAt(strlocation1+2) == dictionary[j].charAt(strlocation1+2)) && (str.charAt(strlocation1+3) == dictionary[j].charAt(strlocation1+3))) {
					bitmaskstr = "1100";
					break;
				}
				else if((str.charAt(strlocation1+1) != dictionary[j].charAt(strlocation1+1)) && (str.charAt(strlocation1+2) == dictionary[j].charAt(strlocation1+2)) && (str.charAt(strlocation1+3) != dictionary[j].charAt(strlocation1+3))) {
					bitmaskstr = "1101";
					break;
				}
				else if((str.charAt(strlocation1+1) != dictionary[j].charAt(strlocation1+1)) && (str.charAt(strlocation1+2) != dictionary[j].charAt(strlocation1+2)) && (str.charAt(strlocation1+3) == dictionary[j].charAt(strlocation1+3))) {
					bitmaskstr = "1110";
					break;
				}
			}
		}
		if(counter > 1 && counter < 4) { //If after checking the whole string, there are between 2 and 3 mismatches
			reserved.put(pointer, true); //Reserve it
			BitmaskBuilder.append("010"); //Code for the BitmaskBuilder format
			BitmaskBuilder.append(mishash.get(strlocation1)); //Write 5 bits for the leftmost mismatch location
			BitmaskBuilder.append(bitmaskstr); //Write 4 bits for the bitmask pattern
			BitmaskBuilder.append(dicthash.get(dictlocation)); //Write the Dictionary word that matched
			cHash.put(pointer, BitmaskBuilder.toString()); //Write the whole String to the cHash
		}
	}
	
	public void TwoBitAnywhereMis(String str, int pointer) {
		int counter = 0;
		int strlocation1 = -1;
		int strlocation2 = -1;
		int dictlocation = -1;
		StringBuilder TwoBitAnywhereMisBuilder = new StringBuilder();
		
		for(int j=0; j<16; j++) {
			for(int i=0; i<str.length(); i++) {
				char a = str.charAt(i);
				char b = dictionary[j].charAt(i);
				
				if(counter > 2) { //If there are more than 2 mismatches with this Dictionary entry, break and try the next 
					counter = 0;
					break;
				}
				
				if(a != b && counter > 1) { //If there are 2 mismatches or more raise the counter
					counter++;
				}
				
				if(a != b && counter == 1) { //If there is a second mismatch raise the counter and keep the location of the mismatch and the dictionary pointer of the 2nd mismatch
					counter++;
					strlocation2 = i;
				}
				
				if(a != b && counter == 0) { //If there is a mismatch raise the counter and keep the location of the mismatch and the dictionary pointer
					counter++;
					strlocation1 = i;
					dictlocation = j;
				}
			}
			
			if(counter == 2) { //If after checking the whole string, there is exactly 2 mismatches, stop searching the Dictionary
				break;
			}
		}
		
		if(counter == 2) { //If after checking the whole string, there is exactly 2 mismatches
			reserved.put(pointer, true); //Reserve it
			TwoBitAnywhereMisBuilder.append("110"); //Code for the TwoBitAnywhereMis format
			TwoBitAnywhereMisBuilder.append(mishash.get(strlocation1)); //Write 5 bits for the 1st mismatch location
			TwoBitAnywhereMisBuilder.append(mishash.get(strlocation2)); //Write 5 bits for the 2nd mismatch location
			TwoBitAnywhereMisBuilder.append(dicthash.get(dictlocation)); //Write the Dictionary word that matched
			cHash.put(pointer, TwoBitAnywhereMisBuilder.toString()); //Write the whole String to the cHash
		}
		
	}
	
	public void OriginalBinaries(String str, int pointer) {
		for(int i=1; i<original.size(); i++) {
			if(reserved.get(i) == false) {
				cHash.put(i, "000" + original.get(i));
			}
		}
	}
	
	public void WriteDictionary() {
		StringBuilder dictBuilder = new StringBuilder();
		
		dictBuilder.append("xxxx\n");
		for(int i=0; i<dictionary.length; i++) {
			dictBuilder.append(dictionary[i]);
			if(i != dictionary.length-1) {
				dictBuilder.append("\n");
			}
		}
		WriteToFile(dictBuilder.toString());
	}

	public void WriteToFile(String str) {
		try {
			writer.write(str);
		} catch (IOException e) {
			System.out.println("Write error! " + e);
		}
	}
}
