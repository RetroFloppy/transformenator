/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2017 by David Schmidt
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Date;

import org.transformenator.Version;
import org.transformenator.internal.EbcdicUtil;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractIBM8Files
 * 
 * Helper app to pull the files off of the virtual file system of IBM 8" disks
 * (typically coming from IBM System/32/34/36 or AS/400).
 * Other disk types that have the "standard" IBM 8" FM header and the rest is
 * their own thing may get some useful information from this - but disks that are
 * not really IBM transfer material are typically better served with a more
 * specialized extractor.
 *
 */
public class ExtractIBM8Files
{
	public static boolean DEBUG = false;
	public static void main(java.lang.String[] args)
	{
		String outputDirectory = "";
		int fileStart = 0, fileLength = 0;
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
				// Read in the entire disk image
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
				// We have good data; get ready to deal with it.
				System.err.println("Read " + inData.length + " bytes.");
				if (args.length > 1)
				{
					// If they wanted an output directory, go ahead and make it.
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("." + File.separator + args[1]);
					}
					// System.out.println("Making directory: ["+baseDirFile+"]");
					baseDirFile.mkdirs();
					outputDirectory = args[1];
				}
				else
				{
					if ((file.getName().toLowerCase().endsWith(".img")) || (file.getName().toLowerCase().endsWith(".bin")))
					{
						// System.out.println("Making image directory: ["+"." + File.separator + file.getName().substring(0, file.getName().length() - 4)+"]");
						outputDirectory = "." + File.separator + file.getName().substring(0, file.getName().length() - 4);
						File baseDirFile = new File(outputDirectory);
						baseDirFile.mkdirs();
					}
				}
				// Ready to go.  Time to face the music.
				int cylinderSize = 0;
				int indexCylinderSize = 0;
				int headSize = 0;
				int sectorSize = 0;
				int volOffset = 0x300;

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
					int lrecl = 0;
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
								lrecl = Integer.parseInt(EbcdicUtil.toAscii(inData,directoryOffset + i + 0x35,4));
								fileLength = fileEnd - fileStart;
								if ((fileName.length() > 0) && (fileLength > 0))
								{
									FileOutputStream out;
									try
									{
										if ((UnsignedByte.intValue(inData[fileStart]) == 0xc6) &&
												(UnsignedByte.intValue(inData[fileStart + 1]) == 0xd4) &&
												(UnsignedByte.intValue(inData[fileStart + 2]) == 0xe3) &&
												(UnsignedByte.intValue(inData[fileStart + 3]) == 0xf1))
										{
											// We have a FMT1 record
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
											// Since we have a FMT record, let's spin past it.
											fileStart += sectorSize;
											fileLength = fileEnd - fileStart;
										}
										String fullname = new String(outputDirectory + File.separator + fileName);
										if (fileStart + fileLength <= inData.length)
										{
											out = new FileOutputStream(fullname);
											System.err.println("Creating file: " + fullname + " (LRECL 0x"+Integer.toHexString(lrecl)+")");
											out.write(inData, fileStart, fileLength);
											out.flush();
											out.close();
											
											if ((creationYear > 0) && (creationMonth > 0) && (creationDay > 0))
											{
											    Calendar c = Calendar.getInstance();
											    c.set(creationYear, creationMonth, creationDay);
												setFileCreationDate(fullname,c.getTime());
											}
										}
										else
											System.err.println("Error: file " + fullname + " would exceed the capacity of the disk image.");
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
				else
				{
					System.err.println("ERROR: Unable to determine disk geometry.");
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void setFileCreationDate(String filePath, Date creationDate) throws IOException
	{
	    BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(filePath), BasicFileAttributeView.class);
	    FileTime time = FileTime.fromMillis(creationDate.getTime());
	    attributes.setTimes(time, time, time);
	}
	
	public static void help()
	{
		System.err.println();
		System.err.println("ExtractIBM8Files " + Version.VersionString + " - Extract files from IBM disk images.");
		System.err.println();
		System.err.println("Usage: ExtractIBM8Files infile [out_directory]");
	}
}