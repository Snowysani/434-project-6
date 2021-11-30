package edu.tamu.csce434;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Vector;

public class Scanner {
	
	//constants
	private java.util.Map< String, Integer > keywordTokens = new java.util.HashMap< String, Integer >();
	private Vector<Integer> digit = new Vector<Integer>();
	private Vector<Integer> letter = new Vector<Integer>();
	private Vector<String> relOP = new Vector<String>();
	
	//temporary files
	private Vector<Integer> rawData;
    private int curcharacter;  //temporary current character
    private FileReader inputReader; //file
	
    //holds identifiers and makes sure there are no repeats
    private Vector<String> idents = new Vector<String>();
    
    public int sym; // current token on the input 
    public int val; // value of last number encountered
    public int id;  // index of last identifier
    
    
	public void closefile() {
		try {
			inputReader.close();
		} catch(IOException e) {
			System.out.println("Cannot close file."); 
		}
	}
	
	private Integer ChooseDesciptiveToken (String lexeme) {
		
		
		if(keywordTokens.containsKey(lexeme)) {
			sym = keywordTokens.get(lexeme);
			return -1;
		}
		sym = 61;
		return 61;
	}
	
	
	private String makeString() {
		String result = "";		
		
		for (int i=0; i<rawData.size(); i++) {
			int temp = rawData.elementAt(i);
			result += (char) temp;
		}
		
		return result;
	}
	
	private boolean stringCompare(String s1, String s2) {
		if (s1.length() != s2.length())
			return false;
		for (int i=0; i<s1.length(); i++) {
			if (s1.charAt(i) != s2.charAt(i))
				return false;
		}
		return true;
	}
	
	private void checkIdent(String ident){
    	for (int i=0; i<idents.size(); i++) {
			if(stringCompare(idents.elementAt(i), ident)) {
				id = i;
				return;
			}
		}
		//else
		idents.add(ident);
		id = idents.size()-1;
		return;
	}
	
	/**
	 * Advance to the next token 
	 */
    public void Next() {
    	if(curcharacter == 255) {
    		sym = 255;
    		return;
    	}
    	
    	rawData = new Vector<Integer>();
    	
    	//spaces and tabs
    	while (curcharacter == 32) {
    		Advance();
    	}
    	
    	//Diagram for "/" = 47
    	if (curcharacter == '/') {
    		Advance();
    		if (curcharacter == '/') {
    			Advance();
    			while (curcharacter != '\n') {
    				Advance();
    				if(curcharacter == 255) {
    					sym = 255;
    					return;
    				}
    			}
    			Advance();
    			Next();
    			return;
    		}
    		else {
    			sym = 2;
    			return;
    		}
    	}
    	
    	// checking for idents
    	if (Character.isLowerCase(curcharacter)) {
    		rawData.add(curcharacter);    		
    		Advance();
    		while(Character.isLowerCase(curcharacter) | Character.isDigit(curcharacter)) {
    			rawData.add(curcharacter);
    			Advance();
    		}
    		
    		String temp;
    		temp = makeString();
    		if (61 == ChooseDesciptiveToken(temp)) {
    			checkIdent(temp);
    		};
    		return;
    	}
    	
    	//finding the numbers
    	if (Character.isDigit(curcharacter)) {
    		rawData.add(curcharacter);
    		Advance();
    		while(Character.isDigit(curcharacter)) {
    			rawData.add(curcharacter);
    			Advance();
    		}
    		sym = 60;
    		
    		// reset val and replace it with the new value
    		val = 0;
    		if (rawData.size() > 1) {
	    		int j = 1;
	    		for (int i=rawData.size()-1; i>=0; i--) {
	    			val += Character.getNumericValue(rawData.elementAt(i)) * j;
	    			j *= 10;
	    		}
    		}
    		else 
    			val = Character.getNumericValue(rawData.elementAt(0));
    		
    		return;
    	}
    	
    	if (curcharacter == ',') {
    		sym = 31;
    		Advance();
    		return;
    	}
    	
    	if (curcharacter == ';') {
    		sym = 70;
    		Advance();
    		return;
    	} 
    	
    	if (curcharacter == '!') {
    		Advance();
    		if (curcharacter == '=') {
        		Advance();
        		sym = 21;
        		return;
        	}
    	}
    	
    	if (curcharacter == '<') {
    		Advance();
    		//a dash afterwards
    		if (curcharacter == '-') {
    			Advance();
    			sym = 40; // becomes token
    		}
    		else if(curcharacter == '='){ // 61 = "="
    			Advance();
    			sym = 24; //leq
    		}
    		else { //only less than
    			sym = 22; //lss
    		}
    		return;
    	}
    	
    	// > token
    	if (curcharacter == '>') {
    		Advance();
    		// = 
    		if(curcharacter == '='){ // 61 = "="
    			Advance();
    			sym = 23; //geq
    		}
    		else { //only greater than
    			sym = 25; //gtr
    		}
    		return;
    	}
    	
    	if(curcharacter == '=') {
    		Advance();
    		if(curcharacter == '=') {
        		Advance();
        		sym = 20;
        		return;
        	}
    	}
    	
    	if(curcharacter == '*') {
    		Advance();
    		sym = 1;
    		return;
    	}
    	
    	if(curcharacter == '+') {
    		Advance();
    		sym = 11;
    		return;
    	}
    	
    	if(curcharacter == '-') {
    		Advance();
    		sym = 12;
    		return;
    	}
    	
    	if(curcharacter == '.') {
    		Advance();
    		sym = 30;
    		return;
    	}
    	if(curcharacter == '(') {
    		Advance();
    		sym = 50;
    		return;
    	}
    	if(curcharacter == ')') {
    		Advance();
    		sym = 35;
    		return;
    	}
    	if(curcharacter == '{') {
    		Advance();
    		sym = 150;
    		return;
    	}
    	if(curcharacter == '}') {
    		Advance();
    		sym = 80;
    		return;
    	}
    	if(curcharacter == '[') {
    		Advance();
    		sym = 32;
    		return;
    	}
    	if(curcharacter == ']') {
    		Advance();
    		sym = 34;
    		return;
    	}
    	
    	sym = 0;
    	Advance();
    	Next();
    	return;
	}
    
    /**
     * Move to next char in the input
     */
	public void Advance() {
		try {
			int CurrentInt;
			if((CurrentInt = inputReader.read()) != -1){
				curcharacter = CurrentInt;
			}
			else {
				curcharacter = 255;
			}
		} catch(IOException e) {
			 System.out.println("character advancement error."); 
		}
	}

    public Scanner(String fileName) {
        try {
        	
        	inputReader = new FileReader(fileName);
        }
        catch(FileNotFoundException e) { 
            System.out.println("File Not Found."); 
        }

        // assign values to the constant variables
		keywordTokens.put("main", 200);
		keywordTokens.put("procedure", 113);
		keywordTokens.put("function", 112);
		keywordTokens.put("array", 111);
		keywordTokens.put("var", 110);
		keywordTokens.put("return", 103);
		keywordTokens.put("while", 102);
		keywordTokens.put("if", 101);
		keywordTokens.put("call", 100);
		keywordTokens.put("else", 90);
		keywordTokens.put("fi", 82);
		keywordTokens.put("od", 81);
		keywordTokens.put("let", 77);
		keywordTokens.put("semi", 70);
		keywordTokens.put("then", 41);
		keywordTokens.put("do", 42);
        
        for (int i=48; i<=57; i++) {
        	digit.add(i);
        }
        for (int i=97; i<=122; i++) {
        	letter.add(i);
        }
    	relOP.add("==");
    	relOP.add("!=");
    	relOP.add("<");
    	relOP.add("<=");
    	relOP.add(">");
    	relOP.add(">=");
    	
    	//read each character and append into token vector
    	this.Advance();

        //set initial global variables
        this.Next();
    }

    /**
     * Converts given id to name; returns null in case of error
     */
    public String Id2String(int id) { 
    	if(idents.size()-1 >= id) 
    	{
    		return idents.elementAt(id);
    	}
    	return null;
    }

    /**
     * Signal an error message
     * 
     */
    public void Error(String errorMsg) {
        System.out.println(errorMsg);
    }

    /**
     * Converts given name to id; returns -1 in case of error
     */
    public int String2Id(String name) {
    	try {
    		return Integer.valueOf(name);
    	}
    	catch(Error e) {
        	return -1;
    	}
    }
}

