/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 - 2017 by David Schmidt
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
import java.util.Arrays;

import org.transformenator.internal.UnsignedByte;
import org.transformenator.internal.Version;

/*
 * ExtractHPFiles
 * 
 * Helper app to pull the files off of the virtual file system of HP instrument (not LIF) disk image.
 * 
 * Floppy disk geometry: 2 sides, 256 bytes per sector, 16 sectors per track, 35 tracks
 * Bernoulli disk: as dumped by 'dd conv=noerror,sync" to ensure symmetric sizes; 20MB cartridge assumed
 *  - 10MB cartridge support could be trivially added if the start location of the FAT were known
 *
 */
public class ExtractHPFiles
{

	public static void main(java.lang.String[] args)
	{
		String outputDirectory = "";
		int fileStart = 0, fileLength = 0, fileType = 0;
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
				if (inData.length < 21430272) // If the image is smaller than a Bernoulli disk, assume it's a little floppy
				{
					/*
					 * Catalog starts on track 0, second sector and stretches to the
					 * end of the track.
					 * 
					 * Catalog entries are 32 (0x20) bytes long; stuff we know/care
					 * about: bytes 0x00-0x0a: Filename (space padded) bytes
					 * 0x0e-0x0f: File start (in sectors) bytes 0x12-0x13: File
					 * length (in sectors)
					 */
					for (int i = 0x0200; i < 0x1000; i += 0x20)
					{
						String filename = "";
						if ((inData[i] != 0x00) && (inData[i] != -1) && (inData[i + 0x0a] != -1))
						{
							fileStart = UnsignedByte.intValue(inData[i + 0x0f], inData[i + 0x0e]) * 256;
							fileLength = UnsignedByte.intValue(inData[i + 0x13], inData[i + 0x12]) * 256;
							int j;
							for (j = 0; j < 10; j++)
							{
								if (inData[j + i] != 0x00)
								{
									filename += (char) inData[j + i];
								}
								else
									break;
							}
							// Find the end-of-file marker, trim down to that length
							boolean foundEOF = false;
							for (j = fileStart + fileLength - 1; j > fileStart; j--)
							{
								// System.err.println("file start: "+fileStart+" file length: "+fileLength+" j: "+j);
								if ((inData[j] == -1) && (inData[j+1] == -1))
								{
									// System.err.println("Found final 0xffff at "+(j-fileStart));
									foundEOF = true;
									break;
								}
								// System.err.println(Integer.toHexString(UnsignedByte.intValue(inData[j])));
								if (UnsignedByte.intValue(inData[j]) == 0xff)
								{
									continue;
								}
								if (UnsignedByte.intValue(inData[j]) == 0xef)
								{
									// System.err.println("Found a trailing 0xef at "+(j-fileStart));
									foundEOF = true;
									break;
								}
							}
							if (foundEOF)
								fileLength = j - fileStart;
							// System.out.println("Found file: "+filename+" Start: 0x"+Integer.toHexString(fileStart)+" End: 0x"+Integer.toHexString(fileStart+fileLength-1)+" Length: 0x"+Integer.toHexString(fileLength));
							filename = filename.trim();
							if ((filename.length() > 0) && (fileLength > 0))
							{
								FileOutputStream out;
								try
								{
									String fullname = new String(outputDirectory + File.separator + filename);
									if (fileStart + fileLength < inData.length)
									{
										out = new FileOutputStream(fullname);
										System.err.println("Creating file: " + fullname);
										out.write(inData, fileStart, fileLength);
										out.flush();
										out.close();
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
						else
							break;
					}
 				}
				else
				// It's a bigger (i.e. Bernoulli) image
				{
					/*
					 * Catalog starts at 0xa10000 and stretches to A2ffff on 20MB cartridges, right around the center.
					 * 
					 * Catalog entries are 32 (0x20) bytes long; stuff we know/care about:
					 * bytes 0x00: (Unknown file marker)
					 * bytes 0x01-0x09: Filename (space padded)
					 * bytes 0x0a-0x0f: User name
					 * bytes 0x10-0x11: File type
					 * bytes 0x12-0x13: File start (in sectors)
					 * bytes 0x14-0x15: Final (or next available) sector of file
					 */
					for (int i = 0xa10000; i < 0xa30000; i += 0x20)
					{
//						if (UnsignedByte.intValue(inData[i]) == 0x98) // Good/live file marker
						if (UnsignedByte.intValue(inData[i]) != 0x00) // Anything but null
						{
							String filename = "", fileSuffix = "", filePrefix = "", fileTypeString = "";
							fileStart = UnsignedByte.intValue(inData[i + 0x13], inData[i + 0x12]);
							fileType = UnsignedByte.intValue(inData[i + 0x11], inData[i + 0x10]);
							switch (fileType)
							{
								case 0x02: fileTypeString = "source"; break;
								case 0x03: fileTypeString = "reloc"; break;
								case 0x04: fileTypeString = "listing"; break;
								case 0x05: fileTypeString = "link_sym"; break;
								case 0x06: fileTypeString = "emul_com"; break;
								case 0x07: fileTypeString = "link_com"; break;
								case 0x08: fileTypeString = "trace"; break;
								case 0x0a: fileTypeString = "data"; break;
								case 0x0c: fileTypeString = "asmb_sym"; break;
								case 0x0d: fileTypeString = "absolute"; break;
								case 0x0e: fileTypeString = "comp_sym"; break;
								default: break;
							}
							int j;
							for (j = 1; j < 10; j++)
							{
								if (inData[j + i] != 0x00)
								{
									filePrefix += (char) inData[j + i];
								}
								else
									break;
							}
							for (j = 10; j < 16; j++)
							{
								if (inData[j + i] != 0x00)
								{
									fileSuffix += (char) inData[j + i];
								}
								else
									break;
							}
							filename = filePrefix.trim() + "." + fileSuffix.trim() + "." + fileTypeString;
							// System.out.println("Found file: "+filename+" Start: 0x"+Integer.toHexString(fileStart)+" End: 0x"+Integer.toHexString(fileEnd)+" Length: 0x"+Integer.toHexString(fileLength));
							filename = filename.trim();
							if ((filename.length() > 0) && (fileTypeString.length() > 0))
							{
								FileOutputStream out;
								try
								{
									String fullname = new String(outputDirectory + File.separator + filename);
									if (fileStart + fileLength < inData.length)
									{
										out = new FileOutputStream(fullname);
										System.err.println("Creating file: " + fullname);
										dumpFileChain(out, inData, fileStart, 0x10000, 1);
										out.flush();
										out.close();
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

	public static void dumpFileChain(FileOutputStream out, byte[] inData, int currentSector, int preambleOffset, int firstSectorComp) throws IOException
	{
		int realOffset = currentSector * 0x1000 + preambleOffset;
		int nextSector = UnsignedByte.intValue((byte) inData[realOffset + 0xfff], (byte) inData[realOffset + 0xffe]);
		/*
		System.err.println("dumpFileChain: currentSector: " + Integer.toHexString(currentSector) + 
				" nextSector: "+ Integer.toHexString(nextSector) + 
				" nS pointer address: " + Integer.toHexString(realOffset + 0xfff));
		*/
		if (realOffset < inData.length)
		{
			// System.err.println("dumpFileChain: realOffset: "+Integer.toHexString(realOffset));
			byte range[] = Arrays.copyOfRange(inData, realOffset+2+firstSectorComp, realOffset + 0xffe);
			out.write(range);
			// System.err.println("dumpFileChain: nextSector: "+ Integer.toHexString(nextSector));
			if ((nextSector != 0xffff) && // Not standard end of sector chain
					(nextSector != 0) &&  // Not zero, which is almost certainly an error
					(nextSector * 0x1000 + preambleOffset < inData.length) && // Lies within the image
					(nextSector != currentSector)) // Not the sector we just came from
				// Still have to worry about loops... those won't be detected
				dumpFileChain(out, inData, nextSector, preambleOffset, 0);
		}
	}

	public static String describe(boolean verbose)
	{
		return "Extract files from Hewlett-Packard disk images."+
				(verbose?"  These are typically from HP instrument (not LIF) disks.":"");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractHPFiles " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: ExtractHPFiles infile [out_directory]");
	}
}