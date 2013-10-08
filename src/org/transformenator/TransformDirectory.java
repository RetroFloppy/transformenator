/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 by David Schmidt
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

package org.transformenator;

import java.io.File;

public class TransformDirectory
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args.length > 2)
		{
			Transformation transform = new Transformation(args[0]);
			if (args.length == 3)
				tranformDirectory(transform, args[0],args[1], args[2]);
			else if (args.length == 4)
				tranformDirectory(transform, args[0],args[1], args[2], args[3]);
			else
				help();
		}
		else
			help();
	}

	public static void tranformDirectory(Transformation transform, String transform_name, String in_directory, String out_directory)
	{
		/*
		 * Pick a suitable file suffix based on the final part of the transform name
		 */
		String suffix_guess = "txt";
		if (transform_name.toLowerCase().endsWith("_rtf"))
			suffix_guess = "rtf";
		else if (transform_name.toLowerCase().endsWith("_html"))
			suffix_guess = "html";
		tranformDirectory(transform, transform_name, in_directory, out_directory, suffix_guess);
	}

	public static void tranformDirectory(Transformation transform, String transform_name, String in_directory, String out_directory, String file_suffix)
	{
		/*
		 * Get the files in the in_directory
		 * For each file, check if it's a file or a directory.  
		 *  - If a file: Transform it
		 *  - If a directory: recursively call the directory checker
		 */
		File inDirFile = new File(in_directory);
		File outDirFile = new File(out_directory);
		outDirFile.mkdirs();
		// System.out.println("mkdirs:  "+out_directory);
		try
		{
			File[] files = inDirFile.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					tranformDirectory(transform,transform_name,in_directory + java.io.File.separator + files[i].getName(),out_directory + java.io.File.separator + files[i].getName(), file_suffix);
				}
				else if (!files[i].isHidden())
				{
					System.out.println("Transforming file: "+files[i]+" to file: "+out_directory + java.io.File.separator + files[i].getName()+"."+file_suffix);
					transform.createOutput(""+files[i],out_directory + java.io.File.separator + files[i].getName()+"."+file_suffix);
				}
			}
		}
		catch (Throwable t1)
		{
			System.err.println(t1);
		}
	}

	public static void help()
	{
		System.err.println();
		System.err.println("TransformDirectory - Recursively apply transforms to a filesystem");
		System.err.println();
		System.err.println("Syntax: TransformDirectory transform in_directory out_directory [suffix]");
	}
}
