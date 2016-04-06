#!/bin/bash/
for i in {2..12}; do
	#Launch run.sh with core number as i and disowned
	bash run.sh $i &> logs/core$i.out&
	echo $! > logs/core$i.out&
done
