/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2015 - 2025 by David Schmidt
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class PanasonicKX extends ADetangler
{
	static int SIZE_SS = 0;
	static int SIZE_DS = 1;
	static int zeroesBegin[] = { 0x0c, 0x0c }; // + 0x09
	static int topOfBAM[] = { 0x600, 0x800 }; // BAM starts on both disk sizes at 0x200
	static int directoryStart[] = { 0xa00, 0xe00 };
	static int directoryEnd[] = { 0x1800, 0x1c00 };
	static int sectorAddrOffset[] = { 3, 4 };

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		// isDebugMode = true;
		ByteArrayOutputStream currentOut = null;
		// Panasonic disks came in SS and DS sizes.  Inspect the image size; if it's larger than SS * 1.5, it's DS.
		int diskSize = inData.length > (360 * 1024 * 1.5)? SIZE_DS: SIZE_SS;
		int sectorMap[] = new int[topOfBAM[diskSize]-0x200 / 3];

		loadBAM(inData, sectorMap, isDebugMode);
		for (int i = directoryStart[diskSize]; i < directoryEnd[diskSize]; i+=0x20)
		{
			String filename = "";
			int bamEntry = UnsignedByte.intValue(inData[i+0x1a],inData[i+0x1b]);
			int fileLength = UnsignedByte.intValue(inData[i+0x1c],inData[i+0x1d]);
			int j, result = 0;
			for (j = zeroesBegin[diskSize]; j < zeroesBegin[diskSize] + 0x09; j++)
				result += inData[i+j];
			filename = "";
			String filenameSuffix = "";
			if (UnsignedByte.intValue(inData[i]) == 0xe5)
			{
				filenameSuffix = " (deleted)";
			}
			int filetype = UnsignedByte.intValue(inData[i+0x0a]);
			for (j = 0x00; j < 0x0a; j++)
			{
				if (UnsignedByte.intValue(inData[i+j]) != 0xe5)
					filename += (char)inData[i+j];
			}
			filename = filename.trim()+filenameSuffix;
			if ((filename.length() > 0) && (result == 0))
			{
				try
				{
					if (isDebugMode)
						System.out.println("Found file: "+filename+" File type: "+fileTypeString(filetype) + " BAM entry: 0x"+Integer.toHexString(bamEntry)+" Length: 0x"+Integer.toHexString(fileLength));
					currentOut = new ByteArrayOutputStream();
					boolean isFirst = true;
					int bytesDumped = 0;
					do
					{
					  int inferredSector = sectorMap[bamEntry];
					  if (inferredSector == 0xfff)
					  {
						  // Just do this one (and likely stop?)
						  inferredSector = bamEntry + 1;
					  }
					  if (isDebugMode)
						  System.out.println("Dumping BAM entry 0x"+Integer.toHexString(bamEntry) + " which is sector 0x"+Integer.toHexString(inferredSector)+" BAM originally pointed to: 0x"+Integer.toHexString(sectorMap[bamEntry]));
					  dumpSector(currentOut, inData, inferredSector, diskSize, isFirst, isDebugMode);
					  isFirst = false;
					  bytesDumped += 400;
					  bamEntry++;
					  if (isDebugMode)
						  System.out.println("Next BAM entry is 0x"+Integer.toHexString(bamEntry)+" which is sector 0x"+Integer.toHexString(sectorMap[bamEntry]));
					} while ((bytesDumped < fileLength+0x400) && (sectorMap[bamEntry]) > 0);
					if (isDebugMode)
						System.out.println("...dumped 0x"+Integer.toHexString(bytesDumped)+" bytes total.");
					currentOut.flush();
					parent.emitFile(currentOut.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename + fileSuffix);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	void dumpSector(ByteArrayOutputStream currentOut, byte inData[], int sector, int diskSize, boolean skipHeader, boolean isDebugMode)
	{
		int offset = (sector + sectorAddrOffset[diskSize]) * 0x400; // Depending on disk size, the sector differential is either 3 or 4
		int length = 0x400;
		// If this is the fist sector, skip over the header
		if (isDebugMode)
			System.out.println("About to dump sector 0x"+Integer.toHexString(sector)+" from offset 0x"+Integer.toHexString(offset)+" for 0x"+Integer.toHexString(length)+" bytes.");
		if (skipHeader)
		{
			length -= 0x110; // Header is 0x110 on both disk sizes
			offset += 0x110;
			if (isDebugMode)
				System.out.println("Skipping header, really only dumping from offset 0x"+Integer.toHexString(offset)+" for 0x"+Integer.toHexString(length)+" bytes.");
		}
		currentOut.write(inData, offset, length);
		if (isDebugMode)
		{
			// Send out the whole sector as a string
			String sectorString = new String(Arrays.copyOfRange(inData, offset, offset+length), java.nio.charset.StandardCharsets.UTF_8);
			// System.out.println(sectorString);
			// System.out.println();
		}
	}

	String fileTypeString(int filetype)
	{
		String typeString = "unknown";
		switch (filetype)
		{
			case 0x20: typeString = "text";
			break;
			case 0x21:
			case 0x22:
			case 0x23:
				 typeString = "text w/mailmerge";
			break;
			case 0x24: typeString = "binary";
			break;
			default: typeString += " (0x"+Integer.toHexString(filetype)+")";
		}
		return typeString;
	}

	void loadBAM(byte inData[], int[] bam, boolean isDebugMode)
	{
		byte b1n1, b1n2, b2n1, b2n2, b3n1, b3n2;
		byte nb1, nb2, nb3, nb4;
		int sector1, sector2;
		int offset;

		for (int i = 0; i < bam.length/2; i+=2)
		{
			offset = 0x200 + ((i/2)*3);
			// {b1n1:b1n2,b2n1:b2n2,b3n1:b3n2}->{b2n2:b1n1,b1n2:b3n1,b3n2:b2n1}
			// 03 40 00 -> 00 30 04 (which is 0x0003 and 0x0004)
			// 0f 00 01 -> 00 f1 00 (which is 0x000F and 0x0010)
			b1n1 = UnsignedByte.hiNibble(inData[offset+0]);
			b1n2 = UnsignedByte.loNibble(inData[offset+0]);
			b2n1 = UnsignedByte.hiNibble(inData[offset+1]);
			b2n2 = UnsignedByte.loNibble(inData[offset+1]);
			b3n1 = UnsignedByte.hiNibble(inData[offset+2]);
			b3n2 = UnsignedByte.loNibble(inData[offset+2]);
			nb1 = UnsignedByte.composeByte((byte) 0x00, b2n2);
			nb2 = UnsignedByte.composeByte(b1n1, b1n2);
			nb3 = UnsignedByte.composeByte((byte) 0x00, b3n1);
			nb4 = UnsignedByte.composeByte(b3n2,b2n1);
			sector1 = UnsignedByte.intValue(nb2, nb1);
			// System.out.println("Adding sector 0x"+Integer.toHexString(sector1));
			bam[i] = sector1;
			sector2 = UnsignedByte.intValue(nb4, nb3);
			// System.out.println("Adding sector 0x"+Integer.toHexString(sector2));
			bam[i+1] = sector2;
			// System.out.println("BAM record: 0x"+UnsignedByte.toString(inData[offset])+UnsignedByte.toString(inData[offset+1])+UnsignedByte.toString(inData[offset+2])+" (0x"+Integer.toHexString(sector1)+", 0x"+Integer.toHexString(sector2)+")");
		}
		int j = 0;
		if (isDebugMode)
		{
			System.out.println("BAM table:\n");
			for (short i = 0; i < bam.length; i++)
			{
				int localSum = 0;
				if (i == 0)
				{
					System.out.println("        00     01     02     03     04     05     06     07     08     09     0a     0b     0c     0d     0e     0f");
					System.out.print("0000  ");
				}
		    short value = (short)bam[i];
		    String hexString = String.format("%04x", value & 0xFFFF);
		    localSum += bam[i];
				System.out.print("0x"+hexString+" ");
				if (j++ > 14)
				{
					j=0;
					if (localSum == 0) // Stop dumping out the table after a full row of zeroes
						break;
					localSum = 0;
					System.out.println();
					hexString = String.format("%04x", (i + 1) & 0xFFFF);
					System.out.print(hexString+"  ");
				}
			}
			System.out.println("\n");
		}
	}
}
