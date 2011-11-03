#!/bin/bash

echo '*************************************'
echo Example of usage of the Nagios plugin
echo '*************************************'
echo Take a look inside this file!
echo Remember to set up into the script the following:
echo    1. the JAVA_HOME where java 1.7 is
echo    2. Where the plugin is, including job to be executed as the test
echo    3. Job descriptor path, xml
echo    4. ProActive configuration file
echo There are no other special parameters needed. Nagios parameters are optional, -c -w -H.
echo To see what is going on, you can also add the flag --debug
echo '*************************************'
echo Probing, default timeout: 60 seconds...
./check_job_submission --timeout 300 --debug 3 --jobname test --hostname node0 --port 8090
