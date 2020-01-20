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


import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

//import utilities.CombinationGenerator;
import utilities.Settings;

//import Temp.SubsetReference;
import Dijkstra.DijkstraEngine;


public class Graph 
{

	public final static int NO_EDGE = 0;
	private HPListGraph<Integer, Double> m_matrix;
	private int nodeCount=0;
	private ArrayList<myNode> nodes;
	private HashMap<Integer, HashMap<Integer,myNode>> freqNodesByLabel;
	private HashMap<Integer, HashMap<Integer,myNode>> nodesByLabel;
	private ArrayList<Integer> sortedFreqLabels; //sorted by frequency !!! Descending......
	
	private ArrayList<Point> sortedFreqLabelsWithFreq;
	
	private HashMap<Double, Integer> edgeLabelsWithFreq;
	private ArrayList<Double> freqEdgeLabels;
	
	private int freqThreshold;
	public int getFreqThreshold() {
		return freqThreshold;
	}

	private int m_id;
	
	
	public Graph(int ID, int freqThresh) 
	{
		
		sortedFreqLabels= new ArrayList<Integer>();
		sortedFreqLabelsWithFreq = new ArrayList<Point>();
		
		m_matrix= new HPListGraph<Integer, Double>();
		m_id=ID;
		nodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		
		freqNodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		nodes= new ArrayList<myNode>();
		
		edgeLabelsWithFreq = new HashMap<Double, Integer>();
		freqEdgeLabels = new ArrayList<Double>();
		
		invertedNodeLabels = new HashMap<Integer, String>();
		
		freqThreshold=freqThresh;
		
		if(StaticData.hashedEdges!=null)
		{
			StaticData.hashedEdges = null;
			System.out.println(StaticData.hashedEdges.hashCode());//throw exception if more than one graph was created
		}
		StaticData.hashedEdges = new HashMap<String, HashMap<Integer, Integer>[]>();
	}
	
	//ALE - to initialize graph without establishing a freqThreshold
	public Graph(int ID) 
	{
		
		sortedFreqLabels= new ArrayList<Integer>();
		sortedFreqLabelsWithFreq = new ArrayList<Point>();
		
		m_matrix= new HPListGraph<Integer, Double>();
		m_id=ID;
		nodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		
		freqNodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		nodes= new ArrayList<myNode>();
		
		edgeLabelsWithFreq = new HashMap<Double, Integer>();
		freqEdgeLabels = new ArrayList<Double>();
		
		invertedNodeLabels = new HashMap<Integer, String>();
		
		//IMPORTANT CHANGE HERE
		if(StaticData.hashedEdges!=null)
		{
			//avoid creating more than one graph in the form of hashedEdges map.
			//if this is for a new freqThreshold, just reset hashedEdges
			StaticData.resetHashedEdges();
			//StaticData.hashedEdges = null;
			//System.out.println(StaticData.hashedEdges.hashCode());//throw exception if more than one graph was created
		}
		else
			StaticData.hashedEdges = new HashMap<String, HashMap<Integer, Integer>[]>();
	}
	
	//ALE
	public void printSortedFreqLabels() {
		for(int i = 0; i < sortedFreqLabels.size(); ++i)
			System.out.println("SortedFreqLabel " + i + " " + sortedFreqLabels.get(i));
	}
	
	public ArrayList<Integer> getSortedFreqLabels() {
		return sortedFreqLabels;
	}
	
	public ArrayList<Double> getFreqEdgeLabels() {
		return this.freqEdgeLabels;
	}

	public HashMap<Integer, HashMap<Integer,myNode>> getFreqNodesByLabel()
	{
		return freqNodesByLabel;
	}
	
	public void loadFromFile(String fileName) throws Exception
	{		
		String text = "";
		final BufferedReader bin = new BufferedReader(new FileReader(new File(fileName)));
		File f = new File(fileName);
		FileInputStream fis = new FileInputStream(f);
		byte[] b = new byte[(int)f.length()];
		int read = 0;
		while (read < b.length) {
		  read += fis.read(b, read, b.length - read);
		}
		text = new String(b);
		final String[] rows = text.split("\n");
		
		// read graph from rows
		// nodes
		int i = 0;
		int numberOfNodes=0;
		for (i = 1; (i < rows.length) && (rows[i].charAt(0) == 'v'); i++) {
			final String[] parts = rows[i].split("\\s+");
			final int index = Integer.parseInt(parts[1]);
			final int label = Integer.parseInt(parts[2]);
			if (index != i - 1) {
				throw new ParseException("The node list is not sorted", i);
			}
			
			addNode(label);
			myNode n = new myNode(numberOfNodes, label);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(label);
			if(tmp==null)
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(label, tmp);
			}
			tmp.put(n.getID(), n);
			numberOfNodes++;
		}
		nodeCount=numberOfNodes;
		// edges
		for (; (i < rows.length) && (rows[i].charAt(0) == 'e'); i++) {
			final String[] parts = rows[i].split("\\s+");
			final int index1 = Integer.parseInt(parts[1]);
			final int index2 = Integer.parseInt(parts[2]);
			final double label = Double.parseDouble(parts[3]);
			addEdge(index1, index2, label);
		}
		
		//now prune the infrequent nodes
		
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= nodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();			
			if(ar.getValue().size()>=freqThreshold)
			{
				sortedFreqLabelsWithFreq.add(new Point(ar.getKey(),ar.getValue().size()));
				freqNodesByLabel.put(ar.getKey(), ar.getValue());
			}
		}
		
		Collections.sort(sortedFreqLabelsWithFreq, new freqComparator());
		
		for (int j = 0; j < sortedFreqLabelsWithFreq.size(); j++) 
		{
			sortedFreqLabels.add(sortedFreqLabelsWithFreq.get(j).x);
			
		}
		
		bin.close();		
	}
	
	//we create the graph and get rid of nodes and edges that are not frequent (taking into consideration the freqThreshold)
	int labelIndex = 0;
	Map<String, Integer> nodeLabels = new HashMap<String, Integer>();
	HashMap<Integer, String> invertedNodeLabels;
	public void loadFromFile_Ehab(String fileName) throws Exception
	{		
		String text = "";
		final BufferedReader rows = new BufferedReader(new FileReader(new File(fileName)));
		
		// read graph from rows
		// nodes
		int counter = 0;
		int numberOfNodes=0; //ALE, we use this as the original ID for our nodes. It starts at 0 and is sequential
		String line;
		String tempLine;
		rows.readLine();
		while ((line = rows.readLine()) !=null && (line.charAt(0) == 'v')) {
			final String[] parts = line.split("\\s+");
			final int index = Integer.parseInt(parts[1]);
			//ALE modified
			String nodeLabel = parts[2];
			
			int label = -1;
            if(!nodeLabels.containsKey(nodeLabel))
            {
            	label = labelIndex;
            	nodeLabels.put(nodeLabel, labelIndex);
            	invertedNodeLabels.put(labelIndex, nodeLabel);
            	labelIndex++;
            }
            else
            	label = nodeLabels.get(nodeLabel);
            
			//final int label = Integer.parseInt(parts[2]);
			if (index != counter) {
				System.out.println(index+" "+counter);
				throw new ParseException("The node list is not sorted", counter);
			}
			
			addNode(label);
			myNode n = new myNode(numberOfNodes, label);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(label);
			if(tmp==null)
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(label, tmp);
			}

			tmp.put(n.getID(), n);
			numberOfNodes++;
			counter++;
		}
		nodeCount=numberOfNodes;
		tempLine = line;
		
		// edges
		
		//use the first edge line
		if(tempLine.charAt(0)=='e')
			line = tempLine;
		else
			line = rows.readLine();
		
		if(line!=null)
		{
			do
			{
				final String[] parts = line.split("\\s+");
				final int index1 = Integer.parseInt(parts[1]);
				final int index2 = Integer.parseInt(parts[2]);
				final double label = Double.parseDouble(parts[3]);
				addEdge(index1, index2, label);
			} while((line = rows.readLine()) !=null && (line.charAt(0) == 'e'));
		}
		
		//prune infrequent edge labels
		for (Iterator<  java.util.Map.Entry< Double,Integer> >  it= this.edgeLabelsWithFreq.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Double,Integer > ar =  it.next();			
			if(ar.getValue().doubleValue()>=freqThreshold)
			{
				this.freqEdgeLabels.add(ar.getKey());
			}
		}
		
		//now prune the infrequent nodes
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= nodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();			
			if(ar.getValue().size()>=freqThreshold)
			{
				sortedFreqLabelsWithFreq.add(new Point(ar.getKey(),ar.getValue().size())); //this is not sorted yet. It's just the frequent labels
				freqNodesByLabel.put(ar.getKey(), ar.getValue());
			}
		}
		
		//we sort sortedFreqLabelsWithFreq
		Collections.sort(sortedFreqLabelsWithFreq, new freqComparator());
		
		//we populate sortedFreqLabels with sortedFreqLabelsWithFreq 
		for (int j = 0; j < sortedFreqLabelsWithFreq.size(); j++) 
		{
			sortedFreqLabels.add(sortedFreqLabelsWithFreq.get(j).x);
		}
		
		//prune frequent hashedEdges
		Vector toBeDeleted = new Vector();
		Set<String> s = StaticData.hashedEdges.keySet();
		for (Iterator<String>  it= s.iterator(); it.hasNext();) 
		{
			String sig =  it.next();
			HashMap[] hm = StaticData.hashedEdges.get(sig);
			if(hm[0].size()<freqThreshold || hm[1].size()<freqThreshold)
			{
				toBeDeleted.addElement(sig);
			}
			else
				;
		}
		Enumeration<String> enum1 = toBeDeleted.elements();
		while(enum1.hasMoreElements())
		{
			String sig = enum1.nextElement();
			StaticData.hashedEdges.remove(sig);
		}
		
		rows.close();		
	}
	
	
	//ALE - separate file reading and initial pruning (does not consider )
	public void xloadFromFile_Alejandra(String fileName) throws Exception
	{		
		String text = "";
		final BufferedReader rows = new BufferedReader(new FileReader(new File(fileName)));
		
		// read graph from rows
		// nodes
		int counter = 0;
		int numberOfNodes=0; //ALE, we use this as the original ID for our nodes. It starts at 0 and is sequential
		String line;
		String tempLine;
		rows.readLine();
		while ((line = rows.readLine()) !=null && (line.charAt(0) == 'v')) {
			final String[] parts = line.split("\\s+");
			final int index = Integer.parseInt(parts[1]);
			
			String nodeLabel = parts[2];
			
			int label = -1;
            if(!nodeLabels.containsKey(nodeLabel))
            {
            	label = labelIndex;
            	nodeLabels.put(nodeLabel, labelIndex);
            	invertedNodeLabels.put(labelIndex, nodeLabel);
            	labelIndex++;
            }
            else
            	label = nodeLabels.get(nodeLabel);
			//final int label = Integer.parseInt(parts[2]);
			if (index != counter) {
				System.out.println(index+" "+counter);
				throw new ParseException("The node list is not sorted", counter);
			}
			
			addNode(label); //ALE, this adds the node into m_matrix, which is an HPListGraph object
			myNode n = new myNode(numberOfNodes, label);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(label);
			if(tmp==null)  //ALE, if this is the first time we read this label, create a map for it
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(label, tmp);
			}

			tmp.put(n.getID(), n); //ALE, this will change the value already inserted in nodesByLabel, as they are inserted by reference.
			numberOfNodes++;
			counter++;
		}
		nodeCount=numberOfNodes;
		tempLine = line;
		
		// edges
		
		//use the first edge line
		if(tempLine.charAt(0)=='e')
			line = tempLine;
		else
			line = rows.readLine();
		
		if(line!=null)
		{
			do
			{
				final String[] parts = line.split("\\s+");
				final int index1 = Integer.parseInt(parts[1]);
				final int index2 = Integer.parseInt(parts[2]);
				final double label = Double.parseDouble(parts[3]);
				addEdge(index1, index2, label); //ALE, this populates edgeLabelsWithFreq
			} while((line = rows.readLine()) !=null && (line.charAt(0) == 'e'));
		}
		
		//set originalHashedEdges
		StaticData.setOriginalHashedEdges();
		
		rows.close();		
	}
	
	//ALE - separate file reading and initial pruning
	public void loadFromFile_Alejandra(String fileName) throws Exception
	{		
		ArrayList<MyPair> nodesInDegree = new ArrayList<MyPair>();
		ArrayList<MyEdge> edgesToAdd = new ArrayList<MyEdge>();
		String text = "";
		final BufferedReader rows = new BufferedReader(new FileReader(new File(fileName)));
		
		// read graph from rows
		// nodes
		int counter = 0;
		int numberOfNodes=0; //ALE, we use this as the original ID for our nodes. It starts at 0 and is sequential
		String line;
		String tempLine;
		rows.readLine();
		while ((line = rows.readLine()) !=null && (line.charAt(0) == 'v')) {
			final String[] parts = line.split("\\s+");
			final int index = Integer.parseInt(parts[1]);
			
			String nodeLabel = parts[2];
			
			nodesInDegree.add(new MyPair(nodeLabel, 0)); //add node to this list, so we can start keeping track of its indegree
			
			//final int label = Integer.parseInt(parts[2]);
			if (index != counter) {
				System.out.println(index+" "+counter);
				throw new ParseException("The node list is not sorted", counter);
			}
			
			numberOfNodes++;
			counter++;
		}
		nodeCount=numberOfNodes;
		tempLine = line;
		
		// edges
		
		//use the first edge line
		if(tempLine.charAt(0)=='e')
			line = tempLine;
		else
			line = rows.readLine();
		
		if(line!=null)
		{
			do
			{
				final String[] parts = line.split("\\s+");
				final int index1 = Integer.parseInt(parts[1]); //ALE, this is the parent node
				final int index2 = Integer.parseInt(parts[2]); //ALE, this is the child node
				//final double label = Double.parseDouble(parts[3]);
				
				nodesInDegree.get(index2).addToInDegree(); //update count of incoming edges for the child node
				edgesToAdd.add(new MyEdge(index1, index2));
				//addEdge(index1, index2, label); //ALE, this populates edgeLabelsWithFreq
			} while((line = rows.readLine()) !=null && (line.charAt(0) == 'e'));
		}
		
		//go through vertex list
		for(int i = 0; i < nodesInDegree.size(); ++i)
		{
			//create label
			String nodeLabel = nodesInDegree.get(i).getInDegree() + "-" + nodesInDegree.get(i).getLabel();
			int label = -1;
			if(!nodeLabels.containsKey(nodeLabel))
            {
            	label = labelIndex;
            	nodeLabels.put(nodeLabel, labelIndex);
            	invertedNodeLabels.put(labelIndex, nodeLabel);
            	labelIndex++;
            }
            else
            	label = nodeLabels.get(nodeLabel);
			
			addNode(label); //ALE, this adds the node into m_matrix, which is an HPListGraph object
			//we can use i safely as our node ID since we read the vertex info in order and we inserted it in that same order into the nodesIndegree list
			myNode n = new myNode(i, label);//myNode n = new myNode(numberOfNodes, label);
			nodes.add(n);
			HashMap<Integer,myNode> tmp = nodesByLabel.get(label);
			if(tmp==null)  //ALE, if this is the first time we read this label, create a map for it
			{
				tmp = new HashMap<Integer,myNode>();
				nodesByLabel.put(label, tmp);
			}

			tmp.put(n.getID(), n); //ALE, this will change the value already inserted in nodesByLabel, as they are inserted by reference.
			
		}
		
		//go through edge list
		for(int i = 0; i < edgesToAdd.size(); ++i)
		{
			addEdge(edgesToAdd.get(i).getA(), edgesToAdd.get(i).getB(), 0); //ALE, this populates edgeLabelsWithFreq... params: nodeA, nodeB, label
		}
		
		//set originalHashedEdges
		StaticData.setOriginalHashedEdges();
		
		rows.close();		
	}
	
	//ALE separate file reading and initial pruning
	public void pruneNonfrequentElements(int freqThreshold)
	{
		//set the freqThreshold
		this.freqThreshold = freqThreshold;
		
		//reset the containers for frequent elements
		freqNodesByLabel= new HashMap<Integer, HashMap<Integer,myNode>>();
		freqEdgeLabels = new ArrayList<Double>();
		sortedFreqLabels= new ArrayList<Integer>();
		sortedFreqLabelsWithFreq = new ArrayList<Point>();
		
		//prune infrequent edge labels
		for (Iterator<  java.util.Map.Entry< Double,Integer> >  it= this.edgeLabelsWithFreq.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Double,Integer > ar =  it.next();			
			if(ar.getValue().doubleValue()>=freqThreshold)
			{
				this.freqEdgeLabels.add(ar.getKey());
			}
		}
		
		//now prune the infrequent nodes
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= nodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();			
			if(ar.getValue().size()>=freqThreshold)
			{
				sortedFreqLabelsWithFreq.add(new Point(ar.getKey(),ar.getValue().size())); //this is not sorted yet. It's just the frequent labels
				freqNodesByLabel.put(ar.getKey(), ar.getValue());
			}
		}
		
		//we sort sortedFreqLabelsWithFreq
		Collections.sort(sortedFreqLabelsWithFreq, new freqComparator());
		
		//we populate sortedFreqLabels with sortedFreqLabelsWithFreq 
		for (int j = 0; j < sortedFreqLabelsWithFreq.size(); j++) 
		{
			sortedFreqLabels.add(sortedFreqLabelsWithFreq.get(j).x);
		}
		
		//prune frequent hashedEdges
		StaticData.resetHashedEdges(); //bring it back to how it was when the file was first read
		Vector toBeDeleted = new Vector();
		Set<String> s = StaticData.hashedEdges.keySet();
		for (Iterator<String>  it= s.iterator(); it.hasNext();) 
		{
			String sig =  it.next();
			HashMap[] hm = StaticData.hashedEdges.get(sig);
			if(hm[0].size()<freqThreshold || hm[1].size()<freqThreshold)
			{
				toBeDeleted.addElement(sig);
			}
			else
				;
		}
		Enumeration<String> enum1 = toBeDeleted.elements();
		while(enum1.hasMoreElements())
		{
			String sig = enum1.nextElement();
			StaticData.hashedEdges.remove(sig);
		}
	}
	
	//ALE
	public Map<String, Integer> getNodeLabelsDictionary()
	{
		return nodeLabels;
	}
	
	//ALE
	public HashMap<Integer, String> getInvertedNodeLabelsDictionary()
	{
		return invertedNodeLabels;
	}
	
	//ALE
	public void printNodeLabelsDictionary()
	{
		for (Map.Entry<String, Integer> entry : nodeLabels.entrySet()) {
		    System.out.println(entry.getKey() + ": " + entry.getValue());
		}
	}
	
	//ALE
	public void printInvertedNodeLabelsDictionary()
	{
		System.out.println("Size of inverted labels dictionary: " + invertedNodeLabels.size());
		for (Map.Entry<Integer, String> entry : invertedNodeLabels.entrySet()) {
		    System.out.println(entry.getKey() + ": " + entry.getValue());
		}
	}
	
	public void printFreqNodes()
	{
		//ALE
		System.out.println("ALE, these are my frequent NODES.");
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= freqNodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
			
			//System.out.println("Freq Label: "+ar.getKey()+" with size: "+ar.getValue().size());
			System.out.println("Freq NODE Label: "+ar.getKey()+" appears "+ar.getValue().size() + " times.");
		}
	}
	
	//1 hop distance for the shortest paths
	public void setShortestPaths_1hop()
	{
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= freqNodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
			
			HashMap<Integer,myNode> freqNodes= ar.getValue(); //ALE these are the nodes that have this label
			int counter=0;
			//ALE we are going through each of these nodes and printing the count, but also setting reachable nodes.
			for (Iterator<myNode> iterator = freqNodes.values().iterator(); iterator.hasNext();) 
			{
				myNode node =  iterator.next();
				//ALE IMPORTANT with changing freqs: reset node neighbors (reachable nodes) every time we change freqThreshold as we should not include non frequent nodes in the nodes' neighbors list
				//node.resetNodeDetails();
				//System.out.println(counter++);
				node.setReachableNodes_1hop(this, freqNodesByLabel); //ALE we essentially populate the children and parents lists for each node object
			}
		}
		System.out.println("ALE populated freq children and parents for each node, DONE!");
	}
	
	public void setShortestPaths(DijkstraEngine dj)
	{
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= freqNodesByLabel.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
			
			HashMap<Integer,myNode> freqNodes= ar.getValue();
			int counter=0;
			for (Iterator<myNode> iterator = freqNodes.values().iterator(); iterator.hasNext();) 
			{
				myNode node =  iterator.next();
				dj.execute(node.getID(),null);
				System.out.println(counter++);
				node.setReachableNodes(dj, freqNodesByLabel, this.getListGraph());
			}
		}
	}
	
	public myNode getNode(int ID)
	{
		return nodes.get(ID);
	}
	
	public HPListGraph<Integer, Double> getListGraph()
	{
		return m_matrix;
	}
	public int getID() {
		return m_id;
	}
	
	public int getDegree(int node) {

		return m_matrix.getDegree(node);
	}
		
	public int getNumberOfNodes()
	{
		return nodeCount;
	}
	
	 
	public int addNode(int nodeLabel) {
		return m_matrix.addNodeIndex(nodeLabel);
	}
	public int addEdge(int nodeA, int nodeB, double edgeLabel) 
	{
		Integer I = edgeLabelsWithFreq.get(edgeLabel); 
		
		if(I==null)
			edgeLabelsWithFreq.put(edgeLabel, 1); //if edge is new, freq is 1
		else
			edgeLabelsWithFreq.put(edgeLabel, I.intValue()+1); //if edge existed, update freq
		
		//add edge frequency
		int labelA = nodes.get(nodeA).getLabel();
		int labelB = nodes.get(nodeB).getLabel();
		
		String hn;
		
		hn = labelA+"_"+edgeLabel+"_"+labelB; //this is edge ID
		
		HashMap<Integer,Integer>[] hm = StaticData.hashedEdges.get(hn); 
		if(hm==null)
		{
			hm = new HashMap[2];
			hm[0] = new HashMap();
			hm[1] = new HashMap();
					
			StaticData.hashedEdges.put(hn, hm);
		}
		else
		{}
		hm[0].put(nodeA, nodeA);
		hm[1].put(nodeB, nodeB);
		
		return m_matrix.addEdgeIndex(nodeA, nodeB, edgeLabel, 1);
	}
	
	//ALE
	public ArrayList<myNode> getNodes()
	{
		return nodes;
	}
	
	//ALE
	public Set<Integer> getPossibleFreqThresholds()
	{
		System.out.println(StaticData.getAllEdgesFrequencies());
		return StaticData.getAllEdgesFrequencies();
	}
}
