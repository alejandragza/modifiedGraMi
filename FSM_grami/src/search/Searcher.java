/**
 * This file was modified by Alejandra Garza 01-19-2020
 * Copyright 2014 Mohammed Elseidy, Ehab Abdelhamid

This file is part of Grami.

Grami is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

Grami is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Grami.  If not, see <http://www.gnu.org/licenses/>.
 */

package search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import statistics.Statistics;
import utilities.MyPair;
import utilities.Settings;
import utilities.StopWatch;
import Dijkstra.*;
import dataStructures.DFSCode;
import dataStructures.DFScodeSerializer;
import dataStructures.Edge;
import dataStructures.GSpanEdge;
import dataStructures.Graph;
import dataStructures.HPListGraph;
import dataStructures.IntFrequency;
import dataStructures.MyGraph;
import dataStructures.gEdgeComparator;
import dataStructures.myNode;

public class Searcher<NodeType, EdgeType> 
{

	private Graph singleGraph;
	private IntFrequency freqThreshold;
	private int distanceThreshold;
	private ArrayList<Integer> sortedFrequentLabels;
	private ArrayList<Double> freqEdgeLabels;
	Map<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>> initials;
	private int type;
	public ArrayList<HPListGraph<NodeType, EdgeType>> result;
	public static Hashtable<Integer, Vector<Integer>> neighborLabels;
	public static Hashtable<Integer, Vector<Integer>> revNeighborLabels;
	ArrayList<DFSCode<NodeType, EdgeType>> fsInfo; //ALE
	
	private String path;
	
	/*public Searcher(String path, int freqThreshold,int shortestDistance) throws Exception
	{
		this.freqThreshold= new IntFrequency(freqThreshold);
		this.distanceThreshold=shortestDistance;
		singleGraph = new Graph(1,freqThreshold); //id of graph object = 1, freqThreshold = freqThreshold
		singleGraph.loadFromFile_Ehab(path); //we load the graph from the file we read
		this.path = path;
		sortedFrequentLabels=singleGraph.getSortedFreqLabels(); //we receive labels sorted by frequency DESC
		freqEdgeLabels = singleGraph.getFreqEdgeLabels(); //this does not apply (SWAN does not label their edges)
		DenseRoutesMap x = new DenseRoutesMap(singleGraph); //this creates an object from singleGraphs list implementation m_matrix
		DijkstraEngine d = new DijkstraEngine(x,shortestDistance);
		
		singleGraph.printFreqNodes();
        singleGraph.setShortestPaths_1hop();
	}*/
	
	//ALE modified
	public Searcher(String path, int shortestDistance) throws Exception
	{
		//this.freqThreshold= new IntFrequency(freqThreshold);
		this.distanceThreshold=shortestDistance;
		singleGraph = new Graph(1); //singleGraph = new Graph(1,freqThreshold); //id of graph object = 1, freqThreshold = freqThreshold
		singleGraph.loadFromFile_Alejandra(path); //singleGraph.loadFromFile_Ehab(path); //we load the graph from the file we read
		this.path = path;
		
		
		/* moving these to setFreqThreshold as they depend on that
		sortedFrequentLabels=singleGraph.getSortedFreqLabels(); //we receive labels sorted by frequency DESC
		freqEdgeLabels = singleGraph.getFreqEdgeLabels(); //this does not apply (SWAN does not label their edges)
		DenseRoutesMap x = new DenseRoutesMap(singleGraph); //this creates an object from singleGraphs list implementation m_matrix
		DijkstraEngine d = new DijkstraEngine(x,shortestDistance);
		
		singleGraph.printFreqNodes();
        singleGraph.setShortestPaths_1hop();*/
	}
	
	//ALE
	public void setFreqThreshold(int freqThreshold)
	{
		this.freqThreshold= new IntFrequency(freqThreshold);
		
		singleGraph.pruneNonfrequentElements(freqThreshold);
		
		sortedFrequentLabels=singleGraph.getSortedFreqLabels(); //we receive labels sorted by frequency DESC
		freqEdgeLabels = singleGraph.getFreqEdgeLabels(); //this does not apply (SWAN does not label their edges)
		DenseRoutesMap x = new DenseRoutesMap(singleGraph); //this creates an object from singleGraphs list implementation m_matrix
		DijkstraEngine d = new DijkstraEngine(x,distanceThreshold);
		
		singleGraph.printFreqNodes();
        singleGraph.setShortestPaths_1hop();
	}
	
	public void initialize()
	{
		//creo que initials guarda las edges frecuentes, como una fase de pruning inicial
		initials= new TreeMap<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>>(new gEdgeComparator<NodeType, EdgeType>());
		HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel=  singleGraph.getFreqNodesByLabel();
		singleGraph.printSortedFreqLabels();
		HashSet<Integer> contains= new HashSet<Integer>();
		//go through all labels of frequent nodes
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= freqNodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
			int firstLabel=ar.getKey();
			System.out.println("*ALE, tenemos esta label " + firstLabel);
			contains.clear();
			HashMap<Integer,myNode> tmp = ar.getValue();
			//go through all nodes of this label
			for (Iterator<myNode> iterator = tmp.values().iterator(); iterator.hasNext();) 
			{
				myNode node =  iterator.next();
				System.out.println("***ALE, tenemos este node" + node.getID() + " con esta label " + node.getLabel());
				//ALE iterate over node's children (reachableNodes' KEYSET)
				HashMap<Integer, ArrayList<MyPair<Integer, Double>>> neighbours=node.getReachableWithNodes();
				node.printOutReachableNodes();
				if(neighbours!=null)
				for (Iterator<Integer>  iter= neighbours.keySet().iterator(); iter.hasNext();) 
				{
					int secondLabel = iter.next(); //ALE get the label of child node
					int labelA=sortedFrequentLabels.indexOf(firstLabel);
					int labelB=sortedFrequentLabels.indexOf(secondLabel);
					
					//iterate over all neighbor nodes to get edge labels as well
					//ALE for each label in reachableNodes map, iterate through the neighbors
					for (Iterator<MyPair<Integer, Double>>  iter1= neighbours.get(secondLabel).iterator(); iter1.hasNext();)
					{
						MyPair<Integer, Double> mp = iter1.next();
						double edgeLabel = mp.getB();
						if(!freqEdgeLabels.contains(edgeLabel))
							continue;
						
						int secondNodeID = mp.getA();
					
						final GSpanEdge<NodeType, EdgeType> gedge = new GSpanEdge <NodeType, EdgeType>().set(0, 1, labelA, (int)edgeLabel, labelB, 1, firstLabel, secondLabel);
						
						//ALE I think you first generate a label for the edge and add it to initials if it's not already there
						if(!initials.containsKey(gedge))
						{
						
							final ArrayList<GSpanEdge<NodeType, EdgeType>> parents = new ArrayList<GSpanEdge<NodeType, EdgeType>>(
								2);
							parents.add(gedge);
							parents.add(gedge);
						
							HPListGraph<NodeType, EdgeType> lg = new HPListGraph<NodeType, EdgeType>();
							gedge.addTo(lg);
							DFSCode<NodeType, EdgeType> code = new DFSCode<NodeType,EdgeType>(sortedFrequentLabels,singleGraph,null).set(lg, gedge, gedge, parents);
						
							initials.put(gedge, code); //ALE we add the gedge label and the subgraph code
							
							System.out.println("ALE agregamos nueva gedge " + gedge.toString() + " con code " + code.toString());
						}
					}
				}
			}
		}
		//we will delete from initials anything that is below the freqThreshold
		for (final Iterator<Map.Entry<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>>> eit = initials
				.entrySet().iterator(); eit.hasNext();) {
			final DFSCode<NodeType, EdgeType> code = eit.next().getValue();
			if (freqThreshold.compareTo(code.frequency()) > 0) {
				eit.remove();
			}
			else
				;
		}
		System.out.println("ALE done deleting non candidates!");
		
		neighborLabels = new Hashtable();
		revNeighborLabels = new Hashtable();
		for (final Iterator<Map.Entry<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>>> eit = initials
				.entrySet().iterator(); eit.hasNext();) 
		{
			final DFSCode<NodeType, EdgeType> code = eit.next().getValue();
			System.out.println("Initial with Gedge "+ code.getLast()+ " code: "+code);
			
			int labelA;
			int labelB;
			GSpanEdge<NodeType, EdgeType> edge = code.getFirst();
			if (edge.getDirection() == Edge.INCOMING) {
				labelA = edge.getThelabelB();
				labelB = edge.getThelabelA();
			} else {
				labelB = edge.getThelabelB();
				labelA = edge.getThelabelA();
			}
			//add to labels
			Vector temp = neighborLabels.get(labelA);
			if(temp==null)
			{
				temp = new Vector();
				neighborLabels.put(labelA, temp);
			}
			temp.addElement(labelB);
			//add reverse labels
			temp = revNeighborLabels.get(labelB);
			if(temp==null)
			{
				temp = new Vector();
				revNeighborLabels.put(labelB, temp);
			}
			temp.addElement(labelA);
		}
		
		
	}
	
	//ALE
	public void printInitials()
	{
		System.out.println("ALE this is what we have in initials");
		for (final Iterator<Map.Entry<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>>> eit = initials
				.entrySet().iterator(); eit.hasNext();) 
		{
			Map.Entry<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>> myEntry = eit.next();
			final GSpanEdge<NodeType, EdgeType> gspanedgex = myEntry.getKey();
			final DFSCode<NodeType, EdgeType> code = myEntry.getValue();
			System.out.println("Gedge "+ gspanedgex.toString() + " code: "+code);
		}
		System.out.println("**************________________\n\n");
	}
	
	//ALE
	public void printResult()
	{
		for (int i = 0; i < result.size(); i++) 
		{		
			//String out=DFScodeSerializer.serialize(sr.result.get(i));
		}
	}
	
	public void search()
	{
		Algorithm<NodeType, EdgeType> algo = new Algorithm<NodeType, EdgeType>();
		algo.setInitials(initials);
		RecursiveStrategy<NodeType, EdgeType> rs = new RecursiveStrategy<NodeType, EdgeType>();
		result= (ArrayList<HPListGraph<NodeType, EdgeType>>)rs.search(algo,this.freqThreshold.intValue()); //ALE THIS HOLDS THE FREQUENT SUBGRAPHS STRUCTURE
		
		//ALE get FSInfo
		fsInfo = rs.getFSInfo(); //ALE with this we can reconstruct every individual subgraph
	}
	
	//ALE
	public void printFSInfo()
	{
		for(int i = 0; i < fsInfo.size(); i++)
		{
			System.out.println("\n\n------------\n New subgraph");
			System.out.println(DFScodeSerializer.serialize(result.get(i)));
			fsInfo.get(i).printVariables();
		}
	}
	
	//ALE - call this once we have result
	public void initFrequentStructures(HashMap<String, Boolean> alreadyMined, int minSize)
	{
		for(int i = 0; i < fsInfo.size(); i++)
		{
			String key = result.get(i).toString();
			if(result.get(i).getNodeCount() >= minSize && !alreadyMined.containsKey(key))
			{
				//if(i == 36)
				//{
				result.get(i).setRoughInfo(fsInfo.get(i)); //assign the individual nodes information to the relevant frequent structure
				
				result.get(i).buildGenericGraph(singleGraph);
				result.get(i).generateSubgraphInstances();
				//}
				alreadyMined.put(key, false);
			}
			
		}
	}
	
	//ALE - call this once we have result
	public ArrayList<MyGraph> fetchSubgraphInstances(int i)
	{
		result.get(i).setRoughInfo(fsInfo.get(i)); //assign the individual nodes information to the relevant frequent structure
		
		result.get(i).buildGenericGraph(singleGraph);
		result.get(i).generateSubgraphInstances(); //populates listOfSubgraphs in HPListGraph
		
		return result.get(i).getListOfSubgraphs();

	}
	
	private int getNumOfDistinctLabels(HPListGraph<NodeType, EdgeType> list)
    {
        HashSet<Integer> difflabels= new HashSet<Integer>();
        for (int i = 0; i < list.getNodeCount(); i++) 
        {
            int label= (Integer)list.getNodeLabel(i);
            if(!difflabels.contains(label))
                difflabels.add(label);
        }
        
        return difflabels.size();
    }
	
	public Graph getSingleGraph()
	{
		return singleGraph;
	}
	
}
