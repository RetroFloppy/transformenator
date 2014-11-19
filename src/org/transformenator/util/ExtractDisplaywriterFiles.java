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
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

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
			/*
			 * Two modes of operation:
			 * 
			 * 1) dump all sectors and records out one by one Increment by the
			 * remainder if the remainder is not zero ... mention if the bytes
			 * of the remainder are not zero
			 * 
			 * 2) start at the center record and follow all the links
			 */
			if (inData != null)
			{
				int track0Offset = 0; //3328;
				System.err.println("Read " + inData.length + " bytes.");
				if (inData.length > 1000000)
				{
					// track0Offset = 3328 * 3;
				}
				// Loop over the entire disk looking for records
				int i = 0, j = 0;
				int delta = 0;
				/*
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
							//i += delta;
							// delta is non-zero; that means i moved already
						}
					}
				}
				*/
				int offset = 0x021c00;
				int total = offset + emitRecord(inData, offset, true);
				boolean done = false;
				while (!done)
				{
					offset += 256;
					if (getRecordEyecatcher(inData, offset).equals("DEXT (0xe0)"))
					{
						emitRecord(inData, offset, true);
					}
					if (offset > total)
					{
						done = true;
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

	public static String getPointerType(byte inData[], int offset)
	{
		String retString = "";
		int newRecType = UnsignedByte.intValue(inData[offset + 2]) * 256 + UnsignedByte.intValue(inData[offset + 3]);
		int newRecLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
		retString = getRecordEyecatcher(inData, offset);
		retString += "  Type: 0x" + Integer.toHexString(0x10000 | newRecType).substring(1);
		retString += " Length: 0x" + Integer.toHexString(0x1000000 | newRecLen).substring(1);
		return retString;
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

	public static boolean emitTextRecord(byte inData[], int offset)
	{
		boolean foundEnd = false;
		int theEnd = 256;
		for (int i = 0; i < 256; i++)
		{
			if (inData[offset + i] == 0x0c)
			{
				foundEnd = true;
				theEnd = i;
			}
		}
		System.err.println(EbcdicUtil.toAscii(inData, offset, theEnd));
		return foundEnd;
	}

	public static int emitRecord(byte inData[], int offset, boolean dive)
	{
		String ec = getRecordEyecatcher(inData, offset);
		int ret = 0;
		int recLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
		if (!ec.equals("----"))
		{
			System.err.print("0x" + Integer.toHexString(0x1000000 | offset).substring(1) + ": " + ec);
			System.err.println(" with length: 0x" + Integer.toHexString(0x10000 | UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1])).substring(1));
			if (ec.equals("DEXT (0xe0)"))
			{
				// Note - DEXT entries will not have others following it in the same sector 
				for (int q = 4; q < recLen; q += 4)
				{
					int newRec = UnsignedByte.intValue(inData[offset + q]) * 65536;
					newRec += UnsignedByte.intValue(inData[offset + q + 1]) * 256;
					newRec += UnsignedByte.intValue(inData[offset + q + 2]);
					if ((newRec < inData.length) && (newRec > 0) && (newRec != 0x021c00))
					{
						System.err.println("0x" + Integer.toHexString(0x1000000 | offset).substring(1) +"  Pointer: 0x" + Integer.toHexString(0x1000000 | newRec).substring(1) + ": ");
						if (dive)
							emitRecord(inData, newRec, true);
					}
				}
				// Note - DEXT entries will not have others following it in the same sector 
			}
			else if (ec.equals("DSL2 (0x60)"))
			{
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("TXHD (0xe1)"))
			{
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("EHL1 (0x20)"))
			{
				ret = UnsignedByte.intValue(inData[offset + 0x10]) * 65536+
				UnsignedByte.intValue(inData[offset + 0x11]) * 256+
				UnsignedByte.intValue(inData[offset + 0x12]);
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("ABM  (0x40)"))
			{
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("TXHD (0xe1)"))
			{
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("FDAT (0xe2)"))
			{
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("NAME (0x80)"))
			{
				System.err.println("  Document name: [" + EbcdicUtil.toAscii(inData, offset + 4, 44).trim() + "]");
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("DATE (0xa0)"))
			{
				System.err.println("  Date data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("DOCS (0xc0)"))
			{
				System.err.println("  Docs data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
				emitRecord(inData, offset + recLen, dive);
			}
			else if (ec.equals("TEXT (0xe8)"))
			{
				System.err.println("  Text data:");
				System.err.println(EbcdicUtil.toAscii(inData, offset, recLen).trim());
			}
		}
		return ret;
	}

	public static int dumpBareRecord(byte inData[], int offset)
	{
		String ec = getRecordEyecatcher(inData, offset);
		int recLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
		System.err.print("0x" + Integer.toHexString(0x1000000 | offset).substring(1) + ": " + ec);
		if (!ec.equals("----"))
		{
			System.err.println(" with length: 0x" + Integer.toHexString(0x10000 | UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1])).substring(1));
			if (ec.equals("DEXT (0xe0)"))
			{
				// Note - DEXT entries will not have others following it in the same sector 
				for (int q = 4; q < recLen; q += 4)
				{
					int newRec = UnsignedByte.intValue(inData[offset + q]) * 65536;
					newRec += UnsignedByte.intValue(inData[offset + q + 1]) * 256;
					newRec += UnsignedByte.intValue(inData[offset + q + 2]);
					if ((newRec < inData.length) && (newRec > 0) && (newRec != 0x021c00))
					{
						System.err.println("  Pointer: 0x" + Integer.toHexString(0x1000000 | newRec).substring(1) + ": ");
					}
				}
				// Note - DEXT entries will not have others following it in the same sector 
				offset += recLen;
			}
			else if (ec.equals("DSL2 (0x60)"))
			{
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("TXHD (0xe1)"))
			{
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("EHL1 (0x20)"))
			{
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("ABM  (0x40)"))
			{
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("TXHD (0xe1)"))
			{
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("FDAT (0xe2)"))
			{
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("NAME (0x80)"))
			{
				System.err.println("  Document name: [" + EbcdicUtil.toAscii(inData, offset + 4, 44).trim() + "]");
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("DATE (0xa0)"))
			{
				System.err.println("  Date data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("DOCS (0xc0)"))
			{
				System.err.println("  Docs data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
				offset = dumpBareRecord(inData, offset + recLen);
			}
			else if (ec.equals("TEXT (0xe8)"))
			{
				System.err.println("  Text data:");
				System.err.println(EbcdicUtil.toAscii(inData, offset, recLen).trim());
				offset += recLen;
			}
		}
		else
		{
			// No known type... check for zero-ness
			int delta = offset % 256;
			if ((delta > 0) && (delta <= 253))
			{
				if ((inData[offset + 0] == 0x00) && (inData[offset + 1] == 0x00) && (inData[offset + 2] == 0x00) && (inData[offset + 3] == 0x00))
				{
					// "next" length is zero... so skip to the end of this sector
					offset += (256 - delta);
					System.err.println(" (----)");
				}
			}
			else
			{
				boolean foundOne = false;
				for (int i = offset; i < offset + delta; i++)
				{
					if (inData[i] != 0x00)
					{
						foundOne = true;
						System.err.println("non-zero data.");
					}
				}
				if (!foundOne)
					System.err.print(" (----)");
				System.err.println();
			}
		}
		return offset;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractDisplaywriterFiles " + Version.VersionString + " - Extract files from Displaywriter word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractDisplaywriterFiles infile [out_directory]");
	}
}