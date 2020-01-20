package dataStructures;

import java.util.*;

//ALE - auxiliary pair (edge) class
public class MyEdge 
{
	 int nodeA;
	 int nodeB;
	 
	 public MyEdge(int a, int b)
	 {
		 nodeA = a;
		 nodeB = b;
	 }
	 
	 public int getA()
	 {
		 return nodeA;
	 }
	 
	 public int getB()
	 {
		 return nodeB;
	 }
	 
	 public String toString()
	 {
		 return "" + nodeA + " -> " + nodeB;
	 }
	 
	 public Set<Integer> toSet()
	 {
		 Set<Integer> aux = new HashSet<Integer>();
		 aux.add(nodeA);
		 aux.add(nodeB);
		 return aux;
	 }
}
