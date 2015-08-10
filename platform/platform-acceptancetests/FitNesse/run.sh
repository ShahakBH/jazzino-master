#!/bin/sh
java -Xms256m -Xmx512m -cp lib/fitnesse-20081201.jar fitnesse.FitNesse -p 8085 -e 0 -o $1 $2 $3 $4 $5


