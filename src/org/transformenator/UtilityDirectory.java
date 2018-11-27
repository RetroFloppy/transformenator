/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2018 by David Schmidt
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
import java.lang.reflect.Method;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.Version;

public class UtilityDirectory
{

	public static void main(String[] args)
	{
		if (args.length == 3)
		{
			try
			{
				Class<?> utility = Class.forName("org.transformenator.util." + args[0]);
				Method main = utility.getMethod("main", String[].class);
				utilDirectory(args[0], main, args[1], args[2]);
			}
			catch (ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (NoSuchMethodException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (SecurityException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
			help();
	}

	public static void utilDirectory(String utilName, Method main, String in_directory, String out_directory)
	{
		/*
		 * Get the files in the in_directory For each file, check if it's a file or a directory. - If a file: Transform it - If a directory: recursively call utilDirectory
		 */
		File inDirFile = new File(in_directory);
		if (inDirFile.exists())
		{
			File outDirFile = new File(out_directory);
			outDirFile.mkdirs();
			// System.out.println("mkdirs:  "+out_directory);
			try
			{
				File[] files = inDirFile.listFiles();
				if (files != null)
				{
					for (int i = 0; i < files.length; i++)
					{
						if (files[i].isDirectory())
						{
							utilDirectory(utilName, main, in_directory + java.io.File.separator + files[i].getName(), out_directory + java.io.File.separator + files[i].getName());
						}
						else if (!files[i].isHidden())
						{
							String newFileName = conditionFileName(files[i].getName());
							System.out.println("Running "+utilName+" on file: " + files[i] + " to file: " + out_directory + java.io.File.separator + newFileName);
							String[] params = { files[i].toString(), out_directory + java.io.File.separator + newFileName };
							main.invoke(null, (Object) params); // static method doesn't have an instance
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

	public static String conditionFileName(String inName)
	{
		// Any sneaky characters getting in... replace them here.
		// String nameReplaced = inName.replace('�', '_');
		// nameReplaced = nameReplaced.replace('�', '_');
		return inName;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("UtilityDirectory " + Version.VersionString + " - Recursively apply utility function to all files in a filesystem");
		System.err.println();
		System.err.println("Usage: UtilityDirectory <utility> <in_directory> <out_directory>");
		System.err.println();
		FileInterpreter.listUtilities(false);
	}
}
