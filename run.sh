#!/bin/bash/
cd /home/jaan/WCAGgrader/grader
comm --nocheck-order -3 ../warcs ../parsed_warcs.txt > ../warcs_diff.txt
#For loop does not work, reads to memory so we use while
while [ $(wc -l ../warcs_diff.txt | cut -d ' ' -f 1) -gt 0 ] 
do
	i=$(head -n 1 ../warcs_diff.txt)
	echo $i >> ../parsed_warcs.txt
	scp jaan@deepweb.ut.ee:/mnt/$i .
	echo $i > $$.txt
	echo -n $i - >> ../parsed_warcs_time.txt
	if [ "$#" -ne 1 ];
		#If no argument given, dont set a core
		then /usr/bin/time -af %E -o ../parsed_warcs_time.txt bin/nutchwax import cur_warc.txt
		#If argument given, set core
	#Instead of cur_warc, give with pid
		
		else /usr/bin/time -af %E -o ../parsed_warcs_time.txt taskset -c $1 bin/nutchwax import cur_warc.txt
	fi
	rm $i
	comm --nocheck-order -3 ../warcs ../parsed_warcs.txt > ../warcs_diff.txt
done
