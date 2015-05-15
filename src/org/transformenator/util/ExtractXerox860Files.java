/*
k * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2015 by David Schmidt
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
import java.util.Iterator;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractXerox860Files
 * 
 * This helper app pulls the files off of the virtual file system of Xerox 860
 * word processor disks.
 *
 * Directory sectors point to indices to files, which in turn hold pointers
 * to text sectors.  The pointer math is different in the directory sectors vs.
 * the (index pointers and sector header previous/next pointers).
 * 
 * Todo: needs a way to turn debug on and off, and single sided disks will need 
 * a new way to set/reset the track 0 offset.  It could also calculate the track 0
 * offset by counting backwards 76 tracks from the end.
 */
public class ExtractXerox860Files
{
	public static boolean DEBUG = false;

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			int directoryArray[] = new int[1024 * 1024];
			int fileCount = 0, directoryAccumulator = 0;
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
				int header[] = new int[8];
				int track0Offset = 3328;
				int indexSector = 1;
				if (DEBUG)
					System.err.println("Read " + inData.length + " bytes.");
				if (inData.length > 600000)
					track0Offset *= 2;
				if (DEBUG)
					System.err.println("Track 0 offset: "+track0Offset+" calculation: "+(inData.length - track0Offset) % (512 * 8));
				if ((inData.length - track0Offset) % (512 * 8) == 0)
				{
					int i;
					for (int absSector = 0; absSector < (inData.length - track0Offset) / 512; absSector++)
					{
						for (i = 0; i < 8; i++)
						{
							header[i] = UnsignedByte.intValue(inData[absSector * 512 + i + track0Offset]);
						}
						switch (header[5])
						{
						case 0x01:
							// Index sector
							if (false) //DEBUG)
							{
								System.err.println("0x" + Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": Index sector " + indexSector++);
								emitHeader(header);
								System.err.println();
							}
							break;
						case 0x24:
							// Directory sector
							if (DEBUG)
							{
								System.err.println("0x" + Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": Directory sector with " + header[6] + " entries.  Next dir sector: 0x" + Integer.toHexString(0x1000000 | absOffset(header[3], header[4], track0Offset)).substring(1).toUpperCase() + " Previous dir sector: 0x" + Integer.toHexString(0x1000000 | absOffset(header[1], header[2], track0Offset)).substring(1).toUpperCase());
								emitHeader(header);
								System.err.println();
							}
							int numDirEntries = 0;
							int nextChar;
							for (i = 8; i < 512; i++)
							{
								nextChar = UnsignedByte.intValue(inData[absSector * 512 + i + track0Offset]);
								directoryArray[i + directoryAccumulator - 8] = nextChar;
								if (nextChar == 0xfe)
									numDirEntries++;
								if (numDirEntries == header[6])
									break;
							}
							fileCount += header[6];
							directoryAccumulator += i - 8;
							break;
						case 0x28:
							// Program (28) sector
							//System.err.println("0x"+Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": Program (28) sector");
							//emitHeader(header);
							//System.err.println();
							break;
						case 0x40:
							// System sector
							//System.err.println("0x"+Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": System sector");
							//emitHeader(header);
							//System.err.println();
							break;
						case 0x80:
							// Text sector
							if (false) //DEBUG)
							{
								if (header[3] == 0)
								{
									System.err.println("0x" + Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": File data sector + end of file");
									emitHeader(header);
									emitTextSector(absSector, false, inData, track0Offset);
								}
								else
								{
									if (header[1] == 0x00)
									{
										System.err.println("0x" + Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": File data sector + start of file");
										emitHeader(header);
									}
									else
									{
										System.err.println("0x" + Integer.toHexString(0x1000000 | (absSector * 512) + track0Offset).substring(1).toUpperCase() + ": File data sector");
										emitHeader(header);
									}
									emitTextSector(absSector, false, inData, track0Offset);
								}
							}
							break;
						default:
							break;
						}
					}
					if (DEBUG)
						System.err.println(fileCount + " directory entries found:");
					emitDirectory(directoryArray, inData, directoryAccumulator, directory, track0Offset);
				}
				else
					System.err.println("Image file size doesn't make sense.");
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void emitHeader(int header[])
	{
		emitHeader(header, 0);
	}

	public static void emitHeader(int header[], int offset)
	{
		System.err.print("Header: ");
		for (int i = 0; i < 8; i++)
		{
			System.err.print("0x" + UnsignedByte.toString(header[offset + i]) + " ");
		}
		System.err.println();
	}

	public static void emitHeader(byte header[], int offset)
	{
		System.err.print("Header: ");
		for (int i = 0; i < 8; i++)
		{
			System.err.print("0x" + UnsignedByte.toString(header[offset + i]) + " ");
		}
		System.err.println();
	}

	public static void emitIndex(FileOutputStream outFile, byte inData[], int absOffset, int track0Offset)
	{
		int track, sector, origSector;
		int nextTrack, nextSector;
		int prevTrack, prevSector;
		int offset = absOffset;
		if (DEBUG)
			System.err.println("Index:  ");
		ArrayList<Integer> sectorsInIndex = new ArrayList<Integer>();
		ArrayList<Integer> visitedSectors = new ArrayList<Integer>();
		for (int i = 18; i < 512; i += 6)
		{
			if (inData[offset + i] == 0)
				break;
			track = (UnsignedByte.intValue(inData[offset + i]));
			origSector = UnsignedByte.intValue(inData[offset + i + 1]);
			if (origSector > 8)
				sector = origSector - 0x80 + 8;
			else
				sector = origSector;
			if (DEBUG)
				System.err.print("Mapped track/sector: 0x" + UnsignedByte.toString(track) + "/0x" + UnsignedByte.toString(origSector));
			sector = (sector - 1) * 512;
			track = (track - 1) * 8192;
			if ((track + sector) > 0)
			{
				if (DEBUG)
					System.err.print(" (Adding: 0x" + Integer.toHexString(0x1000000 | track + sector + track0Offset).substring(1).toUpperCase() + ") ");
				sectorsInIndex.add(track + sector + track0Offset);
			}
			if (track + sector + track0Offset + 4 < inData.length)
			{
				nextTrack = UnsignedByte.intValue(inData[track + sector + track0Offset + 3]);
				nextSector = UnsignedByte.intValue(inData[track + sector + track0Offset + 4]);
				prevTrack = UnsignedByte.intValue(inData[track + sector + track0Offset + 1]);
				prevSector = UnsignedByte.intValue(inData[track + sector + track0Offset + 2]);
				if (DEBUG)
				{
					System.err.print(" at offset 0x" + Integer.toHexString(0x1000000 | (track + sector + track0Offset)).substring(1).toUpperCase());
					System.err.print(" Prev offset: 0x" + Integer.toHexString(0x1000000 | absOffset(prevTrack, prevSector, track0Offset)).substring(1).toUpperCase());
					System.err.println(" Next offset: 0x" + Integer.toHexString(0x1000000 | absOffset(nextTrack, nextSector, track0Offset)).substring(1).toUpperCase());
					System.err.print("  That sector's ");
					emitHeader(inData, track + sector + track0Offset);
				}
			}
			else
			{
				if (DEBUG)
					System.err.println("Error: index would have pointed us out of bounds.");
			}
		}
		if (!sectorsInIndex.isEmpty())
		{
			boolean isFirst = true;
			int nextOffset = sectorsInIndex.get(0);
			if (DEBUG)
			{
				System.err.println("We have " + sectorsInIndex.size() + " index entries.");
				System.err.println("First sector in the group is: " + Integer.toHexString(0x1000000 | nextOffset).substring(1).toUpperCase());
			}
			while (nextOffset > 0)
			{
				if (sectorsInIndex.contains(nextOffset))
				{
					if (DEBUG)
						System.err.println("Ok, found the next offset: " + Integer.toHexString(0x1000000 | nextOffset).substring(1).toUpperCase());
					if (!visitedSectors.contains(nextOffset))
					{
						visitedSectors.add(nextOffset);
						if (nextOffset + track0Offset < inData.length)
							nextOffset = writeTextSector(outFile, nextOffset, inData, track0Offset, isFirst);
						else
						{
							// if (DEBUG)
							System.err.println("Error: attempt to follow index out of bounds.");
						}
					}
					else
					{
						if (DEBUG)
							System.err.println("Hmmm, we've already been to offset " + Integer.toHexString(0x1000000 | nextOffset).substring(1).toUpperCase() + ".  Stopping this chain.");
						nextOffset = 0;
					}
					isFirst = false;
				}
				else
				{
					boolean recovered = false;
					if (DEBUG)
					{
						System.err.println("Hmmm, the next offset (0x" + Integer.toHexString(0x1000000 | nextOffset).substring(1).toUpperCase() + ") wasn't in the index.");
					}
					// Look through the whole table.  Is anyone's prev pointer the same as this next pointer?  If so, that's where we go next.
					//public static int peekBack(byte inData[], int offset, int track0Offset)
					Iterator<Integer> it = sectorsInIndex.iterator();
					while (it.hasNext())
					{
						int tempSect = (Integer) it.next();
						if (peekBack(inData, tempSect, track0Offset) == nextOffset)
						{
							nextOffset = tempSect;
							recovered = true;
							if (DEBUG)
								System.err.println("Ok, we found someone claiming this offset... returning 0x" + Integer.toHexString(0x1000000 | nextOffset).substring(1).toUpperCase());
							break;
						}
					}
					if (!recovered)
					{
						if (DEBUG)
							System.err.println("Unable to find the backwards pointer to this offset; giving up.");
						nextOffset = 0;
					}
				}
			}
			if (DEBUG)
				System.err.println("Ok, all done with this index.");
		}
	}

	public static int absOffset(int track, int nativeSector, int track0Offset)
	{
		if (nativeSector > 8)
			nativeSector = nativeSector - 0x80 + 8;
		if ((nativeSector == 0) && (track == 0))
			return 0;
		else
			return ((nativeSector - 1) * 512) + ((track - 1) * 8192) + track0Offset;
	}

	public static int absOffsetFromDirectory(int big, int mid, int small, int track0Offset)
	{
		int bigValue = (big - 0x40) * 131072; // 16 full tracks, 2 sides; so that's 16 * 512 * 8 * 2 (131072, or 128k)
		int middleValue = ((mid - 0x40) / 2 * 4096);
		int smallValue = (small - 0x41) * 512;
		return bigValue + middleValue + smallValue + track0Offset - 8192;
	}

	public static void emitDirectory(int directoryArray[], byte inData[], int length, String directory, int track0Offset)
	{
		FileOutputStream outFile = null;
		char ch;
		String fileName = "", fileType = "";
		int field = 0, chCount = 0, fileCount = 1;
		int three = 0, two = 0, one = 0;
		if (DEBUG)
			System.err.println("Num: Name                 Created\tUpdated\tType\tPages\tSectors");
		for (int i = 0; i < length; i++)
		{
			ch = (char) directoryArray[i];
			switch (ch)
			{
			case 0xfe:
				field = 0;
				chCount = 0;
				three = 0;
				two = 0;
				one = 0;
				fileName = "";
				fileType = "";
				if (DEBUG)
					System.err.println("");
				fileCount++;
				break;
			case 0xfd:
				if (DEBUG)
				{
					if (field == 0)
					{
						for (int j = 0; j < 21 - chCount; j++)
							System.err.print(" ");
					}
					else
						System.err.print("\t");
				}
				chCount = 0;
				field++;
				break;
			default:
				if (DEBUG && (field == 0) && (chCount == 0))
				{
					if (fileCount < 10)
						System.err.print("00" + fileCount + ": ");
					else if (fileCount < 100)
						System.err.print("0" + fileCount + ": ");
					else
						System.err.print(fileCount + ": ");
				}
				if (field == 0)
				{
					// Fix up a few wild characters
					if ((ch == '>') || (ch == '<') || (ch == '$') || (ch == '\\') || (ch == '/') || (ch == '`') || (ch == '*'))
						ch = '_';
					fileName += ch;
				}
				if (field == 3)
				{
					if (ch != ' ')
						fileType += ch;
				}
				if (field < 6)
				{
					chCount++;
					if ((ch >= 150) && (ch <= 151))
					{
						if (DEBUG)
							System.err.print(' ');
					}
					else if (((ch >= 63) && (ch < 123)) || ((ch >= 32) && (ch <= 58)))
					{
						if (DEBUG)
							System.err.print(ch);
					}
					else
						chCount--;
				}
				else if (field == 6)
				{
					chCount++;
					if (chCount < 4)
					{
						if (DEBUG)
							System.err.print(" " + UnsignedByte.toString(ch));
					}
					if (chCount == 1)
						three = ch;
					else if (chCount == 2)
						two = ch;
					else if (chCount == 3)
					{
						one = ch;
						if (DEBUG)
							System.err.println(" Offset: 0x" + Integer.toHexString(0x1000000 | (absOffsetFromDirectory(three, two, one, track0Offset))).substring(1).toUpperCase());
						try
						{
							if (fileType.equals("D80"))
								fileType = " (Deleted)";
							else
								fileType = "";
							outFile = new FileOutputStream(directory + fileName + fileType);
							System.err.println("Writing file " + fileName);
							emitIndex(outFile, inData, absOffsetFromDirectory(three, two, one, track0Offset), track0Offset);
							try
							{
								outFile.flush();
								outFile.close();
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
							outFile = null;
						}
						catch (FileNotFoundException e)
						{

						}
					}
				}
				break;
			}
		}
	}

	public static int writeTextSector(FileOutputStream outFile, int offset, byte inData[], int track0Offset, boolean first)
	{
		char ch;
		int track = 0, sector = 0, origSector;
		int prevTrack, prevSector, prevOrigSector;
		if (DEBUG)
			System.err.println("writeTextSector visiting offset 0x" + Integer.toHexString(0x1000000 | offset).substring(1).toUpperCase());
		if (UnsignedByte.intValue(inData[offset + 5]) == 0x80)
		{
			if (UnsignedByte.intValue(inData[offset + 8]) != 0x00)
			{
				for (int i = 8; i < 512; i++)
				{
					ch = (char) inData[offset + i];
					if (ch != 0)
						try
						{
							outFile.write(ch);
						}
						catch (IOException e)
						{

						}
				}
				track = (UnsignedByte.intValue(inData[offset + 3]));
				origSector = UnsignedByte.intValue(inData[offset + 4]);
				if (origSector > 8)
					sector = origSector - 0x80 + 8;
				else
					sector = origSector;
				prevTrack = (UnsignedByte.intValue(inData[offset + 1]));
				prevOrigSector = UnsignedByte.intValue(inData[offset + 2]);
				if (prevOrigSector > 8)
					prevSector = prevOrigSector - 0x80 + 8;
				else
					prevSector = prevOrigSector;
				if (DEBUG)
				{
					System.err.println("Next track    : 0x" + UnsignedByte.toString(track) + " Sector: 0x" + UnsignedByte.toString(sector) + " Native sector: 0x" + UnsignedByte.toString(origSector) + " Next offset: 0x" + Integer.toHexString(0x1000000 | absOffset(track, origSector, track0Offset)).substring(1).toUpperCase());
					System.err.println("Previous track: 0x" + UnsignedByte.toString(prevTrack) + " Sector: 0x" + UnsignedByte.toString(prevSector) + " Native sector: 0x" + UnsignedByte.toString(prevOrigSector) + " Prev offset: 0x" + Integer.toHexString(0x1000000 | absOffset(prevTrack, prevOrigSector, track0Offset)).substring(1).toUpperCase());
				}
				sector = (sector - 1) * 512;
				track = (track - 1) * 8192;
				if (first == false)
				{
					if (prevSector + prevTrack == 0)
					{
						track = 0;
						sector = 0;
					}
					else
					{
						if (track + sector > 0)
						{
							int backPtr = peekBack(inData, track + sector + track0Offset, track0Offset);
							if (backPtr != offset)
							{
								if (DEBUG)
									System.err.println("Errrr, next sector's back pointer (0x" + Integer.toHexString(0x1000000 | backPtr).substring(1).toUpperCase() + ") doesn't point back to where it came from (0x" + Integer.toHexString(0x1000000 | offset).substring(1).toUpperCase() + ").  That's probably bad.");
								else
									System.err.println("Found a structural problem with this file (pointer inconsistency).");
								//track = 0;
								//sector = 0;
							}
						}
					}
				}
			}
			else
			{
				if (DEBUG)
					System.err.println("Zero found in the first byte of a text sector.");
			}
		}
		else
		{
			if (DEBUG)
				System.err.println("Whoah, this sector isn't a text sector.");
			else
				System.err.println("Found a structural problem with this file (unexpected sector type).");
		}
		if (track + sector == 0)
			return 0;
		else
			return track + sector + track0Offset;
	}

	public static int peekBack(byte inData[], int offset, int track0Offset)
	{
		// Find this sector's previous pointer
		int prevTrack, prevOrigSector, prevSector;
		if (offset + track0Offset < inData.length)
		{
			prevTrack = (UnsignedByte.intValue(inData[offset + 1]));
			prevOrigSector = UnsignedByte.intValue(inData[offset + 2]);
			if (prevOrigSector > 8)
				prevSector = prevOrigSector - 0x80 + 8;
			else
				prevSector = prevOrigSector;
			prevSector = (prevSector - 1) * 512;
			prevTrack = (prevTrack - 1) * 8192;

			return prevTrack + prevSector + track0Offset;
		}
		else
			return 0;
	}

	public static void emitTextSector(int sector, boolean isFirst, byte inData[], int track0Offset)
	{
		char ch;
		int begin = 8;
		if (isFirst)
			begin += 57;
		for (int i = begin; i < 512; i++)
		{
			ch = (char) UnsignedByte.intValue(inData[(sector * 512) + i + track0Offset]);
			if (((ch >= 150) && (ch <= 151)) || ((ch == 153)))
				System.err.print(' ');
			else if (((ch >= 63) && (ch < 123)) || ((ch >= 32) && (ch <= 58)))
				System.err.print(ch);
		}
		System.err.println();
		System.err.println();
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractXerox860Files " + Version.VersionString + " - Extract files from Xerox 860 word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractXerox860Files infile [out_directory]");
	}
}