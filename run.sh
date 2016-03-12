#!/bin/bash/
cd /home/jaan/WCAGgrader/grader
for i in $(cat /warcs/ser_manifest); do
	sshpass -p '' scp jaan@deepweb.ut.ee:/mnt/$i .
	echo $i downloaded
	echo $i > cur_warc.txt
	echo -n $i ' - ' >> ../parsed_warcs.txt
	/usr/bin/time -af %E -o ../parsed_warcs.txt bin/nutchwax import cur_warc.txt
	echo $i audited
	rm $i
done
rm cur_warc.txt
