#!/bin/sh
# Usage: transformdirectory.sh transform origindir destdir
if [ "$3" != "" ]
then
  export MY_HOME="`pwd`"
  cd $2
  mkdir $3
  for file in ./*
  do
    java -jar $MY_HOME/transformenator.jar "$1" "$file" "$3.txt"
  done
  cd $MY_HOME
else
  echo "Usage: transformdirectory.sh transform origindir destdir"
fi
