/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2017 - 2018 by David Schmidt
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
//import java.util.Calendar;

import org.transformenator.internal.EbcdicUtil;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class IBM8 extends ADetangler 
{
	public static boolean DEBUG = false;

	/*
	 * Pull the files off of the virtual file system of IBM 8" disks
	 * (typically coming from IBM System/32/34/36 or AS/400).
	 * Other disk types that have the "standard" IBM 8" FM header and the rest is
	 * their own thing may get some useful information from this - but disks that are
	 * not really IBM transfer material are typically better served with a more
	 * specialized extractor.
	 *
	 */
	public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		// Ready to go.  Time to face the music.
		int cylinderSize = 0;
		int indexCylinderSize = 0;
		int headSize = 0;
		int sectorSize = 0;
		int volOffset = 0x300;
		int fileStart = 0, fileLength = 0;

		if (inData.length == 1258496) // AS/400 / System/36 disks
			volOffset = 0x600; 
		// Track 0 side 0 sector 7 on each IBM formatted diskette contains the Volume Label.
		if (DEBUG)
		{
			System.err.println("System identifier: "+EbcdicUtil.toAscii(inData,volOffset+0x18,0x0c));
			System.err.println("Owner identifier:  "+EbcdicUtil.toAscii(inData,volOffset+0x25,0x0d));
		}
		switch (EbcdicUtil.asciiChar(inData[volOffset + 0x4b]))
		{
			case ' ' : sectorSize = 128; break;
			case '1' : sectorSize = 256; break;
			case '2' : sectorSize = 512; break;
			case '3' : sectorSize = 1024; break;
			default: 
				System.err.println("ERROR: Unable to determine sector size; sector identifier is ["+EbcdicUtil.asciiChar(inData[volOffset + 0x4b])+"] (0x"+Integer.toHexString(UnsignedByte.intValue(inData[volOffset + 0x4b]))+")");
				break;
		}
		switch (EbcdicUtil.asciiChar(inData[volOffset + 0x47]))
		{
			case ' ': indexCylinderSize = 3328;
				headSize = 0;
				if (DEBUG)
					System.err.print("IBM Diskette 1 format");
				if (sectorSize == 128)
					cylinderSize = sectorSize * 26;
				else if (sectorSize == 256)
					cylinderSize = sectorSize * 15;
				else if (sectorSize == 512)
					cylinderSize = sectorSize * 8;
				break;
			case '2': indexCylinderSize = 6656;
				headSize = 1;
				if (DEBUG)
					System.err.print("IBM Diskette 2 format");
				if (sectorSize == 128)
					cylinderSize = sectorSize * 26;
				else if (sectorSize == 256)
					cylinderSize = sectorSize * 15;
				break;
			case 'M': indexCylinderSize = 9984;
				headSize = 1;
				if (DEBUG)
					System.err.print("IBM Diskette 2D format");
				if (sectorSize == 256)
					cylinderSize = sectorSize * 26;
				else if (sectorSize == 512)
					cylinderSize = sectorSize * 15;
				else if (sectorSize == 1024)
					cylinderSize = sectorSize * 8;
				break;
			case 0xb6: indexCylinderSize = 13312;
				headSize = 1;
				if (DEBUG)
					System.err.print("IBM AS/400-System/36");
				cylinderSize = sectorSize * 8;
				break;
			default:
				System.err.println("ERROR: Unable to determine disk density; density identifier is ["+EbcdicUtil.asciiChar(inData[volOffset + 0x47])+"] (0x"+Integer.toHexString(UnsignedByte.intValue(inData[volOffset + 0x47]))+")");
				break;
		}
		if (inData.length == 1258496) // AS/400 / System/36 disks
		{
			indexCylinderSize = 13312;
		}
		cylinderSize *= (headSize + 1); // Double-sided gets double cylinder sized
		if ((sectorSize > 0) && (cylinderSize > 0) && (indexCylinderSize > 0))
		{
			if (DEBUG)
				System.err.println(", Sector size: "+sectorSize+" Sectors/Cyl: "+cylinderSize / ((headSize + 1) * sectorSize)+" Cylinder size: "+cylinderSize+" Index Cyl size: "+indexCylinderSize);
			int directoryOffset = volOffset + 128;
			int startCyl,startHead,startSector;
			int endCyl,endHead,endSector;
			for (int i = 0; i < indexCylinderSize - directoryOffset; i += 128)
			{
				// System.err.println(EbcdicUtil.asciiChar(inData[directoryOffset + i]));
				if (EbcdicUtil.asciiChar(inData[directoryOffset + i]) != 'D')
				{
					String fileName = EbcdicUtil.toAscii(inData,directoryOffset + i + 0x05,0x10).trim();
					try
					{
						startCyl = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x1c,2));
						startHead = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x1e,1));
						startSector = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x1f,2));
						endCyl = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x22,2));
						endHead = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x24,1));
						endSector = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x25,2));
						if (DEBUG)
						{
							System.err.print("Found a file ("+startCyl+"/"+startHead+"/"+startSector+" - ");
							System.err.println(endCyl+"/"+endHead+"/"+endSector+"): " + fileName);
							System.err.println("Program identifier: "+EbcdicUtil.toAscii(inData,directoryOffset + i + 0x5f,0x0d));
						}
						fileStart = indexCylinderSize + ((startCyl - 1) * cylinderSize) + (startHead * cylinderSize / 2) + ((startSector - 1) * sectorSize);
						int fileEnd = sectorSize + indexCylinderSize + ((endCyl - 1 ) * cylinderSize) + (endHead * cylinderSize / 2) + ((endSector - 1) * sectorSize);
						if (DEBUG)
						{
							System.err.print("File starts at offset: 0x"+Integer.toHexString(fileStart));
							System.err.println(" and ends at offset: 0x"+Integer.toHexString(fileEnd));
						}
						/*
						int creationYear = 0;
						int creationMonth = 0;
						int creationDay = 0;
						try
						{
							creationYear = 1900 + Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x2f, 2)); 
							creationMonth = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x31, 2)); 
							creationDay = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x33, 2)); 
						}
						catch (NumberFormatException e)
						{
							// Eat it - no big deal, we just don't get the HDR1 dates
						}
						*/
						// int lrecl = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x35,4));
						fileLength = fileEnd - fileStart;
						if ((fileName.length() > 0) && (fileLength > 0))
						{
							ByteArrayOutputStream out;
							try
							{
								if ((UnsignedByte.intValue(inData[fileStart]) == 0xc6) &&
										(UnsignedByte.intValue(inData[fileStart + 1]) == 0xd4) &&
										(UnsignedByte.intValue(inData[fileStart + 2]) == 0xe3) &&
										(UnsignedByte.intValue(inData[fileStart + 3]) == 0xf1))
								{
									// We have an FMT1 record
									/*
									try
									{
										creationYear = 1900 + Integer.parseInt(EbcdicUtil.toAscii(inData,fileStart + 0x0f, 2)); 
										creationMonth = Integer.parseInt(EbcdicUtil.toAscii(inData,fileStart + 0x11, 2)); 
										creationDay = Integer.parseInt(EbcdicUtil.toAscii(inData,fileStart + 0x13, 2)); 
									}
									catch (NumberFormatException e)
									{
										// Eat it - no big deal, we just don't get the FMT1 dates
									}
									*/
									// Since we have a FMT record, let's spin past it.
									fileStart += sectorSize;
									fileLength = fileEnd - fileStart;
								}
								if (fileStart + fileLength <= inData.length)
								{
									out = new ByteArrayOutputStream();
									out.write(inData, fileStart, fileLength);
									out.flush();
									parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
									/*
									if ((creationYear > 0) && (creationMonth > 0) && (creationDay > 0))
									{
									    Calendar c = Calendar.getInstance();
									    c.set(creationYear, creationMonth, creationDay);
										setFileCreationDate(fullname,c.getTime());
									}
									*/
								}
								else
									System.err.println("Error: file " + fileName+fileSuffix + " would exceed the capacity of the disk image.");
							}
							catch (IOException io)
							{
								io.printStackTrace();
							}
						}
					}
					catch (NumberFormatException e)
					{
						if (DEBUG)
							System.err.println("ERROR: Unable to determine file head/tail.");
					}
				}
			}
		}
	}
}
