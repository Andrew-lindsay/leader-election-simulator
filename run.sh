#!/bin/bash

if [ $# -lt 2 ]
then
  echo "Please supply graph and elect files"
  echo "./run.sh ds_graph.txt ds_elect.txt"
  exit
fi

# java files not located in this directory

# compile java code
javac Network.java Node.java

# run java code with the provided arguements
java Network $1 $2