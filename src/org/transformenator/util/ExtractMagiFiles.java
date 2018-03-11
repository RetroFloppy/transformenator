/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2016 by David Schmidt
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
 * ExtractMagiFiles
 * 
 * Helper app to pull the files off of the virtual file system of a
 * Magi Major Leaguer 8" disk image.
 * 
 * Disk geometry: 1 side, 128 bytes per sector, 26 sectors per track, 77 tracks
 *
 */
public class ExtractMagiFiles
{

	public static void main(java.lang.String[] args)
	{
		FileOutputStream out;

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
					baseDirFile.mkdir();
				}
				/*
				 * Catalog resides on the first track only.
				 * 
				 * Catalog entries are 16 (0x10) bytes long:
				 * bytes 0x00-0x01: File length (in 128 byte sectors)
				 * bytes 0x02-0x03: Starting sector (MSB * track + LSB * sector)
				 * byte 0x04: 0x81 - system file; 0x01 - regular file
				 * bytes 0x05-0x0c: Filename
				 * bytes 0x0d-0x0f: File suffix
				 * File suffix of "KIL" likely means deleted file
				 *
				 */
				for (int i = 0x20; i < 0xd00; i += 0x10)
				{
					String filename = "";
					String suffix = "";
					if (inData[i + 04] == 0x01)
					{
						int j;
						for (j = 0x05; j <= 0x0c; j++)
						{
							filename += (char) inData[j + i];
						}
						filename = filename.trim();
						for (j = 0x0d; j <= 0x0f; j++)
						{
							suffix += (char) inData[j + i];
						}
						if (!suffix.equals("KIL"))
						{
							byte fnb[];
							filename = filename + "." + suffix;
							int startAddress = (UnsignedByte.intValue(inData[i + 2]) - 1) * 128 +  (UnsignedByte.intValue(inData[i + 3]) * 128 * 26);
							int length =  UnsignedByte.intValue(inData[i],inData[i + 1]);
							// System.out.println("Creating file: " + filename + "  Start: "+Integer.toHexString(startAddress)+" Length: "+Integer.toHexString(length));
							try
							{
								String fullname;
								if (args.length == 2)
								{
									fullname = new String(args[1]) + File.separator + filename;
								}
								else
									fullname = filename;
								out = new FileOutputStream(fullname);
								System.err.println("Creating file: " + fullname);
								fnb = Arrays.copyOfRange(inData, startAddress, startAddress + length*128);
								out.write(fnb);
								out.flush();
								out.close();
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
		else
		{
			// wrong args
			help();
		}
	}

	
	public static void help()
	{
		System.err.println();
		System.err.println("ExtractMagiFiles " + Version.VersionString + " - Extract files from Magi Major Leaguer disk images.");
		System.err.println();
		System.err.println("Usage: ExtractMagiFiles infile [out_directory]");
	}

}