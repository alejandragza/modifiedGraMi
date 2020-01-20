/**
 * This file was modified by Alejandra Garza 01-19-2020
 * created May 30, 2006
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
package dataStructures;

import java.awt.List;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import CSP.Variable; //ALE

/**
 * List-based implementation of the HPMutableGraph interface.
 * <p>
 * It stores the required node and edge informations in different int-arrays.
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
public class HPListGraph<NodeType, EdgeType> implements
		HPMutableGraph<NodeType, EdgeType> {

	/**
	 * 
	 * This class is a factory to create new graphs of this type.
	 * 
	 * @author Marc Woerlein (woerlein@informatik.uni-erlangen.de)
	 * 
	 * @param <NodeType>
	 * @param <EdgeType>
	 */
	@SuppressWarnings("hiding")
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 3527286763882922740L;

	private static int ID = 0;

	/** to mark a node or edge as deleted */
	static final Object DELETED = new Object();

	//ALE, the constant ints here represent positions
	private static final int NODEA = 0, NODEB = 1, DIRECTION = 2,
			NEXTEMPTY = 0, EMPTYNODE = 0, EMPTYEDGE = 1, NODECOUNT = 2,
			EDGECOUNT = 3, MAXNODE = 4, MAXEDGE = 5, DEGREE = 0, INDEGREE = 1,
			OUTDEGREE = 2, FIRSTEDGE = 3, DEFAULTSIZE = 4, DEFAULTNODESIZE = 6;

	private static final double RESIZE_SCALE = 1.5;

	/** stores the name of the graph */
	private final String name;

	/** stores the ID of the graph */
	private final int id;

	/**
	 * stores the first deleted Edge/Node ([0/1]), the node/edge count([2/3]),
	 * and the max-node/edge index([4/5])
	 */
	final int[] status;
	

	/**
	 * stores for each node in the array[nodeIndex] the degree ([x][0]), the
	 * in/outdegree ([x][1/2]) and the connected edge [x][3..].
	 * 
	 * It also contains the single linked deleted list at [x][NEXTEMPTY] in each
	 * deleted node. -1 marks the end of this list.
	 */
	int[][] node_edges;
	
	/**
	 * stores the first node of each edge at [edgeIdx*3+NODEA], the second node
	 * at [edgeIdx*3+NODEB] and the direction in respect to nodeA at
	 * [edgeIdx*3+DIRECTION].
	 * 
	 * It also contains the single linked deleted list at [edgeIdx*3+NEXTEMPTY]
	 * in each deleted edge. -1 marks the end of the list.
	 */
	private int[] edge_nodes_and_direction;
	
	private int[] isFrequentNodes;
	
	NodeType[] node_labels;

	EdgeType[] edge_labels;


	private transient BitSet edges;

	private transient BitSet nodes;
	
	DFSCode<NodeType, EdgeType> roughInfo; //ALE
	GenericNode[] genericNodes; //ALE
	Variable[] genericNodeVariables; //ALE



	/**
	 * creates an empty graph
	 */
	public HPListGraph() {
		this("" + ID, ID++);
	}

	
	
	/**
	 * creates an empty graph with the given name
	 * 
	 * @param name
	 */
	public HPListGraph(final String name) {
		this(name, ID++);
	}

	/**
	 * creates an empty graph with the given ID and the given name
	 * 
	 * @param name
	 * @param id
	 */
	private HPListGraph(final String name, final int id) {
		this(name, id, DEFAULTSIZE, DEFAULTSIZE);
		for (int i = 0; i < DEFAULTSIZE; ++i) {
			node_edges[i] = new int[DEFAULTNODESIZE];
		}
		status[EMPTYNODE] = -1;
		status[EMPTYEDGE] = -1;
	}

	@SuppressWarnings("unchecked")
	private HPListGraph(final String name, final int id, final int nodeSize,
			final int edgeSize) {
		this.name = name;
		this.id = id;
		node_edges = new int[nodeSize][];
		edge_nodes_and_direction = new int[edgeSize * 3];
		node_labels = (NodeType[]) new Object[nodeSize];
		edge_labels = (EdgeType[]) new Object[edgeSize];
		status = new int[MAXEDGE + 1];
	}

	@SuppressWarnings("unchecked")
	private final int _addEdge(final int na, final int nb,
			final EdgeType label, int direction) {
		int idx = status[EMPTYEDGE];
		if (idx < 0) {// no empty edge available
			idx = status[MAXEDGE]++;
			final int length = edge_labels.length;
			if (idx >= length) {// resize nodes
				final int newlength = (int) (RESIZE_SCALE * idx);
				System.arraycopy(edge_nodes_and_direction, 0,
						edge_nodes_and_direction = new int[3 * newlength], 0,
						3 * length);
				System.arraycopy(edge_labels, 0,
						edge_labels = ((EdgeType[]) new Object[newlength]), 0,
						length);
			}
		} else {
			status[EMPTYEDGE] = edge_nodes_and_direction[idx * 3 + NODEA];
		}
		edge_nodes_and_direction[idx * 3 + NODEA] = na;
		edge_nodes_and_direction[idx * 3 + NODEB] = nb;
		edge_nodes_and_direction[idx * 3 + DIRECTION] = direction;
		_addEdgeToNode(na, idx, direction);
		_addEdgeToNode(nb, idx, -direction);
		++status[EDGECOUNT];
		edge_labels[idx] = label;
		edgesChanged();
		return idx;
	}
	
	//given two node ids, return null in case of failure
	public EdgeType getEdgeLabel(int na, int nb)
	{
		int edgeIdx = getEdge(na, nb);
		if(edgeIdx<0) return null;
		else return getEdgeLabel(edgeIdx);
	}

	private final void _addEdgeToNode(final int ni, final int ei,
			final int direction) {
		int[] node = node_edges[ni];
		final int pos = ++node[DEGREE] + FIRSTEDGE - 1;
		final int length = node.length;
		if (pos >= length) { // resize array
			node_edges[ni] = new int[(int) (pos * RESIZE_SCALE)];
			System.arraycopy(node, 0, node = node_edges[ni], 0, length);
		}
		node[pos] = ei;
		if (direction == Edge.INCOMING) {
			++node[INDEGREE];
		}
		if (direction == Edge.OUTGOING) {
			++node[OUTDEGREE];
		}
	}

	@SuppressWarnings("unchecked")
	private final int _addNode(final NodeType label) {
		int idx = status[EMPTYNODE]; //ALE I think this identifies gaps in the node_labels array that occurred when we deleted nodes
		if (idx < 0) {// no empty nodes (gaps) available
			//ALE if there are no gaps, we give this node a sequential id
			idx = status[MAXNODE];
			++status[MAXNODE];
			final int length = node_labels.length;
			//ALE if we have added more nodes than the space we have in node_labels, we increase it's length by 50%
			if (idx >= length) {// resize nodes
				final int newlength = (int) (RESIZE_SCALE * idx);
				System.arraycopy(node_edges, 0,
						node_edges = new int[newlength][], 0, length);
				System.arraycopy(node_labels, 0,
						node_labels = ((NodeType[]) new Object[newlength]), 0,
						length);
				// initialize empty node_edges 
				for (int i = length; i < newlength; ++i) {
					node_edges[i] = new int[DEFAULTNODESIZE]; //ALE this is our new node row
				}
			}
		} else {
			//ALE if we are taking a gap to place a new node, we need to update the gap.
			status[EMPTYNODE] = node_edges[idx][NEXTEMPTY]; //ALE see where the next empty index is
			node_edges[idx][DEGREE] = node_edges[idx][INDEGREE] = node_edges[idx][OUTDEGREE] = 0; //ALE set positions 0-2 to 0
		}
		++status[NODECOUNT];
		node_labels[idx] = label;
		nodesChanged();
		assert node_edges[0] != null : dumpGraph("_addNode(" + label.toString()
				+ ") " + idx);
		return idx;
	}

	@SuppressWarnings("unchecked")
	final void _deleteEdge(final int ei) {
		_deleteEdgeFromNode(ei, edge_nodes_and_direction[ei * 3 + NODEA],
				edge_nodes_and_direction[ei * 3 + DIRECTION]);
		_deleteEdgeFromNode(ei, edge_nodes_and_direction[ei * 3 + NODEB],
				-edge_nodes_and_direction[ei * 3 + DIRECTION]);
		edge_nodes_and_direction[ei * 3 + NEXTEMPTY] = status[EMPTYEDGE];
		status[EMPTYEDGE] = ei;
		--status[EDGECOUNT];
		edge_labels[ei] = (EdgeType) DELETED;
		edgesChanged();
	}

	private final void _deleteEdgeFromNode(final int ei, final int ni,
			final int direction) {
		final int[] node = node_edges[ni];
		final int lastPos = --node[DEGREE] + FIRSTEDGE;
		if (direction == Edge.INCOMING) {
			--node[INDEGREE];
		}
		if (direction == Edge.OUTGOING) {
			--node[OUTDEGREE];
		}
		int i = lastPos;
		while (node[i] != ei) {
			--i;
		}
		node[i] = node[lastPos];
	}

	@SuppressWarnings("unchecked")
	final void _deleteNode(final int ni) {
		final int[] node = node_edges[ni];
		for (int eii = node[DEGREE] + FIRSTEDGE - 1; eii >= FIRSTEDGE; --eii) {
			_deleteEdge(node[eii]);
		}
		node[NEXTEMPTY] = status[EMPTYNODE];
		status[EMPTYNODE] = ni;
		node_labels[ni] = (NodeType) DELETED;
		nodesChanged();
	}
	
	public void setFreqStatus(Map<NodeType, ArrayList<Integer>> embeddings)
	{
		isFrequentNodes = new int[getNodeCount()];
		Collection<ArrayList<Integer>> embsList = embeddings.values();
		for (Iterator iterator = embsList.iterator(); iterator.hasNext();)
		{
			ArrayList<Integer> embs = (ArrayList<Integer>)iterator.next();
			for (int i = 0; i < embs.size(); i++) 
			{
				int embedding= embs.get(i);
				isFrequentNodes[embedding]=1;
			}
		}
	}
	
	public int isFrequent(int embedding)
	{
		return isFrequentNodes[embedding];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPMutableGraph#addEdgeIndex(int, int, null, int)
	 */
	public int addEdgeIndex(final int nodeAIdx, final int nodeBIdx,
			final EdgeType label, final int direction) {
		return _addEdge(nodeAIdx, nodeBIdx, label, direction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPMutableGraph#addNodeAndEdgeIndex(int, null,
	 *      null, int)
	 */
	public int addNodeAndEdgeIndex(final int nodeAIdx,
			final NodeType nodeLabel, final EdgeType edgeLabel,
			final int direction) {
		final int nodeBIdx = addNodeIndex(nodeLabel);
		_addEdge(nodeAIdx, nodeBIdx, edgeLabel, direction);
		return nodeBIdx;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPMutableGraph#addNodeIndex(null)
	 */
	public int addNodeIndex(final NodeType label) {
		return _addNode(label);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.utils.Clonable#clone()
	 */
	@Override
	public HPGraph<NodeType, EdgeType> clone() {
		assert this.node_edges[0] != null : dumpGraph("clone(): original wrong");

		final int ns = node_labels.length;
		final int es = edge_labels.length;
		final HPListGraph<NodeType, EdgeType> ret = new HPListGraph<NodeType, EdgeType>(
				name, ID++, ns, es);
		System.arraycopy(this.status, 0, ret.status, 0, this.status.length);

		assert this.node_edges.length == ns : dumpGraph("clone(): node_edges.length wrong "
				+ this.node_edges.length + " " + ns);
		assert ret.node_edges.length == ns : ret
				.dumpGraph("clone(): node_edges.length wrong "
						+ ret.node_edges.length + " " + ns);

		for (int i = ns - 1; i >= 0; --i) {
			ret.node_edges[i] = new int[this.node_edges[i].length];
			System.arraycopy(this.node_edges[i], 0, ret.node_edges[i], 0,
					this.node_edges[i].length);
		}
		System.arraycopy(this.edge_nodes_and_direction, 0,
				ret.edge_nodes_and_direction, 0, 3 * es);
		for (int i = this.node_labels.length - 1; i >= 0; --i) {
			ret.node_labels[i] = this.node_labels[i];// clone??
		}
		for (int i = this.edge_labels.length - 1; i >= 0; --i) {
			ret.edge_labels[i] = this.edge_labels[i];// clone??
		}

		assert ret.node_edges[0] != null : dumpGraph("clone(): original")
				+ "\n" + ret.dumpGraph("clone(): clone");

		return ret;
	}

	private String dumpGraph(final String message) {
		final StringBuilder b = new StringBuilder();
		
		return "DUMPING........DUMB!!";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#edgeIndexIterator()
	 */
	public IntIterator edgeIndexIterator() {
		return new IntIterator() {
			private final int max = status[MAXEDGE];

			private int next = -1, last = -1;

			public boolean hasNext() {
				if (next >= max) {
					return false;
				}
				if (next > 0) {
					return edge_labels[next] != DELETED;
				}
				for (next = last + 1; next < max
						&& edge_labels[next] == DELETED; ++next) {
					;// step to next valid edge
				}
				return (next < max);
			}

			public int next() {
				if (hasNext()) {
					last = next;
					next = -1;
					return last;
				}
				throw new NoSuchElementException();
			}

			public void remove() {
				_deleteEdge(last);
			};
		};
	}

	private final void edgesChanged() {
		
		edges = null;
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getDegree(int)
	 */
	public int getDegree(final int nodeIdx) {
		return node_edges[nodeIdx][DEGREE];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getDirection(int)
	 */
	public int getDirection(final int edgeIdx) {
		return edge_nodes_and_direction[edgeIdx * 3 + DIRECTION];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getDirection(int, int)
	 */
	public int getDirection(final int edgeIdx, final int nodeIdx) {
		if (edge_nodes_and_direction[edgeIdx * 3 + NODEA] == nodeIdx) {
			return edge_nodes_and_direction[edgeIdx * 3 + DIRECTION];
		}
		if (edge_nodes_and_direction[edgeIdx * 3 + NODEB] == nodeIdx) {
			return -edge_nodes_and_direction[edgeIdx * 3 + DIRECTION];
		}
		throw new IllegalArgumentException("node index " + nodeIdx
				+ " is invalid for the edge " + edgeIdx + "!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getEdge(int, int)
	 */
	public int getEdge(final int nodeAIdx, final int nodeBIdx) {
		final int[] nodeA = this.node_edges[nodeAIdx];
		for (int i = nodeA[DEGREE] + FIRSTEDGE - 1; i >= FIRSTEDGE; --i) {
			final int edgeIdx = nodeA[i];
			final int NA = this.edge_nodes_and_direction[edgeIdx * 3 + NODEA];
			final int NB = this.edge_nodes_and_direction[edgeIdx * 3 + NODEB];
			final int Dir = this.edge_nodes_and_direction[edgeIdx * 3
					+ DIRECTION];
			if ((NA == nodeAIdx && NB == nodeBIdx && Dir != Edge.INCOMING)
					|| (NA == nodeBIdx && NB == nodeAIdx && Dir != Edge.OUTGOING)) {
				return edgeIdx;
			}
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getEdgeCount()
	 */
	public int getEdgeCount() {
		return status[EDGECOUNT];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getEdgeIndices(int)
	 */
	public IntIterator getEdgeIndices(final int nodeIdx) {
		return new IntIterator() {
			private final int[] node = node_edges[nodeIdx];

			private int pos = FIRSTEDGE;

			public boolean hasNext() {
				return pos < node[DEGREE] + FIRSTEDGE;
			}

			public int next() {
				if (hasNext()) {
					return node[pos++];
				}
				throw new NoSuchElementException();
			}

			public void remove() {
				_deleteEdge(node[pos - 1]);
				--pos;
			};
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getEdgeLabel(int)
	 */
	public EdgeType getEdgeLabel(final int edgeIdx) {
		return edge_labels[edgeIdx];
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getEdges()
	 */
	public BitSet getEdges() {
		if (edges == null) {
			synchronized (this) {
				if (edges == null) {
					edges = new BitSet(status[MAXEDGE]);
					for (int i = status[MAXEDGE] - 1; i >= 0; --i) {
						if (this.isValidEdge(i)) {
							edges.set(i);
						}
					}
				}
			}
		}
		return edges;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getID()
	 */
	public int getID() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getInDegree(int)
	 */
	public int getInDegree(final int nodeIdx) {
		return node_edges[nodeIdx][INDEGREE];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getInEdgeIndices(int)
	 */
	public IntIterator getInEdgeIndices(final int nodeIdx) {
		return new IntIterator() {
			private final int[] node = node_edges[nodeIdx];

			private int next = -1, last = FIRSTEDGE - 1;

			public boolean hasNext() {
				if (next >= node[DEGREE] + FIRSTEDGE) {
					return false;
				}
				if (next >= FIRSTEDGE) {
					return true;
				}
				for (next = last + 1; next < node[DEGREE] + FIRSTEDGE
						&& getDirection(node[next], nodeIdx) != Edge.INCOMING; ++next) {
					;// step to next incomming edge
				}
				return (next < node[DEGREE] + FIRSTEDGE);
			}

			public int next() {
				if (hasNext()) {
					last = next;
					next = -1;
					return node[last];
				}
				throw new NoSuchElementException();
			}

			public void remove() {
				_deleteEdge(node[last]);
				--last;
			};
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getMaxEdgeIndex()
	 */
	public int getMaxEdgeIndex() {
		return status[MAXEDGE];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getMaxNodeIndex()
	 */
	public int getMaxNodeIndex() {
		return status[MAXNODE];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodeA(int)
	 */
	public int getNodeA(final int edgeIdx) {
		return edge_nodes_and_direction[edgeIdx * 3 + NODEA];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodeB(int)
	 */
	public int getNodeB(final int edgeIdx) {
		return edge_nodes_and_direction[edgeIdx * 3 + NODEB];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodeCount()
	 */
	public int getNodeCount() {
		return status[NODECOUNT];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodeEdge(int, int)
	 */
	public int getNodeEdge(final int nodeIdx, final int pos) {
		return node_edges[nodeIdx][FIRSTEDGE + pos];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodeLabel(int)
	 */
	public NodeType getNodeLabel(final int nodeIdx) {
		return node_labels[nodeIdx];
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodeNeigbour(int, int)
	 */
	public int getNodeNeigbour(final int nodeIdx, final int pos) {
		return getOtherNode(getNodeEdge(nodeIdx, pos), nodeIdx);
	}

	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getNodes()
	 */
	public BitSet getNodes() {
		if (nodes == null) {
			synchronized (this) {
				if (nodes == null) {
					nodes = new BitSet(status[MAXNODE]); //ALE we initialize the array with the amount of nodes we have
					for (int i = status[MAXNODE] - 1; i >= 0; --i) {
						if (this.isValidNode(i)) {
							nodes.set(i);
						}
					}
				}
			}
		}
		return nodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getOtherNode(int, int)
	 */
	public int getOtherNode(final int edgeIdx, final int nodeIdx) {
		if (edge_nodes_and_direction[edgeIdx * 3 + NODEA] == nodeIdx) {
			return edge_nodes_and_direction[edgeIdx * 3 + NODEB];
		}
		if (edge_nodes_and_direction[edgeIdx * 3 + NODEB] == nodeIdx) {
			return edge_nodes_and_direction[edgeIdx * 3 + NODEA];
		}
		throw new IllegalArgumentException("node index " + nodeIdx
				+ " is invalid for the edge " + edgeIdx + "!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getOutDegree(int)
	 */
	public int getOutDegree(final int nodeIdx) {
		return node_edges[nodeIdx][OUTDEGREE];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#getOutEdgeIndices(int)
	 */
	public IntIterator getOutEdgeIndices(final int nodeIdx) {
		return new IntIterator() {
			private final int[] node = node_edges[nodeIdx];

			private int next = -1, last = FIRSTEDGE - 1;

			public boolean hasNext() {
				if (next >= node[DEGREE] + FIRSTEDGE) {
					return false;
				}
				if (next >= FIRSTEDGE) {
					return true;
				}
				for (next = last + 1; next < node[DEGREE] + FIRSTEDGE
						&& getDirection(node[next], nodeIdx) != Edge.OUTGOING; ++next) {
					;// step to next outgoing edge
				}
				return (next < node[DEGREE] + FIRSTEDGE);
			}

			public int next() {
				if (hasNext()) {
					last = next;
					next = -1;
					return node[last];
				}
				throw new NoSuchElementException();
			}

			public void remove() {
				_deleteEdge(node[last]);
				--last;
			};
		};
	}

	public boolean isValidEdge(final int edgeIdx) {
		return !(edgeIdx < 0 || edgeIdx >= status[MAXEDGE] || edge_labels[edgeIdx] == DELETED);
	}

	public boolean isValidNode(final int nodeIdx) {
		//ALE we are checking if the node new ID is invalid (negative or greater than our biggest index) or whether it has been deleted
		return !(nodeIdx < 0 || nodeIdx >= status[MAXNODE] || node_labels[nodeIdx] == DELETED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#nodeIndexIterator()
	 */
	public IntIterator nodeIndexIterator() {
		return new IntIterator() {
			private final int max = status[MAXNODE];

			private int next = -1, last = -1;

			public boolean hasNext() {
				if (next >= max) {
					return false;
				}
				if (next > 0) {
					return node_labels[next] != DELETED;
				}
				for (next = last + 1; next < max
						&& node_labels[next] == DELETED; ++next) {
					;// step to next valid node
				}
				return (next < max);
			}

			public int next() {
				if (hasNext()) {
					last = next;
					next = -1;
					return last;
				}
				throw new NoSuchElementException();
			}

			public void remove() {
				_deleteNode(last);
			};
		};
	}

	private final void nodesChanged() {
		nodes = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPMutableGraph#removeEdge(int)
	 */
	public boolean removeEdge(final int edgeIdx) {
		if (!this.isValidEdge(edgeIdx)) {
			return false;
		}
		this._deleteEdge(edgeIdx);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPMutableGraph#removeNode(int)
	 */
	public boolean removeNode(final int nodeIdx) {
		if (!this.isValidNode(nodeIdx)) {
			return false;
		}
		this._deleteNode(nodeIdx);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#setEdgeLabel(int, null)
	 */
	public void setEdgeLabel(final int edgeIdx, final EdgeType label) {
		edge_labels[edgeIdx] = label;
		edgesChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.parsemis.graph.HPGraph#setNodeLabel(int, null)
	 */
	public void setNodeLabel(final int nodeIdx, final NodeType label) {
		node_labels[nodeIdx] = label;
		nodesChanged();
	}

	public String toString() {
		return DFScodeSerializer.serialize(this);
	}
	
	//ALE
	public void setRoughInfo(DFSCode<NodeType, EdgeType> roughInfo)
	{
		this.roughInfo = roughInfo;
		genericNodeVariables = roughInfo.getCurrentVariables();	
	}
	
	//ALE build generic subgraph
	/*public void buildGenericGraph_(Graph singleGraph)
	{
		ArrayList<myNode> originalNodes = singleGraph.getNodes();
		ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL edges involved in ALL replicas of this structure
		
		//generate edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			
			//to store candidates for mappings
			Set<Integer> parents;
			Set<Integer> children;
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				parents = genericNodeVariables[node1].getNodeIDs();
				children = genericNodeVariables[node2].getNodeIDs();
			}
			else
			{
				//node2 -> node1
				parents = genericNodeVariables[node2].getNodeIDs();
				children = genericNodeVariables[node1].getNodeIDs();
			}
			
			//for each parent, find its child and insert the edge
			for(int parent : parents)
			{
				//System.out.print(parent + ", ");
				
				//1. get original node info
				myNode parentNode = originalNodes.get(parent);
				
				//2. get the children of the parent
				Set<Integer> allChildrenOfParent = parentNode.getChildrenIDs();
				
				//3. do intersection of allChildrenOfParent with possible children
				Set<Integer> intersection = new HashSet<Integer>(children);
				intersection.retainAll(allChildrenOfParent);
				
				//4. add relevant edges
				for(int edge : intersection)
				{
					allEdges.add(new MyEdge(parent, edge));
				}
				
			}
			//System.out.println();
		}
		
		System.out.println("\n\n\nall edges");
		for(int i = 0; i < allEdges.size(); ++i)
		{
			System.out.println(allEdges.get(i).toString());
		}
		
		int i = 0;
		ArrayList<MyGraph> subgraphInstances = new ArrayList<>();
		while(i < allEdges.size())
		{
			MyEdge auxEdge = allEdges.get(i);
			
			//create subgraph instance
			MyGraph auxSubgraph = new MyGraph();
			
			auxSubgraph.addEdge(auxEdge);
			
			int j = i + 1;
			while(j < allEdges.size())
			{
				Set<Integer> candidateEdgeToAdd = allEdges.get(j).toSet();
				
				//get intersection
				Set<Integer> intersection = new HashSet<Integer>(auxSubgraph.nodesInvolved);
				intersection.retainAll(candidateEdgeToAdd);
				if(intersection.size() > 0)
				{
					//connect edge to the subgraph
					auxSubgraph.addEdge(allEdges.get(j));
					allEdges.remove(j); //remove edge from candidates
				}
				else
				{
					++j;
				}
			}
			
			System.out.println(auxSubgraph.toString());
			subgraphInstances.add(auxSubgraph);
			++i;
		}*/
		
		/*
		//first, info on generic nodes
		Variable[] auxVars = roughInfo.getCurrentVariables();	
		genericNodes = new GenericNode[auxVars.length];
		for(int j = 0; j < auxVars.length; ++j)
		{
			genericNodes[j].setGenericID(auxVars[j].getID());
			genericNodes[j].setIndividualNodes(auxVars[j].getNodeIDs());
		}
		
		//populate in genericNodes info about generic edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				genericNodes[node1].genericEdges.add(node2);
				genericNodes[node2].genericParents.add(node1);
			}
			else
			{
				//node2 -> node1
				genericNodes[node2].genericEdges.add(node1);
				genericNodes[node1].genericParents.add(node2);
			}
		}
		
	}*/
	
	/*public void buildGenericGraph__(Graph singleGraph)
	{
		ArrayList<myNode> originalNodes = singleGraph.getNodes();
		ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL edges involved in ALL replicas of this structure
		
		//generate edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			
			//to store candidates for mappings
			Set<Integer> parents;
			Set<Integer> children;
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				parents = genericNodeVariables[node1].getNodeIDs();
				children = genericNodeVariables[node2].getNodeIDs();
			}
			else
			{
				//node2 -> node1
				parents = genericNodeVariables[node2].getNodeIDs();
				children = genericNodeVariables[node1].getNodeIDs();
			}
			
			//for each parent, find its child and insert the edge
			for(int parent : parents)
			{
				//System.out.print(parent + ", ");
				
				//1. get original node info
				myNode parentNode = originalNodes.get(parent);
				
				//2. get the children of the parent
				Set<Integer> allChildrenOfParent = parentNode.getChildrenIDs();
				
				//3. do intersection of allChildrenOfParent with possible children
				Set<Integer> intersection = new HashSet<Integer>(children);
				intersection.retainAll(allChildrenOfParent);
				
				//4. add relevant edges
				for(int edge : intersection)
				{
					allEdges.add(new MyEdge(parent, edge));
				}
				
			}
			//System.out.println();
		}
		
		//maintain a copy of edges in case it is necessary to restore
		ArrayList<MyEdge> allEdgesOriginal = allEdges;
		System.out.println("\n\n\nall edges");
		for(int i = 0; i < allEdges.size(); ++i)
		{
			System.out.println(allEdges.get(i).toString());
		}
		
		int i = 0;
		ArrayList<MyGraph> subgraphInstances = new ArrayList<>();
		int sizeOfSubgraphs = genericNodeVariables.length;
		System.out.println("ALE subgraph should be of size " + sizeOfSubgraphs);
		while(i < allEdges.size())
		{
		*/
			/*System.out.println("\n\n\nall edges");
			for(int a = 0; a < allEdges.size(); ++a)
			{
				System.out.println(allEdges.get(a).toString());
			}*/
			/*
			MyEdge auxEdge = allEdges.get(i);
			
			//create subgraph instance
			MyGraph auxSubgraph = new MyGraph();
			
			auxSubgraph.addEdge(auxEdge);
			
			System.out.println("First edge " + auxEdge.toString());
			
			int j = i + 1;
			while(auxSubgraph.nodesInvolved.size() <= sizeOfSubgraphs && j < allEdges.size())
			{
				Set<Integer> candidateEdgeToAdd = allEdges.get(j).toSet();
				//System.out.println("Candidate " + allEdges.get(j).toString());
				
				//get intersection
				Set<Integer> intersection = new HashSet<Integer>(auxSubgraph.nodesInvolved);
				intersection.retainAll(candidateEdgeToAdd);
				if(intersection.size() > 0)
				{
					//if all nodes that should be in the subgraph are already identified, do not add more nodes, only edges with the nodes.
					if(auxSubgraph.nodesInvolved.size() == sizeOfSubgraphs)
					{
						if(intersection.size() == 2) //no new nodes
						{
							//connect edge to the subgraph
							auxSubgraph.addEdge(allEdges.get(j));
							allEdges.remove(j); //remove edge from candidates
						}
						else
						{
							break; //there is a new node => we would make a bigger subgraph, so we DO NOT ADD IT TO THE SUBGRAPH and we break
						}
					}
					else
					{
						//connect edge to the subgraph
						auxSubgraph.addEdge(allEdges.get(j));
						allEdges.remove(j); //remove edge from candidates
					}
				}
				else
				{
					++j;
				}
				//System.out.println("size of nodesInvolved " + auxSubgraph.nodesInvolved.size());
				if(j == allEdges.size() && auxSubgraph.nodesInvolved.size() < sizeOfSubgraphs)//if(j == allEdges.size() - 1 && auxSubgraph.nodesInvolved.size() < sizeOfSubgraphs)
				{
					//System.out.println("Llegamos al fin y no se ha completado el subgrafo");
					allEdges = allEdgesOriginal;
					j = 0;
				}
			}
			
			System.out.println(auxSubgraph.toString());
			subgraphInstances.add(auxSubgraph);
			//++i;
			allEdges.remove(i); //remove it from candidates
		}
		
	}*/
	
	/*public void buildGenericGraph___(Graph singleGraph)
	{
		System.out.println("***GRAPH");
		ArrayList<myNode> originalNodes = singleGraph.getNodes();
		//ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL edges involved in ALL replicas of this structure
		
		Map<String, ArrayList<MyEdge>> allEdgesMap = new HashMap<String, ArrayList<MyEdge>>();
		
		//generate edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			String edgeKey = ""; //set the key once you have the direction
			
			//to store candidates for mappings
			Set<Integer> parents;
			Set<Integer> children;
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				parents = genericNodeVariables[node1].getNodeIDs();
				children = genericNodeVariables[node2].getNodeIDs();
				edgeKey = node1 + " " + node2;
			}
			else
			{
				//node2 -> node1
				parents = genericNodeVariables[node2].getNodeIDs();
				children = genericNodeVariables[node1].getNodeIDs();
				edgeKey = node2 + " " + node1;
			}
			
			//for each parent, find its child and insert the edge
			ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL edges involved in ALL replicas of this structure
			for(int parent : parents)
			{
				//System.out.print(parent + ", ");
				
				//1. get original node info
				myNode parentNode = originalNodes.get(parent);
				
				//2. get the children of the parent
				Set<Integer> allChildrenOfParent = parentNode.getChildrenIDs();
				
				//3. do intersection of allChildrenOfParent with possible children
				Set<Integer> intersection = new HashSet<Integer>(children);
				intersection.retainAll(allChildrenOfParent);
				
				//4. add relevant edges
				for(int edge : intersection)
				{
					allEdges.add(new MyEdge(parent, edge));
				}
			}*/
			
			/*System.out.println("GENERIC " + edgeKey);
			for(int i = 0; i < allEdges.size(); ++i)
				System.out.println(allEdges.get(i).toString());*/
			//5. push into map
			/*allEdgesMap.put(edgeKey, new ArrayList<MyEdge>(allEdges));
			//System.out.println();
		}
		
		//get first edge to use to build the subgraph instances
		String firstEdgeKey = allEdgesMap.keySet().iterator().next();
		ArrayList<MyEdge> firstEdgeInstances = allEdgesMap.values().iterator().next();
		//allEdgesMap.remove(firstEdgeKey);
		int sizeOfSubgraphs = genericNodeVariables.length;
		ArrayList<MyGraph> subgraphInstances = new ArrayList<>();*/
		
		//print map
		/*for (Map.Entry<String, ArrayList<MyEdge>> entry : allEdgesMap.entrySet())
		{
			System.out.println("\nGENERIC EDGE: " + entry.getKey());
			ArrayList<MyEdge> candidateEdges = entry.getValue();
			
			for(int i = 0; i < candidateEdges.size(); ++i)
			{
				System.out.println(candidateEdges.get(i).toString());
			}
			
		}*/
		
		
		/*for(int i = 0; i < firstEdgeInstances.size(); ++i)
		{
			MyEdge auxEdge = firstEdgeInstances.get(i);
			System.out.println("first edge "+auxEdge.toString());
			
			//create subgraph instance and add the first edge
			MyGraph auxSubgraph = new MyGraph(sizeOfSubgraphs);
			if(auxSubgraph.addEdge(auxEdge, firstEdgeKey))
				System.out.println("added");
			
			
			for (Map.Entry<String, ArrayList<MyEdge>> entry : allEdgesMap.entrySet()) 
			{
			    String edgeKey = entry.getKey();
			    
			    if(edgeKey != firstEdgeKey)
			    {
			    	ArrayList<MyEdge> candidateEdges = entry.getValue();
				    
				    boolean pendingToAdd = true;
				    int j = 0;
				    while(pendingToAdd == true && j < candidateEdges.size())
				    {
				    	//System.out.println("check with "+ candidateEdges.get(j).toString());
				    	Set<Integer> candidateEdgeToAdd = candidateEdges.get(j).toSet();
						//System.out.println("Candidate " + allEdges.get(j).toString());
						
						//get intersection
						Set<Integer> intersection = new HashSet<Integer>(auxSubgraph.nodesInvolved);
						intersection.retainAll(candidateEdgeToAdd);
						if(intersection.size() > 0)
						{
							if(intersection.size() == 2)
							{
								//repeated edge, skip it
								++j;
							}
							else
							{
								//try to connect edge to the subgraph
								if(auxSubgraph.addEdge(candidateEdges.get(j), edgeKey))
								{
									//++j;
									//candidateEdges.remove(j); //actually, DO NOT remove edge from candidates, as it might be part of overlapping graphs
									pendingToAdd = false;
								}
								else
								{
									++j; //try with the next one
								}
							}
							
						}
						else
						{
							++j; //try with the next one
						}
							
				    }
			    }
			    
			}
			
			//add subgraph to list
			System.out.println(auxSubgraph.toString());
			subgraphInstances.add(auxSubgraph);
			
		}
		
		
	}*/
	
	//ALE working method that looks rough
	public void xbuildGenericGraph(Graph singleGraph)
	{
		System.out.println("***GRAPH");
		ArrayList<myNode> originalNodes = singleGraph.getNodes();
		//ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL edges involved in ALL replicas of this structure
		
		Map<String, ArrayList<MyEdge>> allEdgesMap = new HashMap<String, ArrayList<MyEdge>>();
		
		//generate edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			String edgeKey = ""; //set the key once you have the direction
			
			//to store candidates for mappings
			Set<Integer> parents;
			Set<Integer> children;
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				parents = genericNodeVariables[node1].getNodeIDs();
				children = genericNodeVariables[node2].getNodeIDs();
				edgeKey = node1 + " " + node2;
			}
			else
			{
				//node2 -> node1
				parents = genericNodeVariables[node2].getNodeIDs();
				children = genericNodeVariables[node1].getNodeIDs();
				edgeKey = node2 + " " + node1;
			}
			
			//for each parent, find its child and insert the edge
			ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL edges involved in ALL replicas of this structure
			for(int parent : parents)
			{
				//System.out.print(parent + ", ");
				
				//1. get original node info
				myNode parentNode = originalNodes.get(parent);
				
				//2. get the children of the parent
				Set<Integer> allChildrenOfParent = parentNode.getChildrenIDs();
				
				//3. do intersection of allChildrenOfParent with possible children
				Set<Integer> intersection = new HashSet<Integer>(children);
				intersection.retainAll(allChildrenOfParent);
				
				//4. add relevant edges
				for(int edge : intersection)
				{
					allEdges.add(new MyEdge(parent, edge));
				}
			}
			
			/*System.out.println("GENERIC " + edgeKey);
			for(int i = 0; i < allEdges.size(); ++i)
				System.out.println(allEdges.get(i).toString());*/
			//5. push into map
			allEdgesMap.put(edgeKey, new ArrayList<MyEdge>(allEdges));
			//System.out.println();
		}
		
		//get first edge to use to build the subgraph instances
		String firstEdgeKey = allEdgesMap.keySet().iterator().next();
		ArrayList<MyEdge> firstEdgeInstances = allEdgesMap.values().iterator().next();
		//allEdgesMap.remove(firstEdgeKey);
		int sizeOfSubgraphs = genericNodeVariables.length;
		ArrayList<MyGraph> subgraphInstances = new ArrayList<>();
		
		//print map
		for (Map.Entry<String, ArrayList<MyEdge>> entry : allEdgesMap.entrySet())
		{
			System.out.println("\nGENERIC EDGE: " + entry.getKey());
			ArrayList<MyEdge> candidateEdges = entry.getValue();
			
			for(int i = 0; i < candidateEdges.size(); ++i)
			{
				System.out.println(candidateEdges.get(i).toString());
			}
			
		}
		
		ArrayList<MyGraph> listOfSubgraphs = new ArrayList<MyGraph>();
		for(int i = 0; i < firstEdgeInstances.size(); ++i)
		{
			MyEdge auxEdge = firstEdgeInstances.get(i);
			System.out.println("first edge "+auxEdge.toString());
			
			//create subgraph instance and add the first edge
			MyGraph currentSubgraph = new MyGraph(sizeOfSubgraphs);
			if(currentSubgraph.addEdge(auxEdge, firstEdgeKey))
			{
				listOfSubgraphs.add(currentSubgraph);
				System.out.println("added");
			}
		}
		
		
		for (Map.Entry<String, ArrayList<MyEdge>> entry : allEdgesMap.entrySet()) 
		{
			//at this point, all subgraphs have the same amount of edges
			//we will try to extend every subgraph by one edge
			
		    String edgeKey = entry.getKey();
		    
		    if(edgeKey != firstEdgeKey) //skip the first genericEdge as we already created the subgraphs using that
		    {
		    	System.out.println("Generic extension: " + edgeKey);
		    	ArrayList<MyGraph> extendedSubgraphs = new ArrayList<MyGraph>(); //reset this list
		    	ArrayList<MyEdge> candidateEdges = entry.getValue();
		    	for(int i = 0; i < listOfSubgraphs.size(); ++i)
				{
		    		//for each graph, we check all candidate edges
				    boolean pendingToAdd = true;
				    //int j = 0;
				    //while(pendingToAdd == true && j < candidateEdges.size())
				    for(int j = 0; j < candidateEdges.size(); ++j)
				    {
				    	System.out.println("Pre-extended subgraph: " + listOfSubgraphs.get(i).getNodesInvolved());
				    	//MyGraph auxSubgraph = listOfSubgraphs.get(i); //we use the pre-extended version of the subgraph to get variants
				    	//MyGraph auxSubgraph = new MyGraph(sizeOfSubgraphs, listOfSubgraphs.get(i).getGraph(), listOfSubgraphs.get(i).getNodesInvolved(), listOfSubgraphs.get(i).getGenericToActualIDs()); //we use the pre-extended version of the subgraph to get variants
				    	MyGraph auxSubgraph = new MyGraph(listOfSubgraphs.get(i));
				    	
				    	System.out.println("check sg" + auxSubgraph.getNodesInvolved() + " with "+ candidateEdges.get(j).toString());
				    	Set<Integer> candidateEdgeToAdd = candidateEdges.get(j).toSet();
						//System.out.println("Candidate " + allEdges.get(j).toString());
						
						//get intersection
						Set<Integer> intersection = new HashSet<Integer>(auxSubgraph.getNodesInvolved());
						intersection.retainAll(candidateEdgeToAdd);
						if(intersection.size() > 0)
						{
							if(intersection.size() == 2)
							{
								//repeated edge, skip it
							}
							else
							{
								//try to connect edge to the subgraph
								if(auxSubgraph.addEdge(candidateEdges.get(j), edgeKey))
								{
									//candidateEdges.remove(j); //actually, DO NOT remove edge from candidates, as it might be part of overlapping graphs
									pendingToAdd = false;
									//add it to our pending list
									//MyGraph extended = new MyGraph(sizeOfSubgraphs, auxSubgraph.getGraph(), auxSubgraph.getNodesInvolved(), auxSubgraph.getGenericToActualIDs());
									MyGraph extended = new MyGraph(auxSubgraph);
									extendedSubgraphs.add(extended);
									System.out.println("Added to extended subgraphs " + extended.nodesInvolved);
									
								}
								else
								{
									//try with the next one
								}
							}
							
						}
						else
						{
							//try with the next one
						}
							
				    }
				}
		    	
		    	//once we are done extending subgraphs by one edge, replace the pre-extended sg list with the extended sg list
		    	listOfSubgraphs = extendedSubgraphs;
		    }
		    
		}
		
		//print instances
		for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			System.out.println(listOfSubgraphs.get(i).toString());
		}
		
		printGenericNodeVariables();
	
	}
	
	Map<String, ArrayList<MyEdge>> allEdgesMap = new HashMap<String, ArrayList<MyEdge>>();
	public void xxbuildGenericGraph(Graph singleGraph)
	{
		//we know that this is a fictional subgraph. It provides generic IDs for each of its vertices. Hence, the edges are also in terms of those generic IDs
		//this method uses the generic edges and the mappings of each generic ID to all of the real node IDs to now create a structure that maps generic edges to the edges using real node IDs
		System.out.println("***GRAPH");
		ArrayList<myNode> originalNodes = singleGraph.getNodes();
		
		//Map<String, ArrayList<MyEdge>> allEdgesMap = new HashMap<String, ArrayList<MyEdge>>();
		
		//generate edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			String edgeKey = ""; //set the key once you have the direction
			
			//to store candidates for mappings
			Set<Integer> parents;
			Set<Integer> children;
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				parents = genericNodeVariables[node1].getNodeIDs();
				children = genericNodeVariables[node2].getNodeIDs();
				edgeKey = node1 + " " + node2;
			}
			else
			{
				//node2 -> node1
				parents = genericNodeVariables[node2].getNodeIDs();
				children = genericNodeVariables[node1].getNodeIDs();
				edgeKey = node2 + " " + node1;
			}
			
			//for each parent, find its child and insert the edge
			ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL real edges involved for this generic edge
			for(int parent : parents)
			{
				//System.out.print(parent + ", ");
				
				//1. get original node info
				myNode parentNode = originalNodes.get(parent);
				
				//2. get the children of the parent
				Set<Integer> allChildrenOfParent = parentNode.getChildrenIDs();
				
				//3. do intersection of allChildrenOfParent with possible children
				Set<Integer> intersection = new HashSet<Integer>(children);
				intersection.retainAll(allChildrenOfParent);
				
				//4. add relevant edges
				for(int edge : intersection)
				{
					allEdges.add(new MyEdge(parent, edge));
				}
			}
			
			/*System.out.println("GENERIC " + edgeKey);
			for(int i = 0; i < allEdges.size(); ++i)
				System.out.println(allEdges.get(i).toString());*/
			//5. push into map
			allEdgesMap.put(edgeKey, new ArrayList<MyEdge>(allEdges));
			//System.out.println();
		}
		
	}
	
	ArrayList<String> genericEdgeKeys = new ArrayList<String>();
	ArrayList<ArrayList<MyEdge>> allCandidateEdges = new ArrayList<ArrayList<MyEdge>>();
	public void buildGenericGraph(Graph singleGraph)
	{
		//we know that this is a fictional subgraph. It provides generic IDs for each of its vertices. Hence, the edges are also in terms of those generic IDs
		//this method uses the generic edges and the mappings of each generic ID to all of the real node IDs to now create a structure that maps generic edges to the edges using real node IDs
		System.out.println("***GRAPH");
		ArrayList<myNode> originalNodes = singleGraph.getNodes();
		
		//Map<String, ArrayList<MyEdge>> allEdgesMap = new HashMap<String, ArrayList<MyEdge>>();
		
		//generate edges
		for (int edgeIdx = edges.nextSetBit(0); edgeIdx >= 0; edgeIdx = edges.nextSetBit(edgeIdx + 1)) 
		{
			int node1=getNodeA(edgeIdx);
			int node2=getNodeB(edgeIdx);
			String edgeKey = ""; //set the key once you have the direction
			
			//to store candidates for mappings
			Set<Integer> parents;
			Set<Integer> children;
			
			if(getDirection(edgeIdx)>=0)
			{
				//node1 -> node2
				parents = genericNodeVariables[node1].getNodeIDs();
				children = genericNodeVariables[node2].getNodeIDs();
				edgeKey = node1 + " " + node2;
			}
			else
			{
				//node2 -> node1
				parents = genericNodeVariables[node2].getNodeIDs();
				children = genericNodeVariables[node1].getNodeIDs();
				edgeKey = node2 + " " + node1;
			}
			
			//for each parent, find its child and insert the edge
			ArrayList<MyEdge> allEdges = new ArrayList<>(); //this is to store ALL real edges involved for this generic edge
			for(int parent : parents)
			{
				//System.out.print(parent + ", ");
				
				//1. get original node info
				myNode parentNode = originalNodes.get(parent);
				
				//2. get the children of the parent
				Set<Integer> allChildrenOfParent = parentNode.getChildrenIDs();
				
				//3. do intersection of allChildrenOfParent with possible children
				Set<Integer> intersection = new HashSet<Integer>(children);
				intersection.retainAll(allChildrenOfParent);
				
				//4. add relevant edges
				for(int edge : intersection)
				{
					allEdges.add(new MyEdge(parent, edge));
				}
			}
			
			/*System.out.println("GENERIC " + edgeKey);
			for(int i = 0; i < allEdges.size(); ++i)
				System.out.println(allEdges.get(i).toString());*/
			//5. push into lists
			allCandidateEdges.add(new ArrayList<MyEdge>(allEdges));
			genericEdgeKeys.add(edgeKey);
			//System.out.println();
		}
		
	}
	
	ArrayList<MyGraph> listOfSubgraphs = new ArrayList<MyGraph>();
	//ALE
	public void generateSubgraphInstances()
	{
		//get first edge to use to build the subgraph instances
		String firstEdgeKey = genericEdgeKeys.get(0);
		ArrayList<MyEdge> firstEdgeInstances = allCandidateEdges.get(0);
		int sizeOfSubgraphs = genericNodeVariables.length;
		
		//print map
		for (int i = 0; i < genericEdgeKeys.size(); ++i)
		{
			System.out.println("\nGENERIC EDGE: " + genericEdgeKeys.get(i));
			ArrayList<MyEdge> candidateEdges = allCandidateEdges.get(i);
			
			for(int j = 0; j < candidateEdges.size(); ++j)
			{
				System.out.println(candidateEdges.get(j).toString());
			}
			
		}
		
		
		//ArrayList<MyGraph> listOfSubgraphs = new ArrayList<MyGraph>();
		//use first edge to populate listOfSubgraphs initially
		for(int i = 0; i < firstEdgeInstances.size(); ++i)
		{
			MyEdge auxEdge = firstEdgeInstances.get(i);
			System.out.println("first edge "+auxEdge.toString());
			
			//create subgraph instance and add the first edge
			MyGraph currentSubgraph = new MyGraph(sizeOfSubgraphs);
			if(currentSubgraph.addEdge(auxEdge, firstEdgeKey))
			{
				listOfSubgraphs.add(currentSubgraph);
				System.out.println("added");
			}
		}
		
		
		for (int a = 1; a < genericEdgeKeys.size(); ++a) 
		{
			//at this point, all subgraphs in listOfSubgraph have the same amount of edges
			//we will try to extend every subgraph by one edge
			
			//we start at a = 1 to skip the first genericEdge as we already created the subgraphs using that
			
			String edgeKey = genericEdgeKeys.get(a);
	    
	    	System.out.println("Generic extension: " + edgeKey);
	    	ArrayList<MyGraph> extendedSubgraphs = new ArrayList<MyGraph>(); //reset this list
	    	ArrayList<MyEdge> candidateEdges = allCandidateEdges.get(a);
	    	for(int i = 0; i < listOfSubgraphs.size(); ++i)
			{
	    		//for each graph, we check all candidate edges
			    boolean pendingToAdd = true;
			    //int j = 0;
			    //while(pendingToAdd == true && j < candidateEdges.size())
			    for(int j = 0; j < candidateEdges.size(); ++j)
			    {
			    	System.out.println("Pre-extended subgraph: " + listOfSubgraphs.get(i).getNodesInvolved());
			    	//MyGraph auxSubgraph = listOfSubgraphs.get(i); //we use the pre-extended version of the subgraph to get variants
			    	//MyGraph auxSubgraph = new MyGraph(sizeOfSubgraphs, listOfSubgraphs.get(i).getGraph(), listOfSubgraphs.get(i).getNodesInvolved(), listOfSubgraphs.get(i).getGenericToActualIDs()); //we use the pre-extended version of the subgraph to get variants
			    	MyGraph auxSubgraph = new MyGraph(listOfSubgraphs.get(i));
			    	
			    	System.out.println("check sg" + auxSubgraph.getNodesInvolved() + " with "+ candidateEdges.get(j).toString());
			    	Set<Integer> candidateEdgeToAdd = candidateEdges.get(j).toSet();
					//System.out.println("Candidate " + allEdges.get(j).toString());
					
					//get intersection
					Set<Integer> intersection = new HashSet<Integer>(auxSubgraph.getNodesInvolved());
					intersection.retainAll(candidateEdgeToAdd);
					if(intersection.size() > 0)
					{
						if(intersection.size() == 2)
						{
							//repeated edge, skip it
						}
						else
						{
							//try to connect edge to the subgraph
							if(auxSubgraph.addEdge(candidateEdges.get(j), edgeKey))
							{
								//candidateEdges.remove(j); //actually, DO NOT remove edge from candidates, as it might be part of overlapping graphs
								pendingToAdd = false;
								//add it to our pending list
								//MyGraph extended = new MyGraph(sizeOfSubgraphs, auxSubgraph.getGraph(), auxSubgraph.getNodesInvolved(), auxSubgraph.getGenericToActualIDs());
								MyGraph extended = new MyGraph(auxSubgraph);
								extendedSubgraphs.add(extended);
								System.out.println("Added to extended subgraphs " + extended.nodesInvolved);
								
							}
							else
							{
								//try with the next one
							}
						}
						
					}
					else
					{
						//try with the next one
					}
						
			    }
			}
	    	
	    	//once we are done extending subgraphs by one edge, replace the pre-extended sg list with the extended sg list
	    	listOfSubgraphs = extendedSubgraphs;
		    
		}
		
		//maybe it is best if we print our list once we take a look at the variants
		groupSubgraphInstanceVariants();
		//print instances
		/*for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			System.out.println(listOfSubgraphs.get(i).toString());
		}
		
		printGenericNodeVariables();*/
	}
	
	//ALE - problematic because we are not following the order of the edges as they appear in the DFSCode
	public void xgenerateSubgraphInstances()
	{
		//get first edge to use to build the subgraph instances
		String firstEdgeKey = allEdgesMap.keySet().iterator().next();
		ArrayList<MyEdge> firstEdgeInstances = allEdgesMap.values().iterator().next();
		int sizeOfSubgraphs = genericNodeVariables.length;
		
		//print map
		for (Map.Entry<String, ArrayList<MyEdge>> entry : allEdgesMap.entrySet())
		{
			System.out.println("\nGENERIC EDGE: " + entry.getKey());
			ArrayList<MyEdge> candidateEdges = entry.getValue();
			
			for(int i = 0; i < candidateEdges.size(); ++i)
			{
				System.out.println(candidateEdges.get(i).toString());
			}
			
		}
		
		//ArrayList<MyGraph> listOfSubgraphs = new ArrayList<MyGraph>();
		//use first edge to populate listOfSubgraphs initially
		for(int i = 0; i < firstEdgeInstances.size(); ++i)
		{
			MyEdge auxEdge = firstEdgeInstances.get(i);
			System.out.println("first edge "+auxEdge.toString());
			
			//create subgraph instance and add the first edge
			MyGraph currentSubgraph = new MyGraph(sizeOfSubgraphs);
			if(currentSubgraph.addEdge(auxEdge, firstEdgeKey))
			{
				listOfSubgraphs.add(currentSubgraph);
				System.out.println("added");
			}
		}
		
		
		for (Map.Entry<String, ArrayList<MyEdge>> entry : allEdgesMap.entrySet()) 
		{
			//at this point, all subgraphs in listOfSubgraph have the same amount of edges
			//we will try to extend every subgraph by one edge
			
		    String edgeKey = entry.getKey();
		    
		    if(edgeKey != firstEdgeKey) //skip the first genericEdge as we already created the subgraphs using that
		    {
		    	System.out.println("Generic extension: " + edgeKey);
		    	ArrayList<MyGraph> extendedSubgraphs = new ArrayList<MyGraph>(); //reset this list
		    	ArrayList<MyEdge> candidateEdges = entry.getValue();
		    	for(int i = 0; i < listOfSubgraphs.size(); ++i)
				{
		    		//for each graph, we check all candidate edges
				    boolean pendingToAdd = true;
				    //int j = 0;
				    //while(pendingToAdd == true && j < candidateEdges.size())
				    for(int j = 0; j < candidateEdges.size(); ++j)
				    {
				    	System.out.println("Pre-extended subgraph: " + listOfSubgraphs.get(i).getNodesInvolved());
				    	//MyGraph auxSubgraph = listOfSubgraphs.get(i); //we use the pre-extended version of the subgraph to get variants
				    	//MyGraph auxSubgraph = new MyGraph(sizeOfSubgraphs, listOfSubgraphs.get(i).getGraph(), listOfSubgraphs.get(i).getNodesInvolved(), listOfSubgraphs.get(i).getGenericToActualIDs()); //we use the pre-extended version of the subgraph to get variants
				    	MyGraph auxSubgraph = new MyGraph(listOfSubgraphs.get(i));
				    	
				    	System.out.println("check sg" + auxSubgraph.getNodesInvolved() + " with "+ candidateEdges.get(j).toString());
				    	Set<Integer> candidateEdgeToAdd = candidateEdges.get(j).toSet();
						//System.out.println("Candidate " + allEdges.get(j).toString());
						
						//get intersection
						Set<Integer> intersection = new HashSet<Integer>(auxSubgraph.getNodesInvolved());
						intersection.retainAll(candidateEdgeToAdd);
						if(intersection.size() > 0)
						{
							if(intersection.size() == 2)
							{
								//repeated edge, skip it
							}
							else
							{
								//try to connect edge to the subgraph
								if(auxSubgraph.addEdge(candidateEdges.get(j), edgeKey))
								{
									//candidateEdges.remove(j); //actually, DO NOT remove edge from candidates, as it might be part of overlapping graphs
									pendingToAdd = false;
									//add it to our pending list
									//MyGraph extended = new MyGraph(sizeOfSubgraphs, auxSubgraph.getGraph(), auxSubgraph.getNodesInvolved(), auxSubgraph.getGenericToActualIDs());
									MyGraph extended = new MyGraph(auxSubgraph);
									extendedSubgraphs.add(extended);
									System.out.println("Added to extended subgraphs " + extended.nodesInvolved);
									
								}
								else
								{
									//try with the next one
								}
							}
							
						}
						else
						{
							//try with the next one
						}
							
				    }
				}
		    	
		    	//once we are done extending subgraphs by one edge, replace the pre-extended sg list with the extended sg list
		    	listOfSubgraphs = extendedSubgraphs;
		    }
		    
		}
		
		//maybe it is best if we print our list once we take a look at the variants
		//groupSubgraphInstanceVariants();
		//print instances
		for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			System.out.println(listOfSubgraphs.get(i).toString());
		}
		
		printGenericNodeVariables();
	}
	
	//ALE first version for grouping
	public void xgroupSubgraphInstanceVariants()
	{
		//some of the subgraphs that we built and pushed into listOfSubgraphs will have variants
		//Conditions for two graphs to be variants
		//1. They share nodes
		//2. For the nodes they do share, the same mapping is followed in both
		
		for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			MyGraph aux = listOfSubgraphs.get(i);
			
			int j = i + 1;
			while(j < listOfSubgraphs.size()) //for(int j = i + 1; j < listOfSubgraphs.size(); ++j)
			{
				if(aux.getNodesInvolved().equals(listOfSubgraphs.get(j).getNodesInvolved()))
				{
					listOfSubgraphs.remove(j);
				}
				else if(aux.isVariant(listOfSubgraphs.get(j)))
				{
					aux.insertAsVariant(listOfSubgraphs.get(j));
					listOfSubgraphs.remove(j);
				}
				else
				{
					//try with the next one
					++j;
				}
			}
		}
		
		//by the end, our listOfSubgraphs should only have freqThreshold amount of elements
		//print instances
		System.out.println("***AFTER GROUPING VARIANTS***");
		for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			System.out.println(listOfSubgraphs.get(i).toString());
		}
		
		printGenericNodeVariables();
		
		
	}
	
	//ALE second version. More thorough
	public void groupSubgraphInstanceVariants()
	{
		//some of the subgraphs that we built and pushed into listOfSubgraphs will have variants
		//Conditions for two graphs to be variants
		//1. They share nodes
		//2. For the nodes they do share, the same mapping is followed in both
		
		//1st  pass: remove duplicates
		//System.out.println("BEFORE removing duplicates size is " + listOfSubgraphs.size());
		ArrayList<MyGraph> noDuplicates = new ArrayList<MyGraph>();
		for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			MyGraph aux = listOfSubgraphs.get(i);
			
			if(aux.checked == false)
			{
				//System.out.println(listOfSubgraphs.get(i).getNodesInvolved());
				aux.validMappings.add(aux.getInvertedGenericToActualIDs()); //add own mapping to the set of valid mappings for this instance
				
				for(int j = i + 1; j < listOfSubgraphs.size(); ++j)
				{
					if(aux.getNodesInvolved().equals(listOfSubgraphs.get(j).getNodesInvolved()))
					{
						aux.validMappings.add(listOfSubgraphs.get(j).getInvertedGenericToActualIDs()); //if they are the same, add the mapping to the valid mappings list
						listOfSubgraphs.get(j).setChecked(); //established this instance has been checked so it is not considered later on
						//System.out.println("   "+listOfSubgraphs.get(j).getNodesInvolved());
					}
					else
					{
						//++j; //try with the next one
					}
				}
				noDuplicates.add(new MyGraph(aux)); //checked is reset here
			}
			
		}
		//System.out.println("AFTER removing duplicates size is " + noDuplicates.size());
		listOfSubgraphs = new ArrayList<MyGraph>();
		
		//2nd pass: group variants using the noDuplicates list
		for(int i = 0; i < noDuplicates.size(); ++i)
		{
			MyGraph aux = noDuplicates.get(i);
			System.out.println("Checking variants for " + aux.nodesInvolved);
			//System.out.println("My mappings");
			//aux.printAllValidMappings();
			int count = 0;
			if(aux.checked == false)
			{
				for(int j = i + 1; j < noDuplicates.size(); ++j)
				{
					if(noDuplicates.get(j).checked == false)
					{
						//System.out.println("   vs " + noDuplicates.get(j).nodesInvolved);
						//noDuplicates.get(j).printAllValidMappings();
						if(aux.isVariant(noDuplicates.get(j)))
						{
							aux.insertAsVariant(noDuplicates.get(j));
							noDuplicates.get(j).setChecked();
							count++;
							
						}
						
					}
					
				}
				listOfSubgraphs.add(new MyGraph(aux));
			}
			System.out.println("FOund " + count + " variannts");
			
		}
		
		//by the end, our listOfSubgraphs should only have freqThreshold amount of elements
		//print instances
		/*System.out.println("***AFTER GROUPING VARIANTS***");
		for(int i = 0; i < listOfSubgraphs.size(); ++i)
		{
			System.out.println(listOfSubgraphs.get(i).toString());
		}
		
		printGenericNodeVariables();
		*/
		
		System.out.println("***Qty of instances " + listOfSubgraphs.size());
		
		
	}
	
	//ALE
	public ArrayList<MyGraph> getListOfSubgraphs()
	{
		return listOfSubgraphs;
	}
	
	//ALE
	public void printGenericNodeVariables()
	{
		for(int i = 0; i < genericNodeVariables.length; ++i)
			System.out.println(genericNodeVariables[i].toString());
	}
	
}
