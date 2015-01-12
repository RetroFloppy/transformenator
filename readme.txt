New for this version:
=====================

 * New transforms: Canon ETW1 word processor, IBM Displaywriter, Multimate

 * New function: ExtractDisplaywriterFiles - extract EBCDIC files from Displaywriter 8" disk images
 
 * New function: ExtractSeawellFiles - extract files from Seawell DOS (early 6502-based DOS) 8" disk images 

 * New function: ExtractMemoryWriterFiles - extract files from a disk image from an unknown word processor, possibly Xerox MemoryWriter 

Transformenator introduction:
=============================

Transformenation is something that should be possible to do with some rudimentary shell scripting. You should be able to run a binary file through sed or awk and have byte sequences change to different byte sequences.

But you can't.

You could probably run a file through a hex dumper, change hex values, then un-transform it back to binary. But that's kind of a pain too. The problem is that sed and awk work on lines, defined as things that are delineated by what they consider line ending characters like 0x0d or 0x0a. But what if your data stream contains 0x0d and 0x0a bytes - but you don't want them to count as line endings? What if you need to remove nulls, hex zeroes, or whatever you want to call them from a binary file or data stream?

Your're stuck.

Maybe that's why you're here. Transformenator can help.

Invocation:
java -jar transformenator.jar transform infile outfile

See http://transformenator.sourceforge.net for details.
