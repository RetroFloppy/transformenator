/*
 * Transformenator - perform transformation operations on files
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

package org.transformenator.detanglers;

import java.util.Arrays;

import org.transformenator.internal.EbcdicUtil;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Displaywriter extends ADetangler
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		_interpreter = interpreter;
		if (fileSuffix.length() > 0)
		{
			if (!fileSuffix.startsWith("."))
				_fileSuffix = "." + fileSuffix;
			else
				_fileSuffix = fileSuffix;
		}
		else
			_fileSuffix = "";
		newBuf = new byte[inData.length];
		for (int i = 0; i < inData.length; i += 128)
		{
			if (getRecordEyecatcher(inData, i).equals("EHL1 (0x20)"))
			{
				if (debugLevel == 1)
					System.err.println("Searching, found EHL1 at raw 0x" + Integer.toHexString(i));
				int total = UnsignedByte.intValue(inData[i + 0x12], inData[i + 0x11]);
				if (debugLevel == 1)
					System.err.println("  total EHL1 length = 0x" + Integer.toHexString(total));
				if (total > 0)
				{
					locEHL1 = i;
					break;
				}
			}
		}
		if (locEHL1 > -1)
		{
			if (inData.length > (0x75000 * 2))
			{
				// Clip off the first cylinder of image
				delta = locEHL1 - 0x75000;
				locEHL1 = 0x75000;
			}
			else
			{
				// Clip off the first cylinder of image
				delta = locEHL1 - 0x21c00;
				locEHL1 = 0x21c00;
			}
		}
		else
		{
			if (debugLevel == 1)
				System.err.println("No EHL1 record found after exhaustive search.");
		}
		if (debugLevel == 3)
			delta = 0;
		clippedImage = Arrays.copyOfRange(inData, delta, inData.length - delta);
		/*
		 * There are three modes of operation (debugLevel):
		 * 
		 * 0) Extract and create all files found; names will be ASCII, content will be EBCDIC
		 * 
		 * 1) Starting with the EHL1 record, follow all records and pointers, dumping contents
		 * 
		 * 2) Scrape the entire disk image and dump out all records found
		 * 
		 * 3) Full emergency recovery: scrape the entire disk image and dump out just text records found
		 */
		if ((clippedImage != null) && (locEHL1 > -1))
		{
			int offset = locEHL1;
			int total = offset + processRecord(clippedImage, outDirectory, inFile, offset, true, debugLevel);
			boolean done = false;
			while (!done)
			{
				offset += 256;
				if (getRecordEyecatcher(clippedImage, offset).equals("DEXT (0xe0)"))
				{
					processRecord(clippedImage, outDirectory, inFile, offset, true, debugLevel);
				}
				if (offset > total)
				{
					done = true;
				}
			}
			startFile(outDirectory, inFile, "");
		}
		else
		{
			// No textual data found
			if (debugLevel == 1)
				System.err.println("Unable to locate an EHL1 record anywhere on this image.");
			else
				System.err.println("Unable to find DisplayWriter file structure on this image.");
		}

	}

	public int processRecord(byte inData[], String outDirectory, String inFile, int offset, boolean dive, int debugLevel)
	{
		String ec = getRecordEyecatcher(inData, offset);
		int ret = 0;
		if (offset + 4 < inData.length)
		{
			int recLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
			if (!ec.equals("----"))
			{
				if ((debugLevel == 1) || (debugLevel == 2))
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
						if (debugLevel > 0)
							System.err.println("   DEXT newRec: 0x" + Integer.toHexString(0x1000000 | newRec).substring(1));
						if ((newRec < inData.length) && (newRec > 0))
						{
							if (debugLevel > 0)
							{
								System.err.print("0x" + Integer.toHexString(0x1000000 | offset + q).substring(1) + ": Pointer: 0x" + Integer.toHexString(0x1000000 | newRec).substring(1) + ": ");
								if (newRec == locEHL1)
									System.err.print("(Won't follow since it's the EHL1 pointer)");
								System.err.println();
							}
							if ((dive) && (newRec != locEHL1))
								processRecord(inData, outDirectory, inFile, newRec, true, debugLevel);
						}
					}
					// Note - DEXT entries will not have others following it in the same sector
				}
				else if (ec.equals("EHL1 (0x20)"))
				{
					// Dig out the EHL1 length, return it when we pop back out
					ret = UnsignedByte.intValue(inData[offset + 0x10]) * 65536 + UnsignedByte.intValue(inData[offset + 0x11]) * 256 + UnsignedByte.intValue(inData[offset + 0x12]);
					if (dive)
						processRecord(inData, outDirectory, inFile, offset + recLen, dive, debugLevel);
				}
				else if (ec.equals("NAME (0x80)"))
				{
					String newName = EbcdicUtil.toAscii(inData, offset + 4, 44).trim().replace("\\", "-").replace("/", "-").replace("?", "-");
					if (debugLevel > 0)
						System.err.println("  Document name: [" + newName + "]");
					else
					{
						startFile(outDirectory, inFile, newName);
					}
					if (dive)
						processRecord(inData, outDirectory, inFile, offset + recLen, dive, debugLevel);
				}
				else if (ec.equals("DATE (0xa0)"))
				{
					if (debugLevel > 0)
						System.err.println("  Date data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
					if (dive)
						processRecord(inData, outDirectory, inFile, offset + recLen, dive, debugLevel);
				}
				else if (ec.equals("DOCS (0xc0)"))
				{
					if (debugLevel > 0)
						System.err.println("  Docs data: [" + EbcdicUtil.toAscii(inData, offset, recLen).trim() + "]");
					if (dive)
						processRecord(inData, outDirectory, inFile, offset + recLen, dive, debugLevel);
				}
				else if (ec.equals("TEXT (0xe8)"))
				{
					if (debugLevel > 0)
					{
						if ((debugLevel == 1) || (debugLevel == 2))
						{
							System.err.println("  Text data:");
						}
						System.err.println(EbcdicUtil.toAscii(inData, offset + 5, recLen - 5).trim());
					}
					else
					{
						// Write out this text record, skipping the header
						for (int i = offset + 5; i < offset + recLen; i++)
						{
							if (inData[i] == 0x2b) // Control Sequence Prefix (CSP)
							{
								// We're inside a Control Sequence Prefix... branch around the length
								i += UnsignedByte.intValue(inData[i + 2]) + 1;
							}
							else
							{
								if (currentName == "")
									startFile(outDirectory, inFile, "FileRecovery");
								newBuf[newBufCursor++] = inData[i];
							}
						}
					}
				}
				else
				{
					if (dive)
						processRecord(inData, outDirectory, inFile, offset + recLen, dive, debugLevel);
				}
			}
			else
			{
				if (debugLevel > 0)
					System.err.println("   BAD record length out of bounds");
			}
		}
		return ret;
	}

	public void startFile(String outDirectory, String inFile, String name)
	{
		if (newBufCursor > 0)
		{
			// System.err.println("DEBUG: Displaywriter finishing file: \"" + currentName + "\" with length "+newBufCursor);
			// Clip out the "file", send to parent for interpretation
			currentFile = Arrays.copyOfRange(newBuf, 0, newBufCursor);
			_interpreter.emitFile(currentFile, outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), currentName + _fileSuffix);
			currentFile = null;
		}
		//if ((name != null) && (name.length() > 0))
		//	System.err.println("DEBUG: Displaywriter starting file: \"" + name + "\"");
		newBufCursor = 0;
		currentName = name;
	}

	public int dumpBareRecord(byte inData[], String outDirectory, String inFile, int offset, int debugLevel)
	{
		String ec = getRecordEyecatcher(inData, offset);
		int recLen = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
		if (!ec.equals("----"))
		{
			if ((debugLevel == 2) || ((debugLevel == 3) && (ec.equals("TEXT (0xe8)"))))
				processRecord(inData, outDirectory, inFile, offset, false, debugLevel);
			offset += recLen;
		}
		else
		{
			if (debugLevel == 2)
			{
				System.err.print("0x" + Integer.toHexString(0x1000000 | offset).substring(1) + ": " + ec);
			}
			// No known type... check for zero-ness
			int delta = offset % 256;
			if (delta > 0)
			{
				// "next" length is zero... so skip to the end of this sector
				offset += (256 - delta);
				if (debugLevel == 2)
				{
					System.err.println(" (----)");
					// Want to see the data in this sector? Uncomment the following:
					// System.err.println(EbcdicUtil.toAscii(inData, offset, 256 - delta));
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
						System.err.println(" Found non-zero data.  This is probably bad.");
					}
				}
				if (debugLevel == 2)
				{
					if (!foundOne)
						System.err.print(" (----)");
					System.err.println();
					System.err.println(EbcdicUtil.toAscii(inData, offset, 256 - delta));
				}
			}
		}
		return offset;
	}

	public static String getRecordEyecatcher(byte inData[], int offset)
	{

    String recordEyecatcher = "----";

		if (offset+3 < inData.length)
		{
			int recLength = UnsignedByte.intValue(inData[offset + 0]) * 256 + UnsignedByte.intValue(inData[offset + 1]);
			int recTypeHi = UnsignedByte.intValue(inData[offset + 2]);
			int recTypeLo = UnsignedByte.intValue(inData[offset + 3]);
			//		String recordEyecatcher = "---- (0x"+Integer.toHexString(0x100 | recTypeHi).substring(1)+")";
			if ((recTypeHi == 0x20) && (recTypeLo == 0x00) && (recLength == 0x19))
				recordEyecatcher = "EHL1 (0x20)";
			else if ((recTypeHi == 0x40) && (recTypeLo == 0x00))
				recordEyecatcher = "ABM  (0x40)"; // Allocation bitmap
			else if ((recTypeHi == 0x60) && (recTypeLo == 0x00))
				recordEyecatcher = "DSL2 (0x60)"; // Document header
			else if (recTypeHi == 0x80)
				recordEyecatcher = "NAME (0x80)"; // Document name
			else if (recTypeHi == 0xA0)
				recordEyecatcher = "DATE (0xa0)"; // Document date
			else if (recTypeHi == 0xC0)
				recordEyecatcher = "DOCS (0xc0)"; // Document starting lines
			else if (recTypeHi == 0xE0)
				recordEyecatcher = "DEXT (0xe0)"; // Disk extent table
			else if (recTypeHi == 0xE1)
				recordEyecatcher = "TXHD (0xe1)"; // Text header
			else if (recTypeHi == 0xE2)
				recordEyecatcher = "FDAT (0xe2)"; // Formatting data
			else if (recTypeHi == 0xE3)
				recordEyecatcher = "E304 (0xe3)"; // Text wrapper of some sort
			else if (recTypeHi == 0xE5)
				recordEyecatcher = "E504 (0xe5)"; // Text wrapper of some sort
			else if (recTypeHi == 0xE8)
				recordEyecatcher = "TEXT (0xe8)"; // Text data
		}
		return recordEyecatcher;
	}

	FileInterpreter _interpreter = null;
	int debugLevel = 0; // Debug levels: 0 = none; 1 = follow EHL1 chain; 2 = dump all sectors of disk image; 3 = scan entire disk for text records
	byte[] clippedImage = null;
	byte[] newBuf, currentFile = null;
	int newBufCursor = 0;
	int locEHL1 = -1;
	int delta = 0;
	String currentName = "";
	String _fileSuffix = "txt";
}
