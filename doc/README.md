# Transformenator introduction

Transformenation is something that should be possible to do with some rudimentary shell scripting. You should be able to run a binary file through sed or awk and have byte sequences change to different byte sequences.

But you can't. Maybe that's why you're here. Transformenator can help.

It turns out that this is really, really useful when faced with files from ancient word processors, for example.  They used all kinds of crazy binary annotations within a file (this is before the days of text markup, remember).  With Transformenator, it's easy to swap out those binary annotations for HTML or RTF tags that all of a sudden make those ancient files readable again, maybe even with their original formatting and highlighting intact.  Many samples come built into the Transformenator package that can make such file conversions easy.

# Usage

Invoke the transformenator Java jar file from any command line, ant script, or what have you. A set of rules (comprising a transform) are applied to the input file and written to the output file. You can use one of the included sample transforms, and you can write your own as simply as creating a text file.

For example:
```java -jar transformenator.jar transform infile out_directory```

Where:
 - `transform` is the name of a file containing the set of transformations you want to make
 - `infile` is the original file or directory to act on
 - `out_directory` is the the directory location of the resulting file(s) after making all transforminations 

See https://github.com/RetroFloppy/transformenator/wiki for details.