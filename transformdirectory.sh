#!/bin/sh
#
# Usage: transformdirectory.sh transform origindir destdir
# Set TRANSFORM_HOME to the location of the transformenator.jar file.
#
if [ "$3" != "" ]
then
  export MY_HOME="`pwd`"
  cd $2
  mkdir $3
  for file in ./*
  do
    java -jar $TRANSFORM_HOME/transformenator.jar "$1" "$file" "$3/$file.txt"
  done
  cd $MY_HOME
else
  echo "Usage: transformdirectory.sh transform origindir destdir"
fi
