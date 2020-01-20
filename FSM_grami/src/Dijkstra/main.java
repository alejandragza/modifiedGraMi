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

package Dijkstra;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.HashMap; //ALE
import java.util.Map;

import automorphism.Automorphism;

import CSP.ConstraintGraph;
import CSP.DFSSearch;

import pruning.SPpruner;

import search.Searcher;
import statistics.DistinctLabelStat;
import statistics.TimedOutSearchStats;

import utilities.CommandLineParser;
import utilities.DfscodesCache;
import utilities.Settings;
import utilities.StopWatch;

import dataStructures.DFSCode;
import dataStructures.DFScodeSerializer;
import dataStructures.Graph;
import dataStructures.HPListGraph;
import dataStructures.MyGraph;
import dataStructures.Query;
import dataStructures.StaticData; //ALE
import decomposer.Decomposer;

public class main {
	static int APPROX=0;
	static int EXACT=1;
	
	static int FSM=0;
	
	
	public static void main(String[] args) 
	{
		int maxNumOfDistinctNodes=1;
		
		//default frequency
		//TEMPORAL int freq=1000;
		//int freq = 2;
		
		//parse the command line arguments
		//CommandLineParser.parse(args);
		
		//if(utilities.Settings.frequency>-1)
			//freq = utilities.Settings.frequency;
		
		Searcher<String, String> sr=null;
		StopWatch watch = new StopWatch();	
		watch.start();
		
		//we will only store subgraphs that have >= minSizeOfGraphs number of vertices
		HashMap<String, Boolean> alreadyMined = new HashMap<String, Boolean>();
		
		//dictionary for nodeLabels
		HashMap<Integer, String> invertedNodeLabels = new HashMap<Integer, String>();
		
		//ALE - this is so we can run the tool from the command line
		String file = "";
		int minSizeOfGraphs = 3; //default
		int minFreqThreshold = -1;
		if(args.length > 0)
		{
			file = args[0];
			minSizeOfGraphs = Integer.parseInt(args[1]);
			if(args.length == 3)
				minFreqThreshold = Integer.parseInt(args[2]);
		}
		
		if(minFreqThreshold == -1 || minFreqThreshold < 2)
		{
			minFreqThreshold = 2;
		}
		
		StaticData.setMinSizeOfGraphs(minSizeOfGraphs);
		
		try
		{
			//sr = new Searcher<String, String>("sample1.lg", 1); //sr = new Searcher<String, String>("swanTest5.lg", freq, 1);
			//sr.getSingleGraph().printNodeLabelsDictionary();
			//System.out.println(sr.getSingleGraph().getListGraph().toString());
			
			//Set<Integer> x = sr.getSingleGraph().getPossibleFreqThresholds(); //this set stores the frequencies for individual edges in the graph
			//System.out.println("Max frequency of edges found: " + StaticData.getMaxFrequencyOfEdges());
			int freq = -1;
			
			
			//for(int freq = freqStart; freq > 2; --freq)
			//for(int freq = 12; freq > 1; --freq)
			do
			//for(int freq = 2; freq > 1; --freq)
			{	
				if(file.length() > 0)
					sr = new Searcher<String, String>(file, 1);
				else
					sr = new Searcher<String, String>("sample1.lg", 1);
				
				if(freq == -1)
				{
					freq = StaticData.getMaxFrequencyOfEdges(); //start on the max frequency
					
					//check that minFreqThreshold is actually lower than or equal to initial freq value. Otherwise, we would get infinite loop.
					if(minFreqThreshold > freq)
					{
						//Option 1: break
						System.out.println("This value for minThreshold does not generate any frequent subgraphs.\nFinished.");
						return;
						//Option 2: minFreqThreshold = freq; //if the minThreshold is larger than initial freq, simply execute the FSM with a valid freq and stop.
					}
				}
				
				
				
				sr.setFreqThreshold(freq); //ALE
		
	
				sr.initialize();
				sr.printInitials(); //ALE
				sr.search();
					//sr.printFSInfo(); //ALE
				//sr.initFrequentStructures(alreadyMined, minSizeOfGraphs); //we replaced this with sr.fetchSubgraphInstances() when writing to file
				sr.getSingleGraph().printNodeLabelsDictionary();
				sr.getSingleGraph().printInvertedNodeLabelsDictionary();
				//Set<Integer> x = sr.getSingleGraph().getPossibleFreqThresholds();
				
				watch.stop();
			
					
				//write output file for the following things:
				//1- time
				//2- number of resulted patterns
				//3- the list of frequent subgraphs
				FileWriter fw;
				if(invertedNodeLabels.size() == 0)
					invertedNodeLabels = sr.getSingleGraph().getInvertedNodeLabelsDictionary();
				try
				{
					String fName = "Output_mod_freq"+freq+".txt";
				
					fw = new FileWriter(fName);
					fw.write(watch.getElapsedTime()/1000.0+"\n");
					fw.write(sr.result.size()+"\n");
				
					//write the frequent subgraphs
					int sgNumber = 0;
					for (int i = 0; i < sr.result.size(); i++) 
					{	
						String out = DFScodeSerializer.serialize(sr.result.get(i));
						if(sr.result.get(i).getNodeCount() >= minSizeOfGraphs && !alreadyMined.containsKey(out))//if(alreadyMined.containsKey(out) &&  alreadyMined.get(out) == false)
						{
							alreadyMined.put(out, true);
							
							//write generic structure
							out = DFScodeSerializer.serialize(sr.result.get(i), invertedNodeLabels);
							fw.write(i+":\n");//fw.write(sgNumber+":\n");//fw.write(i+":\n");
							fw.write("Size: " + sr.result.get(i).getNodeCount() + "\n");
							fw.write(out);
							
							//write instances
							ArrayList<MyGraph> subgraphInstances = sr.fetchSubgraphInstances(i);
							out = "Number of instances: " + subgraphInstances.size() + "\nInstances:\n";
							int n = 0;
							for(MyGraph aux : subgraphInstances)
							{
								out += "#" + n + ": ";
								out += aux.toString() +"\n";
								n++;
							}
							fw.write(out);
							
							sgNumber++;
						}
					}
					fw.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				//update freq
				freq--;
			
			}while(freq >= minFreqThreshold);	
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//ORIGINAL
		//try
		//{
			/*TEMPORAL if(Settings.fileName==null)
			{
				System.out.println("You have to specify a filename");
				System.exit(1);
			}
			else*/
			/*{
				//TEMPORAL sr = new Searcher<String, String>(Settings.datasetsFolder+Settings.fileName, freq, 1);
				sr = new Searcher<String, String>("swanTest5.lg", freq, 1);
				sr.setFreqThreshold(freq); //ALE
			}
		
			sr.initialize();
			sr.printInitials(); //ALE
			sr.search();
			//sr.printFSInfo(); //ALE
			sr.initFrequentStructures();
			sr.getSingleGraph().printNodeLabelsDictionary();
			Set<Integer> x = sr.getSingleGraph().getPossibleFreqThresholds();
			
			watch.stop();
		
				
			//write output file for the following things:
			//1- time
			//2- number of resulted patterns
			//3- the list of frequent subgraphs
			FileWriter fw;
			try
			{
				System.out.println("ALE VAMOS A ESCRIBIR AL ARCHIVO");
				String fName = "Output_mod_freq"+freq+".txt";
			
				fw = new FileWriter(fName);
				fw.write(watch.getElapsedTime()/1000.0+"\n");
				fw.write(sr.result.size()+"\n");
			
				//write the frequent subgraphs
				for (int i = 0; i < sr.result.size(); i++) 
				{		
					String out=DFScodeSerializer.serialize(sr.result.get(i));
				
					fw.write(i+":\n");
					fw.write(out);
				}
				fw.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		System.out.println("Amount of repeated structures: " + alreadyMined.size());
		System.out.println("File: " + file);
		System.out.println("minSize: " + minSizeOfGraphs);
		System.out.println("minFreqThreshold: " + minFreqThreshold);
	}
}
