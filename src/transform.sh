#!/bin/sh
#
# Shell script Invoker for Transform - call with no parameters for usage instructions 
#
# Set TRANSFORM_HOME to the absolute location of the transformenator.jar file.  The default
# location is the current working directory otherwise.
#
if [ "$TRANSFORM_HOME" == "" ]
then
  export TRANSFORM_HOME="."
fi

if [ "$4" != "" ]
then
	java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.Transform "$1" "$2" "$3" "$4"
else
	if [ "$3" != "" ]
	then
		java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.Transform "$1" "$2" "$3"
	else
		if [ "$2" != "" ]
		then
			java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.Transform "$1" "$2"
		else
			if [ "$1" != "" ]
			then
				java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.Transform "$1"
			else
				java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.Transform
			fi
		fi
	fi
fi
