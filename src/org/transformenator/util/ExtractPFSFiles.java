/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2016 by David Schmidt
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractPFSFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of an
 * Apple II PFS-formatted PFS:Write disk.
 * 
 */
public class ExtractPFSFiles
{
	public static String baseName;

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 1) || (args.length == 2))
		{
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			byte[] inData = null;
			try
			{
				InputStream input = null;
				try
				{
					inData = new byte[(int) file.length()];
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
					int skew[] = {0,14,13,12,11,10,9,8,7,6,5,4,3,2,1,15};
					for (int i = 0; i < result.length / 4096; i++)
					{
						for (int j = 0; j < 16; j++)
						{
							System.arraycopy( result, (j*256)+i*4096, inData, (skew[j]*256)+i*4096, 256 );
						}
					}
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
				System.err.println("Read " + inData.length + " bytes.");
				if (args.length == 2)
				{
					/*
					 * If they wanted an output directory, go ahead and make it.
					 */
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("." + File.separator + args[1]);
					}
					baseDirFile.mkdir();
					baseName = new String(args[1]) + File.separator;
				}
				/* Now pull the files out of the image. */
				for (int i = 0x2e; i < 0x1ff; i += 18)
				{
					/*
					 * Directory sector
					 */
					if ((inData[i] != 0x00) && (UnsignedByte.intValue(inData[i]) < 16))
					{
						// System.err.println("DEBUG: found file name length: " + inData[i]);
						String filename = "";
						// Build the filename
						for (int j = 1; j < UnsignedByte.intValue(inData[i]) + 1; j++)
						{
							filename += (char) inData[i + j];
						}
						// Find file's starting "block"
						int fileStart = (UnsignedByte.intValue(inData[i + 16])+1) * 512;
						//System.err.println("DEBUG: found file: " + filename + " Starting at offset: "+fileStart);
						FileOutputStream out;
						try
						{
							if (baseName != null)
								filename = baseName + filename;
							System.err.println("Creating file " + filename);
							out = new FileOutputStream(filename, false);
							int j;
							// Figure out if this sector holds the EOF marker or not
							for (j = fileStart; j < inData.length; j++)
							{
								if (inData[j] == 0x0e)
									break;
							}
							out.write(inData, fileStart, j-fileStart);
							out.flush();
							out.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
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
		System.err.println("ExtractPFSFiles " + Version.VersionString + " - Extract files from Apple II PFS:Write disk images.  Files will need to be further post-processed with the pfswrite transform.");
		System.err.println();
		System.err.println("Usage: ExtractPFSFiles infile [out_directory]");
	}
}
