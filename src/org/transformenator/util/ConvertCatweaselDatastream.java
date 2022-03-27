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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.transformenator.internal.UnsignedByte;
import org.transformenator.internal.Version;

/*
 * ConvertCatweaselDatastream
 * 
 *
 */
public class ConvertCatweaselDatastream
{
	public static int STATE_SEEKING_AM1 = 1;
	public static int STATE_SEEKING_AM2 = 2;
	public static int STATE_SEEKING_ID1 = 3;
	public static int STATE_SEEKING_ID2 = 4;
	public static int STATE_SEEKING_ID3 = 5;
	public static int STATE_SEEKING_CRC = 6;
	public static boolean DEBUG = false;
	public static int accum = 0;
	public static int bits = 0;
	public static int state = STATE_SEEKING_AM1;

	public static void main(java.lang.String[] args)
	{
		String outputFile = "";
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
				// Read in the entire cwtool raw output file (our input file)
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
					outputFile = args[1];
				}
				// Ready to go.  Time to face the music.
				int cursor = 0x20; // Skip over the cwtool preamble
				int thisTrack;
				int offset;
				int nextTrack = 0;
				while (nextTrack < inData.length)
				{
					thisTrack = UnsignedByte.intValue(inData[cursor + 1], inData[cursor + 2]);
					offset = UnsignedByte.intValue(inData[cursor + 4], inData[cursor + 5]) + (UnsignedByte.intValue(inData[cursor + 6], inData[cursor + 7]) * 65536);
					nextTrack = cursor + offset + 0x08;
					if (thisTrack % 2 == 0) // only even tracks
					{
						System.out.println("thisTrack: " + thisTrack + " offset: 0x" + Integer.toHexString(offset) + " nextTrack: 0x" + Integer.toHexString(nextTrack) + " cursor: 0x" + Integer.toHexString(cursor));
						cursor += 8; // Now we're at the start of this track's data
						createHistogram(inData, cursor, offset);
						// analyzeTrack(inData, cursor, offset);
					}
					cursor = nextTrack;
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void createHistogram(byte[] trackData, int start, int length)
	{
		int histogram[] = new int[256];

		for (int i = 0; i < length; i++)
		{
			histogram[UnsignedByte.intValue(trackData[start + i])]++;
		}
		for (int i = 9; i < 32; i++)
		{
			System.out.println("i: " + i + " " + histogram[i]);
		}
	}

	public static void analyzeTrack(byte[] trackData, int start, int length)
	{
		for (int i = 0; i < length; i++)
		{
			processInterval(UnsignedByte.intValue(trackData[start + i]));
		}
		System.out.println();
	}

	double adj = 0.0;

	public static long processInterval(int sample)
	{
		int len;
		int mfmthresh1 = 17;
		int mfmthresh2 = 23;

		if (sample <= mfmthresh1)
		{
			/* Short */
			len = 2;
		}
		else if (sample <= mfmthresh2)
		{
			/* Medium */
			len = 3;
		}
		else
		{
			/* Long */
			len = 4;
		}
		processBit(true);
		while (--len > 0)
			processBit(false);
		return accum;
	}

	public static void processBit(boolean bit)
	{
		bits++;
		if (bit)
		{
			accum = (accum << 1) + 1;
		}
		else
		{
			accum = (accum << 1) + 0;
		}
		if (state == STATE_SEEKING_ID1)
		{
			// We have previously found an AM1 mark; wait for 32 bits to pass
			if (bits == 32)
			{
				// System.out.println("ID1 accum: 0x"+Integer.toHexString(accum));
				System.out.println("ID1: Track: " + intMFM());
				accum = 0;
				bits = 0;
				state = STATE_SEEKING_ID2;
			}
		}
		if (state == STATE_SEEKING_ID2)
		{
			// We have previously found ID1; wait for 32 more bits to pass
			if (bits == 32)
			{
				int id2 = intMFM();
				// System.out.println("ID2 accum: 0x"+Integer.toHexString(accum));
				System.out.println("ID2: Sector: " + UnsignedByte.loByte(id2) + " Recs/track: " + UnsignedByte.hiByte(id2));
				accum = 0;
				bits = 0;
				state = STATE_SEEKING_ID3;
			}
		}
		if (state == STATE_SEEKING_ID3)
		{
			// We have previously found an ID2; wait for 32 more bits to pass
			if (bits == 32)
			{
				// System.out.println("ID3 accum: 0x"+Integer.toHexString(accum));
				System.out.println("ID3: " + Integer.toHexString(intMFM()));
				accum = 0;
				bits = 0;
				state = STATE_SEEKING_AM2;
			}
		}
		// System.out.println("accum: 0x"+Integer.toHexString(accum) + " accum & 0x7ffff: 0x"+Integer.toHexString(accum & 0x7ffff));
		if (accum == 0x11112244)
		{
			// System.out.println("accum: 0x"+Integer.toHexString(accum));
			System.out.println("Found AM1 field.");
			state = STATE_SEEKING_ID1;
			accum = 0;
			bits = 0;
		}
		if (accum == 0x11112245)
		{
			// System.out.println("accum: 0x"+Integer.toHexString(accum));
			System.out.println("Found AM2 field.");
			state = STATE_SEEKING_AM1;
			accum = 0;
			bits = 0;
		}
	}

	public static int intMFM()
	{
		// System.out.println("intMFM, accum: 0x"+Integer.toHexString(accum));
		int retVal = 0;
		// Remove clock bits, pull out 16 data bits
		for (int i = 0; i < 32; i++)
		{
			if (i % 2 == 1)
			{
				retVal = (retVal << 1) + ((accum & 0x80000000) == 0x80000000 ? 1 : 0);
				// System.out.println("  accum: "+(accum & 0x80000000)+" adding bit "+i+": "+((accum & 0x80000000) == 0x80000000 ? 1 : 0));
			}
			// System.out.print("accum before: "+Integer.toHexString(accum));
			accum = accum << 1;
			// System.out.println(" after: "+Integer.toHexString(accum));
		}
		// System.out.println (" intMFM retVal: 0x"+Integer.toHexString(retVal));
		return retVal;
	}

	public static String describe(boolean verbose)
	{
		return "Convert Catweasel cwtool raw output to disk image file."+
				(verbose?"":"");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ConvertCatweaselDatastream " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: ConvertCatweaselDatastream infile [outfile]");
	}
}