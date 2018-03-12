# Two-phase Architecture

The transformenation of files needs to be able to support the following scenarios:

1. Computational reorganization of a file (i.e. de-indexing); the resulting file name can be processed one of two ways:
  - No filename change, requiring a different destination directory (i.e. `dir1/file.name` produces `dir2/file.name`)
  - New file name is discovered as part of the reorganization operation (i.e. `dir1/file.name` produces `dir2/whole-new-name`)
2. Simple text substitution (i.e. 0x0d -> 0x0d0a) of a file; resulting file name can be processed one of two ways:
  - Automatic file suffix addition based on transform file suffix (i.e. `transform_rtf` of `file.name` produces `file.name.rtf`)
  - No filename change, requiring a different destination directory (i.e. `transform_rtf` of `dir1/file.name` produces `dir2/file.name`)

Invocation of a program to make these file transformenations should be this:
``` 
TransformFile transform_spec input_file output_directory [suffix]
```
The specified `output_directory` is created if it doesn't exist.
This transforms `input_file` to `output_directory/input_file.txt` by default, or in the case of the transform having an `_rtf`
suffix such as `tansform_spec_rtf`, then the new file name would be `output_directory/input_file.rtf`.
Alternatively, a suffix can be supplied as an optional parameter to specify a new suffix independent of what might otherwise have been applied.
It can be specified to add no suffix at all by supplying two double quotes (i.e. `""`) as the optinal suffix parameter. 

In the case where there is computational reorganization of the file done prior to output file creation, the name of the
generated file may be completely changed.
The same rules to add a file suffix apply.

Invocation of a program to make these file transformenations recursively should be this:
``` 
TransformDirectory transform_spec input_directory output_directory [suffix]
```
This will create a filesystem directory tree `output_directory` that mirrors `input_directory`, and contains all the files
from `input_directory` as transformed by `transform_spec` with all the suffix addition rules as specified earlier.

Separate from, and prior to these two phases is the extraction of individual files from a (virtual) disk image;
that is handled by the various `ExtractFooFiles` utility programs.  

