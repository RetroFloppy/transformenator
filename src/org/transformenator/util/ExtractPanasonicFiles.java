/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2015 - 2018 by David Schmidt
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
 * ExtractPanasonicFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of Panasonic KX-*
 * word processor disks.
 *
 */
public class ExtractPanasonicFiles
{
	public static String baseName;
	public static FileOutputStream currentOut = null;

	public static void main(java.lang.String[] args)
	{
		baseName = "";
		if ((args.length == 1) || (args.length == 2))
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
				baseName = new String(args[1]) + File.separator;
			}
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
					System.err.println("Read " + result.length + " bytes.");
					/*
					 * We have the whole sheebang pulled into 'result' now.
					 */
					emitFiles(result, baseName);
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
		}
		else
		{
			// wrong args
			help();
		}
	}

	static void emitFiles(byte[] inData, String baseName)
	{
		// Panasonic disks came in SS and DS sizes.  Inspect the image size; if it's larger than SS * 1.5, it's DS.
		int diskSize = inData.length > (360 * 1024 * 1.5)? SIZE_DS: SIZE_SS;
		for (int i = 0x100; i < 0x1c00; i+=0x20)
		{
			String filename = "";
			int j, result = 0;
			for (j = zeroesBegin[diskSize]; j < zeroesBegin[diskSize] + 0x09; j++)
				result += inData[i+j];
			filename = "";
			for (j = 0x00; j < 0x0a; j++)
			{
				if (UnsignedByte.intValue(inData[i+j]) != 0xe5)
					filename += (char)inData[i+j];
			}
			filename = filename.trim();
			if ((filename.length() > 0) && (result == 0))
			{
				System.err.println("Creating file: " + baseName + filename);
				try
				{
					currentOut = new FileOutputStream(baseName + filename);
					int fileHead = UnsignedByte.intValue(inData[i+0x1a],inData[i+0x1b]);
					int fileLength = UnsignedByte.intValue(inData[i+0x1c],inData[i+0x1d]);
					fileHead = (fileHead * 0x400) + fileHeadOffset[diskSize];
					fileLength = fileLength - 0x100;
					for (j = fileHead; j < fileHead+fileLength; j++)
					{
						if (UnsignedByte.intValue(inData[j]) != 0xc5)
							currentOut.write(inData[j]);
						else
							break;
					}
					currentOut.flush();
					currentOut.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	static String normalizeName(String name)
	{
		/*
		 * Some characters that might come out in a filename are illegal these days - so modify those
		 */
		char[] newName = name.toCharArray();
		if (name != null)
		{
			char c;
			for (int i = 0; i < newName.length; i++)
			{
				c = newName[i];
				switch (c)
				{
				case '/':
					newName[i] = '-';
					break;
				case ':':
					newName[i] = '-';
					break;
				case '&':
					newName[i] = '_';
					break;
				}
			}
		}
		return new String(newName).trim();
	}

	static byte[] followFile(byte[] inData, byte fileNumber)
	{
		/*
		 * Make one contiguous array of data out of a file number
		 */
		byte[] fileContents = null;
		// System.err.println("Following file 0x" + UnsignedByte.toString(fileNumber));
		int count = 0;
		for (int i = 0; i < 640; i++)
		{
			if (inData[i] == fileNumber)
			{
				count++;
			}
		}
		if (count > 0)
		{
			fileContents = new byte[count * 256];
			count = 0; // Start over counting
			for (int i = 0; i < 640; i++)
			{
				if (inData[i] == fileNumber)
				{
					for (int j = 0; j < 256; j++)
					{
						fileContents[(count * 256) + j] = inData[(i * 256) + j];
					}
					count++;
				}
			}
		}
		return fileContents;
	}

	public static String describe(boolean verbose)
	{
		return "Extract files from Panasonic KX-* word processor disk images."+
				(verbose?"":"");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractPanasonicFiles " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: ExtractPanasonicFiles infile [out_directory]");
	}

	static int SIZE_SS = 0;
	static int SIZE_DS = 1;
	static int zeroesBegin[] = { 0x0c, 0x0c }; // + 0x09
	static int fileHeadOffset[] = { 0x1110, 0x1510 };
}