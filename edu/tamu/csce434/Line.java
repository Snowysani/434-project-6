package edu.tamu.csce434;

import java.util.ArrayList;

public class Line
{
    Integer lineNumber;
    String operator; // The beginning statement in IR (Ex. MOVE, MUL, WRITE)
    Result SetVar = new Result();
    ArrayList<Result> UsedVars = new ArrayList<Result>(); // something like that
    String FunctionName;
}
