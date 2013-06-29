New for v1.4:

 * Bytes within a range can be shifted by a constant value.
   - This makes PETSCII or similar shifted-ASCII changes simple (i.e. [41..5a] = 61)

 * Recursive transform specs - the meaning of '=' and '#' have been swapped since being introduced in 1.3.  The default '=' is now to move past the substitution, which is expected to be the typical intention.  The much less commonly needed '#' now means to feed the substitution back for subsequent rules to act on.
   - This can cause loops if your specs change data to the same thing (i.e. "0d # 0d0a")

Transformenator introduction:

Transformenation is something that should be possible to do with some rudimentary shell scripting. You should be able to run a binary file through sed or awk and have byte sequences change to different byte sequences.

But you can't.

You could probably run a file through a hex dumper, change hex values, then un-transform it back to binary. But that's kind of a pain too. The problem is that sed and awk work on lines, defined as things that are delineated by what they consider line ending characters like 0x0d or 0x0a. But what if your data stream contains 0x0d and 0x0a bytes - but you don't want them to count as line endings? What if you need to remove nulls, hex zeroes, or whatever you want to call them from a binary file or data stream?

Your're stuck.

Maybe that's why you're here. Transformenator can help.

Invocation:
java -jar transformenator.jar transform infile.bin outfile.bin

See http://transformenator.sourceforge.net for details.
