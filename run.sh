#!/bin/bash/
cd /home/jaan/WCAGgrader/grader
comm --nocheck-order -3 ../warcs ../parsed_warcs.txt > ../warcs_diff.txt
for i in $(cat ../warcs_diff.txt); do
	echo -n downloading $i...
	sshpass -p '' scp jaan@deepweb.ut.ee:/mnt/$i .
	echo downloaded
	echo $i > cur_warc.txt
	echo -n $i - > parsed_warcs_time.txt
	/usr/bin/time -af %E -o ../parsed_warcs_time.txt bin/nutchwax import cur_warc.txt
	echo $i >> ../parsed_warcs.txt
	echo $i audited
	rm $i
done
rm cur_warc.txt
