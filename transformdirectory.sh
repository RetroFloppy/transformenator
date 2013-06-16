#!/bin/sh
#
# Usage: transformdirectory.sh transform origindir destdir [suffix]
# Set TRANSFORM_HOME to the location of the transformenator.jar file.
#
if [ "$3" != "" ]
then
  export MY_HOME="`pwd`"
  cd $2
  mkdir $3
  for file in ./*
  do
    if [ "$1" = "valdocs" ]
    then
      java -jar $TRANSFORM_HOME/transformenator.jar "$1" "$file" "$3"
    else
      if [ "$4" != "" ]
      then
        java -jar $TRANSFORM_HOME/transformenator.jar "$1" "$file" "$3/$file.$4"
      else
        java -jar $TRANSFORM_HOME/transformenator.jar "$1" "$file" "$3/$file.txt"
      fi
    fi
  done
  cd $MY_HOME
else
  echo "Usage: transformdirectory.sh transform origindir destdir [suffix]"
fi
