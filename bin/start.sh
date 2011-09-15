#!/bin/bash
echo Setting the CLASSPATH...

#LIBPATH=/user/mjost/home/Projects/ProActiveSchedulingResourcing-3.1.0_src/dist/lib/
LIBPATH=/auto/sop-nas2a/u/sop-nas2a/vol/home_oasis/mjost/Projects/ProActiveSchedulingResourcing-3.1.0_src/dist/lib/

CLASSPATH=
for FILE in $LIBPATH/*; 
do
	CLASSPATH=`echo $FILE`:$CLASSPATH
done

echo $CLASSPATH
export CLASSPATH
echo Startiig the application...
java -classpath $LIBPATH -jar test.jar
