#!/bin/bash

#cd /home/jaan/Dropbox/Ülikool/Baka/java/nutchwax-0.13/
echo "Building nutchWAX:"
ant
mkdir plugins
rm -dr plugins/*
cp -r build/plugins/* plugins/
echo "Copying done!"