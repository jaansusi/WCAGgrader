#!/bin/bash/

kill $(ps aux | grep 'bash run.sh' | head -n 1 | cut -c10-15)
kill $(ps -u jaan | grep java | cut -c1-5)
