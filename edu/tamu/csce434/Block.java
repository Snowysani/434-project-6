package edu.tamu.csce434;

import java.util.ArrayList;

public class Block
{
    int BlockNumber;
    ArrayList<Line> lines = new ArrayList<Line>(); // list of lines that are in that block
    ArrayList<Integer> childrenIndexes = new ArrayList<Integer>(); // this is fine
    ArrayList<Integer> predecessors = new ArrayList<Integer>();
    
    String StatementType = new String(); // used to see if we are in IF or WHILE (used for labeling arrows in block diagram)
}