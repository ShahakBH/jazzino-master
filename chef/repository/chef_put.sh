#!/bin/bash

case $1 in 
	nodes)
		for file in $( find ./nodes -type f -name '*.json') 
		do 
			knife node from file $file
		done ;;
	roles)
		for file in $( find ./roles -type f -name '*.json')
		do
			knife role from file $file
		done ;;
	cookbooks)
		for file in $( ls -p cookbooks | grep / | awk -F '/' '{print $1}' ) 
		do
			knife cookbook upload $file
			
		done ;;
	*)
		print "Error not a valid switch"
		return 1;
esac




