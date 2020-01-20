package dataStructures;

import java.util.*;

//ALE
class GenericNode 
{
	int genericID; //Variable ID
	Set<Integer> individualNodes; //node IDs in the original graph
	Set<Integer> genericParents;
	Set<Integer> genericEdges;
	
	public GenericNode()
	{
		
	}
	
	public void setIndividualNodes(Set<Integer> individualNodes)
	{
		this.individualNodes = individualNodes;
	}
	
	public void setGenericID(int genericID)
	{
		this.genericID = genericID;
	}
}
