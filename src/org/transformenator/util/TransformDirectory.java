/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2015 by David Schmidt
 * david__schmidt at users.sourceforge.net
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

package org.transformenator.util;

import java.io.File;

import org.transformenator.Transformation;
import org.transformenator.Version;

public class TransformDirectory
{

	public static void main(String[] args)
	{
		if (args.length > 1)
		{
			if (args.length == 2)
				tranformDirectory(null, args[0], args[1]);
			else
			{
				Transformation transform = new Transformation(args[0]);
				if (args.length == 3)
					tranformDirectory(transform, args[0], args[1], args[2], false);
				else if (args.length == 4)
					tranformDirectory(transform, args[0], args[1], args[2], args[3], false);
				else
					help();
			}
		}
		else
			help();
	}

	public static void tranformDirectory(Transformation transform, String transform_name, String in_directory)
	{
		if (transform_name.equals("fix_filenames"))
		{
			/*
			 * Guess a suitable file suffix based on the final part of the transform name
			 */
			String suffix_guess = "txt";
			tranformDirectory(transform, transform_name, in_directory, in_directory, suffix_guess, true);
		}
		else
			help();
	}

	public static void tranformDirectory(Transformation transform, String transform_name, String in_directory, String out_directory, boolean only_fix_filenames)
	{
		/*
		 * Guess a suitable file suffix based on the final part of the transform name
		 */
		String suffix_guess = "txt";
		if (transform_name.toLowerCase().endsWith("_rtf"))
			suffix_guess = "rtf";
		else if (transform_name.toLowerCase().endsWith("_html"))
			suffix_guess = "html";
		tranformDirectory(transform, transform_name, in_directory, out_directory, suffix_guess, only_fix_filenames);
	}

	public static void tranformDirectory(Transformation transform, String transform_name, String in_directory, String out_directory, String file_suffix, boolean only_fix_filenames)
	{
		/*
		 * Get the files in the in_directory For each file, check if it's a file or a directory. - If a file: Transform it - If a directory: recursively call tranformDirectory
		 */
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
								fixFilename(files[i]);
							}
							else
							{
								String newFileName = conditionFileName(files[i].getName());
								System.out.println("Transforming file: " + files[i] + " to file: " + out_directory + java.io.File.separator + newFileName + "." + file_suffix);
								transform.createOutput(files[i].toString(), out_directory + java.io.File.separator + newFileName + "." + file_suffix, file_suffix);
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
			System.err.println("Error: Specified in_directory does not exist.");
		}
	}

	public static File fixFilename(File infile)
	{
		String name = infile.getName(), newName = "";
		// System.out.println("Checking file: " + name);
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
			case 32: // Space
				if (j == 0)  // Leading space?  Baaaaad.
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
		System.err.println();
		System.err.println("TransformDirectory " + Version.VersionString + " - Recursively apply transform to all files in a filesystem");
		System.err.println();
		System.err.println("Usage: TransformDirectory <transform> <in_directory> <out_directory> [suffix]");
		System.err.println("       TransformDirectory fix_filenames <in_directory>");
	}
}
