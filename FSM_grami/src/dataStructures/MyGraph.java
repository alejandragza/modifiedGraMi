package dataStructures;

import java.util.*;

//ALE
public class MyGraph 
{
	HashMap<Integer, MySkinnyNode> graph; //key: nodeID, value: skinny node. This is the "adjacency matrix"
	Set<Integer> nodesInvolved; //will help with intersections and to do merges
	HashMap<Integer, Integer> genericToActualIDs;
	HashMap<Integer, Integer> invertedGenericToActualIDs;
	ArrayList<MyGraph> variants; //this stores all of the other found subgraphs that are variants of this one
	ArrayList<HashMap<Integer, Integer>> validMappings; //this stores mappings of generic IDs to actual node IDs that are valid for this subgraph instance
	boolean checked;
	
	ArrayList<MyEdge> edges;
	
	public MyGraph(int size)
	{
		graph = new HashMap<Integer, MySkinnyNode>();
		nodesInvolved = new HashSet<Integer>();
		genericToActualIDs = new HashMap<Integer, Integer>();
		invertedGenericToActualIDs = new HashMap<Integer, Integer>();
		initializeMap(size);
		
		validMappings = new ArrayList<HashMap<Integer, Integer>>();
		
		checked = false;
		
		edges = new ArrayList<MyEdge>();
	}
	
	//constructor for graphs created for a variant
	public MyGraph(int size, HashMap<Integer, MySkinnyNode> graph, Set<Integer> nodesInvolved, HashMap<Integer, Integer> genericToActualIDs, HashMap<Integer, Integer> invertedGenericToActualIDs)
	{
		this.graph = graph;
		this.nodesInvolved = nodesInvolved;
		this.genericToActualIDs = genericToActualIDs;
		this.invertedGenericToActualIDs = invertedGenericToActualIDs;
	}
	
	//copy constructor for variants
	public MyGraph(MyGraph toCopy)
	{
		graph = new HashMap<Integer, MySkinnyNode>();
		graph.putAll(toCopy.graph);
		
		nodesInvolved = new HashSet<Integer>();
		nodesInvolved.addAll(toCopy.nodesInvolved);
		
		genericToActualIDs = new HashMap<Integer, Integer>();
		genericToActualIDs.putAll(toCopy.genericToActualIDs);
		
		invertedGenericToActualIDs = new HashMap<Integer, Integer>();
		invertedGenericToActualIDs.putAll(toCopy.invertedGenericToActualIDs);
		
		validMappings = new ArrayList<HashMap<Integer, Integer>>();
		validMappings.addAll(toCopy.validMappings);
		
		checked = false;
		
		edges = new ArrayList<MyEdge>();
		edges.addAll(toCopy.edges);
		
		//also do not forget to push the variants
		if(toCopy.variants != null)
		{
			variants = new ArrayList<MyGraph>();
			variants.addAll(toCopy.variants);
		}
	}
	
	public HashMap<Integer, MySkinnyNode> getGraph()
	{
		return graph;
	}
	
	public Set<Integer> getNodesInvolved()
	{
		return nodesInvolved;
	}
	
	public HashMap<Integer, Integer> getGenericToActualIDs()
	{
		return genericToActualIDs;
	}
	
	public boolean isValid(int genericID, int actualPotentialID)
	{
		int val = genericToActualIDs.get(genericID);
		if(val != -1)
		{
			if(val == actualPotentialID)
				return true;
			else
				return false;
		}
		else
		{
			return true;
		}
	}
	
	public boolean addEdge(MyEdge auxEdge, String mapping)
	{
		//split the mapping
		String[] mappingSplit = mapping.split(" ");
		int genericIDA = Integer.parseInt(mappingSplit[0]);
		int genericIDB = Integer.parseInt(mappingSplit[1]);
		
		if(isValid(genericIDA, auxEdge.getA()) && isValid(genericIDB, auxEdge.getB()))
		{
			//only if both mappings are valid, map the edge nodes and actually add the edge
			genericToActualIDs.put(genericIDA, auxEdge.getA());
			genericToActualIDs.put(genericIDB, auxEdge.getB());
			
			//also add entries to the invertedGenericToActualIDs map
			invertedGenericToActualIDs.put(auxEdge.getA(), genericIDA);
			invertedGenericToActualIDs.put(auxEdge.getB(), genericIDB);
			
			//ALE WARNING, tal vez debas checar primero si ya existe el nodo en graph (para no redefinir el nodo y omitir edges)
			MySkinnyNode a = new MySkinnyNode();
			a.ID = auxEdge.getA();
			a.edges.add(auxEdge.getB());
			MySkinnyNode b = new MySkinnyNode();
			b.ID = auxEdge.getB();
			
			//add to "adjacency matrix"
			graph.put(a.ID, a);
			graph.put(b.ID, b);
			
			//add to nodesInvolved
			nodesInvolved.add(auxEdge.getA());
			nodesInvolved.add(auxEdge.getB());
			
			//add to edge list
			edges.add(auxEdge);
			return true; //edge was added
		}
		else
			return false; //do not add this edge, as it does not follow the already established mapping
		
		
	}
	
	public String xtoString()
	{
		String x = "Subgraph instance: \n- Nodes involved: ";
		String edgeInfo = "";
		for (MySkinnyNode node : graph.values()) 
		{
			x += node.ID + " ";
			edgeInfo += node.getEdgesString();
		}
		//TEMPORARY
		//x += "\n" + edgeInfo;
		x += printMapping();
		//get info on variants
		if(variants != null && variants.size() > 0)
		{
			x += "\n The following variations were also found for this graph:";
			for(int i = 0; i < variants.size(); ++i)
			{
				x += "\n#" + (i+1);
				x += "\n"+variants.get(i).getStrictGraphInfo();
			}
		}
		
		
		return x;
	}
	
	public String toString()
	{
		String x = "Nodes involved: " + nodesInvolved + "\n";
		for (MyEdge edge : edges) 
		{
			x += edge.toString() + "\n";
		}
		//TEMPORARY
		//x += "\n" + edgeInfo;
		
		//get info on variants
		if(variants != null && variants.size() > 0)
		{
			x += "\n These appear to be variations of this graph: ";
			for(int i = 0; i < variants.size(); ++i)
			{
				x += "\n    Variation #" + (i+1);
				x += "\n    "+variants.get(i).getStrictGraphInfo();
			}
			x += "\n";
		}
		
		return x;
	}
	
	public String getStrictGraphInfo()
	{
		String x = "Nodes involved: " + nodesInvolved;
		/*String edgeInfo = "";
		for (MySkinnyNode node : graph.values()) 
		{
			x += node.ID + " ";
			edgeInfo += node.getEdgesString();
		}*/
		x += "\n";
		return x;
	}
	
	public void initializeMap(int size)
	{
		for(int i = 0; i < size; ++i)
		{
			genericToActualIDs.put(i, -1);
		}
	}
	
	public HashMap<Integer, Integer> getInvertedGenericToActualIDs()
	{
		//invert all of the entries in genericToActualIDs
		//this will help when determining whether a subgraph is a variant of another
		return invertedGenericToActualIDs;
		
	}
	
	//ALE - first version
	public boolean xisVariant(MyGraph toCheck)
	{
		//this method determines if subgraph toCheck is a variant of this subgraph
		Set<Integer> toCheckNodes = toCheck.getNodesInvolved();
		//get intersection
		Set<Integer> intersection = new HashSet<Integer>(this.getNodesInvolved());
		intersection.retainAll(toCheckNodes);
		if(intersection.size() > 0)
		{
			//check that all of the mappings match key: genericID, value: actualID
			HashMap<Integer, Integer> mappingToCheck = toCheck.getInvertedGenericToActualIDs();
			int qtyMatches = 0;
			for(int sharedNode : intersection)
			{
				   if(this.invertedGenericToActualIDs.get(sharedNode) == mappingToCheck.get(sharedNode))
					   qtyMatches += 1;
			}
			
			System.out.println(nodesInvolved + " vs " + toCheckNodes + " qtyMatches: " + qtyMatches);
			
			if(qtyMatches == intersection.size())
				return true;
			else
				return false;
			
		}
		else
			return false;
	}
	
	//ALE - more thorough check for variant. It now takes into consideration multiple valid mappings for one subgraph, if applicable
	public boolean isVariant(MyGraph toCheck)
	{
		//this method determines if subgraph toCheck is a variant of this subgraph
		Set<Integer> toCheckNodes = toCheck.getNodesInvolved();
		//get intersection
		Set<Integer> intersection = new HashSet<Integer>(this.getNodesInvolved());
		intersection.retainAll(toCheckNodes);
		if(intersection.size() > 0)
		{
			//check that all of the mappings match key: genericID, value: actualID
			ArrayList<HashMap<Integer, Integer>> mappingsToCheck = toCheck.getValidMappings();
			for(int  i = 0; i < mappingsToCheck.size(); ++i)
			{
				HashMap<Integer, Integer> mapToCheck = mappingsToCheck.get(i);
				//System.out.print("CHECK " + toCheck.getMapping(i));
				for(int j = 0; j < this.validMappings.size(); ++j)
				{
					HashMap<Integer, Integer> myCurrentMap = validMappings.get(j);
					System.out.print("CHECK " + toCheck.getMapping(i));
					System.out.print("   vs " + getMapping(j));
					int qtyMatches = 0;
					for(int sharedNode : intersection)
					{
						   if(myCurrentMap.get(sharedNode) == mapToCheck.get(sharedNode))
						   {
							   //maybe let's change the approach, since some mappings are interchangeable but that is not logged here (because the algorithm is a little greedy)
							   //if there is a match, consider that the instances share some of the structure, and so they overlap => take it as variants
							   //qtyMatches += 1;
							   return true;
						   }
							   
					}
					/*System.out.println("  Size of intersection " + intersection.size() + " vs qtyMatches " + qtyMatches);
					if(qtyMatches == intersection.size())
						return true;*/
				}
			}
			
			//if we checked all mappings of the possible variant vs all mappings of this instance and there were no valid matches, it is not a variant
			return false;
		}
		else return false;

	}
	
	public void insertAsVariant(MyGraph foundVariant)
	{
		if(variants == null)
			variants = new ArrayList<MyGraph>();
		
		variants.add(foundVariant);
		int before = validMappings.size();
		//also add the validMappings of foundVariant to the validMappings of this subgraph instance
		validMappings.addAll(foundVariant.getValidMappings());
		System.out.println("Before: "+ before +", We now have " + validMappings.size() + " valid mappings");
	}
	
	public String printMapping()
	{
		String x = "Mapping\n";
		for (Map.Entry<Integer, Integer> entry : genericToActualIDs.entrySet()) 
		{
			//System.out.println("generic " + entry.getKey() + " is " + entry.getValue());
			x += "generic " + entry.getKey() + " is " + entry.getValue() +"\n";
		}
		return x;
	}
	
	public String getMapping(int i)
	{
		String x = "Mapping: ";
		for (Map.Entry<Integer, Integer> entry : validMappings.get(i).entrySet()) 
		{
			//System.out.println("generic " + entry.getKey() + " is " + entry.getValue());
			x += "generic " + entry.getValue() + " is " + entry.getKey() +", ";
		}
		x += "\n";
		return x;
	}
	
	public void printAllValidMappings()
	{
		for(int i = 0; i < validMappings.size(); ++i)
			System.out.println(getMapping(i));
	}
	
	public ArrayList<HashMap<Integer, Integer>> getValidMappings()
	{
		return validMappings;
	}
	
	public void setChecked()
	{
		checked = true;
	}
	
	
}
