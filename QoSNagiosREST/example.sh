#!/bin/bash

echo '*************************************'
echo Example of usage of the Nagios plugin
echo '*************************************'
echo Take a look inside this file!
echo Remember to set up into the script the following:
echo    1. the JAVA_HOME 
echo    2. Where the plugin is
echo '*************************************'
echo Probing, default timeout: 60 seconds...
./check_rest_api --critical 300 --debug 3 
