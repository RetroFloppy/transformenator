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
import org.transformenator.internal.OfficeSys6Util;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractOfficeSystem6Files
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of an
 * IBM Office System 6 8" disk, as extracted with Dave Dunfield's IMGDISK (i.e. FM encoded,
 * 26x128 for first track, 8*512 for remaining 76 tracks, single-sided).
 * 
 */
public class ExtractOfficeSystem6Files
{
	public static String baseName;

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
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
				int indexOffset = 0x591b;
				// System.err.println("DEBUG: initial indexOffset: " + indexOffset + " max indexOffset: " + 0x597d);
				int j;
				/* Now pull the files out of the image. */
				for (int i = 0xd00; i < 0xd00 + 5 * 4096; i += 512)
				{
					/*
					 * Directory
					 */
					if (inData[i + 1] == 0x0a)
					{
						int len = 0;
						for (int k = 0; k < 37; k++)
						{
							if (UnsignedByte.intValue(inData[i + 0x1db + k]) == 0xe2)
							{
								len = k;
								// System.err.println("Found end at: "+k);
							}
						}
						String filename = OfficeSys6Util.toAscii(inData, i + 0x1db, len);
						// System.err.println("DEBUG: found file: " + filename);
						int trackIndex = 0;
						boolean dumpedYet = false;
						for (j = indexOffset; j < 0x597d; j++)
						{
							if ((UnsignedByte.intValue(inData[j]) == 0xca))
							{
								// System.err.println("DEBUG: Found a file track index...");
								for (trackIndex = j + 1; trackIndex < 0x597d; trackIndex++)
								{
									// System.err.println("DEBUG: Found a track: "+UnsignedByte.toString(inData[trackIndex]));
									if ((UnsignedByte.intValue(inData[trackIndex]) == 0xca) || (UnsignedByte.intValue(inData[trackIndex]) == 0xc0))
									{
										break;
									}
									else
									{
										if (baseName != null)
											dumpTrack(inData, trackToOffset(inData[trackIndex]), baseName+filename, dumpedYet);
										else
											dumpTrack(inData, trackToOffset(inData[trackIndex]), filename, dumpedYet);
										dumpedYet = true;
									}
								}
								j = trackIndex;
								break;
							}
						}
						// System.err.println("DEBUG: Setting indexOffset to: " + j);
						indexOffset = j;
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

	public static void dumpTrack(byte[] inData, int offset, String fileName, boolean append)
	{

		if (!append)
			System.err.println("Creating file " + fileName);

		FileOutputStream out;
		try
		{
			out = new FileOutputStream(fileName, append);
			out.write(inData, offset, 4096);
			out.flush();
			out.close();
			// System.err.println("DEBUG: Dumped track:  "+UnsignedByte.toString(offsetToTrack(offset))+" from 0x"+Integer.toHexString(offset)+"...appended: "+append);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public static int trackToOffset(int track)
	{
		return (track - 1) * 4096 + 0xd00;
	}

	public static int offsetToTrack(int offset)
	{
		return (offset - 0xd00) / 4096 + 1;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractOfficeSystem6Files " + Version.VersionString + " - Extract files from IBM Office System 6 disk images.  Files will need to be further post-processed with the office_system_6 transform.");
		System.err.println();
		System.err.println("Usage: ExtractOfficeSystem6Files infile [out_directory]");
	}
}
