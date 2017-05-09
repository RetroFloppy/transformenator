This is one of many tools that RetroFloppy.com creates and gives away free
to do the work of bringing old files into the new world.  If you have no
idea how to use these tools, or just want this work to be done for you, 
that's no problem - head over to http://retrofloppy.com and let us know. 

New for this version:
=====================
New utilities:
 - ExtractCSV - update that lets you specify field positions in a "by example" way using the LAYOUT keyword
 - ExtractEnsoniqFiles to extract sequencer files from 10-sector Ensoniq floppy disk images
 - ExtractIBM8Files to extract files on any of several densities of IBM 8" disk images
 - ExtractMagiFiles to extract files from Magi Major Leaguer 8" disk images
 - ExtractNBIFiles to extract files from NBI word processor disk images
 - ExtractPanasonicFiles to extract files from Panasonic (especially KX models) word processor disk images

New transforms: 
 - aol - Rudimentary AOL message formatter
 - panasonic - Panasonic KX word processor formatter
 - writers_choice - IBM Writer's Choice word processor formatter
 
Transformenator introduction:
=============================

Transformenation is something that should be possible to do with some rudimentary shell scripting. You should be able to run a binary file through sed or awk and have byte sequences change to different byte sequences.

But you can't.

You could probably run a file through a hex dumper, change hex values, then un-transform it back to binary. But that's kind of a pain too. The problem is that sed and awk work on lines, defined as things that are delineated by what they consider line ending characters like 0x0d or 0x0a. But what if your data stream contains 0x0d and 0x0a bytes - but you don't want them to count as line endings? What if you need to remove nulls, hex zeroes, or whatever you want to call them from a binary file or data stream?

You're stuck.

Maybe that's why you're here. Transformenator can help.

Invocation:
java -jar transformenator.jar transform_name infile outfile

See http://transformenator.sourceforge.net for details.
