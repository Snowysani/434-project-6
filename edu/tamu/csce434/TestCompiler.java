package edu.tamu.csce434;

import java.io.*;
import java.util.*;

public class TestCompiler {
    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Usage: TestCompiler <code file> <data file>");
            return;
        }

        try {
            // Redirect System.in from DLX to data file
            InputStream origIn = System.in,
                        newIn = new BufferedInputStream(
                                new FileInputStream(args[1]));
            System.setIn(newIn);
        
            Compiler c = new Compiler(args[0]);
            int prog[] = c.getProgram();
            if (prog == null) {
                System.out.println("Error compiling program");
                return;
            }

            DLX.load(prog);
            DLX.execute();

            System.setIn(origIn);
            newIn.close();
        } catch (IOException e) {
            System.out.println("Error reading input files");
        }
        
        
        
        // Coloring Graph data
        
        RegisterData GraphColoring = new RegisterData();
        
        GraphColoring.CreateTestCase();
		
		// Running algorithm to set colors to the nodes
        GraphColoring.ColoringNodes();
		
		// Printing the graph into a DOT file
		try {
        	File file = new File("RegisterAllocation.txt");
        	FileOutputStream OutputFile = new FileOutputStream(file);
        	
        	GraphColoring.PrintingInteferenceGraph(OutputFile); 
        }
        catch (Exception e){
        	System.out.print(e.getLocalizedMessage());
        }
    }

}