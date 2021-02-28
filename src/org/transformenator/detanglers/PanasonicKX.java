/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2015 - 2018 by David Schmidt
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class PanasonicKX extends ADetangler
{
	static int SIZE_SS = 0;
	static int SIZE_DS = 1;
	static int zeroesBegin[] = { 0x0c, 0x0c }; // + 0x09
	static int fileHeadOffset[] = { 0x1110, 0x1510 };
  static int headerLength[] = {0x110, 0x510 };

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
    List<Integer> sectorChain = new ArrayList<Integer>();
		ByteArrayOutputStream currentOut = null;
		// Panasonic disks came in SS and DS sizes.  Inspect the image size; if it's larger than SS * 1.5, it's DS.
		int diskSize = inData.length > (360 * 1024 * 1.5)? SIZE_DS: SIZE_SS;
		for (int i = 0x203; i < 0x600; i+=0x03)
		{
		  // Skip zeroes in case they are interspersed
		  if ((UnsignedByte.intValue(inData[i]) == 0x00) &&
		      (UnsignedByte.intValue(inData[i+1]) == 0x00) &&
		      (UnsignedByte.intValue(inData[i+2]) == 0x00))
		    continue;
		  decodeSectors(inData, i, sectorChain);
		}
		int f = 1;
		Iterator<Integer> iter = sectorChain.iterator();

		/*
		currentSector = iter.next();
    if (currentSector == 0xfff)
    */
		for (int i = 0xa00; i < 0x1c00; i+=0x20)
		{
			String filename = "";
			int currentSector = 0;
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
					//System.err.println("File: "+filename+" File type: "+fileTypeString(filetype));
					currentOut = new ByteArrayOutputStream();
					int fileHead = UnsignedByte.intValue(inData[i+0x1a],inData[i+0x1b]);
					int fileLength = UnsignedByte.intValue(inData[i+0x1c],inData[i+0x1d]);
					fileLength = fileLength - 0x100;
					dumpSector(currentOut, inData, fileHead, diskSize, true);
					while (iter.hasNext())
					{
					  currentSector = iter.next();
					  if (currentSector != 0xfff)
					    dumpSector(currentOut, inData, currentSector, diskSize, false);
					  else
					    break;
					}
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

	void dumpSector(ByteArrayOutputStream currentOut, byte inData[], int sector, int diskSize, boolean skipHeader)
	{
	  int offset = sector * 0x400 + 0x1000;
	  int length = 0x400;
	  // If this is the fist sector, skip over the header
	  if (skipHeader)
	  {
	    length -= headerLength[diskSize];
	    offset += headerLength[diskSize];
	  }
	  // System.out.println("About to dump from offset 0x"+Integer.toHexString(offset)+" for 0x"+Integer.toHexString(length)+" bytes.");
	  currentOut.write(inData, offset, length);
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

	void decodeSectors(byte inData[], int offset, List<Integer> fileChain)
	{
	  byte b1n1, b1n2, b2n1, b2n2, b3n1, b3n2;
	  byte nb1, nb2, nb3, nb4;
	  int sector1, sector2;
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
    if ((sector1 > 0) && (sector1 != 0x800))
      fileChain.add(sector1);
    sector2 = UnsignedByte.intValue(nb4, nb3);
    if ((sector2 > 0) && (sector2 != 0x800))
      fileChain.add(sector2);
    /*
    System.out.println("BAM record: 0x"+UnsignedByte.toString(inData[offset])+UnsignedByte.toString(inData[offset+1])+UnsignedByte.toString(inData[offset+2])+" (0x"+Integer.toHexString(sector1)+", 0x"+Integer.toHexString(sector2)+")");
    if (sector1 == 0xfff)
      System.out.println("^^^ File separator");
    if (sector1 == 0x800)
      System.out.println("END OF TABLE");
    if (sector2 == 0x800)
      System.out.println("END OF TABLE");
    */
	}
}