#!/bin/bash

cd grader
echo "Building nutchWAX:"

ant
#mkdir plugins
rm -dr plugins/parse-htmlraw/
mkdir plugins/parse-htmlraw/
cp -r build/plugins/parse-htmlraw/* plugins/parse-htmlraw/.
echo "Copying done!"
