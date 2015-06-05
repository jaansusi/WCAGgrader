#!/bin/bash

#cd /home/jaan/Dropbox/Ülikool/Baka/java/nutchwax-0.13/
echo "Building nutchWAX:"
ant
#rm -dr plugins/parse-htmlraw/*
cp build/plugins/parse-htmlraw/* plugins/parse-htmlraw/
echo "Copying done!"
