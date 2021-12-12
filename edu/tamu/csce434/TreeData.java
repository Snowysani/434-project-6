package edu.tamu.csce434;

import java.util.ArrayList;

public class TreeData {
    Block node;
    ArrayList<Block> children;

    public Block getBlock ()
    {
        return this.node;
    }

    public void addChild(Block child)
    {
        children.add(child);
    }

    public TreeData()
    {

    }

    public TreeData(Block b)
    {
        node = b;
        children = new ArrayList<>();
    }

}
