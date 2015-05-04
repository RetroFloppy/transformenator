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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractMemoryWriterFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of a
 * word processor disk of some unknown origin, possibly the Xerox
 * Memorywriter.  It might be something else altogether.
 * 
 * The disk geometry is 256 bytes per sector, 16 sectors per track, 40 tracks per side, one sided.
 *
 */
public class ExtractMemoryWriterFiles
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
				/* Now pull the files out of the image. */
				for (int i = 0x10100 + 25; i < 0x10100 + 0x100 + 25; i += 19)
				{
					/*
					 * Directory entry structure: 0x00: 3 bytes file number
					 * 0x03: 10 bytes of name 0x0d: 2 bytes something 0x15: 2
					 * bytes first sector pointer 0x17: 2 bytes something
					 */
					int fileNumber = (inData[i] * 1024) + (inData[i + 1] * 256) + inData[i + 2];
					if (fileNumber > 0)
					{
						byte fnb[] = new byte[10];
						/* Extract this file */
						int firstOffset = (inData[i + 15] - 1) * 0x1000 + ((inData[i + 16] - 1) * 256);
						// System.err.print("DEBUG: file number found: " + fileNumber + " first track/sector: " + UnsignedByte.toString(inData[i + 15]) + "/" + UnsignedByte.toString(inData[i + 16]));
						// System.err.println(" offset:" + Integer.toHexString(firstOffset));
						fnb = Arrays.copyOfRange(inData, i + 3, i + 12);
						String fileName = new String(fnb).trim();
						FileOutputStream out;
						if (!fileName.isEmpty())
						{
							try
							{
								out = new FileOutputStream(baseName + fileName);
								System.err.println("Creating file: " + fileName);
								decodeFile(inData, out, firstOffset);
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
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void decodeFile(byte[] inData, FileOutputStream out, int sectorOffset) throws IOException
	{
		/*
		 * A sector has its first few bytes listing the following: 0x00
		 */
		int nextOffset = ((UnsignedByte.intValue(inData[sectorOffset + 2]) - 1) * 0x10 + (UnsignedByte.intValue(inData[sectorOffset + 3]) - 1)) * 0x100;
		int realOffset = ((UnsignedByte.intValue(inData[sectorOffset + 2])) * 0x10 + (UnsignedByte.intValue(inData[sectorOffset + 3]))) * 0x100;

		/* Debug
		if (realOffset > 0)
		{
			String message = "\n\nAt offset " + Integer.toHexString(sectorOffset) + ", the next offset is: " + Integer.toHexString(nextOffset) + "\n";
			out.write(message.getBytes());
		}
		else
		{
			String message = "\n\nAt offset " + Integer.toHexString(sectorOffset) + ", Found the end of the file.\n";
			out.write(message.getBytes());
		}
		int rangeEnd = UnsignedByte.intValue(inData[sectorOffset + 4]) - 1;
		// System.err.println("rangeEnd: " + rangeEnd);
		if (rangeEnd > 0)
		{
			byte range[] = Arrays.copyOfRange(inData, sectorOffset + 8, sectorOffset + rangeEnd + 9);
			out.write(range);
		}
		*/

		if (realOffset > 0)
			decodeFile(inData, out, nextOffset);
	}

	public static String offsetToTS(int offset)
	{
		int track = offset / 0x100;
		int sector = UnsignedByte.loByte(offset / 0x10) / 0x010;
		return Integer.toHexString(track) + "/" + sector;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractMemoryWriterFiles " + Version.VersionString + " - Extract files from some unknown word processor (possibly Xerox MemoryWriter) disk images.");
		System.err.println();
		System.err.println("Usage: ExtractMemoryWriterFiles infile [out_directory]");
	}
}
