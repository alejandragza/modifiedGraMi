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

import java.util.HashMap;
import java.util.TreeSet; //ALE
import java.util.Set; //ALE
import java.util.Comparator; //ALE

//ALE
class MyComparator implements Comparator<Integer> 
{ 
	//comparator to sort frequencies set in descending order
	@Override
    public int compare(Integer a, Integer b) 
    { 
        return b - a; 
    }
} 

public class StaticData {

	public static HashMap<String, HashMap<Integer, Integer>[]> hashedEdges;
	public static HashMap<String, HashMap<Integer, Integer>[]> originalHashedEdges; //ALE
	public static int maxFrequencyOfEdges = -1; //ALE
	
	public static int minSizeOfGraphs = 0;
	
	public static int getHashedEdgesFreq(String sig)
	{
		HashMap<Integer, Integer>[] hm = hashedEdges.get(sig);
		if(hm==null)
			return 0;
		int freq = hm[0].size();
		if(freq>hm[1].size())
			return hm[1].size();
		return freq;
	}
	public static int counter = 0;
	
	//ALE
	public static Set<Integer> getAllEdgesFrequencies()
	{
		Set<Integer> frequencies = new TreeSet<Integer>();
		//this method returns all frequencies of all of the edges IN DESCENDING ORDER.
		for(String edgeID : hashedEdges.keySet()) 
		{
			frequencies.add(getHashedEdgesFreq(edgeID));
			
			if(maxFrequencyOfEdges < getHashedEdgesFreq(edgeID))
				maxFrequencyOfEdges = getHashedEdgesFreq(edgeID); 
		}
		
		return frequencies;
		
	}
	
	//ALE
	public static void resetHashedEdges()
	{
		//this will turn the pruned hashedEdges back into the non pruned one, useful when preparing FSM for a new freqThreshold
		hashedEdges.clear();
		hashedEdges.putAll(originalHashedEdges);
	}
	
	//ALE
	public static void setOriginalHashedEdges()
	{
		originalHashedEdges = new HashMap<String, HashMap<Integer, Integer>[]>();
		originalHashedEdges.putAll(hashedEdges);
	}
	
	//ALE
	public static int getMaxFrequencyOfEdges()
	{
		//this method finds the maximum frequency (support) for individual edges in the graph (AKA once we find this number, we know that it is no use to do FSM with a freqThreshold above this number, as no frequent edges will be found)
		for(String edgeID : hashedEdges.keySet()) 
		{	
			//System.out.println("Edge " + edgeID + " appearances: " + getHashedEdgesFreq(edgeID));
			if(maxFrequencyOfEdges < getHashedEdgesFreq(edgeID))
				maxFrequencyOfEdges = getHashedEdgesFreq(edgeID); 
		}
		
		return maxFrequencyOfEdges;
		
	}
	
	//ALE
	public static void setMinSizeOfGraphs(int x)
	{
		minSizeOfGraphs = x;
	}
}
