# Transformenator Architecture

The transformenation of files needs to be able to support the following scenarios:

1. Computational reorganization of a file (i.e. de-indexing); the resulting file name can be processed one of two ways:
  - No filename change, requiring a different destination directory (i.e. `dir1/file.name` produces `dir2/file.name`)
  - New file name is discovered as part of the reorganization operation (i.e. `dir1/file.name` produces `dir2/whole-new-name`)
2. Simple text substitution (i.e. 0x0d -> 0x0d0a) of a file; resulting file name can be processed one of two ways:
  - Automatic file suffix addition based on transform file suffix (i.e. `transform_rtf` of `file.name` produces `file.name.rtf`)
  - No filename change, requiring a different destination directory (i.e. `transform_rtf` of `dir1/file.name` produces `dir2/file.name`)

Invocation of a program to make these file transformenations should be this:
``` 
Transform transform_spec input output_directory [suffix]
```
The specified `output_directory` is created if it doesn't exist.
If `input` is a file, then the `transform_spec` is applied to that file.  If `input` is a directory, then `transform_spec` is applied to every file in that directory, and directories are scanned recursively for additional files to transform.  This has the side-effect of creating a filesystem directory tree `output_directory` that mirrors `input`.
The file `input` will be converted to `output_directory/input_file.txt` by default, or in the case of the transform having an `_rtf`
suffix such as `tansform_spec_rtf`, then the new file name would be `output_directory/input_file.rtf`.
Alternatively, a suffix can be supplied as an optional parameter to specify a new suffix independent of what might otherwise have been applied.
It can be specified to add no suffix at all by supplying two double quotes (i.e. `""`) as the optional suffix parameter. 

In the case where there is computational reorganization of the file done prior to output file creation, the name of the
generated file may be completely changed.
The same rules to add a file suffix apply.

Separate from, and prior to these reorganization and substitution phases is the extraction of individual files from a (virtual) disk image;
that is handled by the various `ExtractFooFiles` utility programs.  
