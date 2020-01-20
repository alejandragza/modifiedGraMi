package dataStructures;

import java.util.*;

//ALE
public class myGraph 
{
	HashMap<Integer, MySkinnyNode> graph; //key: nodeID, value: skinny node 
	Set<Integer> nodesInvolved; //will help with intersections and to do merges
}
