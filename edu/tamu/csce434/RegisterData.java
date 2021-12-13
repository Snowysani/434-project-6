package edu.tamu.csce434;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

class EdgeType {
	
	public EdgeType() {	}
	String node1;
	String node2;
}

public class RegisterData 
{
	
	public void CreateTestCase_NoSpill() {
		//Test Case
		NodeType node1 = new NodeType();
		node1.node = "a";
		
		NodeType node2 = new NodeType();
		node2.node = "b";
		
		NodeType node3 = new NodeType();
		node3.node = "c";
		
		NodeType node4 = new NodeType();
		node4.node = "x";
		
		NodeType node5 = new NodeType();
		node5.node = "y";
		
		NodeType node6 = new NodeType();
		node6.node = "z";
		
		Nodes.add(node1);
		Nodes.add(node2);
		Nodes.add(node3);
		Nodes.add(node4);
		Nodes.add(node5);
		Nodes.add(node6);
		
		EdgeType edge1 = new EdgeType();
		edge1.node1 = "a";
		edge1.node2 = "c";
		
		EdgeType edge2 = new EdgeType();
		edge2.node1 = "c";
		edge2.node2 = "z";
		
		EdgeType edge3 = new EdgeType();
		edge3.node1 = "c";
		edge3.node2 = "b";
		
		EdgeType edge4 = new EdgeType();
		edge4.node1 = "b";
		edge4.node2 = "x";
		
		EdgeType edge5 = new EdgeType();
		edge5.node1 = "x";
		edge5.node2 = "y";
		
		EdgeType edge6 = new EdgeType();
		edge6.node1 = "y";
		edge6.node2 = "b";
		
		EdgeType edge7 = new EdgeType();
		edge7.node1 = "z";
		edge7.node2 = "a";
		
		Edges.add(edge1);
		Edges.add(edge2);
		Edges.add(edge3);
		Edges.add(edge4);
		Edges.add(edge5);
		Edges.add(edge6);
		Edges.add(edge7);
	}
	
	public void CreateTestCase_Spill() {
		//Test Case
		NodeType node1 = new NodeType();
		node1.node = "a";
		
		NodeType node2 = new NodeType();
		node2.node = "b";
		
		NodeType node3 = new NodeType();
		node3.node = "c";
		
		NodeType node4 = new NodeType();
		node4.node = "x";
		
		NodeType node5 = new NodeType();
		node5.node = "y";
		
		NodeType node6 = new NodeType();
		node6.node = "z";
		
		NodeType node7 = new NodeType();
		node7.node = "p";
		
		Nodes.add(node1);
		Nodes.add(node2);
		Nodes.add(node3);
		Nodes.add(node4);
		Nodes.add(node5);
		Nodes.add(node6);
		Nodes.add(node7);
		
		EdgeType edge1 = new EdgeType();
		edge1.node1 = "a";
		edge1.node2 = "b";
		
		EdgeType edge2 = new EdgeType();
		edge2.node1 = "b";
		edge2.node2 = "c";
		
		EdgeType edge3 = new EdgeType();
		edge3.node1 = "a";
		edge3.node2 = "c";
		
		EdgeType edge4 = new EdgeType();
		edge4.node1 = "a";
		edge4.node2 = "x";
		
		EdgeType edge5 = new EdgeType();
		edge5.node1 = "a";
		edge5.node2 = "y";
		
		EdgeType edge6 = new EdgeType();
		edge6.node1 = "x";
		edge6.node2 = "b";
		
		EdgeType edge7 = new EdgeType();
		edge7.node1 = "y";
		edge7.node2 = "b";
		
		EdgeType edge8 = new EdgeType();
		edge8.node1 = "c";
		edge8.node2 = "x";
		
		EdgeType edge9 = new EdgeType();
		edge9.node1 = "c";
		edge9.node2 = "y";
		
		EdgeType edge10 = new EdgeType();
		edge10.node1 = "x";
		edge10.node2 = "y";
		
		EdgeType edge11 = new EdgeType();
		edge11.node1 = "a";
		edge11.node2 = "z";
		
		EdgeType edge12 = new EdgeType();
		edge12.node1 = "y";
		edge12.node2 = "z";
		
		EdgeType edge13 = new EdgeType();
		edge13.node1 = "b";
		edge13.node2 = "z";
		
		EdgeType edge14 = new EdgeType();
		edge14.node1 = "x";
		edge14.node2 = "z";
		
		EdgeType edge15 = new EdgeType();
		edge15.node1 = "c";
		edge15.node2 = "z";
		
		Edges.add(edge1);
		Edges.add(edge2);
		Edges.add(edge3);
		Edges.add(edge4);
		Edges.add(edge5);
		Edges.add(edge6);
		Edges.add(edge7);
		Edges.add(edge8);
		Edges.add(edge9);
		Edges.add(edge10);
		Edges.add(edge11);
		Edges.add(edge12);
		Edges.add(edge13);
		Edges.add(edge14);
		Edges.add(edge15);
	}
	
	
	private static int NUMREGISTERS = 5;
	
	// Holds the names of all nodes
	private class NodeType {
		String node;
		String color;
		Integer registerNumber;
	}
	private static ArrayList<NodeType> Nodes = new ArrayList<NodeType>();
	
	// Class holds two strings for the Edge list
	private static ArrayList<EdgeType> Edges = new ArrayList<EdgeType>();
	
	
	private static int countNumEdges(String node) {
		
		int count = 0;
		for (int i=0; i<Edges.size(); i++) {
			if (node == Edges.get(i).node1 || node == Edges.get(i).node1) {
				count++;
			}
		}
		return count;
	}
	
	
	private static ArrayList<EdgeType>RemoveEdges (ArrayList<NodeType> tempNodes, ArrayList<EdgeType> tempEdges, int i) {
		for (int j=0; j<tempEdges.size(); j++) {
			if (tempNodes.get(i).node == tempEdges.get(j).node1) {
				tempEdges.remove(j--);
			}
			else if(tempNodes.get(i).node == tempEdges.get(j).node2) {
				tempEdges.remove(j--);
			}
		}
		return tempEdges;
	}	

	// For this project, there is no randomness, this will help with debugging by keeping the output static
	// Since this problem is NP-complete, the best we can do is try this program many times
	// choosing a different node order (random) each time and taking the best outcome. This insures
	// that we have a higher probability of getting the optimal solution.
	public void ColoringNodes(){
		
		ArrayList<EdgeType> tempEdges = new ArrayList<EdgeType>(Edges);
		ArrayList<NodeType> tempNodes = new ArrayList<NodeType>(Nodes);
		ArrayList<String> Stack = new ArrayList<String>();
		
		int indexCounter = 0;
		
		// remove nodes and edges and places them into a stack
		while (tempNodes.size() > 0) {
			
			// There are no nodes with less than k edges (SPILL)
			if (indexCounter >= tempNodes.size()) {
				tempEdges = RemoveEdges(tempNodes, tempEdges, 0);
				Stack.add(tempNodes.get(0).node);
				tempNodes.remove(0);
				indexCounter = 0;
			}
			
			// If the node has less edges than the number of registers
			else if (countNumEdges(tempNodes.get(indexCounter).node) < NUMREGISTERS) {
				tempEdges = RemoveEdges(tempNodes, tempEdges, indexCounter);
				Stack.add(tempNodes.get(indexCounter).node);
				tempNodes.remove(indexCounter);
				indexCounter = 0;
			}
			
			// more edges than registers, but more nodes to check
			else {
				indexCounter++;
			}
		}
		
		ArrayList<String> replacedNodes = new ArrayList<String>();
		ArrayList<Integer> registerColor = new ArrayList<Integer>();
		
		int count;
		
		for (int i=Stack.size()-1; i>=0; i--) 
		{
			
			// Holds the nearby register values
			HashMap<Integer, Boolean> colors = new HashMap<Integer, Boolean>();
			for(int j=0; j<NUMREGISTERS; j++) {
				// this defines all registers as open
				colors.put(j, true);
			}
				
			count = 1;
			for (int j=0; j<Edges.size(); j++) 
			{		
				// Checking nodes already added to the list to the current node
				for (int k=0; k<replacedNodes.size(); k++) 
				{	
					// If either node is the first, and the other is the second, count++
					// Counting the number of edges in a replaced node
					if (Edges.get(j).node1 == replacedNodes.get(k) || Edges.get(j).node1 == Stack.get(i)) {
						if(Edges.get(j).node2 == replacedNodes.get(k) || Edges.get(j).node2 == Stack.get(i)) {
							
							// define the register as closed (already being used)
							colors.put(registerColor.get(k), false);
							System.out.print(registerColor.get(k) + " ");
						}
					}
				}
			}
			System.out.print("\n");
			
			System.out.print(colors  + " " + Stack.get(i)+ "\n");

			int index = -1;
			for (int j=0; j<Nodes.size(); j++) {
				if(Nodes.get(j).node == Stack.get(i)) {
					index = j;
					break;
				}
			}
			
			
			if (colors.get(0)) 
			{
				Nodes.get(index).color = "grey";
				registerColor.add(0);
				replacedNodes.add(Stack.get(i));
			}
			else if (colors.get(1)) 
			{
				Nodes.get(index).color = "blue";
				registerColor.add(1);
				replacedNodes.add(Stack.get(i));
			}
			else if (colors.get(2)) 
			{
				Nodes.get(index).color = "green";
				registerColor.add(2);
				replacedNodes.add(Stack.get(i));
			}
			else if (colors.get(3)) 
			{
				Nodes.get(index).color = "yellow";
				registerColor.add(3);
				replacedNodes.add(Stack.get(i));
			}
			else if (colors.get(4)) 
			{
				Nodes.get(index).color = "orange";
				registerColor.add(4);
				replacedNodes.add(Stack.get(i));
			}
			else {
				Nodes.get(index).color = "red";
				
				// remove edges that connect to that node from final graph
				for (int j=0; j<Edges.size(); j++) {
					if (Edges.get(j).node1 == Nodes.get(index).node || Edges.get(j).node2 == Nodes.get(index).node) {
						Edges.remove(j);
						j--;
					}
				}
			}
		}
	}
	
	public void PrintingInteferenceGraph(FileOutputStream Output) {
		
		try {
			
			Output.write("graph Interference {\n\n".getBytes());
			
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
				
				if (Edges.get(i).node1 != null && Edges.get(i).node2 != null) {
					
					Output.write(Edges.get(i).node1.getBytes());
					Output.write("--".getBytes());
					Output.write(Edges.get(i).node2.getBytes());
					Output.write(";\n".getBytes());
				}
			}
			
			Output.write('\n');
			Output.write('}');
			
		}
		catch (Exception e) {
			System.out.print(e.getLocalizedMessage());
		}
	}
	
}