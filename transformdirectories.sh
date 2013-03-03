#!/bin/sh
# Usage: transformdirectories.sh transform origindir destdir
if [ "$2" != "" ]
then
export MY_HOME="`pwd`"
cd $2
for file in ./*
do
  java -jar $MY_HOME/transformenator.jar -t $MY_HOME/$1 $file > $3/$file.html
done
else
  echo "Please supply a directory name."
fi
cd $MY_HOME
