#!/bin/bash/
for i in {2..12}; do
	#Launch run.sh with core number as i and disowned
	nohup bash run.sh $i &> logs/core$i.out&
done
