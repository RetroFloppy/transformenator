New for this version:
=====================

 * Updated transform: Another JustWrite variant that untangles an index to a chain of sectors; earlier JW is a doubly-linked list
 
 * New transform: Amiga Prowrite, including a bit of untangling processing

 * New utility: ExtractOfficeSystem6Files (and associated transform) for extracting and transforming files from IBM Office System 6 disk images
 
 * New utility: ExtractPFSFiles for extracting files from PFS:write from Apple II disk images
 
 * Updated utility: ExtractNBIFiles now handles both low and high density NBI word processor disk images

 * Updated utility: DOSImage (renamed from UpdateDOSImage) now displays as well as updates a disk image's BIOS Parameter Block (BPB); also now removes Michelangelo virus.

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
