package dataStructures;

import java.util.*;

//ALE - we call it skinny because it is like a lite version of the original node (it will not have all of the edges, only those relevant to the subgraph replica it is part of)
public class MySkinnyNode 
{
	int ID; //original ID (the same ID the node has in the original graph)
	Set<Integer> edges; //only those relevant to the subgraph
	
	public MySkinnyNode()
	{
		edges = new HashSet<Integer>();
	}
	
	public String getEdgesString()
	{
		String edgeInfo = "";
		
		for(int child : edges)
		{
			edgeInfo += ID + " -> " + child + "\n";
		}
		return edgeInfo;
	}
}
