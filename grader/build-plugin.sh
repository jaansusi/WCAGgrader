#!/bin/bash

#cd /home/jaan/Dropbox/Ülikool/Baka/java/nutchwax-0.13/
echo "Building nutchWAX:"
ant
#mkdir plugins
rm -dr plugins/parse-htmlraw/
mkdir plugins/parse-htmlraw/
cp -r build/plugins/parse-htmlraw/* plugins/parse-htmlraw/.
echo "Copying done!"
