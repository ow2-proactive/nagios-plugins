#!/bin/bash

echo '*************************************'
echo Example of usage of the Nagios plugin
echo '*************************************'
echo Take a look inside this file!
echo Remember to set up into the script the following:
echo There are no other special parameters needed. Nagios parameters are optional, -c -w -H.
echo To see what is going on, you can also add the flag --debug
echo '*************************************'
echo Probing, default timeout: 60 seconds...

./check_node_obtaining --timeout=60 --debug=3
