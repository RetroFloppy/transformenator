/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 - 2015 by David Schmidt
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractHPFiles
 * 
 * Helper app to pull the files off of the virtual file system of HP instrument (not LIF) disk image.
 * 
 * Disk geometry: 2 sides, 256 bytes per sector, 16 sectors per track, 35 tracks
 *
 */
public class ExtractHPFiles
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
				/*
				 * Catalog starts on track 0, second sector and stretches to the end of the track.
				 * 
				 * Catalog entries are 32 (0x20) bytes long; stuff we know/care about: bytes 0x00-0x0a: Filename (space padded) bytes 0x0e-0x0f: File start (in sectors) bytes 0x12-0x13: File length (in sectors)
				 */
				int fileStart = 0, fileLength = 0;
				for (int i = 0x0200; i < 0x1000; i += 0x20)
				{
					String filename = "";
					if (inData[i] != 0x00)
					{
						fileStart = UnsignedByte.intValue(inData[i + 0x0f], inData[i + 0x0e]) * 256;
						fileLength = UnsignedByte.intValue(inData[i + 0x13], inData[i + 0x12]) * 256;
						int j;
						for (j = 0; j < 10; j++)
						{
							if (inData[j + i] != 0x00)
							{
								filename += (char) inData[j + i];
							}
							else
								break;
						}
						// Find the end-of-file marker, trim down to that length
						boolean foundEOF = false;
						for (j = fileStart+fileLength-1; j > fileStart; j--)
						{
							// System.err.println(Integer.toHexString(UnsignedByte.intValue(inData[j])));
							if (UnsignedByte.intValue(inData[j]) == 0xff)
							{
								continue;
							}
							if (UnsignedByte.intValue(inData[j]) == 0xef)
							{
								// System.err.println("Found a trailing 0xef at "+(j-fileStart));
								foundEOF = true;
								break;
							}
							else
							{
								break;
							}
						}
						if (foundEOF)
							fileLength = j-fileStart;
						// System.out.println("Found file: "+filename+" Start: 0x"+Integer.toHexString(fileStart)+" End: 0x"+Integer.toHexString(fileStart+fileLength-1)+" Length: 0x"+Integer.toHexString(fileLength));
						filename = filename.trim();
						FileOutputStream out;
						try
						{
							String fullname = new String(outputDirectory + File.separator + filename);
							if (fileStart + fileLength < inData.length)
							{
								out = new FileOutputStream(fullname);
								System.err.println("Creating file: " + fullname);
								out.write(inData, fileStart, fileLength);
								out.flush();
								out.close();
							}
							else
								System.err.println("Error: file "+fullname+" would exceed the capacity of the disk image.");
						}
						catch (IOException io)
						{
							io.printStackTrace();
						}

					}
					else
						break;
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractHPFiles " + Version.VersionString + " - Extract files from HP instrument disk images.");
		System.err.println();
		System.err.println("Usage: ExtractHPFiles infile [out_directory]");
	}

}