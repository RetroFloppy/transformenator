/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 by David Schmidt
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

import org.transformenator.Version;

/*
 * ExtractDisplaywriterFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of Displaywriter
 * word processor disks.  Recall that text will need to be interpreted as EBCDIC.
 *
 */
public class ExtractDisplaywriterFiles
{
	public static int locEHL1 = 0x021c00; // Expected location of EHL1 record of a FM 1-sided disk - we revisit this decision later
	public static String baseName;
	public static FileOutputStream currentOut = null;

	public static void main(java.lang.String[] args)
	{
		baseName = "";
		int debugLevel = 0; // Debug levels: 0 = none; 1 = follow EHL1 chain; 2 = dump all sectors of disk image
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			if (args.length == 2)
			{
				if (args[1].equalsIgnoreCase("-debug1"))
					debugLevel = 1;
				else if (args[1].equalsIgnoreCase("-debug2"))
					debugLevel = 2;
				else
				{
					/*
					 * If they wanted an output directory, go ahead and make it.
					 */
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("."+File.separator+args[1]);
					}
					baseDirFile.mkdir();
					baseName = new String(args[1]) + File.separator;
				}
			}
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
					int trkLen = 128 * 26;
					if (result.length > 1000000)
					{
						locEHL1 = 0x75000; // Expected location of EHL1 record of an MFM 2-sided disk
						// Clip off the first cylinder of image
						inData = Arrays.copyOfRange(result, (trkLen * 3), (trkLen * 3) + result.length);
					}
					else
					{
						locEHL1 = 0x021c00; // Expected location of EHL1 record of a FM 1-sided disk
						// Clip off the first cylinder of image
						inData = Arrays.copyOfRange(result, trkLen, trkLen + result.length);
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
			/*
			 * There are three modes of operation (debugLevel):
			 * 
			 * 0) Extract and create all files found; names will be ASCII, content will be EBCDIC
			 * 
			 * 1) Starting with the EHL1 record, follow all records and pointers, dumping contents
			 * 
			 * 2) Scrape the entire disk image and dump out all records found
			 */
			if (inData != null)
			{
				System.err.println("Read " + inData.length + " bytes.");
				if (debugLevel == 2)
				{
					// Loop over the entire disk looking for records
					int i = 0, j = 0;
					int delta = 0;
					while (i < inData.length)
					{
						delta = i;
						i = dumpBareRecord(inData, i);
						delta = i - delta; // How big was the delta?
						j = i % 256;
						if ((delta == 0) && (j == 0))
						{
							i += 256;
						}
						else
						{
							if (delta == 0)
							{
								i += 256 - j; // Move i past the current record within the current sector
							}
							else
							{
								// delta is non-zero; that means i moved already
							}
						}
					}
				}
				else
				{
					int offset = locEHL1;
					// First, get the length of the EHL1 record; then go nuts
					int total = offset + emitRecord(inData, offset, true, debugLevel);
					boolean done = false;
					while (!done)
					{
						offset += 256;
						if (getRecordEyecatcher(inData, offset).equals("DEXT (0xe0)"))
						{
							emitRecord(inData, offset, true, debugLevel);
						}
						if (offset > total)
						{
							done = true;
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

	public static String getRecordEyecatcher(byte inData[], int offset)
	{
		int recTypeHi = UnsignedByte.intValue(inData[offset + 2]);
		int recTypeLo = UnsignedByte.intValue(inData[offset + 3]);
		String recordEyecatcher = "----";
		if (recTypeHi == 0x20)
			recordEyecatcher = "EHL1 (0x20)";
		else if ((recTypeHi == 0x40) && (recTypeLo == 0x00))
			recordEyecatcher = "ABM  (0x40)";
		else if ((recTypeHi == 0x60) && (recTypeLo == 0x00))
			recordEyecatcher = "DSL2 (0x60)";
		else if (recTypeHi == 0x80)
			recordEyecatcher = "NAME (0x80)";
		else if (recTypeHi == 0xA0)
			recordEyecatcher = "DATE (0xa0)";
		else if (recTypeHi == 0xC0)
			recordEyecatcher = "DOCS (0xc0)";
		else if (recTypeHi == 0xE0)
			recordEyecatcher = "DEXT (0xe0)";
		else if (recTypeHi == 0xE1)
			recordEyecatcher = "TXHD (0xe1)";
		else if (recTypeHi == 0xE2)
			recordEyecatcher = "FDAT (0xe2)";
		else if (recTypeHi == 0xE8)
			recordEyecatcher = "TEXT (0xe8)";
		return recordEyecatcher;
	}

	public static int emitRecord(byte inData[], int offset, boolean dive, int debugLevel)
	{
		String ec = getRecordEyecatcher(inData, offset);
		int ret = 0;
		int recLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
		if (!ec.equals("----"))
		{
			if (debugLevel > 0)
			{
				System.err.print("0x" + Integer.toHexString(0x1000000 | offset).substring(1) + ": " + ec);
				System.err.println(" with length: 0x" + Integer.toHexString(0x10000 | UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1])).substring(1));
			}
			if (ec.equals("DEXT (0xe0)"))
			{
				// Note - DEXT entries will not have others following it in the same sector 
				for (int q = 4; q < recLen; q += 4)
				{
					int newRec = UnsignedByte.intValue(inData[offset + q]) * 65536;
					newRec += UnsignedByte.intValue(inData[offset + q + 1]) * 256;
					newRec += UnsignedByte.intValue(inData[offset + q + 2]);
					if ((newRec < inData.length) && (newRec > 0))
					{
						if (debugLevel > 0)
						{
							System.err.print("0x" + Integer.toHexString(0x1000000 | offset).substring(1) + "  Pointer: 0x" + Integer.toHexString(0x1000000 | newRec).substring(1) + ": ");
							if (newRec == locEHL1)
								System.err.print("(Won't follow since it's the EHL1 pointer)");
							System.err.println();
						}
						if ((dive) && (newRec != locEHL1))
							emitRecord(inData, newRec, true, debugLevel);
					}
				}
				// Note - DEXT entries will not have others following it in the same sector 
			}
			else if (ec.equals("DSL2 (0x60)"))
			{
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("TXHD (0xe1)"))
			{
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("EHL1 (0x20)"))
			{
				// Dig out the EHL1 length, return it when we pop back out
				ret = UnsignedByte.intValue(inData[offset + 0x10]) * 65536 + UnsignedByte.intValue(inData[offset + 0x11]) * 256 + UnsignedByte.intValue(inData[offset + 0x12]);
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("ABM  (0x40)"))
			{
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("TXHD (0xe1)"))
			{
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("FDAT (0xe2)"))
			{
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("NAME (0x80)"))
			{
				String newName = EbcdicUtil.toAscii(inData, offset + 4, 44).trim().replace("\\", "-").replace("/", "-").replace("?", "-");
				if (debugLevel > 0)
					System.err.println("  Document name: [" + newName + "]");
				else
				{
					if (currentOut != null)
					{
						try
						{
							currentOut.flush();
							currentOut.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					try
					{
						System.err.println("Creating file: " + baseName+newName);
						currentOut = new FileOutputStream(baseName+newName);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("DATE (0xa0)"))
			{
				if (debugLevel > 0)
					System.err.println("  Date data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("DOCS (0xc0)"))
			{
				if (debugLevel > 0)
					System.err.println("  Docs data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
				if (dive)
					emitRecord(inData, offset + recLen, dive, debugLevel);
			}
			else if (ec.equals("TEXT (0xe8)"))
			{
				if (debugLevel > 0)
				{
					System.err.println("  Text data:");
					System.err.println(EbcdicUtil.toAscii(inData, offset+5, recLen-5).trim());
				}
				else
				{
					try
					{
						// Write out this text record, skipping the header
						for (int i = offset+5; i < offset+recLen; i++)
						{
							if (inData[i] == 0x2b) // Control Sequence Prefix (CSP)
							{
								// We're inside a Control Sequence Prefix... branch around the length
								i += UnsignedByte.intValue(inData[i+2]) + 1;
							}
							else
								currentOut.write(inData[i]);
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}

	public static int dumpBareRecord(byte inData[], int offset)
	{
		String ec = getRecordEyecatcher(inData, offset);
		int recLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
		if (!ec.equals("----"))
		{
			emitRecord(inData, offset, false, 2);
			offset += recLen;
		}
		else
		{
			System.err.print("0x" + Integer.toHexString(0x1000000 | offset).substring(1) + ": " + ec);
			// No known type... check for zero-ness
			int delta = offset % 256;
			if (delta > 0)
			{
				// "next" length is zero... so skip to the end of this sector
				offset += (256 - delta);
				System.err.println(" (----)");
				// Want to see the data in this sector?  Uncomment the following:
				// System.err.println(EbcdicUtil.toAscii(inData, offset, 256 - delta));
			}
			else
			{
				boolean foundOne = false;
				for (int i = offset; i < offset + delta; i++)
				{
					if (inData[i] != 0x00)
					{
						foundOne = true;
						System.err.println(" Found non-zero data.  This is probably bad.");
					}
				}
				if (!foundOne)
					System.err.print(" (----)");
				System.err.println();
				System.err.println(EbcdicUtil.toAscii(inData, offset, 256 - delta));
			}
		}
		return offset;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractDisplaywriterFiles " + Version.VersionString + " - Extract files from Displaywriter word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractDisplaywriterFiles infile [out_directory]|[-debug1]|[-debug2]");
	}
}