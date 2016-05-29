/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2015 by David Schmidt
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractWangFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of WANG
 * word processor disks.  I've seen a couple of different types: first, the 2200-derived disks
 * that have a central catalog, and second, likely an earlier dedicated word processor (WPS?)
 * that has no catalog, but files with header sectors that follow chains.
 *
 */
public class ExtractWangFiles
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
			if (inData != null)
			{
				// System.err.println("Read " + inData.length + " bytes.");
				byte eyecatcher[] = { 0x57, 0x41, 0x4e, 0x47 }; // "WANG" - part of the WVD specification
				byte range[] = Arrays.copyOfRange(inData, 0x00, 0x04);

				if (Arrays.equals(range, eyecatcher)) // Is the WANG eyecatcher in the disk image?
				{
					int catalogSectors = 0;
					int preambleOffset = 0x100; // Space for WVD preamble
					int catalogOffset = 0x100;
					boolean shouldContinue = false;
					if ((inData[0x100] == 0x00) && (inData[0x2fe] == 0x01))
					{
						catalogSectors = UnsignedByte.intValue(inData[0x101]);
					}
					else if (inData[0x100] == 0x02)
					{
						System.err.println("Caution: disk index type is 2.  May need new means of interpreting disk image.");
						// TODO: Untested...
						catalogSectors = UnsignedByte.intValue(inData[0x102], inData[0x101]);
					}
					else
					{
						System.err.println("Error: disk index type is unknown.  Will need new means of interpreting disk image.");
					}
					if (catalogSectors > 0) // Ok, we have a reasonable catalog number
					{
						if (args.length == 2)
						{
							/*
							 * If they wanted an output directory, go ahead and make it.
							 */
							File baseDirFile = new File(args[1]);
							if (!baseDirFile.isAbsolute())
							{
								baseDirFile = new File("." + File.separator + args[1]);
							}
							baseDirFile.mkdir();
						}
						System.err.println("Number of catalog sectors: "+catalogSectors);
						preambleOffset += catalogSectors * 256;
						catalogOffset += (catalogSectors + 2) * 256;
						// System.err.println("preambleOffset: "+preambleOffset+" catalogOffset: "+catalogOffset);
						int fileIndexOffset = catalogOffset + 4; // The first file in the file index is 4 bytes past the start of the index sector
						do
						{
							// System.err.println("Next index byte: "+UnsignedByte.toString(inData[fileHeaderPointer]));
							if (inData[fileIndexOffset] != (byte) 0xff)
							{
								byte fnb[] = new byte[10];
								fnb = Arrays.copyOfRange(inData, fileIndexOffset + 2, fileIndexOffset + 12);
								String fileName = new String(fnb).trim().replace("\\", "-").replace("/", "-").replace("?", "-");
								fileName = new String(args[1]) + File.separator + fileName;
								int fileHeaderSector = UnsignedByte.intValue(inData[fileIndexOffset + 1], inData[fileIndexOffset]);
								// System.err.println("File found: " + fileName+" at raw sector: "+fileHeaderSector);
								decodeFile(inData, fileName, fileHeaderSector, preambleOffset);
								shouldContinue = true;
							}
							else
								shouldContinue = false;
							fileIndexOffset += 12;
						} while (shouldContinue == true);
					}
					else
					{
						/*
						 * Ok, this isn't a 2200-style disk; check for WP files.
						 */
						boolean foundAny = false;
						for (int i = preambleOffset; i < inData.length; i += 256)
						{
							if ((inData[i + 2] == -1) && (inData[i + 3] == 65)) /* 0xff 0x41 */
							{
								// Filename is 0x0d bytes in, for a length of 25
								// Description 1 is 0x27 bytes in, for a length of 20
								// Description 2 is 0x3c bytes in, for a length of 20
								// Description 3 is 0x50 bytes in, for a length of 20
								byte fnb[] = new byte[25];
								byte dsc1b[] = new byte[20];
								byte dsc2b[] = new byte[20];
								byte dsc3b[] = new byte[20];
								fnb = Arrays.copyOfRange(inData, i + 0x0d, i + 0x0d + 25);
								dsc1b = Arrays.copyOfRange(inData, i + 0x27, i + 0x27 + 20);
								dsc2b = Arrays.copyOfRange(inData, i + 0x3c, i + 0x3c + 20);
								dsc3b = Arrays.copyOfRange(inData, i + 0x50, i + 0x50 + 20);
								String fileName = new String(fnb).trim().replace("\\", "-").replace("/", "-").replace("?", "-");
								String dsc1 = new String(dsc1b).trim();
								String dsc2 = new String(dsc2b).trim();
								String dsc3 = new String(dsc3b).trim();
								int fileID = UnsignedByte.intValue(inData[i + 4]) * 256 + UnsignedByte.intValue(inData[i + 5]);
								fileName = new String(args[1]) + File.separator + fileName + " " +i;
								System.out.println("File found: ["+fileName+"] at location 0x" + Integer.toHexString(i));
								System.out.println("   Description 1: ["+dsc1+"]");
								System.out.println("   Description 2: ["+dsc2+"]");
								System.out.println("   Description 3: ["+dsc3+"]");
								decodeWPFile(inData, fileName, preambleOffset, UnsignedByte.intValue(inData[i]), UnsignedByte.intValue(inData[i + 1]), fileID, 1);
								foundAny = true;
							}
						}
						// foundAny = false;
						if (!foundAny)
						{
							/*
							 * Couldn't find anything at all... scrape the disk surface.
							 */

							// TODO: would be nice if this could be invokable, on demand.
							List<Integer> fileChain;
							Hashtable<Integer, List<Integer>> hash = new Hashtable<Integer, List<Integer>>();
							FileOutputStream out = null;
							for (int track = 0; track < inData.length / 4096; track++)
							{
								for (int sector = 0; sector < 16; sector++)
								{
									int dataOffset = realWPAddress(track, sector, preambleOffset, 1);
									int fileID = UnsignedByte.intValue(inData[dataOffset + 4]) * 256 + UnsignedByte.intValue(inData[dataOffset + 5]);
									if (fileID != 0)
									{
										Integer fid = new Integer(fileID);
										if (!hash.containsKey(fid))
										{
											hash.put(fid, new ArrayList<Integer>());
										}
										fileChain = hash.get(fid);
										fileChain.add(dataOffset);
									}
								}
							}
							for (Map.Entry<Integer, List<Integer>> entry : hash.entrySet())
							{
								Integer key = entry.getKey();
								fileChain = hash.get(key);
								String fileName = new String("recovered_file_" + key);
								fileName = new String(args[1]) + File.separator + fileName;
								try
								{
									out = new FileOutputStream(fileName);
									System.err.println("Creating file: " + fileName);
								}
								catch (IOException e)
								{
									e.printStackTrace();
								}
								for (Iterator<Integer> iter = fileChain.iterator(); iter.hasNext();)
								{
									int myInt = iter.next();
									// System.out.println("gathering offset: "+myInt);
									try
									{
										if (out != null)
										{
											byte range2[] = Arrays.copyOfRange(inData, myInt + 7, myInt + UnsignedByte.intValue(inData[myInt + 2]) + 1);
											out.write(range2);
											out.flush();
										}
									}
									catch (IOException e)
									{
										e.printStackTrace();
									}
								}
							}
						}
					}
				}
				else if ((inData.length % 1024 == 0) && (UnsignedByte.intValue(inData[inData.length - 1]) == 0xff) && ((UnsignedByte.intValue(inData[inData.length - 2]) == 0xff)))
				{
					System.err.println("Unwinding a WANG WP file from a DOS file.");
					if (args.length == 2)
						unwindDOSFile(inData, args[1]);
					else
						unwindDOSFile(inData, args[0] + ".bin");
				}
				else if (inData.length >= 322560)
				{
					byte eyecatcherc[] = { 0x0b, 0x43, 0x61, 0x74, 0x61, 0x6c, 0x6f, 0x67 }; // ".Catalog" - part of the WVD specification
					byte rangec[] = Arrays.copyOfRange(inData, 0x23000, 0x23008);
					if (Arrays.equals(rangec, eyecatcherc))
					{
						seek514Files(inData);
					}
				}
				else
				{
					System.err.println("Input file is not a known Wang format.");
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void unwindDOSFile(byte[] inData, String fileName)
	{
		/*
		 * This is probably a file from a DOS-ish WANG word processor
		 */
		FileOutputStream out;
		try
		{
			out = new FileOutputStream(fileName);
			System.err.println("Creating file: " + fileName);

			/*
			 * These word processors have a list of pointers to the starts of pages, in order.
			 * Each pointer points to a 1k chunk of data that ends in another pointer (or xffff for the end). 
			 */
			for (int i = 258; i < 512; i += 2)
			{
				int pageNumber = UnsignedByte.intValue(inData[i + 1]) * 256 + UnsignedByte.intValue(inData[i]);
				if ((pageNumber > 0) && (pageNumber < 65535))
				{
					//System.err.println("pageNumber: "+pageNumber);
					dumpDOSPage(out, inData, pageNumber);
				}
			}
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void seek514Files(byte[] inData)
	{
		/*
		 * This is probably a WANG-ish WANG word processor
		 */
		int fileNo = 1;
		String fileName = "File";
		FileOutputStream out;
		for (int i = 0; i < inData.length; i += 512)
		{
			if ((UnsignedByte.intValue(inData[i]) == 0x86) && (UnsignedByte.intValue(inData[i + 1]) == 0x32))
			{
				try
				{
					out = new FileOutputStream(fileName + fileNo);
					System.err.println("Creating file: " + fileName + fileNo);
					int x;
					for (x = i; x < inData.length; x++)
					{
						if (inData[x] == 0x1f)
							break;
					}
					byte range[] = Arrays.copyOfRange(inData, i, x);
					out.write(range);
					out.flush();
					out.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				fileNo++;
			}
		}
	}

	public static void dumpDOSPage(FileOutputStream out, byte[] inData, int pageNumber) throws IOException
	{
		int next = 0;
		int endMarker = 1022;
		int offset = pageNumber * 1024;
		// Find the end of data in this block
		for (int i = 0; i < 1022; i++)
		{
			if (inData[i + offset] == 0x1f)
				endMarker = i;
		}
		//System.out.println("Start of page; block: "+UnsignedByte.toString(UnsignedByte.hiByte(pageNumber))+""+UnsignedByte.toString(UnsignedByte.loByte(pageNumber))+" offset: "+UnsignedByte.toString(UnsignedByte.loByte(offset))+""+UnsignedByte.toString(UnsignedByte.hiByte(offset)));
		out.write(inData, offset, endMarker);
		next = UnsignedByte.intValue(inData[offset + 1023]) * 256 + UnsignedByte.intValue(inData[offset + 1022]);
		if (next < 65535)
			dumpDOSPage(out, inData, next);
		//else
		//System.out.println("End.");
	}

	public static void decodeFile(byte[] inData, String shortName, int fileHeaderSector, int preambleOffset)
	{
		/*
		 * Incoming, we have the sector address of the file header; use that to
		 * find the rest of the file.
		 */
		int fileHeaderOffset = fileHeaderSector * 256 + preambleOffset;
		// System.err.println("fileHeaderSector: "+fileHeaderSector+" fileHeaderOffset: "+fileHeaderOffset);
		int firstFileChainSector = UnsignedByte.intValue((byte) inData[fileHeaderOffset + 193 /* 0xc1 */], (byte) inData[fileHeaderOffset + 192 /* 0xc0 */]);
		byte fnb[] = new byte[64];
		fnb = Arrays.copyOfRange(inData, fileHeaderOffset + 64, fileHeaderOffset + 128);
		String longName = new String(fnb).trim().replace("\\", "-").replace("/", "-").replace("?", "-"), fullName;
		// System.err.println("Long name: "+longName);
		FileOutputStream out;
		try
		{
			if (longName.equals(""))
				fullName = shortName;
			else
				fullName = shortName + "-" + longName;
			out = new FileOutputStream(fullName);
			System.err.println("Creating file: " + fullName);
			dumpFileChain(out, inData, firstFileChainSector, preambleOffset);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void dumpFileChain(FileOutputStream out, byte[] inData, int fileChainSector, int preambleOffset) throws IOException
	{
		// System.err.println("dumpFileChain: fileChainSector: " + fileChainSector);
		int textSector = 0, i = 0, textRealOffset;
		if ((fileChainSector > 0) && ((fileChainSector + 1) * 256 < inData.length))
		{
			do
			{
				textSector = UnsignedByte.intValue((byte) inData[realAddress(fileChainSector, preambleOffset) + 0x41 + i], (byte) inData[realAddress(fileChainSector, preambleOffset) + 0x40 + i]);
				if ((textSector > 0) && ((textSector + 1) * 256 < inData.length))
				{
					// System.err.println("dumpFileChain: textSector: " + textSector);
					textRealOffset = realAddress(textSector, preambleOffset);
					// System.err.println("dumpFileChain: textRealOffset: "+textRealOffset);
					byte range[] = Arrays.copyOfRange(inData, textRealOffset, textRealOffset + 256);
					out.write(range);
					i += 2;
				}
			} while ((textSector > 0) && ((textSector + 1) * 256 < inData.length));
			int nextFileChainSector = UnsignedByte.intValue((byte) inData[realAddress(fileChainSector, preambleOffset) + 9], (byte) inData[realAddress(fileChainSector, preambleOffset) + 8]);
			// System.err.println("dumpFileChain: nextFileChainSector: "+nextFileChainSector);
			if (nextFileChainSector != 0)
				dumpFileChain(out, inData, nextFileChainSector, preambleOffset);
		}
	}

	public static void decodeWPFile(byte[] inData, String fullName, int preambleOffset, int track, int sector, int fileID, int skew)
	{
		FileOutputStream out;
		try
		{
			out = new FileOutputStream(fullName);
			System.err.println("Creating file: " + fullName);
			dumpWPFileChain(out, inData, track, sector, fileID, preambleOffset, skew);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void dumpWPFileChain(FileOutputStream out, byte[] inData, int track, int sector, int fileID, int preambleOffset, int skew) throws IOException
	{
		int nextTrack, nextSector, dataOffset;

		dataOffset = realWPAddress(track, sector, preambleOffset, skew);
		int sectorDataLength = UnsignedByte.intValue(inData[dataOffset + 2])+1;
		// System.err.print("Dumping track "+Integer.toHexString(track)+" sector "+Integer.toHexString(sector) + " At 0x"+Integer.toHexString(dataOffset)+" for "+sectorDataLength+" bytes.");
		
		nextTrack = UnsignedByte.intValue(inData[dataOffset]);
		nextSector = UnsignedByte.intValue(inData[dataOffset + 1]);
		// System.err.println("  Next track: "+Integer.toHexString(nextTrack)+" sector "+Integer.toHexString(nextSector)+" at 0x"+Integer.toHexString(realWPAddress(nextTrack, nextSector, preambleOffset, skew)));
		if (sectorDataLength > 7)
		{
			byte range[] = Arrays.copyOfRange(inData, dataOffset + 7, dataOffset + sectorDataLength);
			out.write(range);
		}
		nextTrack = UnsignedByte.intValue(inData[dataOffset]);
		nextSector = UnsignedByte.intValue(inData[dataOffset + 1]);
		if (((nextTrack > 0) && (nextTrack * 4096 <= inData.length)) || (nextTrack == 0 && nextSector > 0))
		{
			if ((nextTrack == track) && (nextSector == sector))
				System.err.println("Error: Loop in file structure; stopping.");
			else
				dumpWPFileChain(out, inData, nextTrack, nextSector, fileID, preambleOffset, skew);
		}
		else
		{
			if (nextTrack != 0)
				System.err.println("Next track would be beyond disk image capacity.");
		}
	}

	public static int mapSector(int sectorIn, int skew)
	{
		int skewedSectorMap[] = { 0, 4, 8, 0x0c, 1, 5, 9, 0x0d, 2, 6, 0x0a, 0x0e, 3, 7, 0x0b, 0x0f };
		if (skew != 0)
			return skewedSectorMap[sectorIn];
		else
			return sectorIn;
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
		System.err.println("ExtractWangFiles " + Version.VersionString + " - Extract files from Wang word processor files or disk images.");
		System.err.println();
		System.err.println("Usage: ExtractWangFiles infile [outfile|out_directory]");
	}
}