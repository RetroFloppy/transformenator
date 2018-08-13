/*
 * Transformenator - perform transformation operations on binary files
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

package org.transformenator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import org.transformenator.internal.UnsignedByte;
import org.transformenator.internal.Version;

/*
 * ExtractMotorolaMdosFiles
 * 
 * Helper app to pull the files off of the virtual file system of a Motorola EXOR MDOS disk image.
 * 
 */

/*

Helpful info, from http://exorsim.sourceforge.net/mdos-tech.html:

MDOS Filesystem information

MDOS uses single sided or double sided 8-inch soft sectored single density diskettes with the IBM 3740 format:
77 cylinders of 26 sectors of 128 bytes. 2002 sectors for a single sided diskette and 4004 sectors for a 
double-sided diskette.

Space is allocated in clusters. Each cluster has four sectors and is 512 bytes.

Total clusters on a single sided diskette is 500, with two unused sectors left over at the end of the disk.
Double-sided diskettes have exactly 1001 clusters.

These sectors are reserved:

Sector 0
    Diskette ID block 
Sector 1
    Cluster Allocation Table (or CAT) bitmap. 1 means allocated. Bit 7 of byte 0 is the first cluster. 
Sector 2
    Lock-out cluster allocation table 
Sectors 3 - 22
    Directory - 20 sectors 
Sectors 23 - 24
    Boot block and MDOS RIB 

Each directory entry is 16 bytes, so there can be up to 160 files on a diskette. Each directory entry looks like this:

0 - 7
    File name 
8 - 9
    Suffix 
10 - 11
    Sector number of first cluster 
12 - 13
    File attributes 
14 - 15
    Unused 

Sectors within a file are addressed by LSN- Logical Sector Number. LSN $FFFF is the first sector of the first
cluster. It contains the the file's RIB- Retrieval Information Block. The first data sector is LSN $0000.

Files are composed of up to 57 contiguous "extents", where each extent can have up to 32 clusters. This works out
to a maximum file size of 912 KB (but the largest disk is 500.5 KB).

The RIB contains a list of SDWs- 16-bit words which give the starting cluster number and size of each extent. The
RIB also contains the load address and starting execution address of binary files.

 */
public class ExtractMotorolaMdosFiles
{
	static int SECTOR_SIZE = 128;
	static int SECTOR_DIR = 3;
	static int SECTOR_DIR_SIZE = 20;

	public static void main(java.lang.String[] args)
	{
		String outputDirectory = "";
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
					if (file.getName().endsWith(".img"))
					{
						// System.out.println("Making image directory: ["+"." + File.separator + file.getName().substring(0, file.getName().length() - 4)+"]");
						outputDirectory = "." + File.separator + file.getName().substring(0, file.getName().length() - 4);
						File baseDirFile = new File(outputDirectory);
						baseDirFile.mkdirs();
					}
				}
				/*
				 * OK, it's all over but the cryin'.
				 */
				boolean deleted1 = false;
				boolean deleted2 = false;
				String fileName = "";
				for (int i = SECTOR_DIR; i < SECTOR_DIR + SECTOR_DIR_SIZE; i++)
				{
					// Check all directory sectors for files
					// System.out.println("Dir sector " + i);
					for (int j = 0; j < SECTOR_SIZE; j += 16)
					{
						// Check a particular sector for files - 16 entries possible
						deleted1 = false;
						deleted2 = false;
						if (inData[i * SECTOR_SIZE + j] != 0)
						{
							fileName = "";
							for (int name = 0; name < 8; name++)
							{
								if (inData[i * SECTOR_SIZE + j + name] == -1)
								{
									fileName += "_";
									if (name == 0)
										deleted1 = true;
									if (name == 1)
										deleted2 = true;
								}
								else
								{
									if (inData[i * SECTOR_SIZE + j + name] != 0x20)
									{
										fileName += (char) inData[i * SECTOR_SIZE + j + name];
									}
								}
							}
							fileName += ".";
							for (int suffix = 8; suffix < 10; suffix++)
							{
								fileName += (char) inData[i * SECTOR_SIZE + j + suffix];
							}
							int sectorStart = UnsignedByte.intValue(inData[i * SECTOR_SIZE + j + 10]) * 256 + UnsignedByte.intValue(inData[i * SECTOR_SIZE + j + 11]) & 0xffff;
							int fileType = UnsignedByte.intValue(inData[i * SECTOR_SIZE + j + 12]) & 7;

							dumpFile(inData, sectorStart, (deleted1 & deleted2), fileType, outputDirectory + File.separator + fileName);
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

	public static void dumpFile(byte[] inData, int ribStart, boolean deleted, int fileType, String fileName)
	{
		if (deleted)
		{
			System.out.println("Noting deleted file: " + fileName);
		}
		else
		{
			System.out.println("Creating file: " + fileName);
			FileOutputStream out = null;
			try
			{
				out = new FileOutputStream(fileName);
				// The first sector contains the RIB
				// System.out.println("RIB sector: 0x" + Integer.toHexString(ribStart));
				int sizeInSectors = UnsignedByte.intValue(inData[(ribStart * SECTOR_SIZE) + 0x77], inData[(ribStart * SECTOR_SIZE) + 0x76]) & 0xffff;
				int totalClusters = sizeInSectors / 0x80;
				if (sizeInSectors % 0x80 > 0)
					totalClusters++;
				int lastByteLastSector = UnsignedByte.intValue(inData[(ribStart * SECTOR_SIZE) + 0x75]) & 0xff;
				if (fileType == 2)
				{
					// System.out.println("lastByteLastSector: 0x" + Integer.toHexString(lastByteLastSector));
					// System.out.println("sizeInSectors: 0x" + Integer.toHexString(sizeInSectors)+" totalClusters: "+totalClusters);
				}
				for (int extent = 0; extent < 64; extent += 2)
				{
					byte buffer[] = null;
					int extentInfo = UnsignedByte.intValue(inData[(ribStart * SECTOR_SIZE) + (extent) + 1], inData[(ribStart * SECTOR_SIZE) + (extent)]);
					int cluster = extentInfo & 0x3ff;
					int startingSector = cluster * 4;
					int sectorsInExtent = (((extentInfo >> 10) & 0x1f) + 1) * 4;
					int writeLength = SECTOR_SIZE * sectorsInExtent;
					if (fileType == 2)
					{
						if (totalClusters == 1)
							writeLength = SECTOR_SIZE * sizeInSectors;
						if (extent / 2 + 1 == totalClusters)
						{
							// System.out.println("Last extent in a binary file; snipping.");
							writeLength -= (SECTOR_SIZE - lastByteLastSector);
						}
						// System.out.println("Extent: "+extent/2+" totalClusters: "+totalClusters+" writeLength: 0x"+Integer.toHexString(writeLength));
					}
					if ((extentInfo & 0x8000) != 0x8000)
					{
						// System.out.println("cluster: 0x" + Integer.toHexString(cluster) + " starting sector: 0x" + Integer.toHexString(startingSector) + " addr: 0x" + Integer.toHexString(startingSector * SECTOR_SIZE) + " sectorsInExtent: 0x" + Integer.toHexString(sectorsInExtent) + " write length: 0x" + Integer.toHexString(writeLength));
						buffer = null;
						if (extent == 0)
						{
							buffer = new byte[writeLength];
							System.arraycopy(inData, ((startingSector + 1) * SECTOR_SIZE), buffer, 0, writeLength);
						}
						else
						{
							buffer = new byte[writeLength];
							System.arraycopy(inData, startingSector * SECTOR_SIZE, buffer, 0, writeLength);
						}
						if (fileType == 5)
						{
							int ign = 0;
							// ASCII file - let's decode it a bit
							for (int n = 0; n != writeLength; ++n)
							{
								int c = UnsignedByte.intValue(buffer[n]) & 0xff;
								if ((c & 0x80) > 0)
								{
									for (int z = 0; z != (c & 0x7f); ++z)
									{
										out.write(' ');
									}
									ign = 0;
								}
								else if (c == 13)
								{
									out.write('\n');
									ign = 1;
								}
								else if (c == 10)
								{
									ign = 1;
								}
								else if (c == 0)
								{
									if (ign == 0)
										break;
									else
										ign = 0;
								}
								else
								{
									ign = 0;
									out.write(c);
								}
							}
						}
						else
						{
							// Otherwise - dump the buffer
							out.write(buffer);
						}
					}
					else
					{
						break;
					}
				}
				out.close();
				// System.out.println();
			}
			catch (IOException io)
			{
				io.printStackTrace();
			}
		}
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractMotorolaMdosFiles " + Version.VersionString + " - Extract files from Motorola EXOR MDOS disk images.");
		System.err.println();
		System.err.println("Usage: ExtractMotorolaMdosFiles infile [out_directory]");
	}

}