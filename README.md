# ModifiedGraMi

ModifiedGraMi is a tool based on GRAMI_DIRECTED_SUBGRAPHS from  [GraMi](https://github.com/ehab-abdelhamid/GraMi/) that solves a variant of the frequent subgraph mining problem.

Main differences:
- ModifiedGraMi aims to find ALL frequent subgraphs (it can run without being tied to a specific minimum frequency threshold).
- ModifiedGraMi outputs every instance of the frequent subgraphs.
- ModifiedGraMi will output only the frequent subgraphs that are of a given minimum size or larger. 
- ModifiedGraMi takes into consideration the in-degree of vertices as a vertex invariant when looking for isomorphisms. A vertex labeled B that originally has three incoming edges will not be equivalent to a vertex labeled B that has one incoming edge, even if in the extracted subgraph only one of those edges is part of it.

## Prerequisites
- Java version that supports CORBA.  Suggestion: Java JRE 1.8 (since it is the latest version that still has CORBA support).
- Ubuntu

## Installing
Download and extract the files.

Compile the tool using build script.

```bash
 build
```
Run the tool using fsm script.
Inputs:
1. input file (in .lg format)
2. minimum size of frequent subgraphs to mine
3. minimum frequency support threshold (optional).

For example, to mine frequent subgraphs that have at least 6 vertices from the graph in file mygraph.lg, we would execute the following:
 ```bash
./fsm -file mygraph.lg -minsize 6
```
If we would like to only output the subgraphs that appear at least 4 times in the graph, we would alternatively run
 ```bash
./fsm -file mygraph.lg -minsize 6 -minfreqthreshold 4
```

## Acknowledgments
- GraMi authors. Original paper: Elseidy, M., Abdelhamid, E., Skiadopoulos, S. & Kalnis, P. (2014). GRAMI: Frequent Subgraph and Pattern Mining in a Single Large Graph. PVLDB, 7(7).


## Notes
    1. Graph is directed. 
    2. Input files should be in .lg format.
    3. Input file should provide a set of vertices and a set of edges
        1. Format:
            1. For vertices: v [ID] [label]
            2. Vertices IDs should be in order and start with ID = 0
            3. For edges: e [ID of parent] [ID of child]
