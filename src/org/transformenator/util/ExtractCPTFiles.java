/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 - 2015 by David Schmidt
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
 * ExtractCPTFiles
 * 
 * This helper app pulls the files off of the virtual file system of CPT
 * word processor disks.
 *
 * CPT word processors have a set of directory sectors that start with a 0x30 ('0') in the
 * first byte.  (There also seems to be a 'deleted' or otherwise non-referenced directory
 * in sectors that start with 0x31 ('1').  
 * File names and a starting track are listed.  There doesn't seem to 
 * be a consistent map in the directory to a sector... so far, it just seems to be 
 * necessary to seek the file named in a track (there will be a 0x01 and a matching file name
 * in the first byte of the target sector) and then read contiguously until you either: 
 * a) hit an EOF, b) hit another file, or c) wrap around and continue on the next sector.
 * 
 */
public class ExtractCPTFiles
{

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			String directory = "";
			if (args.length == 2)
			{
				// They've asked for a directory to dump the files into.
				directory = args[1];
				File baseDirFile = new File(directory);
				if (!baseDirFile.isAbsolute())
				{
					baseDirFile = new File("." + File.separatorChar + args[1]);
				}
				baseDirFile.mkdir();

				if (!directory.endsWith("" + File.separatorChar))
				{
					directory = directory + File.separatorChar;
				}
			}
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
				if (inData.length % (256 * 16) == 0)
				{
					// Ok, we have good sectors.
					int trackLen = 256 * 16;
					int i, j, startSector = -1;
					// byte fnb[] = new byte[256];
					String currentFile = "";
					FileOutputStream outFile = null;
					String continuationFile = "";
					try
					{
						String fileName = "";
						for (i = 0; i < 77; i++)
						{
							if (!fileName.equals(""))
							{
								// System.err.println("...started a new track, but we haven't fininshed file " + fileName + " yet.");
								continuationFile = fileName;
							}
							// Hunt for a logical beginning of a track
							for (j = 0; j < 16; j++)
							{
								int offset = mapSector(j, 1);
								int sectorStart = offset * 256 + (i * trackLen);
								byte startByte = inData[sectorStart];
								boolean pad = false;
								;
								if (startByte == 0x01)
								{
									startSector = j;
									for (int k2 = 1; k2 < 17; k2++)
									{
										if (inData[sectorStart + k2] == 0x02)
											pad = true;
										if (!pad)
											fileName += (char) inData[sectorStart + k2];
									}
									// System.err.println("Found a new file ["+fileName+"] in track "+i);
								}
							}
							if (startSector == 0)
							{
								if (!currentFile.equals(""))
								{
									startSector = 0;
									System.err.println("Hmmm, a file was in progress, but we don't know where to start now in the next track...");
								}
							}
							if (startSector > -1)
							{
								for (j = startSector; j < startSector + 16; j++)
								{
									int offset = mapSector(j, 1);
									// fnb = Arrays.copyOfRange(inData, offset * 256 + (i * trackLen), (offset + 1) * 256 + (i * trackLen));
									fileName = "";
									boolean pad = false;
									int firstByteOfSector = UnsignedByte.intValue(inData[offset * 256 + (i * trackLen)]);
									if (firstByteOfSector == 0x00)
									{
										if (outFile != null)
										{
											outFile.flush();
											outFile.close();
										}
										currentFile = "";
										break;
									}
									else if (firstByteOfSector == 0x01)
									{
										int fnLen = 0;
										for (int k2 = 1; k2 < 17; k2++)
										{
											if (inData[offset * 256 + (i * trackLen) + k2] == 0x02)
											{
												pad = true;
												fnLen = k2 + 1;
											}
											if (!pad)
												fileName += (char) inData[offset * 256 + (i * trackLen) + k2];
										}
										// System.err.println("Found file [" + fileName + "] in track " + i);
										currentFile = fileName;
										if (outFile != null)
										{
											outFile.flush();
											outFile.close();
										}
										System.err.println("Creating file " + (directory + currentFile));
										outFile = new FileOutputStream(directory + currentFile);

										for (int x = fnLen; x < 256; x++)
										{
											outFile.write((char) inData[offset * 256 + (i * trackLen) + x]);
											// System.err.print((char) inData[offset * 256 + (i * trackLen) + x]);
										}
										// System.err.println();
									}
									else if ((firstByteOfSector == 0x30) || (firstByteOfSector == 0x31))
									{
										// Ok, we hit a catalog sector.  This resets us.
										currentFile = "";
										if (outFile != null)
										{
											outFile.flush();
											outFile.close();
										}

										if (!continuationFile.equals(""))
										{
											currentFile = continuationFile;
											outFile = new FileOutputStream(directory + currentFile);
										}
									}
									else if (!currentFile.equals(""))
									{
										// System.err.println("  Dump of sector for file " + currentFile);
										for (int x = 0; x < 256; x++)
										{
											if (inData[offset * 256 + (i * trackLen) + x] == 0x03)
											{
												currentFile = "";
												if (outFile != null)
												{
													outFile.flush();
													outFile.close();
												}
												break;
											}
											else
											{
												outFile.write((char) inData[offset * 256 + (i * trackLen) + x]);
												// System.err.print((char) inData[offset * 256 + (i * trackLen) + x]);
											}
										}
										// System.err.println();
									}
									else
									{
										if (!continuationFile.equals(""))
										{
											currentFile = continuationFile;
											continuationFile = "";
											if (outFile != null)
											{
												outFile.flush();
												outFile.close();
											}
											outFile = new FileOutputStream(directory + currentFile);
											// System.err.println("  (restart) Dump of sector for file " + currentFile);
											for (int x = 0; x < 256; x++)
											{
												if (inData[offset * 256 + (i * trackLen) + x] == 0x03)
												{
													currentFile = "";
													if (outFile != null)
													{
														outFile.flush();
														outFile.close();
													}
													break;
												}
												else
												{
													outFile.write((char) inData[offset * 256 + (i * trackLen) + x]);
													// System.err.print((char) inData[offset * 256 + (i * trackLen) + x]);
												}
											}
										}
										// System.err.println();
									}
								}
							}
							else
								currentFile = "";
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
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

	public static int mapSector(int sectorIn, int trackIn)
	{
		int skewedSectorMap[] = { 0, 3, 6, 9, 12, 15, 2, 5, 8, 11, 14, 1, 4, 7, 10, 13, 0, 3, 6, 9, 12, 15, 2, 5, 8, 11, 14, 1, 4, 7, 10, 13 };
		return skewedSectorMap[sectorIn];
	}

	public static int realAddress(int sector, int offset)
	{
		return sector * 256 + offset;
	}

	public static int realWPAddress(int track, int sector, int offset, int skew)
	{
		int newSector = mapSector(sector, skew);
		return track * 4096 + newSector * 256 + offset;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractCPTFiles " + Version.VersionString + " - Extract files from CPT word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractCPTFiles infile [out_directory]");
	}
}