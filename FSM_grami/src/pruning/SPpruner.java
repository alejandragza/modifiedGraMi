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

package pruning;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map.Entry;

import utilities.MyPair;
import utilities.Settings;

import CSP.DFSSearch;
import CSP.Variable;
import CSP.VariablePair;
import dataStructures.ConnectedComponent;
import dataStructures.Graph;
import dataStructures.Query;
import dataStructures.myNode;

public class SPpruner 
{
	private Variable[] variables;

	public Variable[] getVariables() {
		return variables;
	}
	public SPpruner() 
	{
		
	}
	
	
	public void getPrunedLists(ArrayList<HashMap<Integer,myNode>> candidatesByNodeID, Query qry)
	{
		
		ArrayList<ConnectedComponent> cls= qry.getConnectedLabels();
		//create the variables
		variables= new Variable[qry.getListGraph().getNodeCount()];
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			int label= qry.getListGraph().getNodeLabel(i);
			variables[i]= new Variable(i, label, candidatesByNodeID.get(i),null,null);
		}
		
		for (int i = 0; i < cls.size(); i++) 
		{
			ConnectedComponent c = cls.get(i);
			int nodeA=c.getIndexA();
			int nodeB=c.getIndexB();
			
			variables[nodeA].addConstraintWith(nodeB, c.getEdgeLabel());
			variables[nodeB].addConstrainedBy(nodeA, c.getEdgeLabel());
		}
		
		AC_3_New(variables, -1);
	}
	
	
	public void getPrunedLists(HashMap<Integer,HashMap<Integer,myNode>> nodesByLabel, Query qry)
	{
		System.out.println("called Automorphism pruned lists");
		HashMap<Integer, HashMap<Integer,myNode>> pruned= new HashMap<Integer, HashMap<Integer,myNode>>();// QueryID -> NodeID->NODE
		ArrayList<ConnectedComponent> cls= qry.getConnectedLabels();
				
		//refine according to nodeLabels
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			int label= qry.getListGraph().getNodeLabel(i);
			pruned.put(i, (HashMap<Integer,myNode>)nodesByLabel.get(label).clone());
		}
		
		//refine according to degree !!
		HashMap<Integer, HashMap<Integer, Integer>> nodeOutLabelDegrees= new HashMap<Integer, HashMap<Integer,Integer>>();//nodeID-->(Label,Degree)
		HashMap<Integer, HashMap<Integer, Integer>> nodeInLabelDegrees= new HashMap<Integer, HashMap<Integer,Integer>>();
				
		for (int i = 0; i < cls.size(); i++) 
		{
			ConnectedComponent c = cls.get(i);
			int nodeA=c.getIndexA();
			int nodeB=c.getIndexB();
			HashMap<Integer, Integer> nodeAmap=nodeOutLabelDegrees.get(nodeA);
			HashMap<Integer, Integer> nodeBmap=nodeInLabelDegrees.get(nodeB);
			if(nodeAmap==null)
			{
				nodeAmap= new HashMap<Integer, Integer>();
				nodeOutLabelDegrees.put(nodeA, nodeAmap);
			}
			if(nodeBmap==null)
			{
				nodeBmap= new HashMap<Integer, Integer>();
				nodeInLabelDegrees.put(nodeB, nodeBmap);
			}
			
			Integer degreeA=nodeAmap.get(c.getLabelB());
			if(degreeA==null)
				degreeA=0;
			Integer degreeB=nodeBmap.get(c.getLabelA());
			if(degreeB==null)
				degreeB=0;
			nodeAmap.put(c.getLabelB(), degreeA+1);
			nodeBmap.put(c.getLabelA(), degreeB+1);
		}
				
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			HashMap<Integer, Integer> degreeOutCons= nodeOutLabelDegrees.get(i);
			HashMap<Integer, Integer> degreeInCons= nodeInLabelDegrees.get(i);
			
			HashMap<Integer,myNode> candidates=pruned.get(i);
			boolean isValidNode=true;
			
			for (Iterator<Entry<Integer, myNode>> it = candidates.entrySet().iterator(); it.hasNext();)
			{
				Entry<Integer, myNode> nodeEntry=it.next();
				myNode node=nodeEntry.getValue();
				isValidNode=true;
				if(degreeOutCons!=null)
				for (Iterator<Entry<Integer, Integer>> iterator = degreeOutCons.entrySet().iterator(); iterator.hasNext();) 
				{
					Entry<Integer, Integer> entry =  iterator.next();
					int label=entry.getKey();
					int degree=entry.getValue();
					
					if(node.getOutDegree(label)<degree)
					{isValidNode=false; break;}
				}
				if(isValidNode && degreeInCons!=null)
				{
					for (Iterator<Entry<Integer, Integer>> iterator = degreeInCons.entrySet().iterator(); iterator.hasNext();) 
					{
						Entry<Integer, Integer> entry =  iterator.next();
						int label=entry.getKey();
						int degree=entry.getValue();
						
						if(node.getinDegree(label)<degree)
						{isValidNode=false; break;}
					}
				}
				if(isValidNode==false)
					it.remove();
			}
		}
				
		//create the variables
		variables= new Variable[qry.getListGraph().getNodeCount()];
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			int label= qry.getListGraph().getNodeLabel(i);
			variables[i]= new Variable(i, label, pruned.get(i),null,null);
		}
		for (int i = 0; i < cls.size(); i++) 
		{
			ConnectedComponent c = cls.get(i);
			int nodeA=c.getIndexA();
			int nodeB=c.getIndexB();
			
			variables[nodeA].addConstraintWith(nodeB, c.getEdgeLabel());
			variables[nodeB].addConstrainedBy(nodeA, c.getEdgeLabel());
		}
		
		AC_3_New(variables, -1);
	}
	
	
	public void getPrunedLists(Graph graph, Query qry,HashMap<Integer, HashSet<Integer>> nonCandidates)
	{
		System.out.println("called pruned lists");
		HashMap<Integer, HashMap<Integer,myNode>> pruned= new HashMap<Integer, HashMap<Integer,myNode>>();// QueryID -> NodeID->NODE

		ArrayList<ConnectedComponent> cls= qry.getConnectedLabels();
				
		//refine according to nodeLabels
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			int label= qry.getListGraph().getNodeLabel(i);
			if(graph.getFreqNodesByLabel().get(label) == null)
			{
				System.out.println("FREQNODESBYLABEL.getLabel(" + label +") is null");
				System.out.println("size of FREQNODESBYLABEL " + graph.getFreqNodesByLabel().size());
			}
			pruned.put(i, (HashMap<Integer,myNode>)graph.getFreqNodesByLabel().get(label).clone());
		}
		
		for (Iterator<Entry<Integer, HashSet<Integer>>> iterator = nonCandidates.entrySet().iterator(); iterator.hasNext();) 
		{
			Entry<Integer, HashSet<Integer>> entry = iterator.next();
			int qryID= entry.getKey();
			HashMap<Integer,myNode> prunedCands= pruned.get(qryID); //ALE noncandidate in pruned
			HashSet<Integer> nonCands= entry.getValue();
			for (Iterator iterator2 = nonCands.iterator(); iterator2.hasNext();) 
			{
				Integer integer = (Integer) iterator2.next();
				prunedCands.remove(integer);
			}
		}
				
		//refine according to degree !!
		HashMap<Integer, HashMap<Integer, Integer>> nodeOutLabelDegrees= new HashMap<Integer, HashMap<Integer,Integer>>();//nodeID-->(Label,Degree)
		HashMap<Integer, HashMap<Integer, Integer>> nodeInLabelDegrees= new HashMap<Integer, HashMap<Integer,Integer>>();
		
		for (int i = 0; i < cls.size(); i++) 
		{
			ConnectedComponent c = cls.get(i);
			int nodeA=c.getIndexA();
			int nodeB=c.getIndexB();
			HashMap<Integer, Integer> nodeAmap=nodeOutLabelDegrees.get(nodeA);
			HashMap<Integer, Integer> nodeBmap=nodeInLabelDegrees.get(nodeB);
			if(nodeAmap==null)
			{
				nodeAmap= new HashMap<Integer, Integer>();
				nodeOutLabelDegrees.put(nodeA, nodeAmap);
			}
			if(nodeBmap==null)
			{
				nodeBmap= new HashMap<Integer, Integer>();
				nodeInLabelDegrees.put(nodeB, nodeBmap);
			}
			
			Integer degreeA=nodeAmap.get(c.getLabelB());
			if(degreeA==null)
				degreeA=0;
			Integer degreeB=nodeBmap.get(c.getLabelA());
			if(degreeB==null)
				degreeB=0;
			nodeAmap.put(c.getLabelB(), degreeA+1);
			nodeBmap.put(c.getLabelA(), degreeB+1);
		}
		
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			HashMap<Integer, Integer> degreeOutCons= nodeOutLabelDegrees.get(i);
			HashMap<Integer, Integer> degreeInCons= nodeInLabelDegrees.get(i);
			
			HashMap<Integer,myNode> candidates=pruned.get(i);
			boolean isValidNode=true;
			
			for (Iterator<Entry<Integer, myNode>> it = candidates.entrySet().iterator(); it.hasNext();)
			{
				Entry<Integer, myNode> nodeEntry=it.next();
				myNode node=nodeEntry.getValue();
				isValidNode=true;
				if(degreeOutCons!=null)
				for (Iterator<Entry<Integer, Integer>> iterator = degreeOutCons.entrySet().iterator(); iterator.hasNext();) 
				{
					Entry<Integer, Integer> entry =  iterator.next();
					int label=entry.getKey();
					int degree=entry.getValue();
					
					if(node.getOutDegree(label)<degree)
					{isValidNode=false; break;}
				}
				if(isValidNode && degreeInCons!=null)
				{
					for (Iterator<Entry<Integer, Integer>> iterator = degreeInCons.entrySet().iterator(); iterator.hasNext();) 
					{
						Entry<Integer, Integer> entry =  iterator.next();
						int label=entry.getKey();
						int degree=entry.getValue();
						
						if(node.getinDegree(label)<degree)
						{isValidNode=false; break;}
					}
				}
				if(isValidNode==false)
					it.remove();
			}
		}
		
		//create the variables
		variables= new Variable[qry.getListGraph().getNodeCount()];
		for (int i = 0; i < qry.getListGraph().getNodeCount(); i++) 
		{
			int label= qry.getListGraph().getNodeLabel(i);
			variables[i]= new Variable(i, label, pruned.get(i),null,null);
		}
		
		for (int i = 0; i < cls.size(); i++) 
		{
			ConnectedComponent c = cls.get(i);
			int nodeA=c.getIndexA();
			int nodeB=c.getIndexB();
			
			variables[nodeA].addConstraintWith(nodeB, c.getEdgeLabel());
			variables[nodeB].addConstrainedBy(nodeA, c.getEdgeLabel());
		}
	}
	
	//insert vp into order according to their variable values length
	private void insertInOrder(LinkedList<VariablePair> Q, VariablePair vp)
	{
		int i = 0;
		Iterator itr = Q.iterator();
	    while(itr.hasNext())
	    {
	    	VariablePair tempVP = (VariablePair)itr.next();
	    	if(tempVP.getMinValuesLength()>vp.getMinValuesLength())
			{
				Q.add(i, vp);
				return;
			}
	    	i++;
	    }
	    Q.add(i, vp);
	}
	
	private void AC_3_New(Variable[] input, int freqThreshold)
	{
		LinkedList<VariablePair> Q= new LinkedList<VariablePair>();
		HashSet<String> contains = new HashSet<String> ();
		VariablePair vp;
		//initialize...
		for (int i = 0; i < input.length; i++) 
		{
			Variable currentVar= input[i];
			ArrayList<MyPair<Integer, Double>> list=currentVar.getDistanceConstrainedWith();
			for (int j = 0; j < list.size(); j++) 
			{
				Variable consVar=variables[list.get(j).getA()];
				vp =new VariablePair(currentVar,consVar,list.get(j).getB());
				Q.add(vp);
				contains.add(vp.getString());
				
			}
		}
		
		while(!Q.isEmpty())
		{
			System.out.println(Q.size());
			vp = Q.poll();
			contains.remove(vp.getString());
			Variable v1 = vp.v1;
			Variable v2 = vp.v2;

			if(v1.getListSize()<freqThreshold || v2.getListSize()<freqThreshold) return;
			int oldV1Size = v1.getListSize();
			int oldV2Size = v2.getListSize();
			refine_Newest(v1, v2, vp.edgeLabel, freqThreshold);
			if(oldV1Size!=v1.getListSize())
			{
				if(v1.getListSize()<freqThreshold) return;
				//add to queue
				ArrayList<MyPair<Integer, Double>> list=v1.getDistanceConstrainedBy();
				for (int j = 0; j < list.size(); j++) 
				{
					Integer tempMP = list.get(j).getA(); 
					Variable consVar=variables[tempMP];
					vp =new VariablePair(consVar,v1,list.get(j).getB());
					if(!contains.contains(vp.getString()))
					{
						insertInOrder(Q, vp);
						//add new variables at the begining
						contains.add(vp.getString());
					}
				}
			}
			if(oldV2Size!=v2.getListSize())
			{
				if(v2.getListSize()<freqThreshold) return;
				//add to queue
				ArrayList<MyPair<Integer, Double>> list=v2.getDistanceConstrainedBy();
				for (int j = 0; j < list.size(); j++) 
				{
					Variable consVar=variables[list.get(j).getA()];
					vp =new VariablePair(consVar,v2, list.get(j).getB());
					if(!contains.contains(vp.getString()))
					{
						insertInOrder(Q, vp);
						contains.add(vp.getString());
					}
				}
			}
		}
	}
	
	/**
	 * should be the fatsest version
	 */
	private void refine_Newest(Variable v1, Variable v2, double edgeLabel, int freqThreshold)
	{
		HashMap<Integer,myNode> listA,listB;
		
		int labelB=v2.getLabel();//lebel of my neighbor
		listA=v1.getList();//the first column
		listB=v2.getList();//the second column
		HashMap<Integer,myNode> newList= new HashMap<Integer,myNode>();//the newly assigned first column
		HashMap<Integer, myNode> newReachableListB = new HashMap<Integer, myNode>();//the newly asigned second column
		
		//go over the first column
		for (Iterator<myNode> iterator = listA.values().iterator(); iterator.hasNext();)
		{
			myNode n1= iterator.next();//get the current node
			if(n1.hasReachableNodes()==false)//prune a node without reachable nodes
				continue;
			
			ArrayList<MyPair<Integer, Double>> neighbors = n1.getRechableWithNodeIDs(labelB, edgeLabel);//get a list of current node's neighbors
			for (Iterator<MyPair<Integer, Double>> iterator2 = neighbors.iterator(); iterator2.hasNext();)//go over each neighbor
			{
				MyPair<Integer, Double> mp = iterator2.next();//get current neighbor details
				//check the second column if it contains the current neighbor node
				if(listB.containsKey(mp.getA()))
				{
					//if true, put the current node in the first column, and the neighbor node in the second column
					newList.put(n1.getID(),n1);
					newReachableListB.put(mp.getA(), listB.get(mp.getA()));
				}
			}
		}
		
		//set the newly assigned columns
		v1.setList(newList);
		v2.setList(newReachableListB);
	}
	
	public static void printSPs(HashMap<Integer, HashMap<Integer,myNode>> pruned)
	{
		
		for (Iterator<  java.util.Map.Entry< Integer, HashMap<Integer,myNode> > >  it= pruned.entrySet().iterator(); it.hasNext();) 
		{
			java.util.Map.Entry< Integer, HashMap<Integer,myNode> > ar =  it.next();
			
			System.out.println("New Freq Label: "+ar.getKey()+" with size: "+ar.getValue().size());
		}
		
		System.out.println("-----------------------------");
	}
}
