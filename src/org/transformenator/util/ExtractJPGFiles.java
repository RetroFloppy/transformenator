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

package org.transformenator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.transformenator.internal.Version;

/*
 * ExtractJPGFiles
 * 
 * In some really crappy Mavica camera disks, the directory is just completely wrong.
 * The data exists on the disks, but the directory entries simply don't point to it.
 * Here, we're relying on files being contiguous (Mavica OS wasn't smart enough to 
 * split files - or at least folks didn't use them that way) and we dumbly end a file
 * when we find another JFIF header or we run out of disk.  It's ugly, but it produces
 * pictures where there were none before.
 * 
 */
public class ExtractJPGFiles
{

	public static void main(java.lang.String[] args)
	{
		String outputDirectory = "";
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
				// Read in the entire disk image
				InputStream input = null;
				try
				{
					int totalBytesRead = 0;
					input = new BufferedInputStream(new FileInputStream(file));
					while (totalBytesRead < result.length)
					{
						int bytesRemaining = result.length - totalBytesRead;
						// input.read() returns -1, 0, or more:
						int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
						if (bytesRead > 0)
						{
							totalBytesRead = totalBytesRead + bytesRead;
						}
					}
					inData = result;
				}
				finally
				{
					if (input != null)
						input.close();
				}
			}
			catch (FileNotFoundException ex)
			{
				System.err.println("Input file \"" + file + "\" not found.");
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
			if (inData != null)
			{
				// We have good data; get ready to deal with it.
				System.err.println("Read " + inData.length + " bytes.");
				if (args.length > 1)
				{
					// If they wanted an output directory, go ahead and make it.
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("." + File.separator + args[1]);
					}
					// System.out.println("Making directory: ["+baseDirFile+"]");
					baseDirFile.mkdirs();
					outputDirectory = args[1];
				}
				else
				{
					if (file.getName().endsWith(".img"))
					{
						// System.out.println("Making image directory: ["+"." + File.separator + file.getName().substring(0, file.getName().length() - 4)+"]");
						outputDirectory = "." + File.separator + file.getName().substring(0, file.getName().length() - 4);
						File baseDirFile = new File(outputDirectory);
						baseDirFile.mkdirs();
					}					
				}
				byte jfifheader[] = { -0x01, -0x28, -0x01, -0x20, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46 }; // JFIF header
				int begin = 0, end = 0;
				int filenum = 1;
				for (int i = 0; i < inData.length - jfifheader.length; i++)
				{
					byte range[] = Arrays.copyOfRange(inData, i, i + jfifheader.length);
					if (Arrays.equals(range, jfifheader)) // Is the WANG eyecatcher in the disk image?
					{
//						System.out.println("DEBUG: Found JFIF header at offset 0x"+Integer.toHexString(i));
						if (begin == 0)
							begin = i;
						else
							end = i;
						if (begin > 0 && end > 0)
						{
							dumpFile(outputDirectory, filenum++, inData, begin, end);
							// System.out.println("DEBUG: Write JFIF file from 0x"+Integer.toHexString(begin)+" to 0x"+Integer.toHexString(end));
						}
						end = 0;
						begin = i;
					}
				}
				if (begin >0 )
				{
					// System.out.println("DEBUG: Write JFIF file from 0x"+Integer.toHexString(begin)+" to 0x"+Integer.toHexString(inData.length));
					dumpFile(outputDirectory, filenum++, inData, begin, inData.length);
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}


	public static void dumpFile(String prefix, int filenum, byte[] inData, int start, int end)
	{
		FileOutputStream out = null;
		String fileName = new String(prefix + File.separator + filenum + ".jpg");
		try
		{
			out = new FileOutputStream(fileName);
			System.err.println("Creating file: " + fileName);
			byte range[] = Arrays.copyOfRange(inData, start, end);
			out.write(range);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractJPGFiles " + Version.VersionString + " - Scrape off jpg files from a disk image in a totally brain-dead fashion.");
		System.err.println();
		System.err.println("Usage: ExtractJPGFiles infile [out_directory]");
	}
}