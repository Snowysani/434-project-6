package edu.tamu.csce434;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
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
		String kind;
		String scope;
		int address;
		int regnum;
		int fixuplocation;
		int value;
	}

	private class Block
	{
		ArrayList<Line> lines; // list of lines that are in that block
		ArrayList<Integer> childrenIndexes; // maybe do this another way
		// TODO: what else?
	}
	private class Line
	{
		Boolean isRelational;
		Integer operator;
		ArrayList<Result> UsedVars; // something like that
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
	
	private Vector<String> preDefIdents = new Vector<String>();
	
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
		if (x.kind == "const" && y.kind == "const") {
			Load(x);
			buf[PC++] = DLX.assemble(op + 16, result.regnum, x.regnum, y.value);
		}
		else if (x.kind == "const" && (y.kind == "reg"||y.kind == "arr")) {
			Load(x);
			buf[PC++] = DLX.assemble(op, x.regnum, x.regnum, y.regnum);
		}
		else if ((x.kind == "reg"||x.kind == "arr") && y.kind == "const") {
			buf[PC++] = DLX.assemble(op + 16, result.regnum, x.regnum, y.value);
		}
		else {	
			buf[PC++] = DLX.assemble(op, result.regnum, x.regnum, y.regnum);
		}
		result.kind = "reg";
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
						scanner.Next();
						return result;
					}
				}
			}
		}
		scanner.Next();
		return result;
		
	}
	
	private Result IndexRegister = new Result();
	
	private Result DESIGNATOR() {
		//fixes problems with same ident in the same line
		//Using one memory location for variable is bad -> new
		Result newLocation = new Result();
		Result result = IDENT();
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
			return result;
		}
		else if (tokenMap.get(ret) == "number") {
			result.kind = "const";
			result.value = GetValue(ret);
		}
		else if (tokenMap.get(ret) == "openparen") {
			scanner.Next();
			result = EXPRESSION();
			expect("closeparen");
			return result;	
		}	
		else if (tokenMap.get(ret) == "call") {
			result = FUNCCALL();
			return result;
		}
		
		scanner.Next();
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
				if(factor.kind == "const" && factor2.kind == "const") 
					factor.value *= factor2.value;
					//multiply the current factor with the incoming factor
				else {
					compute(DLX.MUL, factor, factor, factor2);
				}
			}
			else {
				if(factor.kind == "const" && factor2.kind == "const") 
					factor.value /= factor2.value;
				//divides without creating excess registers
				else {
					compute(DLX.DIV, factor, factor, factor2);
				}
					
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
				if(term.kind == "const" && term2.kind == "const")
					term.value += term2.value;
				else {
					compute(DLX.ADD, term, term, term2);
				}
			}
			else {
				if(term.kind == "const" && term2.kind == "const")
					term.value -= term2.value;
				else {
					compute(DLX.SUB, term, term, term2);
				}
						
			}
			if (term2.kind == "reg")
				DeallocateReg(term2);
			
			s = tokenMap.get(scanner.sym);
		}
		return term;
	}
	
	private Result RELATION() {
		
		//find values of a and b in relationship
		Result A = EXPRESSION();
		String relOP = tokenMap.get(scanner.sym);
		scanner.Next();
		Result B = EXPRESSION();
		
		//create instruction that subtracts the two to get the resulting value
		Result relation = new Result();
		AllocateReg(relation);
		relation.value = A.value - B.value;
		compute(DLX.SUB, relation, A, B);
		
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
		}
		else if(relOP == "neq") {
			buf[PC++] = DLX.assemble(DLX.BEQ, relation.regnum, 0);
		}
		else if(relOP == "lss") {
			buf[PC++] = DLX.assemble(DLX.BGE, relation.regnum, 0);
		}
		else if(relOP == "leq") { //branch Greater than
			buf[PC++] = DLX.assemble(DLX.BGT, relation.regnum, 0);
		}		
		else if(relOP == "gtr") {
			buf[PC++] = DLX.assemble(DLX.BLE, relation.regnum, 0);
		}
		else if(relOP == "geq") {
			buf[PC++] = DLX.assemble(DLX.BLT, relation.regnum, 0);
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
		
		//If we finish the function without a return
		if(setValue == null) {
			Result noReturn = new Result();
			noReturn.kind ="const";
			noReturn.value = 0;
			setValue = noReturn;
		}
		
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
		
		Result holdInput = new Result();
		
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
			
			
			return holdInput;
		}
		
		//outputnum(x)
		else if(stringCompare(scanner.Id2String(scanner.id), preDefIdents.get(1))) { 
			scanner.Next();
			expect("openparen");
			holdInput = EXPRESSION();
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
		expect("if");
		//creating the two initial jump instructions and setting fixuplocation
		Result negJump;
		negJump = RELATION();
		
		expect("then");
		
		Result follow = new Result();
		
		STATSEQUENCE();
		
		if(peek("else")) {
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
		return;
	}
	
	private void WHILESTMT() {
		
		expect("while");
		
		int loopLocation = PC+1;
		Result negJump = RELATION();
		
		expect("do");
		STATSEQUENCE();
		expect("od");
		
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
		epilogue();
		
		return returnValue;
	}
	
	private void STATEMENT() {
		String statementType = tokenMap.get(scanner.sym);
		if(statementType == "let")
			ASSIGNMENT();
		else if(statementType == "call") {
			Result faultyReturn = FUNCCALL();
			DeallocateReg(faultyReturn);
		}
		else if(statementType == "if")
			IFSTATEMENT();
		else if(statementType == "while")
			WHILESTMT();
		else if (statementType == "return")
			RETURNSTMT();
		else
			scanner.Error("Invalid Statement Option");
		return;
	}
	
	private void STATSEQUENCE() {
		String statementType = tokenMap.get(scanner.sym);
		if(statementType == "let" || statementType == "call" || statementType == "if" || statementType == "while" || statementType == "return")
			STATEMENT();
		else	
			scanner.Error("Invalid Statement Option");
		while(tokenMap.get(scanner.sym) == "semicolon") {
			scanner.Next();
			statementType = tokenMap.get(scanner.sym);
			if(statementType == "let" || statementType == "call" || statementType == "if" || statementType == "while" || statementType == "return") {
				STATEMENT();}
			else	
				break;
		}
		return;
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

//        for( int i=0; i < PC; i++) {
//        	System.out.print("(" + (i) + ")" + " " + DLX.disassemble( buf[i] ) + "\n");
//        }
        
        if(tokenMap.get(scanner.sym) != "eof")
        	scanner.Error("No EOF found.");
        scanner.closefile();
		
		return buf;
	}
}