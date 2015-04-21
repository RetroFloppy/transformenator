/*
 * Transformenator - perform transformation operations on binary files
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

import org.transformenator.Version;

/*
 * ExtractXerox860Files
 * 
 * This helper app pulls the files off of the virtual file system of Xerox 860
 * word processor disks.
 *
 * So far what we do is seek a file and then read contiguously until you either: 
 * a) hit an EOF, or b) hit another file.  There are directory sectors with file
 * names, but so far no obvious way to correlate them is forthcoming.
 * 
 */
public class ExtractXerox860Files
{

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
				FileOutputStream outFile = null;
				int header[] = new int[8];
				int track0Offset = 3328;
				int fileNumber = 1;
				System.err.println("Read " + inData.length + " bytes.");
				if (inData.length > 600000)
					track0Offset *= 2;
				if ((inData.length - track0Offset) % (512 * 8) == 0)
				{
					int i;
					boolean blank = false;
					// System.err.println("File size looks good.");
					for (int sector = 0; sector < (inData.length - track0Offset) / 512; sector++)
					{
						blank = false;
						for (i = 0; i < 8; i++)
						{
							header[i] = UnsignedByte.intValue(inData[sector * 512 + i + track0Offset]);
						}
						switch (header[5])
						{
						case 0x01:
							// System.err.println(sector + ": Index sector");
							break;
						case 0x24:
							// System.err.println(sector + ": Directory sector with " + header[6] + " entries");
							for (i = 8; i < 511; i++)
								directoryArray[i + directoryAccumulator - 8] = UnsignedByte.intValue(inData[sector * 512 + i + track0Offset]);
							fileCount += header[6];
							directoryAccumulator += i;
							break;
						case 0x28:
							// System.err.println(sector + ": Program (28) sector");
							break;
						case 0x40:
							// System.err.println(sector + ": System sector");
							break;
						case 0x80:
							if (header[3] == 0)
							{
								// System.err.println(sector + ": File sector end of file " + header[1]);
								writeSector(outFile, sector, inData, track0Offset);
								try
								{
									outFile.flush();
									outFile.close();
								}
								catch (IOException e)
								{
									e.printStackTrace();
								}

							}
							else
							{
								if (header[1] == 0x00)
								{
									// System.err.println(sector + ": File sector start");
									try
									{
										outFile = new FileOutputStream(directory + fileNumber);
										System.err.println("Writing file " + fileNumber);
										fileNumber++;
									}
									catch (FileNotFoundException e)
									{

									}
									// emitTextSector(sector, true, inData, track0Offset);
								}
								else
								{
									// System.err.println(sector + ": File sector from file " + header[3]);
								}
								writeSector(outFile, sector, inData, track0Offset);
							}
							break;
						default:
							blank = true;
							break;
						}
						if (!blank)
						{
							/*
							 * System.err.print("Header: "); for (i = 0; i < 8;
							 * i++) { header[i] =
							 * UnsignedByte.intValue(inData[sector * 512 + i]);
							 * System.err.print("0x" +
							 * UnsignedByte.toString(header[i]) + " "); }
							 * System.err.println();
							 */
						}
					}
					System.err.println(fileCount + " directory entries found:");
					emitDirectory(directoryArray, directoryAccumulator);
				}
				else
					System.err.println("File size doesn't make sense.");
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void emitDirectory(int directoryArray[], int length)
	{
		char ch;
		int field = 0, chCount = 0;
		System.err.println("Name\t\t\tCreated\tUpdated\tType\tPages\tSectors");
		for (int i = 0; i < length; i++)
		{
			ch = (char) directoryArray[i];
			switch (ch)
			{
			case 0xfe:
				field = 0;
				chCount = 0;
				System.err.println("");
				break;
			case 0xfd:
				System.err.print("\t");
				if ((chCount < 8) && (field == 0))
					System.err.print("\t");
				if ((chCount < 16) && (field == 0))
					System.err.print("\t");
				chCount = 0;
				field++;
				break;
			default:
				if (field < 6)
				{
					chCount ++;
					if ((ch >= 150) && (ch <= 151))
						System.err.print(' ');
					else if (((ch >= 63) && (ch < 123)) || ((ch >= 32) && (ch <= 58)))
						System.err.print(ch);
					else
						chCount--;
				}
				else
				{
					// Dump out the four bytes in the final field
					// System.err.print(" "+UnsignedByte.toString(ch));
				}
				break;
			}
		}
	}

	public static void writeSector(FileOutputStream outFile, int sector, byte inData[], int track0Offset)
	{
		char ch;
		for (int i = 8; i < 512; i++)
		{
			ch = (char) inData[(sector * 512) + i + track0Offset];
			if (ch != 0)
				try
				{
					outFile.write(ch);
				}
				catch (IOException e)
				{

				}
		}
	}

	public static void emitTextSector(int sector, boolean isFirst, byte inData[], int track0Offset)
	{
		char ch;
		int begin = 8;
		if (isFirst)
			begin += 57;
		for (int i = begin; i < 512; i++)
		{
			ch = (char) inData[(sector * 512) + i + track0Offset];
			if ((ch >= 150) && (ch <= 151))
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