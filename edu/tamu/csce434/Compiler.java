package edu.tamu.csce434;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

public class Compiler 
{
	//variables that define my compiler
	int buf[] = new int[2500];
	
	static int R[] = new int [32];
	static int PC, op, a, b, c, format;

	//For easy reference
	static int FP = 28;
	static int SP = 29;
	static int DP = 30;
	static int RETURN = 31;
	
	// emulated memory
	static final int MemSize = 10000; // bytes in memory (divisible by 4)

	static int M[] = new int [MemSize/4 - 1];
	
	private class Result 
	{
		String varName;
		String kind;
		String scope;
		int address;
		int regnum;
		int fixuplocation;
		int value;
		int lastSetInstruction;

		Result ()
		{
			varName = "";
			kind = "";
			scope = "";
			address = 0;
			regnum = 0;
			fixuplocation = 0;
			value = 0;
			lastSetInstruction = 0;
		}

		Result(Result r)
		{
			this.varName = r.varName;
			this.kind = r.kind;
			this.scope = r.scope;
			this.address = r.address;
			this.regnum = r.regnum;
			this.fixuplocation = r.fixuplocation;
			this.value = r.value;
			this.lastSetInstruction = r.lastSetInstruction;
		}
	}
	
	private ArrayList<Block> BlockChain = new ArrayList<Block>();

	private class Block
	{
		int BlockNumber;
		ArrayList<Line> lines = new ArrayList<Line>(); // list of lines that are in that block
		ArrayList<Integer> childrenIndexes = new ArrayList<Integer>(); // this is fine
	}

	int calculateCurrentBlockIndex()
	{
		int index = 0;
		for (int i = 0; i < BlockChain.size(); i++)
		{
			index += BlockChain.get(i).lines.size();
		}
		return index;
	}

	// Utilizing a stack to potentially keep the scope of blocks in which lines will be added to. 
	Stack<Block> blockScopeStack = new Stack<Block>();

	// setting a global statement type string to get some information in Line. 
	// TODO: re-investigate this method, see if there's a way we can get the statement type from a non-global level 
	Stack<String> currentStatementType = new Stack<String>();

	// List of the unfinished paths which need to be merged later
	// Used After every StatSequence to merge then and else blocks
	private ArrayList<Integer> ChildlessParents = new ArrayList<Integer>();
	
	private class Line
	{
		Boolean isRelational = false;
		String operator; // The beginning statement in IR (Ex. MOVE, MUL, WRITE)
		Result SetVar = new Result();
		// This is probably not needed, we can set operator to MOVE if assignment
		String statmentType; // utilize a statement type so we know if it's an assignment, maybe.
		ArrayList<Result> UsedVars = new ArrayList<Result>(); // something like that
		String FunctionName;
	}
	
	private java.util.HashMap< String, Vector<Array>> ArrayVariables = new java.util.HashMap< String, Vector<Array>>();
	
	private class Array 
	{
		String name;
		Vector<Integer> size;
		int address;
	}
	
	private edu.tamu.csce434.Scanner scanner;

	int inputNumber;
	
	private java.util.HashMap< Integer, String > tokenMap = new java.util.HashMap< Integer, String >();
	private java.util.HashMap< String, Result > identMap = new java.util.HashMap < String, Result >();
	private java.util.HashMap< String, Integer > ValidFuncNames = new java.util.HashMap < String, Integer >();
	
	// String holds the name of the Function, the vector of Results hold the name of the variables
	private java.util.HashMap< String, java.util.HashMap< String, Result >> FunctionLocals = new java.util.HashMap< String, java.util.HashMap< String, Result >>();
	private java.util.HashMap< String, java.util.HashMap< String, Result >> FunctionArguments = new java.util.HashMap< String, java.util.HashMap< String, Result >>();
	private String functionName;
	private boolean inFunction = false;
	private Result ReturnRegister = new Result();

	// Hold the latest instruction counts for variables. Used for SetVar(s)
	private java.util.HashMap< String, Integer > varInstructionMap = new java.util.HashMap< String, Integer >();
	Stack<Integer> tempLineNumbers = new Stack<Integer>();
	
	private Vector<String> preDefIdents = new Vector<String>();
	
	
	// Writing Line Data to the output file
	private int instructionNumber = 1;
	private void WriteLineData(FileOutputStream OutputFile, int index) {
		
		Block curBlock = BlockChain.get(index);
		try {
			for (int i=0; i<curBlock.lines.size(); i++) {
				
				OutputFile.write(String.valueOf(instructionNumber).getBytes());
				OutputFile.write(". ".getBytes());
				
				if (curBlock.lines.get(i).operator == "Return") {
					OutputFile.write("RET".getBytes());
					OutputFile.write(' ');
					continue;
				}	
				
				// Printing the operator
				OutputFile.write(curBlock.lines.get(i).operator.getBytes());
				OutputFile.write(' ');
				
				// Prints the name of the Set Variable
				if (curBlock.lines.get(i).SetVar.varName != null ) {
					OutputFile.write(curBlock.lines.get(i).SetVar.varName.getBytes());
					OutputFile.write(' ');
				}
				
				// Print the name of Other variables
				if (curBlock.lines.get(i).UsedVars != null ) {
					for (int j=0; j<curBlock.lines.get(i).UsedVars.size(); j++) {
						OutputFile.write(curBlock.lines.get(i).UsedVars.get(j).varName.getBytes());
						OutputFile.write(' ');
					}
				}
				
				if (curBlock.lines.get(i).operator == "call") {
					// OutputFile.write(curBlock.lines.get(i).FunctionName.getBytes());
					// OutputFile.write(' ');
					OutputFile.write(curBlock.lines.get(i).FunctionName.getBytes());
					OutputFile.write(' ');
				}
				
				OutputFile.write("\\l".getBytes());
				instructionNumber++;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	// Writing the arrows between graphs into DOT diagram
	private void ConnectSubgraphs(FileOutputStream OutputFile, int index) {
		
		Block curBlock = BlockChain.get(index);
		try {
			for(int i=0; i<curBlock.childrenIndexes.size(); i++) {
				
				OutputFile.write("  Block_".getBytes());
        		OutputFile.write(String.valueOf(index).getBytes());
        		
        		OutputFile.write(" -> ".getBytes());
        		
        		OutputFile.write("Block_".getBytes());
        		OutputFile.write(String.valueOf(curBlock.childrenIndexes.get(i)).getBytes());
        		OutputFile.write('\n');
			}
		}
		catch (Exception e) {
			System.out.print(e.getLocalizedMessage());
		}
		return;
	}


	// Constructor for the compiler
	public Compiler(String args)
	{
		
		scanner = new Scanner(args);
		
		// fill the tokenMap with the string values from scanner
		tokenMap.put(  0, "error");
		tokenMap.put(  1, "times");
		tokenMap.put(  2, "div");
        tokenMap.put( 11, "plus");
        tokenMap.put( 12, "minus");
        tokenMap.put( 20, "eql");
        tokenMap.put( 21, "neq");
        tokenMap.put( 22, "lss");
        tokenMap.put( 23, "geq");
        tokenMap.put( 24, "leq");
        tokenMap.put( 25, "gtr");
        tokenMap.put( 30, "period");
        tokenMap.put( 31, "comma");
        tokenMap.put( 32, "openbracket");
        tokenMap.put( 34, "closebracket");
        tokenMap.put( 35, "closeparen");
        tokenMap.put( 40, "becomes");
        tokenMap.put( 41, "then");
        tokenMap.put( 42, "do");
        tokenMap.put( 50, "openparen");
        tokenMap.put( 60, "number");
        tokenMap.put( 61, "ident");
        tokenMap.put( 70, "semicolon");
        tokenMap.put( 77, "let");
        tokenMap.put( 80, "end");
        tokenMap.put( 81, "od");
        tokenMap.put( 82, "fi");
        tokenMap.put( 90, "else");
        tokenMap.put(100, "call");
        tokenMap.put(101, "if");
        tokenMap.put(102, "while");
        tokenMap.put(103, "return");
        tokenMap.put(110, "var");
        tokenMap.put(111, "arr");
        tokenMap.put(112, "function");
        tokenMap.put(113, "procedure");
        tokenMap.put(150, "begin");
        tokenMap.put(200, "main");
        tokenMap.put(255, "eof");
        
        //these can be overwritten by a user defined identifier 
        //you can call these like functions
        preDefIdents.add("inputnum");
        preDefIdents.add("outputnum");
        preDefIdents.add("outputnewline");
		
        //creating the initial values for the compiler
        for (int i = 0; i < 32; i++) { R[i] = 0; };
		PC = 0; 
		R[DP] = MemSize - 1;
		ReturnRegister.regnum = 27;
		ReturnRegister.kind = "reg";
        
	}
	
	/* computing different variations of registers with immediate
		op + 16 is the immediate version of each operation
	 	immediate and register can come in any order, just use normal o */
	private void compute (int op, Result result, Result x, Result y) {

		Line termLine = new Line();

		// termLine is always temporary. 
		// Use this in our temporary list. 
		BlockChain.get(BlockChain.size()-1).lines.add(termLine);
		int currentIndex = calculateCurrentBlockIndex();
		tempLineNumbers.push(currentIndex);
		
		Result x_line = new Result(x);

		Result y_line = new Result(y);

		Result separateResult = new Result(result);
		// separateResult.address = result.address;
		// separateResult.fixuplocation = result.fixuplocation;
		// separateResult.kind = result.kind;
		// separateResult.lastSetInstruction = result.lastSetInstruction;
		// separateResult.regnum = result.regnum;
		// separateResult.scope = result.scope;

		if (x_line.kind == "reg" && varInstructionMap.containsKey(x.varName))
		{
			x_line.lastSetInstruction = varInstructionMap.get(x.varName);
			x_line.varName = x.varName + "_" + Integer.toString(x_line.lastSetInstruction);
		}

		if (y_line.kind == "reg" && varInstructionMap.containsKey(y.varName))
		{
			y_line.lastSetInstruction = varInstructionMap.get(y.varName);
			y_line.varName = y.varName + "_" + Integer.toString(y_line.lastSetInstruction);
		}

		if (x.kind == "const" && (y.kind == "reg" || y.kind == "arr")) {
			termLine.UsedVars.add(y_line);
			termLine.UsedVars.add(x_line);
		}
		else {
			termLine.UsedVars.add(x_line);
			termLine.UsedVars.add(y_line);
		}
		
		if (x.kind == "const" && y.kind == "const") {
			Load(x);
			result.regnum = x.regnum;
			buf[PC++] = DLX.assemble(op + 16, result.regnum, x.regnum, y.value);
			
			termLine.operator = DLX.mnemo[op + 16];
		}
		else if (x.kind == "const" && (y.kind == "reg" || y.kind == "arr")) {
			Load(x);
			result.regnum = x.regnum;
			buf[PC++] = DLX.assemble(op + 16, result.regnum, y.regnum, x.regnum);

			termLine.operator = DLX.mnemo[op + 16];
		}
		else if ((x.kind == "reg" || x.kind == "arr") && y.kind == "const") {
			buf[PC++] = DLX.assemble(op + 16, result.regnum, x.regnum, y.value);
			
			termLine.operator = DLX.mnemo[op + 16];
		}
		else {	
			buf[PC++] = DLX.assemble(op, result.regnum, x.regnum, y.regnum);
			
			termLine.operator = DLX.mnemo[op];
		}
		result.varName = "(" + Integer.toString(currentIndex) + ")";

		return;
	}
	
	//Load values onto the stack
	private void Store(Result x) {
		if(x.scope == functionName  && x.scope != "main")
			buf[PC++] = DLX.assemble(DLX.STW, x.regnum, FP, x.address);
		else
			buf[PC++] = DLX.assemble(DLX.STW, x.regnum, DP, x.address);
	}
	
	private Result Load(Result x) {
		
		if(x.kind == "reg") {
			AllocateReg(x);
			if(x.scope == functionName && x.scope != "main")
				buf[PC++] = DLX.assemble(DLX.LDW, x.regnum, FP, x.address);
			else
				buf[PC++] = DLX.assemble(DLX.LDW, x.regnum, DP, x.address);
		}
		else if (x.kind == "const") {
			if (x.value == 0) {
				x.regnum = 0;
				x.kind = "reg";
			}
			else {
				AllocateReg(x);
				buf[PC++] = DLX.assemble(DLX.ADDI, x.regnum, 0, x.value);
			}
		}
		return x;
	}
	
	//Checking for an open register it to our value
	private int AllocateReg(Result x) {
		x.kind = "reg";
		
		for (int i=1; i<27; i++) {
			if (R[i] == 0) {
				R[i] = 1;
				x.regnum = i;
				return i;
			}
		}
		return 0;
	}
	
	private void DeallocateReg(Result x) {
		R[x.regnum] = 0;
		return;
	}
	
	// Use this function to print errors, i is symbol/token value
	private void printError(int i) 
	{
		System.out.println("Unexpected token error: " + i);
	}
	
	private boolean peek(String checkVar) {
		if (stringCompare(checkVar, tokenMap.get(scanner.sym)))
			return true;
		return false;
	}

	private int GetValue(int type) {
		//get the actual value the corresponds to that data type
		if(type == 60) //number
			return scanner.val;
		else if(type == 61) //ident
			return identMap.get(scanner.Id2String(scanner.id)).value;
		return type;
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
	
	// Use this function to accept a Token and and to get the next Token from the Scanner
	private boolean accept(String s) 
	{
		//System.out.println(tokenMap.get(scanner.sym) + " " + s);
		if(stringCompare(tokenMap.get(scanner.sym), s)) {
			return true;
		}
		return false;
	}

	// Use this function whenever your program needs to expect a specific token
	private void expect(String s) 
	{
		if (accept(s)) {
			scanner.Next();
			return;
		}
		System.out.print("Character we exected to get: " + s + ", ");
		printError(scanner.sym);
	
	}
	
	private int CheckIfElement (String function, String elemName) {
		if(ArrayVariables.containsKey(function)) {
			for (int i=0; i<ArrayVariables.get(function).size(); i++) {
				if (stringCompare(ArrayVariables.get(function).get(i).name, elemName)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private String CheckArrayScope(String arrName) {
		if (ArrayVariables.size() != 0 && functionName != "main") {
			if (ArrayVariables.containsKey(functionName)) {
				if(CheckIfElement(functionName, arrName) != -1) {
					return functionName;
				}
			}
		}
		
		return "main"; 
	}
	
	String CurrentArrayName;
	
	private Result IDENT() {
		Result result = new Result();
		int s = scanner.sym; 

		if ( scanner.Id2String(scanner.id) != null ) {
				
			//Checking arguments
			if(FunctionArguments.size() != 0 && functionName != "main") {
				if (FunctionArguments.containsKey(functionName)) {	
					if (FunctionArguments.get(functionName).containsKey(scanner.Id2String(scanner.id))) {
						//get the most recent set of arguments, then find the Result related to the Name of the string
						result = FunctionArguments.get(functionName).get(scanner.Id2String(scanner.id));
						result.kind = "reg";
						result.scope = functionName;
						result.varName = scanner.Id2String(scanner.id);
						scanner.Next();
						return result;
					}
				}
			}	
			
			//First check the Local variable map
			if(FunctionLocals.size() != 0 && functionName != "main") {
				if (FunctionLocals.containsKey(functionName)) {
					if (FunctionLocals.get(functionName).containsKey(scanner.Id2String(scanner.id))) {
						//get the most recent set of arguments, then find the Result related to the Name of the string
						result = FunctionLocals.get(functionName).get(scanner.Id2String(scanner.id));
						result.kind = "reg";
						result.scope = functionName;
						result.varName = scanner.Id2String(scanner.id);
						scanner.Next();
						return result;
					}
				}
			}
			
			//Then check global variable map
			if (identMap.containsKey(scanner.Id2String(scanner.id))) {
				result = identMap.get(scanner.Id2String(scanner.id));
				result.kind = "reg";
				result.scope = "main";
				result.varName = scanner.Id2String(scanner.id);
				scanner.Next();
				return result;
			}		
			
			if (ArrayVariables.size() != 0 && functionName != "main") {
				if (ArrayVariables.containsKey(functionName)) {
					int index;
					if((index = CheckIfElement(functionName, scanner.Id2String(scanner.id))) != -1) {
						result.address = ArrayVariables.get(functionName).get(index).address;
						CurrentArrayName = ArrayVariables.get(functionName).get(index).name;
						result.kind = "arr";
						result.scope = functionName;
						result.varName = scanner.Id2String(scanner.id);
						scanner.Next();
						return result;
					}
				}
			}	
			
			if (ArrayVariables.size() != 0) {
				if (ArrayVariables.containsKey("main")) {
					int index;
					if((index = CheckIfElement("main", scanner.Id2String(scanner.id))) != -1) {
						result.address = ArrayVariables.get("main").get(index).address;
						CurrentArrayName = ArrayVariables.get("main").get(index).name;
						result.kind = "arr";
						result.scope = "main";
						result.varName = scanner.Id2String(scanner.id);
						scanner.Next();
						return result;
					}
				}
			}
		}
		result.varName = scanner.Id2String(scanner.id);
		scanner.Next();
		return result;
		
	}
	
	private Result IndexRegister = new Result();
	
	private Result DESIGNATOR() {
		//fixes problems with same ident in the same line
		//Using one memory location for variable is bad -> new
		Result newLocation = new Result();
		Result result = IDENT();
		newLocation.varName = result.varName;
		newLocation.kind = "reg";
		newLocation.regnum = result.regnum;
		newLocation.address = result.address;
		newLocation.scope = result.scope;
		
		// do this is the ident returns a variable
		if(!peek("openbracket")) {
			Load(newLocation);
			return newLocation;
		}
		
		Vector<Result> findValues = new Vector<Result>();
		
		while (peek("openbracket")) {
			
			expect("openbracket");
			
			Result value = new Result();
			value = EXPRESSION();
			if( value.kind == "const" ) {
				AllocateReg(value);
				buf[PC++] = DLX.assemble(DLX.ADDI, value.regnum, 0, value.value);
			}
			findValues.add(value);
			
			expect("closebracket");
		}
		
		//Load the final address into a register to be used as a dynamic register, not const.
		IndexRegister = findValues.elementAt(0);
		int total = 0;
		int multiplier = 1;
		String arrayScope = CheckArrayScope(CurrentArrayName);
		int index = CheckIfElement(arrayScope, CurrentArrayName);
		
		
		buf[PC++] = DLX.assemble(DLX.MULI, IndexRegister.regnum, IndexRegister.regnum, (multiplier) * 4);
		buf[PC++] = DLX.assemble(DLX.ADDI, IndexRegister.regnum, IndexRegister.regnum, ArrayVariables.get(arrayScope).get(index).address);
		total += findValues.elementAt(0).value * multiplier;
		multiplier *= ArrayVariables.get(arrayScope).get(index).size.elementAt(0);
		
		for (int j=1; j<findValues.size(); j++) 
		{
			Result holdValue = findValues.elementAt(j);
			// if we have sizes = [3][3][3]
			// if we want to find [1][2][1]
			// we calculate 1*(1) + 2*(3) + 1*(9) = 19 
			// We only care that it is unique and within the range of (0-26).
			// multiplier get the previous size of array multiplied to it.
			buf[PC++] = DLX.assemble(DLX.MULI, holdValue.regnum, holdValue.regnum, (multiplier) * 4);
			buf[PC++] = DLX.assemble(DLX.ADD, IndexRegister.regnum, IndexRegister.regnum, holdValue.regnum);
			total += findValues.elementAt(j).value * multiplier;
			multiplier *= ArrayVariables.get(arrayScope).get(index).size.elementAt(j);
			DeallocateReg(holdValue);

		}
		
		
		
		newLocation.address = ArrayVariables.get(arrayScope).get(index).address + (4 * total);
		AllocateReg(newLocation);
		newLocation.kind = "arr";
		
		if(arrayScope != "main")
			buf[PC++] = DLX.assemble(DLX.LDX, newLocation.regnum, FP, IndexRegister.regnum);
		else {
			buf[PC++] = DLX.assemble(DLX.LDX, newLocation.regnum, DP, IndexRegister.regnum);
		}
		return newLocation;
		
	}
	
	
	private Result FACTOR() {
		Result result = new Result();
		int ret = scanner.sym;

		if (tokenMap.get(ret) == "ident") {
			result = DESIGNATOR();
		}
		else if (tokenMap.get(ret) == "number") {
			result.kind = "const";
			result.value = GetValue(ret);
			result.varName = String.valueOf(result.value);
			scanner.Next();
		}
		else if (tokenMap.get(ret) == "openparen") {
			scanner.Next();
			result = EXPRESSION();
			expect("closeparen");
		}	
		else if (tokenMap.get(ret) == "call") {
			result = FUNCCALL();
		}
		return result;
	}
	
	private Result TERM() {
		Result factor = new Result();
		factor = FACTOR();
		String s = tokenMap.get(scanner.sym);
		if(s != "times" && s != "div") {
			return factor;
		}
		
		Result factor2 = new Result();
		while(s == "times" || s == "div") {
			
			scanner.Next();	
			factor2 = FACTOR();	
			
			if (s == "times") {
				compute(DLX.MUL, factor, factor, factor2);
			}
			else {
				compute(DLX.DIV, factor, factor, factor2);
			}
			if (factor2.kind == "reg")
				DeallocateReg(factor2);
			
			s = tokenMap.get(scanner.sym);
		}
		return factor;
	}
	
	private Result EXPRESSION() {
		Result term = new Result();
		term = TERM();
		
		String s = tokenMap.get(scanner.sym);
		if(s != "plus" && s != "minus") {
			return term;
		}
		
		while(s == "plus" || s == "minus") {
			Result term2 = new Result();
			scanner.Next();
			
			term2 = TERM();
			
			if (s == "plus") {
				compute(DLX.ADD, term, term, term2);
			}
			else {
				compute(DLX.SUB, term, term, term2);	
			}
			if (term2.kind == "reg")
				DeallocateReg(term2);
			
			s = tokenMap.get(scanner.sym);
		}
		return term;
	}
	
	private Result RELATION() {
		
		Block previousBlock = BlockChain.get(BlockChain.size() - 1);
		ChildlessParents.add(previousBlock.BlockNumber);

		Block relationBlock = new Block();
		BlockChain.add(relationBlock);
		relationBlock.BlockNumber = BlockChain.size() - 1;

		while(ChildlessParents.size() != 0) {
			BlockChain.get(ChildlessParents.get(0)).childrenIndexes.add(relationBlock.BlockNumber);
			ChildlessParents.remove(0);
		}

		//find values of a and b in relationship
		Result A = EXPRESSION();
		
		String relOP = tokenMap.get(scanner.sym);
		scanner.Next();
		Result B = EXPRESSION();
		
		//create instruction that subtracts the two to get the resulting value
		Result relation = new Result();
		AllocateReg(relation);
		relation.varName = "NONE";
		relation.value = A.value - B.value;
		compute(DLX.CMP, relation, A, B);
		
		
		// TODO: We need a handle on vars A and B to do the comparison.
		// 		 However, we also would greatly benefit from knowing if we are dealing with an if or a while.
		// 		 A potential solution might be to use a stack of which statement type we are looking at. 
		// 		 With enough pops or pushes, we would get back to knowing if we are at an if or a while. I think. Maybe. 
		Line relationLine = new Line(); // A line created for our relational comparisons. 
		relationLine.isRelational = true; 
		relationLine.statmentType = "relation";
		BlockChain.get(BlockChain.size()-1).lines.add(relationLine);
		
		if (A.kind == "reg")
		A.lastSetInstruction = varInstructionMap.get(A.varName);
		if (B.kind == "reg")
		B.lastSetInstruction = varInstructionMap.get(B.varName);

		Result a_line = A;
		Result b_line = B;

		a_line.varName = A.varName + "_" + Integer.toString(A.lastSetInstruction);
		b_line.varName = B.varName + "_" + Integer.toString(B.lastSetInstruction);

		relationLine.UsedVars.add(relation);
		//relationLine.UsedVars.add(a_line);
		//relationLine.UsedVars.add(b_line);
		
		if(A.kind == "reg")
			DeallocateReg(A);
		if(B.kind == "reg")
			DeallocateReg(B);
		
		// create branch instruction based in type relOP and resulting value
		relation.fixuplocation = PC;
		
		// these instructions all have incorrect Branch locations -> opposite branching instruction
		// We want to branch when our relop has an incorrect value
		if(relOP == "eql") {
			buf[PC++] = DLX.assemble(DLX.BNE, relation.regnum, 0);
			relationLine.operator = "BNE";
		}
		else if(relOP == "neq") {
			buf[PC++] = DLX.assemble(DLX.BEQ, relation.regnum, 0);
			relationLine.operator = "BEQ";
		}
		else if(relOP == "lss") {
			buf[PC++] = DLX.assemble(DLX.BGE, relation.regnum, 0);
			relationLine.operator = "BGE";
		}
		else if(relOP == "leq") { //branch Greater than
			buf[PC++] = DLX.assemble(DLX.BGT, relation.regnum, 0);
			relationLine.operator = "BGT";
		}		
		else if(relOP == "gtr") {
			buf[PC++] = DLX.assemble(DLX.BLE, relation.regnum, 0);
			relationLine.operator = "BLE";
		}
		else if(relOP == "geq") {
			buf[PC++] = DLX.assemble(DLX.BLT, relation.regnum, 0);
			relationLine.operator = "BLT";
		}
		return relation;
	}
	
	private void ASSIGNMENT() {
		
		expect("let");
		Result result = DESIGNATOR();
		
		Result IndexRegisterhold = new Result(); //save IndexRegister returnValue
		
		if(result.kind == "arr") {
			IndexRegisterhold.regnum = IndexRegister.regnum;
		}
		
		expect("becomes");
		
		Result setValue = EXPRESSION();
		
		// Insert the assignment line after the expression lines have been created
		Line assignLine = new Line();
		
		varInstructionMap.put(result.varName, calculateCurrentBlockIndex() + 1); // Result gets assigned a new value 
		result.lastSetInstruction = calculateCurrentBlockIndex() + 1;

		String lineVarName = result.varName + "_" + Integer.toString(result.lastSetInstruction);
		//result.varName = lineVarName;
		assignLine.SetVar = result;
		assignLine.SetVar.varName = lineVarName;

		assignLine.operator = "MOVE";

		BlockChain.get(BlockChain.size()-1).lines.add(assignLine);
		
		// if (setValue.kind == "reg")
		// {
		// 	// update the name 
		// 	int setValueInstNumber = varInstructionMap.get(setValue.varName);
		// 	setValue.varName = setValue.varName + Integer.toString(setValueInstNumber); // last time it was updated is the name
		// }

		//If we finish the function without a return
		if(setValue == null) {
			Result noReturn = new Result();
			noReturn.kind ="const";
			noReturn.value = 0;
			setValue = noReturn;
			assignLine.UsedVars.add(noReturn);
		}
		
		// Temporary list of lines 
		// In Compute, we add to that list. 
		// Here, if that list is not empty, use that. 
		// But if it is empty, use SetValue. (x*3) + (x*2)
		assignLine.UsedVars.add(setValue);
		
		if (setValue.kind == "const") {
			Load(setValue);
		}
		
		int identaddress = result.address;
		setValue.address = identaddress;
		
		//store the result into memory
		if (result.kind == "arr") {
			if(CheckArrayScope(CurrentArrayName) != "main")
				buf[PC++] = DLX.assemble(DLX.STX, setValue.regnum, FP, IndexRegisterhold.regnum);
			else
				buf[PC++] = DLX.assemble(DLX.STX, setValue.regnum, DP, IndexRegisterhold.regnum);
			
			DeallocateReg(IndexRegisterhold);
			DeallocateReg(IndexRegister);
		}
		else { // regular identifier
			DeallocateReg(result);
					

			result.regnum = setValue.regnum;
			result.value = setValue.value;
			Store(result);
		}
		
		DeallocateReg(result);
		DeallocateReg(setValue);
		
		return;
	}
	
	private Result FUNCCALL() {
		expect("call");
		
		Line CallLine = new Line();
		CallLine.operator = "CALL";
		BlockChain.get(BlockChain.size()-1).lines.add(CallLine);
		
		Result holdInput = new Result();
		
		CallLine.FunctionName = holdInput.varName;
		
		//inputnum()
		if(stringCompare(scanner.Id2String(scanner.id), preDefIdents.get(0))) { 

			holdInput.kind = "reg";
			AllocateReg(holdInput);
			
			scanner.Next();
			if(tokenMap.get(scanner.sym) == "openparen") {
				scanner.Next();
				expect("closeparen");
				buf[PC++] = DLX.assemble(DLX.RDI, holdInput.regnum);
			}
			
			holdInput.varName = "ReadInput";
			CallLine.UsedVars.add(holdInput);
			
			Result readVar = new Result();
			readVar.varName = "(" + Integer.toString(calculateCurrentBlockIndex()) + ")";
			
			return readVar;
		}
		
		//outputnum(x)
		else if(stringCompare(scanner.Id2String(scanner.id), preDefIdents.get(1))) { 
			scanner.Next();
			expect("openparen");
			
			holdInput = EXPRESSION();
			
			holdInput.lastSetInstruction = varInstructionMap.get(holdInput.varName);
			Result holdInput_line = holdInput;
			holdInput_line.varName = holdInput.varName + "_" + Integer.toString(holdInput.lastSetInstruction);
			CallLine.UsedVars.add(holdInput);
			
			if (holdInput.kind == "const")
				Load(holdInput);
			expect("closeparen");
			//System.out.print(holdInput.value);
			buf[PC++] = DLX.assemble(DLX.WRD, holdInput.regnum);
			DeallocateReg(holdInput);
			DeallocateReg(IndexRegister);
			return holdInput;
		}
		
		//outputnewline()
		else if(stringCompare(scanner.Id2String(scanner.id), preDefIdents.get(2))) { 
			scanner.Next();
			if(peek("openparen")) {
				expect("openparen");
				expect("closeparen");
			}
			
			holdInput.varName = "NewLine";
			CallLine.UsedVars.add(holdInput);
			
			//System.out.print('\n');
			buf[PC++] = DLX.assemble(DLX.WRL);
			return holdInput;
		}
		//call function
		else {
			String funcName = new String();
			for (int i=0; i<ValidFuncNames.size(); i++) {
				funcName = scanner.Id2String(scanner.id);
				if(ValidFuncNames.containsKey(funcName)) {
					
					Vector<Integer> StoredRegNums = new Vector<Integer>();
					//Storing the Registers
					for (int j=1; j<27; j++) {
						if (R[j] != 0) {
							StoredRegNums.add(j);
							buf[PC++] = DLX.assemble(DLX.PSH, j ,SP, -4);
							R[j] = 0; //deallocate register
						}
					}
					
					// Start storing the Function Parameters
					scanner.Next();
					if(peek("openparen")) {
						scanner.Next();
						
						if(!peek("closeparen")) {
							
							holdInput = EXPRESSION();
							if (holdInput.kind == "const")
								Load(holdInput);

							buf[PC++] = DLX.assemble(DLX.PSH, holdInput.regnum, SP, -4);
							DeallocateReg(holdInput);
						
						}
						while(!peek("closeparen")) {
							Result holdInput2 = new Result();
							
							expect("comma");
							holdInput2 = EXPRESSION();
							if (holdInput2.kind == "const")
								Load(holdInput2);
							buf[PC++] = DLX.assemble(DLX.PSH, holdInput2.regnum, SP, -4);
							DeallocateReg(holdInput2);
						}
						
						
						expect("closeparen");
					}
					
					buf[PC++] = DLX.assemble(DLX.JSR, ValidFuncNames.get(funcName)*4);
					
					//Restoring Registers
					for (int j=26; j>0; j--) {
						
						if (StoredRegNums.size() == 0)
							break;
						if (j == StoredRegNums.lastElement()) {
							StoredRegNums.remove(StoredRegNums.size()-1);
							buf[PC++] = DLX.assemble(DLX.POP, j ,SP, 4);
							R[j] = 1; //reallocate register
						}
					}
					Result StoreReturn = new Result();
					AllocateReg(StoreReturn);
					buf[PC++] = DLX.assemble(DLX.ADDI, StoreReturn.regnum, ReturnRegister.regnum, 0);
					
					// StoreReturn.varName = "testing";
					// CallLine.SetVar = StoreReturn;
					
					return StoreReturn;
				}
			}
		}	
		scanner.Error("Not a valid Ident");
		return ReturnRegister;
	}

	private void Fixup( int fixuplocation ) {
		// since I put 0 in the return location, I can just add the fixup location to the end
		// of the already built instruction
		buf[fixuplocation] += PC - fixuplocation;
	}
	
	private void UncondBraFwd( Result branchLoc ) {
		// this is a broken branch forward, will need to be fixed later
		branchLoc.fixuplocation = PC;
		buf[PC++] = DLX.assemble(DLX.BEQ, 0, 0);
	}
	
	private void UncondBraBack( int fixuplocation ) {
		buf[PC++] = DLX.assemble(DLX.BEQ, 0, fixuplocation-PC);
	}
	
	private void IFSTATEMENT() {
		
		// Capturing parent index, so we can set its childrenIndexes later
		int parentIndex = BlockChain.size()-1;
		
		expect("if");
		//creating the two initial jump instructions and setting fixuplocation
		Result negJump;
		negJump = RELATION();
		parentIndex = BlockChain.size()-1;

		expect("then");
		
		// adding thenBlock's index to the Parent's Children
		// the actual block will be created in STATSEQUENCE
		
		BlockChain.get(parentIndex).childrenIndexes.add(BlockChain.size());
		ArrayList<Integer> children = new ArrayList<Integer>();
		
		Result follow = new Result();
		
		STATSEQUENCE();
		
		// managing children, we don't want next elseblock to use these children
		while(ChildlessParents.size() != 0) {
			children.add(ChildlessParents.get(0));
			ChildlessParents.remove(0);
		}
		
		if(peek("else")) {
			
			// adding elseBlock's index to the Parent's Children
			BlockChain.get(parentIndex).childrenIndexes.add(BlockChain.size());
			
			scanner.Next();
			UncondBraFwd(follow);
			
			negJump.address = PC;
			Fixup(negJump.fixuplocation);
			STATSEQUENCE();
			
			Fixup(follow.fixuplocation);
			expect("fi");
		}
		else {
			negJump.address = PC;
			Fixup(negJump.fixuplocation);
			expect("fi");
		}
		DeallocateReg(negJump);
		
		// adding previous childlessParents
		while(children.size() != 0) {
			ChildlessParents.add(children.get(0));
			children.remove(0);
		}
		
		Block CurBlock = new Block();
		CurBlock.BlockNumber = BlockChain.size();
		while(ChildlessParents.size() != 0) {
			BlockChain.get(ChildlessParents.get(0)).childrenIndexes.add(CurBlock.BlockNumber);
			ChildlessParents.remove(0);
		}
		BlockChain.add(CurBlock);
		
		return;
	}
	
	private void WHILESTMT() {
		
		// Capturing parent index, so we can set its childrenIndexes later
		int parentIndex = BlockChain.size()-1;
		// BlockChain.get(parentIndex).childrenIndexes.add(BlockChain.size());
		
		expect("while");
		
		int loopLocation = PC+1;
		Result negJump = RELATION();
		
		parentIndex = BlockChain.size()-1;
		BlockChain.get(parentIndex).childrenIndexes.add(BlockChain.size());

		
		expect("do");
		STATSEQUENCE();
		expect("od");
		
		// Connect all of the unfinished paths back to the first whileBlock
		while (ChildlessParents.size() != 0) {
			BlockChain.get(ChildlessParents.get(0)).childrenIndexes.add(parentIndex);
			ChildlessParents.remove(0);
		}
		
		// Our initial whileBlock becomes "childless" to continue the sequence after it exits
		Block CurBlock = new Block();
		CurBlock.BlockNumber = BlockChain.size();
		BlockChain.get(parentIndex).childrenIndexes.add(CurBlock.BlockNumber);
		BlockChain.add(CurBlock);
		
		UncondBraBack(loopLocation);
		Fixup(negJump.fixuplocation);
		DeallocateReg(negJump);
	}
	
	private Result RETURNSTMT() {
			
		Result returnValue = new Result();
		expect("return");
		
		if( !peek("semicolon") && !peek("else") && !peek("fi") && !peek("end") && !peek("od")) {
			
			returnValue = EXPRESSION();
			
			if(returnValue.regnum != 27) {
				if (returnValue.kind == "const")
					buf[PC++] = DLX.assemble(DLX.ADDI, 27, 0, returnValue.value);
				else
					buf[PC++] = DLX.assemble(DLX.ADDI, 27, returnValue.regnum, 0);
			}
		}
		
		Line ReturnLine = new Line();
		ReturnLine.operator = "Return";
		BlockChain.get(BlockChain.size()-1).lines.add(ReturnLine);
		
		epilogue();
		
		return returnValue;
	}
	
	private void STATEMENT() {
		String statementType = tokenMap.get(scanner.sym);
		if(statementType == "let")
		{
			currentStatementType.add("let");
			ASSIGNMENT();
		}
		else if(statementType == "call") {
			currentStatementType.add("functionCall"); // Maybe move this after funccall, one line after? Perhaps it won't work entirely. Need to do additional testing.
			Result faultyReturn = FUNCCALL();
			DeallocateReg(faultyReturn);
		}
		else if(statementType == "if")
		{
			currentStatementType.add("if");
			IFSTATEMENT();
		}
		else if(statementType == "while")
		{
			currentStatementType.add("while");
			WHILESTMT();
		}
		else if (statementType == "return")
		{
			currentStatementType.add("while");
			RETURNSTMT();
		}
		else
			scanner.Error("Invalid Statement Option");
		return;
	}
	
	private void STATSEQUENCE() {
		//Holds current token name
		Block CurBlock = new Block();
		CurBlock.BlockNumber = BlockChain.size();
		BlockChain.add(CurBlock);
		
		//If there was a previous block(s), add this index as a child to the parents
		while (ChildlessParents.size() != 0) {
			BlockChain.get(ChildlessParents.get(0)).childrenIndexes.add(CurBlock.BlockNumber);
			ChildlessParents.remove(0);
		}
		
		String statementType;
		
		do {
			if (tokenMap.get(scanner.sym) == "semicolon")
				scanner.Next(); // we only need to do this after the first statement
			
			statementType = tokenMap.get(scanner.sym);
			
			if(statementType == "if" || statementType == "let" || statementType == "return" || statementType == "call" || statementType == "while") {
				STATEMENT();
				// when we return from while, we started a new block
				CurBlock = BlockChain.get(BlockChain.size()-1);
			}
			else {	
				break;
			}
			
		} while(tokenMap.get(scanner.sym) == "semicolon");
		
		checkForAndRemoveEmptyBlocks();
		
	}
	
	
	private int GetTotalArraySize() {

		if(arraySizes.size() == 0)
			return 0;
		
		int length = 1;
		for (int i=0 ; i<arraySizes.size(); i++) {
			length *= arraySizes.get(i);
		}
		return (length * 4);
	}
	
	private Vector<Integer> arraySizes;
	
	private String TYPEDECL() {
		
		if (peek("var")) {
			// we expect that there is a variable not an array
			expect("var");
			return "var";
		}
		
		else {
			// we must expect an array if not a var
			expect("arr");
			
			arraySizes = new Vector<Integer>();
			do {
				expect("openbracket");
				
				/* we must expect this is a number, we must check
				beforehand, the get the number from FACTOR (the only location to get number) */
				
				int number;
				if (tokenMap.get(scanner.sym) == "number") {
					number = GetValue(scanner.sym);
					arraySizes.add(number);
				}
				else
					printError(scanner.sym);
				
				scanner.Next();
				
				expect("closebracket");
			} while (peek("openbracket"));
			
			return "arr";
		} 
	}
	
	private void checkForAndRemoveEmptyBlocks() {
		if (BlockChain.get(BlockChain.size()-1).lines.size() == 0) {
			// Put the children back in to get rerouted to next (Restore childlessParents)
			BlockChain.remove(BlockChain.size()-1);
			
			// does stuff (Actually looks through the tree and finds all parents looking at this removed child
			// we can then redirect them back into the child-less parent list)
			for (int i=0; i<BlockChain.size(); i++) {
				if(BlockChain.get(i).childrenIndexes.contains(BlockChain.size())) {
					ChildlessParents.add(BlockChain.get(i).BlockNumber);
					for (int j=0; j<BlockChain.get(i).childrenIndexes.size(); j++) {
						if (BlockChain.get(i).childrenIndexes.get(j) == BlockChain.size()) {
							BlockChain.get(i).childrenIndexes.remove(j);
							continue;
						}
					}
				}
			}
		}
		else {
			
			ChildlessParents.add(BlockChain.get(BlockChain.size()-1).BlockNumber);
		}
	}

	private int CalcVarAddr() {
		int numEntries = 1;
		if (inFunction) 
		{
			// check every every array in the table
			if (ArrayVariables.containsKey(functionName)) {
				for(int i=0; i<ArrayVariables.get(functionName).size(); i++) {
					// for each array get the number of variables(size)
					for(int j=0; j<ArrayVariables.get(functionName).get(i).size.size(); j++) {
						//this is looking through the entire array and storing space for each entry
						numEntries *= ArrayVariables.get(functionName).get(i).size.get(j);
					}
				}
			}
			else {
				numEntries = 0;
			}
			
			if (FunctionLocals.containsKey(functionName))  {
				numEntries += FunctionLocals.get(functionName).size();
			}
			// check every every array in the table

		}
		else 
		{
			
			// check every every array in the table
			if (ArrayVariables.containsKey(functionName)) {
				for(int i=0; i<ArrayVariables.get(functionName).size(); i++) {
					// for each array get the number of variables(size)
					for(int j=0; j<ArrayVariables.get(functionName).get(i).size.size(); j++) {
						//this is looking through the entire array and storing space for each entry
						numEntries *= ArrayVariables.get(functionName).get(i).size.get(j);
					}
				}
			}
			else {
				numEntries = 0;
			}
			numEntries += identMap.size(); 
			
		}
		return -((numEntries) * 4);
	}
	
	
	private void VARDECL() {
		
		String type = TYPEDECL();
		
		if( type == "var" ) {
			if( inFunction ) {
				java.util.HashMap< String, Result > tempIdents = new java.util.HashMap< String, Result >();
				Result ident = new Result();
				
				tempIdents.put(scanner.Id2String(scanner.id), ident);
				ident.address = CalcVarAddr() - (tempIdents.size()*4);
				
				scanner.Next();
				
				while(peek("comma")) {
					Result ident2 = new Result();
					scanner.Next();
					
					tempIdents.put(scanner.Id2String(scanner.id), ident2);
					ident2.address = CalcVarAddr() - (tempIdents.size()*4);
					
					scanner.Next();
				}
				if (FunctionLocals.containsKey(functionName)) {
					FunctionLocals.get(functionName).putAll(tempIdents);
				}
				else
					FunctionLocals.put(functionName, tempIdents);
			}
			
			else {
				Result globals = new Result();
				
				identMap.put(scanner.Id2String(scanner.id), globals);
				globals.address = CalcVarAddr();
				
				scanner.Next();
				
				while(peek("comma")) {
					Result globals2 = new Result();
					scanner.Next();
					
					identMap.put(scanner.Id2String(scanner.id), globals2);
					globals2.address = CalcVarAddr();
					scanner.Next();
				}
			}
		}
		
		else { //type = "arr" / Array value
			
			int lengthValues = GetTotalArraySize();
			Vector<Array> ArrVars = new Vector<Array>();
			Array variable = new Array();
			
			variable.size = arraySizes;
			variable.name = scanner.Id2String(scanner.id);
			ArrVars.add(variable);
			variable.address = CalcVarAddr() - (lengthValues*ArrVars.size());
			
			scanner.Next();
			
			while(peek("comma")) {
				Array variable2 = new Array();
				scanner.Next();
				
				variable2.size = arraySizes;
				variable2.name = scanner.Id2String(scanner.id);
				ArrVars.add(variable2);
				variable2.address = CalcVarAddr() - (lengthValues*ArrVars.size());

				scanner.Next();
			}
			if (ArrayVariables.containsKey(functionName))
				ArrayVariables.get(functionName).addAll(ArrVars);
			else
				ArrayVariables.put(functionName, ArrVars);
		}
		expect("semicolon");
		return;
	}
	
	private void prologue() {
		//Put in our own return location and locals

		buf[PC++] = DLX.assemble(DLX.PSH, RETURN, SP, -4); 
		buf[PC++] = DLX.assemble(DLX.PSH, FP, SP, -4);
		buf[PC++] = DLX.assemble(DLX.ADD, FP, SP, 0);
	}
	
	private void epilogue() {
		
		//Remove all local variables and return
		int localVarSize = -CalcVarAddr();
		if(localVarSize != 0) {
			buf[PC++] = DLX.assemble(DLX.ADDI, SP, SP, localVarSize);
		}
		
		//Come back from jump and restore the registers
		buf[PC++] = DLX.assemble(DLX.POP, FP, SP, 4); // store Frame Pointer
		buf[PC++] = DLX.assemble(DLX.POP, RETURN, SP, 4); // restore return address
		if (FunctionArguments.get(functionName).size() != 0) {
			buf[PC++] = DLX.assemble(DLX.ADDI, SP, SP, (FunctionArguments.get(functionName).size() * 4));
		}
		buf[PC++] = DLX.assemble(DLX.RET, RETURN);
	}
	
	private void FUNCDECL() {
		
		scanner.Next();
		
		//expect ident
		functionName = scanner.Id2String(scanner.id);
		ValidFuncNames.put(functionName, PC);
		
		scanner.Next();
		
		FORMALPARAM();
		FUNCBODY();
		
		expect("semicolon");
		
		epilogue();
	}
	
	private void FORMALPARAM() {
		
		//Storing the parameters of the function on the stack
		java.util.HashMap< String, Result > parameters = new java.util.HashMap < String, Result >();
		
		//Putting the parameters on the stack right after call
		expect("openparen");
		Vector<String> argumentNames = new Vector<String>();
		
		
		if(!peek("closeparen")) {
			
			argumentNames.add(scanner.Id2String(scanner.id));
			scanner.Next();
		}
		
		while(!peek("closeparen")) {
			
			expect("comma");
			argumentNames.add(scanner.Id2String(scanner.id));
			scanner.Next();
		}
		
		for (int i = 1; i <= argumentNames.size(); i++) {
			
			Result parameter = new Result();
			
			//Relative to FP = order * 4 + 8
			parameter.address = ( (argumentNames.size() - i) * 4 + 8);
			parameter.kind = "reg";
			parameter.scope = functionName;
			parameters.put(argumentNames.get(i-1), parameter);
		}
		
		FunctionArguments.put(functionName, parameters);
		
		prologue();
		
		expect("closeparen");
		
		return;
	}
	
	private void FUNCBODY() {
		
		while (peek("var") || peek("arr")) {
			VARDECL();
		}
		if (CalcVarAddr() != 0) {
			buf[PC++] = DLX.assemble(DLX.SUBI, SP, FP, -CalcVarAddr());
		}
		
		expect("begin");
		
		if(!peek("end")) {

			STATSEQUENCE();
		}
		
		expect("end");
	}
	
	
	// Implement this function to start compiling your input file
	public int[] getProgram()  
	{	
		expect("main");
        
		functionName = "main";
		if (peek("var") || peek("arr")) { //try
			while (peek("var") || peek("arr")) {
				VARDECL();
			}
			buf[PC++] = DLX.assemble(DLX.SUBI, SP, DP, -CalcVarAddr());
		}
		else
        	buf[PC++] = DLX.assemble(DLX.SUBI, SP, DP, 0);
        
        Result jumpForward = new Result();
        UncondBraFwd(jumpForward);
        
        while(peek("function") || peek("procedure")) {
        	inFunction = true;
        	FUNCDECL();
        }
        functionName = "main";
        
        Fixup(jumpForward.fixuplocation);
        inFunction = false;
        
        expect("begin");
        STATSEQUENCE();
        expect("end");
  
        expect("period");
        //add eof identifier to buffer
        buf[PC++] = DLX.assemble(DLX.RET, 0);
        
        if(tokenMap.get(scanner.sym) != "eof")
        	scanner.Error("No EOF found.");
        scanner.closefile();
        
        
        //Creating textfile for the GUI
        try {
        	File file = new File("BlockDiagram.txt");
        	FileOutputStream OutputFile = new FileOutputStream(file);
        	
        	// Initial File setup
        	OutputFile.write("digraph  Main {\n\n".getBytes());
        	OutputFile.write("  node [shape=record fontname=Arial];\n\n".getBytes());
        	
        	// Write each block individually
        	for(int i=0; i<BlockChain.size(); i++) {
        		
        		OutputFile.write("  Block_".getBytes());
        		OutputFile.write(String.valueOf(i).getBytes());
        		OutputFile.write("[label=\" B".getBytes());
        		OutputFile.write(String.valueOf(i).getBytes());
        		OutputFile.write('|');
        		
        		// Write Line Data in Subgraph
        		WriteLineData(OutputFile, i);
        		
        		OutputFile.write("\"]\n".getBytes());
        	}
        	OutputFile.write('\n');
        	
        	// put arrows in which connect the boxes
        	for(int i=0; i<BlockChain.size(); i++) {
        		ConnectSubgraphs(OutputFile, i);
        	}
        	OutputFile.write('\n');
   
        	// Finishing the file and closing FileStream
        	OutputFile.write('}');
        	OutputFile.close();
        	System.out.print("success...\n"); 
        }
        catch (Exception e){
        	System.out.print(e.getLocalizedMessage());
        }
        
        //Printing all of the DLX Instructions that we created
        for( int i=0; i < PC; i++) {
        	System.out.print("(" + (i) + ")" + " " + DLX.disassemble( buf[i] ) + "\n");
        }
        
		
		return buf;
	}
}