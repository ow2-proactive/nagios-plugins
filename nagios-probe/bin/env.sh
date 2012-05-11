#!/bin/bash

if [ -z "${JAVA_HOME+xxx}" ]; then 
  export JAVA_HOME=/local/home/nagios/jdk
fi

# CHANGE HERE ACCORDINGLY. 
export PROACTIVELIB=/var/log/nagios/bin/ProActiveSchedulingResourcing-3.1.2_src/dist/lib/
# export PROACTIVELIB=~/bin/ProActiveSchedulingResourcing-3.1.2_src/dist/lib/

# Preparing classpath (do not modify this). 
CPATH=$PROACTIVELIB/ProActive_Scheduler-client.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_Scheduler-core.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive.jar
CPATH=$CPATH:$PROACTIVELIB/commons-cli-1.1.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_SRM-common-client.jar
CPATH=$CPATH:$PROACTIVELIB/commons-httpclient-3.1.jar
CPATH=$CPATH:$PROACTIVELIB/log4j.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_ResourceManager.jar 
CPATH=$CPATH:$PROACTIVELIB/ejb3-persistence.jar

export CPATH
