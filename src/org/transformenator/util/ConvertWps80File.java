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
 * ConvertWps80File
 * 
 * The intent of this helper app is to convert the block structure of DEC Rainbow WPS-80 files.
 * (WPS-80 was marketed by Exceptional Business Solutions (EBS) running on CP/M on the Rainbow.)
 * They have a forward-and-backward linked list structure to 256-byte blocks with inner used-length,
 * very much like WANG did.
 */
public class ConvertWps80File
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
				System.err.println("Read " + inData.length + " bytes.");
				unwindWpsFile(inData, args[1]);
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void unwindWpsFile(byte[] inData, String fileName)
	{
		FileOutputStream out;
		if ((inData.length % 256) > 0)
		{
			System.err.println("Warning: file size is not an integral of 256, this may not be a WPS-80 file");
		}
		try
		{
			out = new FileOutputStream(fileName);
			System.err.println("Creating file: " + fileName);

			/*
			 * The WPS-80 word processor leaves a set of 256-byte chunks, each of which has
			 * a preamble:
			 * next-pointer (2 bytes)
			 * prev-pointer (2 bytes)
			 * unknown (1 byte)
			 * bytes-used (1 byte) 
			 */
			int nextPage = nextWpsPage(inData, 1);
			while (nextPage > 0)
			{
				nextPage = dumpWpsPage(out, inData, nextPage);
			}
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	static int nextWpsPage(byte[] inData, int thisPage)
	{
		int thisOffset = (thisPage - 1) * 256;
		return UnsignedByte.intValue(inData[thisOffset + 1]) + UnsignedByte.intValue(inData[thisOffset]);
	}

	static int dumpWpsPage(FileOutputStream out, byte[] inData, int thisPage) throws IOException
	{
		int nextPage = 0;
		int thisOffset = (thisPage - 1) * 256;
		if (thisOffset < inData.length)
		{
			nextPage = nextWpsPage(inData,thisPage);
			int prevPage = UnsignedByte.intValue(inData[thisOffset + 3]) + UnsignedByte.intValue(inData[thisOffset + 2]);
			int numBytes = UnsignedByte.intValue(inData[thisOffset + 5]);
			int nextOffset = 0;
			int prevOffset = 0;
			if (nextPage > 0)
				nextOffset = (nextPage - 1) * 256;
			if (prevPage > 0)
				prevOffset = (prevPage - 1) * 256;
			System.err.println("offset: 0x" + Integer.toHexString(thisOffset) + " bytes: " + numBytes + " next: " + nextPage + " @offset: 0x" + Integer.toHexString(nextOffset) + " prev: " + prevPage + " @offset: 0x" + Integer.toHexString(prevOffset));
			if (numBytes > 0)
			{
				byte range[] = Arrays.copyOfRange(inData, thisOffset + 6, thisOffset + 6 + numBytes);
				out.write(range);
				out.flush();
			}
		}
		return nextPage;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ConvertWps80File " + Version.VersionString + " - Untangle DEC Rainbow CP/M WPS-80 word processor file.");
		System.err.println();
		System.err.println("Usage: ConvertWps80File <infile> <outfile>");
	}
}