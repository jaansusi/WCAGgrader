#!/bin/bash/
cd /home/jaan/WCAGgrader/grader
comm --nocheck-order -3 ../warcs ../parsed_warcs.txt > ../warcs_diff.txt
for i in $(cat ../warcs_diff.txt); do
	scp jaan@deepweb.ut.ee:/mnt/$i .
	echo $i > cur_warc.txt
	echo -n $i - >> parsed_warcs_time.txt
	/usr/bin/time -af %E -o ../parsed_warcs_time.txt bin/nutchwax import cur_warc.txt
	echo $i >> ../parsed_warcs.txt
	rm $i
done
rm cur_warc.txt
