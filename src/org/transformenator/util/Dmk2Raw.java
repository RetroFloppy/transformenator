/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2017 by David Schmidt
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
 * ConvertCatweaselDatastream
 * 
 *
 */
public class Dmk2Raw
{
	public static void main(java.lang.String[] args)
	{
		FileOutputStream out = null;
		String outputFile = "";
		Boolean rx01Mode = false;
		if (args.length == 2)
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
				// Read in the entire dmk file
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
				outputFile = args[1];
				try
				{
					out = new FileOutputStream(outputFile);
				}
				catch (FileNotFoundException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.err.println("Creating file: " + outputFile);
				// Ready to go.  Time to face the music.
				int numTracks = result[1];
				int trackLength = UnsignedByte.intValue(result[2], result[3]);
				System.err.println("Number of tracks: "+numTracks+" Track length: "+trackLength);
				int zeroOffset = 0x10;
				int trackOffset = 0;
				int dataIndex = 0;
				int sectorOffset = 0;
				int i;
				sectorOffset = UnsignedByte.intValue(result[trackOffset],result[trackOffset+1]);
				dataIndex = trackOffset + sectorOffset + 50;
				int accum1 = 0, accum2 = 1;
				boolean hasData = false;
				int track = 1;
				// Search for non-zero data, and see if it repeats 000011112222 (i.e. rx01)
				// or differs: 00112233 (i.e. rx02)
				while (hasData == false)
				{
					trackOffset = zeroOffset + track * trackLength;
					accum1 = 0;
					accum2 = 0;
					for (i = 0; i < 26624; i += 2)
					{
						accum1 = inData[dataIndex + i];
						accum2 = inData[dataIndex + i + 1];
						if (accum1 > 0)
							hasData = true;
					}
					track ++;
				}
				if (accum1 == accum2)
				{
					System.out.println("Repeating data found; assuming RX01 format.");
					rx01Mode = true;
				}
				for (i = 0; i < numTracks; i++)
				{
					trackOffset = zeroOffset + i * trackLength;
					// System.err.println("Track "+i+" offset: "+Integer.toHexString(trackOffset)+" Value there: 0x"+Integer.toHexString(UnsignedByte.intValue(result[trackOffset],result[trackOffset+1])));
					sectorOffset = UnsignedByte.intValue(result[trackOffset],result[trackOffset+1]);
					int index = 0;
					dataIndex = 0;
					while (sectorOffset > 0)
					{
						// System.err.println("  Sector: "+index/2);
						dataIndex = trackOffset + sectorOffset + 50;
						try
						{
							if (rx01Mode)
							{
								for (int j = 0; j < 256; j++)
								{
									if (j %2 == 0)
										out.write(inData, dataIndex+j, 1);
								}
							}
							else
							{
								out.write(inData, dataIndex, 256);
							}
							// System.err.println("  Offset to data in sector "+index/2+": "+Integer.toHexString(dataIndex)+ "  "+Integer.toHexString(sectorOffset));
						} 
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						index += 2;
						sectorOffset = UnsignedByte.intValue(result[trackOffset+index],result[trackOffset+index+1]);
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

	public static String describe(boolean verbose)
	{
		return "Convert DMK disk image file to raw data."+
				(verbose?"  Autodetects DEC RX01 format and halves the data.":"");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("Dmk2Raw " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: Dmk2Raw infile outfile");
	}
}