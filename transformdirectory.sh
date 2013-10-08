#!/bin/sh
#
# Shell script invoker for TransformDirectory - call with no parameters for usage instructions 
#
# Set TRANSFORM_HOME to the location of the transformenator.jar file.  The default
# location is the current working directory otherwise.
#
if [ "$TRANSFORM_HOME" == "" ]
then
  export TRANSFORM_HOME="."
fi
if [ "$3" != "" ]
then
  if [ "$4" != "" ]
  then
    java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.TransformDirectory "$1" "$2" "$3" "$4"
  else
    java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.TransformDirectory "$1" "$2" "$3"
  fi
else
  java -cp $TRANSFORM_HOME/transformenator.jar org.transformenator.TransformDirectory
fi
