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

package org.transformenator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.transformenator.UnsignedByte;

/*
 * UnpackWangFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of
 * Wang word processor disks.    
 *
 */
public class ExtractWangFiles
{

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
				// System.err.println("Read " + inData.length + " bytes.");
				byte eyecatcher[] = { 0x57, 0x41, 0x4e, 0x47 }; // "WANG" - part
																// of the WVD
																// specification
				byte range[] = Arrays.copyOfRange(inData, 0x00, 0x04);

				if (Arrays.equals(range, eyecatcher)) // Is the WANG eyecatcher in the disk image?
				{
					int catalogSectors = 0;
					int preambleOffset = 0x100; // Space for WVD preamble
					int catalogOffset = 0x100;
					boolean shouldContinue = false;
					if (inData[0x100] == 0x00)
					{
						catalogSectors = UnsignedByte.intValue(inData[0x101]);
					}
					else if (inData[0x100] == 0x02)
					{
						System.err.println("Caution: disk index type is 2.  May need new means of interpreting disk image.");
						// TODO: Untested...
						catalogSectors = UnsignedByte.intValue(inData[0x102], inData[0x101]);
					}
					else
					{
						System.err.println("Error: disk index type is " + inData[0x100] + ", expected 0.  Will need new means of interpreting disk image.");
					}
					if (catalogSectors > 0) // Ok, we have a reasonable catalog number
					{
						if (args.length == 2)
						{
							/*
							 * If they wanted an output directory, go ahead and make it.
							 */
							File baseDirFile = new File(args[1]);
							if (!baseDirFile.isAbsolute())
							{
								baseDirFile = new File("./"+args[1]);
							}
							baseDirFile.mkdir();
						}
						// System.err.println("Number of catalog sectors: "+catalogSectors);
						preambleOffset += catalogSectors * 256;
						catalogOffset += (catalogSectors + 2) * 256;
						// System.err.println("preambleOffset: "+preambleOffset+" catalogOffset: "+catalogOffset);
						int fileIndexOffset = catalogOffset + 4; // The first file in the file index is 4 bytes past the start of the index sector
						do
						{
							// System.err.println("Next index byte: "+UnsignedByte.toString(inData[fileHeaderPointer]));
							if (inData[fileIndexOffset] != (byte) 0xff)
							{
								byte fnb[] = new byte[10];
								fnb = Arrays.copyOfRange(inData, fileIndexOffset + 2, fileIndexOffset + 12);
								String fileName = new String(fnb).trim().replace("\\", "-").replace("/", "-").replace("?", "-");
								fileName = new String(args[1]) + File.separator + fileName;
								int fileHeaderSector = UnsignedByte.intValue(inData[fileIndexOffset + 1], inData[fileIndexOffset]);
								// System.err.println("File found: " + fileName+" at raw sector: "+fileHeaderSector);
								decodeFile(inData, fileName, fileHeaderSector, preambleOffset);
								shouldContinue = true;
							}
							else
								shouldContinue = false;
							fileIndexOffset += 12;
						} while (shouldContinue == true);
					}
					else
						System.err.println("Error: no catalog sectors found.");
				}
				else
				{
					System.err.println("Input file is not Wang WVD format.");
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void decodeFile(byte[] inData, String shortName, int fileHeaderSector, int preambleOffset)
	{
		/*
		 * Incoming, we have the sector address of the file header; use that to
		 * find the rest of the file.
		 */
		int fileHeaderOffset = fileHeaderSector * 256 + preambleOffset;
		// System.err.println("fileHeaderSector: "+fileHeaderSector+" fileHeaderOffset: "+fileHeaderOffset);
		int firstFileChainSector = UnsignedByte.intValue((byte) inData[fileHeaderOffset + 193 /* 0xc1 */], (byte) inData[fileHeaderOffset + 192 /* 0xc0 */]);
		byte fnb[] = new byte[64];
		fnb = Arrays.copyOfRange(inData, fileHeaderOffset + 64, fileHeaderOffset + 128);
		String longName = new String(fnb).trim().replace("\\", "-").replace("/", "-").replace("?", "-"), fullName;
		// System.err.println("Long name: "+longName);
		FileOutputStream out;
		try
		{
			if (longName.equals(""))
				fullName = shortName;
			else
				fullName = shortName + "-" + longName;
			out = new FileOutputStream(fullName);
			System.err.println("Creating file: " + fullName);
			dumpFileChain(out, inData, firstFileChainSector, preambleOffset);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void dumpFileChain(FileOutputStream out, byte[] inData, int fileChainSector, int preambleOffset) throws IOException
	{
		// System.err.println("dumpFileChain: fileChainSector: " + fileChainSector);
		int textSector = 0, i = 0, textRealOffset;
		if ((fileChainSector > 0) && ((fileChainSector+1)*256 < inData.length))
		{
			do
			{
				textSector = UnsignedByte.intValue((byte) inData[realAddress(fileChainSector, preambleOffset) + 0x41 + i], (byte) inData[realAddress(fileChainSector, preambleOffset) + 0x40 + i]);
				if ((textSector > 0) && ((textSector+1)*256 < inData.length))
				{
					// System.err.println("dumpFileChain: textSector: " + textSector);
					textRealOffset = realAddress(textSector, preambleOffset);
					// System.err.println("dumpFileChain: textRealOffset: "+textRealOffset);
					byte range[] = Arrays.copyOfRange(inData, textRealOffset, textRealOffset + 256);
					out.write(range);
					i += 2;
				}
			} while ((textSector > 0) && ((textSector+1)*256 < inData.length));
			int nextFileChainSector = UnsignedByte.intValue((byte) inData[realAddress(fileChainSector, preambleOffset) + 9], (byte) inData[realAddress(fileChainSector, preambleOffset) + 8]);
			// System.err.println("dumpFileChain: nextFileChainSector: "+nextFileChainSector);
			if (nextFileChainSector != 0)
				dumpFileChain(out, inData, nextFileChainSector, preambleOffset);
		}
	}

	public static int realAddress(int sector, int offset)
	{
		return sector * 256 + offset;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractWangFiles v1.7 - Extract files from Wang word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractWangFiles infile [out_directory]");
	}
}