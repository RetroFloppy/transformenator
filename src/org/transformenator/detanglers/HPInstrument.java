/*
 * Transformenator - perform transformation operations on files
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

/*
 * HP instrument (not LIF) file extractor
 * 
 * Floppy disk geometry: 2 sides, 256 bytes per sector, 16 sectors per track, 35 tracks
 * Bernoulli disk: as dumped by 'dd conv=noerror,sync" to ensure symmetric sizes; 20MB cartridge assumed
 *  - 10MB cartridge support could be trivially added if the start location of the FAT were known
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class HPInstrument extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		int fileStart = 0, fileLength = 0, fileType = 0;
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
						ByteArrayOutputStream out;
						try
						{
							if (fileStart + fileLength < inData.length)
							{
								out = new ByteArrayOutputStream();
								out.write(inData, fileStart, fileLength);
								out.flush();
								parent.emitFile(out.toByteArray(), outDirectory, inFile, filename + fileSuffix);
							}
							else
								System.err.println("Error: file " + filename + " would exceed the capacity of the disk image.");
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
//				if (UnsignedByte.intValue(inData[i]) == 0x98) // Good/live file marker
				if (UnsignedByte.intValue(inData[i]) != 0x00) // Anything but null
				{
					String filename = "", filePrefix = "", fileTypeString = "";
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
						ByteArrayOutputStream out;
						try
						{
							if (fileStart + fileLength < inData.length)
							{
								out = new ByteArrayOutputStream();
								System.err.println("Creating file: " + filename);
								dumpFileChain(out, inData, fileStart, 0x10000, 1);
								out.flush();
								out.close();
								// Remove the (last) file suffix, if one exists, from the image file name before sending to emitFile()
								parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename);
							}
							else
								System.err.println("Error: file " + filename + " would exceed the capacity of the disk image.");
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

	public static void dumpFileChain(ByteArrayOutputStream out, byte[] inData, int currentSector, int preambleOffset, int firstSectorComp) throws IOException
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

}