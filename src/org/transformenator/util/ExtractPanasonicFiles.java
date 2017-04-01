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
import org.transformenator.internal.UnsignedByte;

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
		// System.err.print("Filename: " + filename + "  File number: " + UnsignedByte.toString(filenumber));
		// System.err.println();
		for (int i = 0x100; i < 0x1c00; i+=0x20)
		{
			String filename = "";
			int result = 0, j;
			for (j = 0x10; j < 0x19; j++)
				result += inData[i+j];
			if (result == 0x10)
			{
				filename = "";
				for (j = 0x00; j < 0x0a; j++)
				{
					if (UnsignedByte.intValue(inData[i+j]) != 0xe5)
						filename += (char)inData[i+j];
				}
				filename = filename.trim();
				System.err.println("Creating file: " + baseName + filename);
				try
				{
					currentOut = new FileOutputStream(baseName + filename);
					int fileHead = UnsignedByte.intValue(inData[i+0x1a],inData[i+0x1b]);
					fileHead = (fileHead * 0x400) + 0x1400 + 0x110;
					for (j = fileHead; j < inData.length - fileHead; j++)
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

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractPanasonicFiles " + Version.VersionString + " - Extract files from Panasonic word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractPanasonicFiles infile [out_directory]");
	}
}