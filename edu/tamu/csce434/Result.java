package edu.tamu.csce434;

public class Result 
{
    String varName;
    String kind;
    String scope;
    int address;
    int regnum;
    int fixuplocation;
    int value;
    int lastSetInstruction;
    int lineNumber;

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
        lineNumber = 0;
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
        this.lineNumber = r.lineNumber;
    }
}