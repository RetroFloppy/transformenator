/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 by David Schmidt
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

package org.transformenator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class RevealDirectoryEntries
{

	public static void main(java.lang.String[] args)
	{
		if (args.length == 2)
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
				for (int i = 0x2800; i < 0x2C00; i += 0x20)
				{
					if (isValdocFile(inData, i))
						// Make the directory entry visible
						inData[i] = 0x00;
				}
				for (int i = 0x5000; i < 0x5400; i += 0x20)
				{
					if (isValdocFile(inData, i))
						// Make the directory entry visible
						inData[i] = 0x00;
				}
				// Cleaned up the directory - now write the resulting image
				FileOutputStream out;
				try
				{
					out = new FileOutputStream(args[1]);
					out.write(inData);
					out.flush();
					out.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static boolean isValdocFile(byte inData[], int offset)
	{
		boolean retval = false;
		int i;
		if (inData[offset] == 0x60)
		{
			for (i = 1; i < 9; i++)
			{
				if ((inData[offset + i] < 0x30) || (inData[offset + i] > 0x39))
				{
					// Non-numeric data
					retval = false;
					break;
				}
				retval = true;
			}
			if (retval == true)
			{
				if ((inData[offset + 9] == 0x56) && (inData[offset + 10] == 0x41) && (inData[offset + 11] == 0x4c))
					retval = true; // Yes, redundant
				else
					retval = false;
			}
		}
		return retval;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("RevealDirectoryEntries v1.0 - perform transformation operations on image files.");
		System.err.println();
		System.err.println("Syntax: RevealDirectoryEntries inputFile outputFile");
	}

}