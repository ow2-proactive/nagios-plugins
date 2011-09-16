PROACTIVELIB=../../Projects/ProActiveSchedulingResourcing-3.1.0_src/dist/lib
CPATH=bin:../../Projects/thirds/jargs.jar:$PROACTIVELIB/ProActive_Scheduler-client.jar:$PROACTIVELIB/ProActive.jar:$PROACTIVELIB/ProActive_SRM-common-client.jar
java -cp $CPATH  Main -d -r -j /user/mjost/home/Download/jobs/Job_2_tasks.xml -f /user/mjost/home/workspace/QoSNagiosJobSubmitter/ProActiveConfiguration.xml
