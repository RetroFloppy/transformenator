#!/bin/sh
#
# Usage: transformdirectory.sh transform in_directory out_directory [suffix]
# Set TRANSFORM_HOME to the location of the transformenator.jar file.
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
  echo "Usage: transformdirectory.sh transform in_directory out_directory [suffix]"
fi
