package edu.tamu.csce434;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

class EdgeType {
	
	public EdgeType() {	}
	String node1;
	String node2;
}

class NodeType {
	
	public NodeType() {	}
	String node;
	String color;
}

public class RegisterData 
{
	
	public void CreateTestCase() {
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
	
	
	private static int NUMREGISTERS = 5;
	
	// Holds the names of all nodes
	private class NodeType {
		String node;
		String color;
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
				tempEdges = RemoveEdges(tempNodes, tempEdges, indexCounter);
				Stack.add(tempNodes.get(indexCounter).node);
				tempNodes.remove(indexCounter);
				indexCounter = 0;
			}
			
			// If the node has less edges than the number of registers
			if (countNumEdges(tempNodes.get(indexCounter).node) < NUMREGISTERS) {
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
		int count;
		
		for (int i=Stack.size()-1; i>=0; i--) 
		{
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
				replacedNodes.add(Nodes.get(i).node);
			}
			else {
				Nodes.get(i).color = "red";
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
				Output.write(Edges.get(i).node1.getBytes());
				Output.write("--".getBytes());
				Output.write(Edges.get(i).node2.getBytes());
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