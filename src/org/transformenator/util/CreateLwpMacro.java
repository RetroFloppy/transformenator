/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2016 - 2019 by David Schmidt
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

package org.transformenator.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.transformenator.internal.Version;

/*
 * CreateLwpMacro
 * 
 * This helper app creates a Lotus Word Pro macro (lotus2word.lss) in the current working
 * directory that Lotus Word Pro can consume to convert files it understands to Word 2000.
 * Given a source and destination directory (tree), CreateLwpMacro will add entries for each
 * source and destination file within the lotus2word.lss file, as well as create a batch file
 * (CreateDirectories.bat) to create all of the destination directories required (since LWP
 * isn't smart enough to do that for itself).
 * 
 * This app and resulting macro only make sense in the Windows or OS/2 context, as it requires 
 * execution within Lotus Word pro itself.
 *
 */
public class CreateLwpMacro
{
	public static void main(String[] args)
	{
		String in_directory = null, out_directory = null;
		if (args.length == 2)
		{
			in_directory = args[0];
			out_directory = args[1];
			File in_root_dir = new File(in_directory);
			File out_dir_root = new File(out_directory);
			// Remember... path can't be the same (though it probably could, since we append the .doc suffix)
			if (!in_root_dir.getAbsolutePath().equals(out_dir_root.getAbsolutePath()))
			{
				try
				{
					BufferedOutputStream dirs = new BufferedOutputStream(new FileOutputStream("CreateDirectories.bat"));
					BufferedOutputStream lss = new BufferedOutputStream(new FileOutputStream("lotus2word.lss"));
					lss.write("Sub Main\r\n".getBytes());
					if (!out_dir_root.isAbsolute())
					{
						out_dir_root = new File(out_dir_root.getAbsolutePath());
					}
					if (!in_directory.equals(out_directory))
						descendDirectory(dirs, lss, in_root_dir.getAbsolutePath().length(), in_root_dir, out_dir_root);
					else
						help();
					lss.write("End Sub\r\n".getBytes());
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				help();
			}
		}
		else
		{
			help();
		}
	}

	public static void descendDirectory(BufferedOutputStream dirs, BufferedOutputStream lss, int offset, File in_directory, File out_directory)
	{
		/*
		 * Get the files in the in_directory For each file, check if it's a file or a directory.
		 *  - If a file: emit it
		 *  - If a directory: recursively call descendDirectory
		 */
		if (in_directory.exists())
		{
			try
			{
				File[] files = in_directory.listFiles();
				if (files != null)
				{
					// Need to make a directory for this destination
					String test = "mkdir \"" + out_directory.getPath() + in_directory.getAbsolutePath().substring(offset, in_directory.getAbsolutePath().length()) + "\"\r\n";
					dirs.write(test.getBytes());
					for (int i = 0; i < files.length; i++)
					{
						if (files[i].isDirectory())
						{
							descendDirectory(dirs, lss, offset, files[i], out_directory);
						}
						else if (!files[i].isHidden())
						{
							lss.write((".OpenDocument \"" + files[i].getAbsoluteFile() + "\", \"\", \"\"\r\n").getBytes());
							// Work this file
							lss.write((".SaveAs \"" + out_directory.getPath() + in_directory.getAbsolutePath().substring(offset, in_directory.getAbsolutePath().length()) + File.separator + files[i].getName() + ".doc\", \"\", \"MS Word 2000\", False, True, False\r\n").getBytes());
							lss.write((".Close\r\n").getBytes());
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

	public static String describe(boolean verbose)
	{
		return "Create a Lotus Word Pro macro to transform all files in a filesystem to Word .doc format."+
				(verbose?"  Creates lotus2word.lss in the current working directory that Lotus Word Pro can consume to convert files it understands.":"");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("CreateLwpMacro " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: CreateLwpMacro <in_directory> <out_directory>");
		System.err.println("Note: the specified <in_directory> and <out_directory> cannot be the same.");
	}
}
