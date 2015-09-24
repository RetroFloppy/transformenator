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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractNBIFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of NBI
 * word processor disks.
 *
 * NBI disks have sectors with different roles, specified by the second byte of a sector:
 * 
 * Sector headers:
 * 0x<len>00: empty
 * 0x<len>44: directory entry
 * 0x<len>58: system
 * 0x<len>4c: text
 * 
 * Sector structure:
 *
 * Offset Length Meaning
 * ====== ====== ==============
 * 0x00   1      Data length
 * 0x01   1      Sector type
 * 0x02   1      MSB sector number (nibble)
 * 0x03   1      Previous sector number (+1)
 * 0x04   1      Next sector number (+1)
 * 
 * Then, within each text sector, there is are a couple of sector previous/next
 * pointers.  Re-assembling the files involves finding all of the sectors associated with each file
 * and concatenating them in order.
 *
 */
public class ExtractNBIFiles
{
	public static void main(java.lang.String[] args)
	{
		int key = 0;
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
				FileOutputStream out = null;
				if (args.length == 2)
				{
					/*
					 * If they wanted an output directory, go ahead and create it.
					 */
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("." + File.separator + args[1]);
					}
					baseDirFile.mkdir();
				}

				for (int sector = 0; sector < inData.length / 256; sector++)
				{
					if (UnsignedByte.intValue(inData[sector * 256 + 1]) == 0x4c)
					{
						if (calcPrevOffset(sector, inData) == 0)
						{
							String fileName = new String("recovered_file_" + ++key);
							if (args.length == 2)
								fileName = new String(args[1]) + File.separator + fileName;
							try
							{
								out = new FileOutputStream(fileName);
								System.err.println("Creating file: " + fileName);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
							dumpSectorChain(sector, inData, out);
						}
					}
					else if (UnsignedByte.intValue(inData[sector * 256 + 1]) == 0x4c)
					{
						if (calcNextOffset(sector, inData) == 0)
						{
							System.out.println("End of file!");
							System.err.println();
						}
					}
				}
			}
			// scrapeSurface(inData);
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static int calcPrevOffset(int sector, byte[] inData)
	{
		return calcSectorOffset(sector, false, inData);
	}

	public static int calcNextOffset(int sector, byte[] inData)
	{
		return calcSectorOffset(sector, true, inData);
	}

	public static int calcSectorOffset(int sector, boolean next, byte[] inData)
	{
		int offsetHi, offsetLo;
		if (next)
		{
			offsetHi = UnsignedByte.intValue(inData[sector * 256 + 2]) & 15;
			offsetLo = UnsignedByte.intValue(inData[sector * 256 + 4]);
		}
		else
		{
			offsetHi = UnsignedByte.intValue(inData[sector * 256 + 2]) / 16;
			offsetLo = UnsignedByte.intValue(inData[sector * 256 + 3]);
		}
		if (offsetLo == 0)
		{
			if (offsetHi > 0)
			{
				offsetLo = 0xff;
				offsetHi --;
			}
		}
		else
			offsetLo --;
		return offsetHi * 65536 + offsetLo * 256;
	}

	static void dumpSectorChain(int sector, byte[] inData, FileOutputStream out)
	{
		int length = UnsignedByte.intValue(inData[sector * 256]);
		//String offsetStr = "0x"+(Integer.toHexString(sector).toUpperCase())+"00";
		//System.err.println();
		//System.err.print(offsetStr+" next: 0x"+Integer.toHexString(calcNextOffset(sector,inData)).toUpperCase());
		//System.err.print(" prev: 0x"+Integer.toHexString(calcPrevOffset(sector,inData)).toUpperCase());
		//System.err.println(" length: 0x"+Integer.toHexString(length).toUpperCase());

		try
		{
			out.write(inData, (sector * 256 + 5), length);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int nextSector = calcNextOffset(sector, inData);
		if (nextSector > 0)
			dumpSectorChain(nextSector/256, inData, out);
	}

	static void scrapeSurface(byte[] inData)
	{
		for (int sector = 0; sector < inData.length / 256; sector++)
		{
			int sectorId = UnsignedByte.intValue(inData[sector * 256 + 1]);
			switch (sectorId)
			{
				case 0x00:
					// Empty sector
					// System.err.println("Found empty sector.");
					break;
				case 0x4c:
					// Text Sector
					int offset = sector * 256;
					int x;
					String offsetStr = "0x"+(Integer.toHexString(offset).toUpperCase());
					System.err.print(offsetStr+" Text sector; next: 0x"+Integer.toHexString(calcNextOffset(sector,inData)).toUpperCase());
					System.err.print(" prev: 0x"+Integer.toHexString(calcPrevOffset(sector,inData)).toUpperCase());
					System.err.print(" length: 0x"+Integer.toHexString(UnsignedByte.intValue(inData[sector * 256])).toUpperCase());
					System.err.print(" Data: 0x");
					for (x = 2; x < 12; x++)
					{
						System.err.print(UnsignedByte.toString(inData[sector * 256 + x])+" ");
					}
					System.err.println();
					for (x = 5; x < 253; x++)
					{
						int chi = UnsignedByte.intValue(inData[sector * 256 + x]);
						if (chi == 0x7f)
							chi = 32;
						if (chi == 0x82)
							chi = 0x2e;
						if (chi == 0x8e)
							chi = 0x2d;
						if (chi == 0x9c)
							chi = 32;
						if (chi == 0x00)
							chi = 32;
						if (chi == 0xa0) // newline
							chi = 0x0a;
						if (chi == 0xa1) // tab
							chi = 32;
						char ch = (char)chi;
						System.err.print(ch);
					}
					System.err.println();
					/* Print out the final three bytes of the sector...
					System.err.print(UnsignedByte.toString(inData[sector * 256 + 253])+" ");
					System.err.print(UnsignedByte.toString(inData[sector * 256 + 254])+" ");
					System.err.print(UnsignedByte.toString(inData[sector * 256 + 255]));
					System.err.println();
					*/
					System.err.println();
					break;
				case 0x58:
					// System sector
					offset = sector * 256;
					offsetStr = "0x"+Integer.toHexString(offset).toUpperCase();
					System.err.print(offsetStr+" System sector: 0x");
					offset = sector * 256;
					offsetStr = "0x"+Integer.toHexString(offset).toUpperCase();
					for (x = 2; x < 12; x++)
					{
						System.err.print(UnsignedByte.toString(inData[sector * 256 + x])+" ");
					}
					System.err.println();
					/*
					for (x = 5; x < 253; x++)
					{
						int chi = UnsignedByte.intValue(inData[sector * 256 + x]);
						if (chi == 0x7f)
							chi = 32;
						if (chi == 0x82)
							chi = 0x2e;
						if (chi == 0x8e)
							chi = 0x2d;
						if (chi == 0x9c)
							chi = 32;
						if (chi == 0x00)
							chi = 32;
						if (chi == 0xa0) // newline
							chi = 0x0a;
						if (chi == 0xa1) // tab
							chi = 32;
						char ch = (char)chi;
						System.err.print(ch);
					}
					System.err.println();
					System.err.print(UnsignedByte.toString(inData[sector * 256 + 253])+" ");
					System.err.print(UnsignedByte.toString(inData[sector * 256 + 254])+" ");
					System.err.print(UnsignedByte.toString(inData[sector * 256 + 255]));
					System.err.println();
					System.err.println();
					*/
					break;
				case 0x44:
					// Directory sector
					offset = sector * 256;
					offsetStr = "0x"+Integer.toHexString(offset).toUpperCase();
					System.err.print(offsetStr+" Directory sector:");
					System.err.println();
					offset = sector * 256;
					offsetStr = "0x"+Integer.toHexString(offset).toUpperCase();
					x = 0;
					for (int y = 0; y < 8; y++)
					{
						for (int z = 0; z < 32; z++)
						{
							System.err.print(UnsignedByte.toString(inData[sector * 256 + x]));
							x++;
						}
						System.err.println();
					}
					System.err.println();
					break;
				default:
					// Other sector
					offset = sector * 256;
					offsetStr = "0x"+Integer.toHexString(offset).toUpperCase();
					System.err.print(offsetStr+" Other sector; next: 0x"+Integer.toHexString(calcNextOffset(sector,inData)).toUpperCase());
					System.err.print(" prev: 0x"+Integer.toHexString(calcPrevOffset(sector,inData)).toUpperCase());
					System.err.print(" length: 0x"+Integer.toHexString(UnsignedByte.intValue(inData[sector * 256])).toUpperCase());
					System.err.print(" Data: 0x");
					for (x = 0; x < 12; x++)
					{
						System.err.print(UnsignedByte.toString(inData[sector * 256 + x])+" ");
					}
					System.err.println();
					for (x = 5; x < 253; x++)
					{
						int chi = UnsignedByte.intValue(inData[sector * 256 + x]);
						if (chi == 0x7f)
							chi = 32;
						if (chi == 0x82)
							chi = 0x2e;
						if (chi == 0x8e)
							chi = 0x2d;
						if (chi == 0x9c)
							chi = 32;
						if (chi == 0x00)
							chi = 32;
						if (chi == 0xa0) // newline
							chi = 0x0a;
						if (chi == 0xa1) // tab
							chi = 32;
						char ch = (char)chi;
						System.err.print(ch);
					}
					System.err.println();
					System.err.println();
					break;
			}
		}
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractNBIFiles " + Version.VersionString + " - Extract files from NBI word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractNBIFiles infile [out_directory]");
	}
}