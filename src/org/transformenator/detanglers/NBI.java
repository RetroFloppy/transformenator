/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2014 - 2018 by David Schmidt
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

import java.io.ByteArrayOutputStream;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class NBI extends ADetangler
{
	/*
	 * NBI disks have sectors with different roles, specified by the second byte of a 256-byte sector:
	 * 
	 * Sector headers:
	 * 0x<len>00: empty
	 * 0x<len>44: directory entry
	 * 0x<len>48: file informational data
	 * 0x<len>58: system
	 * 0x<len>4c: text
	 * 
	 * Sector structure, low density disks (complete image should be 315,136 bytes):
	 *
	 * Offset Length Meaning
	 * ====== ====== ==============
	 * 0x00   1      Data length
	 * 0x01   1      Sector type
	 * 0x02   1      MSB sector number (nibble)
	 * 0x03   1      Previous sector number
	 * 0x04   1      Next sector number
	 * 
	 * Sector structure, high density disks (complete image should be 1,097,728 bytes):
	 *
	 * Offset Length Meaning
	 * ====== ====== ==============
	 * 0x00   1      Data length
	 * 0x01   1      Sector type
	 * 0x02   2      Previous sector number
	 * 0x04   2      Next sector number
	 *
	 * The final two bytes of a sector are not part of text sectors.
	 * 
 	 */

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		int key = 0;
		boolean isHighDensity = false;

		if (inData.length > 500000)
			isHighDensity = true;
		ByteArrayOutputStream out = null;
		for (int sector = 0; sector < inData.length / 256; sector++)
		{
			if (UnsignedByte.intValue(inData[sector * 256 + 1]) == 0x4c)
			{
				if (calcPrevOffset(sector, inData, isHighDensity) == 0)
				{
					String fileName = new String("recovered_file_" + ++key);
					out = new ByteArrayOutputStream();
					dumpSectorChain(sector, inData, out, isHighDensity);
					parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
				}
			}
		}
		// scrapeSurface(inData, isHighDensity);
	}

	public static int calcPrevOffset(int sector, byte[] inData, boolean isHighDensity)
	{
		return calcSectorOffset(sector, false, inData, isHighDensity);
	}

	public static int calcNextOffset(int sector, byte[] inData, boolean isHighDensity)
	{
		return calcSectorOffset(sector, true, inData, isHighDensity);
	}

	public static int calcSectorOffset(int sector, boolean next, byte[] inData, boolean isHighDensity)
	{
		int offsetHi, offsetLo, result;
		if (isHighDensity)
		{
			if (next) // bytes 4,5 are next ptr
			{
				offsetHi = UnsignedByte.intValue(inData[sector * 256 + 4]);
				offsetLo = UnsignedByte.intValue(inData[sector * 256 + 5]);
			}
			else // bytes 2,3 are prev ptr
			{
				offsetHi = UnsignedByte.intValue(inData[sector * 256 + 2]);
				offsetLo = UnsignedByte.intValue(inData[sector * 256 + 3]);
			}
			if ((offsetLo == 0) && (offsetHi == 0))
				result=0;
			else if ((offsetLo == 255) && (offsetHi == 255))
				result=0;
			else
			{
				result = (offsetHi * 256 + offsetLo) * 256 - 0xc00;
			}
		}
		else // is low density
		{
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
					offsetHi--;
				}
			}
			else
				offsetLo--;
			result = offsetHi * 65536 + offsetLo * 256;
		}
		return result;
	}

	static void dumpSectorChain(int sector, byte[] inData, ByteArrayOutputStream out, boolean isHighDensity)
	{
		int length = UnsignedByte.intValue(inData[sector * 256]);
		//String offsetStr = "0x"+(Integer.toHexString(sector).toUpperCase())+"00";
		//System.err.println();
		//System.err.print(offsetStr+" next: 0x"+Integer.toHexString(calcNextOffset(sector,inData)).toUpperCase());
		//System.err.print(" prev: 0x"+Integer.toHexString(calcPrevOffset(sector,inData)).toUpperCase());
		//System.err.println(" length: 0x"+Integer.toHexString(length).toUpperCase());

		out.write(inData, (sector * 256 + 5), length);
		int nextSector = calcNextOffset(sector, inData, isHighDensity);
		if ((nextSector > 0) && (nextSector/256 != sector))
			dumpSectorChain(nextSector / 256, inData, out, isHighDensity);
	}

	static void scrapeSurface(byte[] inData, boolean isHighDensity)
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
				String offsetStr = "0x" + (Integer.toHexString(offset).toUpperCase());
				System.err.print(offsetStr + " Text sector; next: 0x" + Integer.toHexString(calcNextOffset(sector, inData, isHighDensity)).toUpperCase());
				System.err.print(" prev: 0x" + Integer.toHexString(calcPrevOffset(sector, inData, isHighDensity)).toUpperCase());
				System.err.print(" length: 0x" + Integer.toHexString(UnsignedByte.intValue(inData[sector * 256])).toUpperCase());
				System.err.print(" Data: 0x");
				for (x = 0; x < 14; x++)
				{
					System.err.print(UnsignedByte.toString(inData[sector * 256 + x]) + " ");
				}
				System.err.println();
				int begin = 5, end = 253; // Start with low density specs by default
				if (isHighDensity) // Use high density if required
				{
					begin ++;
					end ++;
				}
				for (x = begin; x < end; x++)
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
					char ch = (char) chi;
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
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
				System.err.print(offsetStr + " System sector: 0x");
				offset = sector * 256;
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
				for (x = 2; x < 12; x++)
				{
					System.err.print(UnsignedByte.toString(inData[sector * 256 + x]) + " ");
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
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
				System.err.print(offsetStr + " Directory sector:");
				System.err.println();
				offset = sector * 256;
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
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
			case 0x48:
				// File information sector
				offset = sector * 256;
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
				System.err.print(offsetStr + " File information sector:");
				System.err.println();
				offset = sector * 256;
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
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
				for (x = 3; x < 255; x++)
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
					char ch = (char) chi;
					System.err.print(ch);
				}
				System.err.println();
				System.err.println();
				break;
			default:
				// Other sector
				offset = sector * 256;
				offsetStr = "0x" + Integer.toHexString(offset).toUpperCase();
				System.err.print(offsetStr + " Other sector; next: 0x" + Integer.toHexString(calcNextOffset(sector, inData, isHighDensity)).toUpperCase());
				System.err.print(" prev: 0x" + Integer.toHexString(calcPrevOffset(sector, inData, isHighDensity)).toUpperCase());
				System.err.print(" length: 0x" + Integer.toHexString(UnsignedByte.intValue(inData[sector * 256])).toUpperCase());
				System.err.print(" Data: 0x");
				for (x = 0; x < 12; x++)
				{
					System.err.print(UnsignedByte.toString(inData[sector * 256 + x]) + " ");
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
					char ch = (char) chi;
					System.err.print(ch);
				}
				System.err.println();
				System.err.println();
				break;
			}
		}
	}
}
