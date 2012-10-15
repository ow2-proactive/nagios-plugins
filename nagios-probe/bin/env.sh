#!/bin/bash

CURRDIR=`dirname $0`

if [ -z "${JAVA_HOME+xxx}" ]; then 
  export JAVA_HOME=/local/home/nagios/jdk
fi

# DO NOT MODIFY FROM HERE.

# Preparing classpath. 
export PROACTIVELIB=$CURRDIR/../lib/scheduling/

CPATH=$PROACTIVELIB/ProActive_Scheduler-client.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_Scheduler-core.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive.jar
CPATH=$CPATH:$PROACTIVELIB/commons-cli-1.1.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_SRM-common-client.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_SRM-common.jar
CPATH=$CPATH:$PROACTIVELIB/commons-httpclient-3.1.jar
CPATH=$CPATH:$PROACTIVELIB/log4j.jar
CPATH=$CPATH:$PROACTIVELIB/ProActive_ResourceManager.jar 
CPATH=$CPATH:$PROACTIVELIB/ejb3-persistence.jar

export CPATH

