/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2018 by David Schmidt
 * 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your 
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.transformenator;

import java.io.File;

import org.transformenator.internal.CSVInterpreter;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.Version;

public class Transform
{

	public static void main(String[] args)
	{
		boolean isCSV = false;
		/*
		System.err.println("DEBUG: args.length: " + args.length);
		for (int q = 0; q < args.length; q++)
		{
			System.err.println("DEBUG: args[" + q + "]: " + args[q]);
		}
		*/
		FileInterpreter fileTransform = null;
		CSVInterpreter csvTransform = null;

		if (args.length > 0)
		{
			fileTransform = new FileInterpreter(args[0]);
			csvTransform = new CSVInterpreter(args[0]);
			if (csvTransform.isOK)
				isCSV = true;
		}

		if (args.length == 0)
			help();
		else if ((args.length == 1) && args[0].equalsIgnoreCase("describe"))
		{
			help(true);
		}
		else if ((args.length == 1) && args[0].equalsIgnoreCase("help"))
		{
			FileInterpreter.listExamples();
		}
		else if ((args.length == 1) && (args[0].equalsIgnoreCase("help-csv") || args[0].equalsIgnoreCase("help_csv")))
		{
			CSVInterpreter.listExamples();
		}
		else if (args.length == 1)
		{
			// Special case: describe the transform if they just give its name
			if (isCSV)
				csvTransform.emitStatus();
			else
				fileTransform.emitStatus();
		}
		else if (args.length == 2)
		{
			// Special case: fix_filenames
			if (args[0].equals("fix_filenames"))
			{
				tranformDirectory(null, args[0], args[1], null, "", true);
			}
			else
				help();
		}
		else if ((args.length == 3) || (args.length == 4))
		{
			/*
			 * Make the output directory
			 */
			File baseDirFile = new File(args[2]);
			if (!baseDirFile.isAbsolute())
			{
				if (!args[2].equals("."))
					baseDirFile = new File("." + File.separator + args[2]);
			}
			baseDirFile.mkdirs();

			String suffix_guess = ".txt";
			if (args[0].toLowerCase().endsWith("_rtf"))
				suffix_guess = ".rtf";
			else if (args[0].toLowerCase().endsWith("_html"))
				suffix_guess = ".html";
			if ((args.length == 2) && !isCSV)
			{
				fileTransform.emitStatus();
				tranformDirectory(null, args[0], args[1]);
			}
			else
			{
				File whatisit = new File(args[1]);
				if (whatisit.isDirectory())
				{
					// Directory-style processing
					// System.err.println("DEBUG: starting directory processing.");
					if (args.length == 3)
					{
						if (isCSV)
							csvDirectory(csvTransform, args[0], args[1], args[2], ".csv", false);
						else
							tranformDirectory(fileTransform, args[0], args[1], args[2], suffix_guess, false);
					}
					else if (args.length == 4)
					{
						if (args[3].startsWith("."))
							suffix_guess = args[3];
						else
							suffix_guess = "."+args[3];
						if (isCSV)
							csvDirectory(csvTransform, args[0], args[1], args[2], suffix_guess, false);
						else
							tranformDirectory(fileTransform, args[0], args[1], args[2], suffix_guess, false);
					}
					else
						help();
				}
				else
				{
					// File-style processing
					// System.err.println("DEBUG: starting single-file processing.");
					if (args.length == 4)
					{
						if (args[3].startsWith("."))
							suffix_guess = args[3];
						else
							suffix_guess = "."+args[3];
						if (fileTransform != null)
						{
							fileTransform.process(args[1], args[2], args[3]);
						}
					}
					else if (args.length == 3)
					{
						if (fileTransform != null)
						{
							fileTransform.process(args[1], args[2], suffix_guess);
						}
					}
				}
			}
		}
		else
			help();
	}

	public static void tranformDirectory(FileInterpreter transform, String transform_name, String in_directory)
	{
		if (transform_name.equals("fix_filenames"))
		{
			tranformDirectory(transform, transform_name, in_directory, in_directory, "", true);
		}
		else
			help();
	}

	public static void tranformDirectory(FileInterpreter transform, String transform_name, String in_directory, String out_directory, String file_suffix, boolean only_fix_filenames)
	{
		/*
		 * Get the files in the in_directory For each file, check if it's a file or a directory. - If a file: Transform it - If a directory: recursively call tranformDirectory
		 */
		// System.err.println("DEBUG: in_directory: [" + in_directory + "]");
		File inDirFile = new File(in_directory);
		if (inDirFile.exists())
		{
			if (!only_fix_filenames)
			{
				File outDirFile = new File(out_directory);
				outDirFile.mkdirs();
				// System.out.println("DEBUG: mkdirs:  "+out_directory);
			}
			try
			{
				File[] files = inDirFile.listFiles();
				if (files != null)
				{
					for (int i = 0; i < files.length; i++)
					{
						// System.err.println("DEBUG: files["+i+"]: "+files[i]);
						if (files[i].isDirectory())
						{
							if (only_fix_filenames)
							{
								File file = fixFilename(files[i]);
								tranformDirectory(transform, transform_name, in_directory + java.io.File.separator + file.getName(), out_directory + java.io.File.separator + file.getName(), file_suffix, only_fix_filenames);
							}
							else
								tranformDirectory(transform, transform_name, in_directory + java.io.File.separator + files[i].getName(), out_directory + java.io.File.separator + files[i].getName(), file_suffix, only_fix_filenames);
						}
						else if (!files[i].isHidden())
						{
							if (only_fix_filenames)
							{
								// System.err.println("DEBUG: Fixing filename "+files[i]);
								fixFilename(files[i]);
							}
							else
							{
								System.out.println("Transforming file: " + files[i] + " to directory: " + out_directory);
								transform.process(files[i].toString(), out_directory, file_suffix);
							}
						}
					}
				}
			}
			catch (Throwable t1)
			{
				t1.printStackTrace();
			}
		}
		else
		{
			System.err.println("Error: Specified directory does not exist.");
		}
	}

	public static void csvDirectory(CSVInterpreter transform, String transform_name, String in_directory, String out_directory, String file_suffix, boolean only_fix_filenames)
	{
		/*
		 * Get the files in the in_directory For each file, check if it's a file or a directory. - If a file: Transform it - If a directory: recursively call tranformDirectory
		 */
		// System.err.println("DEBUG: in_directory: [" + in_directory + "]");
		File inDirFile = new File(in_directory);
		if (inDirFile.exists())
		{
			if (!only_fix_filenames)
			{
				File outDirFile = new File(out_directory);
				outDirFile.mkdirs();
				// System.out.println("mkdirs:  "+out_directory);
			}
			try
			{
				File[] files = inDirFile.listFiles();
				if (files != null)
				{
					for (int i = 0; i < files.length; i++)
					{
						// System.err.println("DEBUG: files["+i+"]: "+files[i]);
						if (files[i].isDirectory())
						{
							if (only_fix_filenames)
							{
								File file = fixFilename(files[i]);
								csvDirectory(transform, transform_name, in_directory + java.io.File.separator + file.getName(), out_directory + java.io.File.separator + file.getName(), file_suffix, only_fix_filenames);
							}
							else
								csvDirectory(transform, transform_name, in_directory + java.io.File.separator + files[i].getName(), out_directory + java.io.File.separator + files[i].getName(), file_suffix, only_fix_filenames);
						}
						else if (!files[i].isHidden())
						{
							if (only_fix_filenames)
							{
								fixFilename(files[i]);
							}
							else
							{
								System.out.println("Transforming file: " + files[i] + " to directory: " + out_directory);
								transform.createOutput(files[i].toString(), out_directory, file_suffix);
							}
						}
					}
				}
			}
			catch (Throwable t1)
			{
				t1.printStackTrace();
			}
		}
		else
		{
			System.err.println("Error: Specified directory does not exist.");
		}
	}

	public static File fixFilename(File infile)
	{
		String name = infile.getName(), newName = "";
		// System.err.println("DEBUG: Checking file: [" + name + "]");
		boolean needsRenamed = false;
		for (int j = 0; j < name.length(); j++)
		{
			char c = name.charAt(j);
			// System.err.println("Character: "+c+" value: "+(int)c);
			switch (c)
			{
			case 8212: // emdash through Dave
				c = '-';
				needsRenamed = true;
				newName += c;
				break;
			case 61474: // backslash through Dave
				c = '-';
				needsRenamed = true;
				newName += c;
				break;
			case 61481: // trailing period through Dave
				c = '.';
				needsRenamed = true;
				newName += c;
				break;
			case 13: // Newline
				c = '_';
				needsRenamed = true;
				newName += c;
				break;
			case 32: // Space
				if ((j == 0) || (j == (name.length() - 1))) // Leading or final space?  Baaaaad.
				{
					c = '_';
					needsRenamed = true;
				}
				newName += c;
				break;
			case 42: // Asterisk
				c = '_';
				needsRenamed = true;
				newName += c;
				break;
			case 58: // Backslash
				c = '-';
				needsRenamed = true;
				newName += c;
				break;
			case 63: // Some Crazy Mac crap
				c = '_';
				needsRenamed = true;
				newName += c;
				break;
			case 92: // Backslash
				c = '-';
				needsRenamed = true;
				newName += c;
				break;
			default:
				if (c > 255) // In case we have unicode characters - replace them
				{
					needsRenamed = true;
					c = '_';
				}
				else if (c == 127) // Let's fix this one
				{
					needsRenamed = true;
					c = '_';
				}
				newName += c;
				break;
			}
		}
		File newFile = new File(infile.getParent() + java.io.File.separator + newName);
		if (needsRenamed)
		{
			System.out.println("Renaming to: " + newFile);
			if (infile.renameTo(newFile))
			{
				return newFile;
			}
			else
			{
				newFile = new File(infile.getParent() + java.io.File.separator + newName + "_");
				System.out.println("Renaming to: " + newFile);
				if (infile.renameTo(newFile))
					return newFile;
				else
				{
					System.err.println("Unable to rename a file!");
					System.exit(-1);
				}
			}
			return newFile;
		}
		else
			return infile;
	}

	public static String conditionFileName(String inName)
	{
		// Any sneaky characters getting in... replace them here.
		String nameReplaced = inName.replace('½', '_');
		nameReplaced = nameReplaced.replace('Â', '_');
		return nameReplaced;
	}

	public static void help()
	{
		help(false);
	}

	public static void help(boolean verbose)
	{
		System.err.println();
		System.err.println("Transform " + Version.VersionString + " - Apply transform specification to a file or directory");
		System.err.println();
		System.err.println("Usage: Transform <transform_spec> <input> <out_directory> [suffix]");
		System.err.println("       Transform fix_filenames <in_directory>");
		System.err.println("       Transform help");
		System.err.println("       Transform help-csv");
		System.err.println();
		System.err.println("See transform specification documentation here:");
		System.err.println("   https://github.com/RetroFloppy/transformenator/wiki/Transform-Specification");
		System.err.println("   https://github.com/RetroFloppy/transformenator/wiki/CSV-Transform-Specification");
		System.err.println();
		FileInterpreter.listInternalTransforms(verbose);
	}
}
