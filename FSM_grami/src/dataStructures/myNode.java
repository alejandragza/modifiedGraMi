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

package dataStructures;

import java.awt.List;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet; //ALE

import org.w3c.dom.ls.LSInput;

import utilities.MyPair;

import Dijkstra.DijkstraEngine;


public class myNode 
{
	private	int ID;
	private	int label;
	private int[] shortestPaths;
	
	//ALE key is node label, and the value is a list of node's IDs with this label and the edgeLabel
	private HashMap<Integer, ArrayList<MyPair<Integer, Double>>> reachableNodes;//represented by Label~<nodeID,edge_label>, represents the outgoing nodes
	
	private HashMap<Integer, ArrayList<MyPair<Integer, Double>>> reachedBYNodes; //represented by Label~<nodeID,edge_label>, represents the ingoing nodes
	
	@Override
	public String toString() {
	
		String format="("+ID+":"+label+")";
		return format;
	}
	
	public myNode(int ID, int label)
	{
		this.ID = ID;
		this.label = label;
	}
	
	//ALE
	public void resetNodeDetails()
	{
		reachableNodes= new HashMap<Integer, ArrayList<MyPair<Integer, Double>>>();
		reachedBYNodes= new HashMap<Integer, ArrayList<MyPair<Integer, Double>>>();
	}
	
	public int getOutDegree(int label)
	{
		if(reachableNodes==null)
			return 0;
		if(reachableNodes.get(label)==null)
			return 0;
		return reachableNodes.get(label).size();
	}
	public int getinDegree(int label)
	{
		if(reachedBYNodes==null)
			return 0;
		if(reachedBYNodes.get(label)==null)
			return 0;
		return reachedBYNodes.get(label).size();
	}
	
	public int getID()
	{
		return ID;
	}
	public int getLabel()
	{
		return label;
	}
	
	public void addreachableNode(myNode node, double edgeLabel)
	{
		if(reachableNodes==null)
			reachableNodes= new HashMap<Integer, ArrayList<MyPair<Integer, Double>>>();
		
		ArrayList<MyPair<Integer, Double>> list=reachableNodes.get(node.getLabel());
		if(list==null)
			{
				list = new ArrayList<MyPair<Integer, Double>>();
				reachableNodes.put(node.getLabel(), list);
			}
		if(!list.contains(node.getID()))
			list.add(new MyPair(node.getID(), edgeLabel)); //ALE, we add the received node param to our children
		node.addreachedBYNodes(this, edgeLabel); //ALE and for the received node, we indicate that it can be reached by this node
		
	}
	
	
	private void addreachedBYNodes(myNode node, double edgeLabel)
	{
		if(reachedBYNodes==null)
			reachedBYNodes= new HashMap<Integer, ArrayList<MyPair<Integer, Double>>>();
		ArrayList<MyPair<Integer, Double>> list=reachedBYNodes.get(node.getLabel());
		if(list==null)
			{
				list = new ArrayList<MyPair<Integer, Double>>();
				reachedBYNodes.put(node.getLabel(), list);
			}
		if(!list.contains(node.getID()))
			list.add(new MyPair(node.getID(), edgeLabel));
	}
	
	public void printOutReachableNodes()
	{
		if(reachableNodes==null)
			return;
		for (Iterator<ArrayList<MyPair<Integer, Double>>> iterator = reachableNodes.values().iterator(); iterator.hasNext();)
		{
			ArrayList<MyPair<Integer, Double>> arr =  iterator.next();
			for (int i = 0; i < arr.size(); i++) 
			{
				System.out.println("Node: "+ID+" is within reach of Node "+arr.get(i));
			}
			
		}
		
	}
	
	public void setReachableNodes(DijkstraEngine dj,HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel, HPListGraph graph)
	{
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= freqNodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
			HashMap<Integer,myNode> tmp = ar.getValue();

			for (Iterator<myNode> iterator = tmp.values().iterator(); iterator.hasNext();) 
			{
				myNode node =  iterator.next();
				if(ID==node.getID())
					continue;
				double dist=dj.getShortestDistance(node.getID());
				if(dist!=Double.MAX_VALUE )
				{
					if(graph.getEdgeLabel(ID, node.getID())!=null)
					{
						double edgeLabel = (Double)graph.getEdgeLabel(ID, node.getID());
						addreachableNode(node, edgeLabel);
					}
					else
					{
						addreachableNode(node, 1);
					}
				}
			}
		}
	}
	
	/**
	 * a fast set reachable function
	 * @param graph
	 * @param freqNodesByLabel
	 */
	//ORIGINAL
	public void xsetReachableNodes_1hop(Graph graph,HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel)
	{
		//get edge for each node
		IntIterator it= graph.getListGraph().getOutEdgeIndices(getID());
		//ALE, we are going through all edges where the node is present, checking if they're frequent, and if they are, adding them to its reachableNodes map
		for (; it.hasNext();) 
		{
			int edge =  it.next(); //ALE, we fetch node's edge ID
			//ALE, graph.getListGraph().getOtherNode(edge, getID()): given the edge ID and the node's ID, we find the ID of the other node this node is connected to. Can be child or parent.
			myNode otherNode = graph.getNode(graph.getListGraph().getOtherNode(edge, getID()));
			//ALE, then we check if this node is frequent, by checking if its label is actually present in freqNodesByLabel map
			if(freqNodesByLabel.containsKey(otherNode.getLabel()))
					addreachableNode(otherNode, graph.getListGraph().getEdgeLabel(getID(), otherNode.getID()));
			
		}
	}
	
	//ALE modified to recalculate the reachable nodes for each node every time freqThreshold value changes 
	public void setReachableNodes_1hop(Graph graph,HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel)
	{
		//get edge for each node
		IntIterator it= graph.getListGraph().getOutEdgeIndices(getID());
		//ALE, we are going through all edges where the node is present, checking if they're frequent, and if they are, adding them to its reachableNodes map
		for (; it.hasNext();) 
		{
			int edge =  it.next(); //ALE, we fetch node's edge ID
			//ALE, graph.getListGraph().getOtherNode(edge, getID()): given the edge ID and the node's ID, we find the ID of the other node this node is connected to. Can be child or parent.
			myNode otherNode = graph.getNode(graph.getListGraph().getOtherNode(edge, getID()));
			//ALE, then we check if this node is frequent, by checking if its label is actually present in freqNodesByLabel map
			if(freqNodesByLabel.containsKey(otherNode.getLabel()))
					addreachableNode(otherNode, graph.getListGraph().getEdgeLabel(getID(), otherNode.getID()));
			else
			{
				//ALE IMPORTANT CHANGE from original version
				//if the label is not frequent as for the current freq value, try to remove it from our reachable nodes
				//this is key for the tool to work with multiple changing freq values
				if(reachableNodes != null && reachableNodes.containsKey(otherNode.getLabel()))
					reachableNodes.remove(otherNode.getLabel());
				if(reachedBYNodes != null && reachedBYNodes.containsKey(otherNode.getLabel()))
					reachedBYNodes.remove(otherNode.getLabel());
					
			}
			
		}
	}
	
	public boolean hasReachableNodes()
	{
		if(reachableNodes==null)
			return false;
		else
			return true;
	}
	
	public boolean isWithinTheRangeOf(int NodeIndex,int nodeLabel)
	{
		if(reachableNodes.get(nodeLabel)==null)
			return false;
		return reachableNodes.get(nodeLabel).contains(NodeIndex);
	}
	 
	public ArrayList<MyPair<Integer, Double>> getRechableWithNodeIDs(int label, double edgeLabel)
	{
		if(reachableNodes==null) return new ArrayList<MyPair<Integer, Double>>();
		
		ArrayList<MyPair<Integer, Double>> tempArr = new ArrayList<MyPair<Integer, Double>>();
		tempArr.addAll(reachableNodes.get(label));
		for(int j=0;j<tempArr.size();j++)
		{
			MyPair<Integer, Double> mp = tempArr.get(j);
			if(mp.getB().doubleValue()!=edgeLabel)
			{
				tempArr.remove(j);
				j--;
			}
		}
		return tempArr;
	}
	 
	 public HashMap<Integer, ArrayList<MyPair<Integer, Double>>> getReachableWithNodes()
	 {
		 return reachableNodes;
	 }
	 
	 public HashMap<Integer, ArrayList<MyPair<Integer, Double>>> getReachableByNodes()
	 {
		 return reachedBYNodes;
	 }
	
	 public ArrayList<MyPair<Integer, Double>> getRechableByNodeIDs(int label, double edgeLabel)
	{
		if(reachedBYNodes==null) return new ArrayList<MyPair<Integer, Double>>();

		ArrayList<MyPair<Integer, Double>> tempArr = new ArrayList<MyPair<Integer, Double>>();
		tempArr.addAll(reachedBYNodes.get(label));
		for(int j=0;j<tempArr.size();j++)
		{
			MyPair<Integer, Double> mp = tempArr.get(j);
			if(mp.getB().doubleValue()!=edgeLabel)
			{
				tempArr.remove(j);
				j--;
			}
		}
		return tempArr;
	}
	 
	//ALE
	 public Set<Integer> getChildrenIDs()
	 {
		 Set<Integer> childrenIDs = new HashSet<Integer>();
		 for (Entry<Integer, ArrayList<MyPair<Integer, Double>>> entry : reachableNodes.entrySet())
		 {
			 ArrayList<MyPair<Integer, Double>> arrayIDs = entry.getValue();
			 for(int i = 0; i < arrayIDs.size(); i++)
			 {
				 childrenIDs.add(arrayIDs.get(i).getA()); //this is the ID in the MyPair
			 }
			 
		 }
	     return childrenIDs;
	 }
}
