package edu.tamu.csce434;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

public class RegisterData 
{
	
	private class varRange {
		String VariableName;
		ArrayList<Integer> startingLine; // Line where the variable is assigned
		ArrayList<Integer> endingLine; // Line where the variable is used
	}
	
	// Holds the names of all nodes
	private class NodeType {
		String node;
		String color;
	}
	private ArrayList<NodeType> Nodes;
	
	// Class holds two strings for the Edge list
	private class EdgeType {
		String node1;
		String node2;
	}
	private ArrayList<EdgeType> Edges;
		
	private void ColoringNodes(){
		
		ArrayList<EdgeType> tempEdges = new ArrayList<EdgeType>();
		for (int i=0; i<Edges.size(); i++) {
			tempEdges.add(Edges.get(i));
		}
		
		// Nodes are not random
		for (int i=0; i<Nodes.size(); i++) {
			for (int j=0; j<tempEdges.size(); j++) {
				if (Nodes.get(i).node == tempEdges.get(j).node1) {
					tempEdges.remove(j--);
				}
				else if(Nodes.get(i).node == tempEdges.get(j).node2) {
					tempEdges.remove(j--);
				}
			}
		}
		
		ArrayList<String> tempNodes = new ArrayList<String>();
		int NUMREGISTERS = 5;
		int count = 1;
		
		for (int i=Nodes.size()-1; i>=0; i--) {
			count = 1;
			for (int j=0; j<Edges.size(); j++) {
				// Checking nodes already added to the list to the current node
				for (int k=0; k<tempNodes.size(); k++) {
					// If either node is the first, and the other is the second, count++
					if (Edges.get(i).node1 == tempNodes.get(k) || Edges.get(i).node1 == Nodes.get(i).node) {
						if(Edges.get(i).node2 == tempNodes.get(k) || Edges.get(i).node2 == Nodes.get(i).node) {
							count++;
						}
					}
				}
			}
			
			// coloring registers
			if (count <= NUMREGISTERS) {
				if (count == 1) {
					Nodes.get(i).color = "grey";
				}
				else if (count == 2) {
					Nodes.get(i).color = "blue";
				}
				else if (count == 3) {
					Nodes.get(i).color = "green";
				}
				else if (count == 4) {
					Nodes.get(i).color = "yellow";
				}
				else if (count == 5) {
					Nodes.get(i).color = "orange";
				}
				
				// Only add if not spilled
				tempNodes.add(Nodes.get(i).node);
			}
			else {
				Nodes.get(i).color = "red";
			}
		}
	}
	
	private void PrintingInteferenceGraph(FileOutputStream Output) {
		
		try {
			
			Output.write("Diagraph Interference {\n\n".getBytes());
			
			for (int i=0; i<Nodes.size(); i++) {
				
				// write the name of the node
				Output.write("  ".getBytes());
				Output.write(Nodes.get(i).node.getBytes());
				
				// Input the color of the node on the same line
				Output.write("[ style = filled color = \"".getBytes());
				Output.write(Nodes.get(i).color.getBytes());
				Output.write("\" ];\n".getBytes());
			}
			
			Output.write('\n');
			
			for (int i=0; i<Edges.size(); i++) {
				
				//Edges.get(i).node1 -- Edges.get(i).node2;
				//Create an edge between nodes
				Output.write("  ".getBytes());
				Output.write(Edges.get(i).node1.getBytes());
				Output.write("--".getBytes());
				Output.write(Edges.get(i).node1.getBytes());
				Output.write(";\n".getBytes());
			}
			
			Output.write('\n');
			Output.write('}');
			
		}
		catch (Exception e) {
			System.out.print(e.getLocalizedMessage());
		}
	}
}