/**
 * This file was modified by Alejandra Garza 01-19-2020
 * created May 16, 2006
 * 
 * @by Marc Woerlein (woerlein@informatik.uni-erlangen.de)
 *
 * Copyright 2006 Marc Woerlein
 * 
 * This file is part of parsemis.
 *
 * Licence: 
 *  LGPL: http://www.gnu.org/licenses/lgpl.html
 *   EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *   See the LICENSE file in the project's top-level directory for details.
 */
package search;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import utilities.DfscodesCache;

import AlgorithmInterface.Algorithm;

import dataStructures.DFSCode;
import dataStructures.GSpanEdge;
import dataStructures.HPListGraph;

import dataStructures.StaticData; //ALE


//import de.parsemis.utils.Frequented;

/**
 * This class represents the local recursive strategy.
 * 
 * @author Marc Woerlein (woerlein@informatik.uni-erlangen.de)
 * 
 * @param <NodeType>
 *            the type of the node labels (will be hashed and checked with
 *            .equals(..))
 * @param <EdgeType>
 *            the type of the edge labels (will be hashed and checked with
 *            .equals(..))
 */
public class RecursiveStrategy<NodeType, EdgeType> implements
		Strategy<NodeType, EdgeType> {

	private Extender<NodeType, EdgeType> extender;

	private Collection<HPListGraph<NodeType, EdgeType>> ret;
	
	private ArrayList<DFSCode<NodeType, EdgeType>> aleRet;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.strategy.Strategy#search(de.parsemis.miner.Algorithm,
	 *      int)
	 */
	
	//this is the function that gets called first
	public Collection<HPListGraph<NodeType, EdgeType>> search(  //INITIAL NODES SEARCH
			final Algorithm<NodeType, EdgeType> algo,int freqThresh) {
		ret = new ArrayList<HPListGraph<NodeType, EdgeType>>();
		
		aleRet = new ArrayList<DFSCode<NodeType, EdgeType>>();
		
		//ALE GSpanExtender
		extender = algo.getExtender(freqThresh); //ALE functionality to extend one (or more) parent nodes to a set of children

		//ALE this iterator is for Map<GSpanEdge<NodeType, EdgeType>, DFSCode<NodeType, EdgeType>> initials. Check Algorithm.java
		for (final Iterator<SearchLatticeNode<NodeType, EdgeType>> it = algo
				.initialNodes(); it.hasNext();) {
			final SearchLatticeNode<NodeType, EdgeType> code = it.next();
			final long time = System.currentTimeMillis();
//			if (VERBOSE) {
//				System.out.print("doing seed " + code + " ...");
//			}
//			if (VVERBOSE) {
//				System.out.println();
//			}
			System.out.println("Searching into: "+code);
			System.out.println("*********************************");
			search(code);
			it.remove();

//			if (VERBOSE) {
//				out.println("\tdone (" + (System.currentTimeMillis() - time)
//						+ " ms)");
			//}
		}
		
		//ALE
		System.out.println("\n\n\nALE BEFORE RETURNING FROM FIRST SEARCH()");
		//for (int i = 0; i < aleRet.size(); i++)  
            //aleRet.get(i).printVariables(); //ALE THIS IS THE FUNCTION TO PRINT THE FREQUENT SUBGRAPH NODES
		System.out.println("\n\n\nALE after loop... size of ret is " + ret.size() + " and aleRet is " + aleRet.size());
		return ret;
	}

	@SuppressWarnings("unchecked")
	private void search(final SearchLatticeNode<NodeType, EdgeType> node) {  //RECURSIVE NODES SEARCH

		System.out.println("*Getting Children");
		final Collection<SearchLatticeNode<NodeType, EdgeType>> tmp = extender
				.getChildren(node); //ALE an iterator over all children of the given node. This line ends up using CanonicalPruningStep call() function
		System.out.println("finished Getting Children");
		System.out.println(node.getLevel()); //ALE (= depth in the search tree) of this node
		for (final SearchLatticeNode<NodeType, EdgeType> child : tmp) {
//			if (VVVERBOSE) {
//				System.out.println("doing child " + child);
//			}
			System.out.println("   branching into: "+child);
			System.out.println("   ---------------------");
			search(child); //ALE search every child recursively
			
			
		}
//		if (VVERBOSE) {
//			System.out.println("node " + node + " done. Store: " + node.store()
//					+ " children " + tmp.size() + " freq ...(apparently we don't know the f)");
//  				+ ((Frequented) node).frequency());
//		}
		if (node.store()) {
			System.out.println("ALE, node " + node + " done AND STORED. Store: " + node.store()
			+ " children " + tmp.size());
			//ALE only store it if subgraph is of minSize or bigger
			if(node.getHPlistGraph().getNodeCount() >= StaticData.minSizeOfGraphs)
			{
				node.store(ret); //ALE we store the fragment into the given set (aka we add it into the arraylist)
				
				aleRet.add((DFSCode)node); //ALE important! We added this to keep track of the vertices
			}
			//node.store(ret); //ALE we store the fragment into the given set (aka we add it into the arraylist)
			
			//aleRet.add((DFSCode)node); //ALE important! We added this to keep track of the vertices
		} else {
			System.out.println("ALE, we are not keeping node " + node + " children " + tmp.size());
			node.release();
		}

		node.finalizeIt();
		System.out.println("ALE back to main search()");
	}
	
	//ALE
	public ArrayList<DFSCode<NodeType, EdgeType>> getFSInfo()
	{
		return aleRet;
	}

}
