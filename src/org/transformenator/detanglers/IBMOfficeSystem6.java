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

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.OfficeSys6Util;
import org.transformenator.internal.UnsignedByte;

public class IBMOfficeSystem6 extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		int indexOffset = 0x591b;
		// System.err.println("DEBUG: initial indexOffset: " + indexOffset + " max indexOffset: " + 0x597d);
		int j;
		/* Now pull the files out of the image. */
		for (int i = 0xd00; i < 0xd00 + 5 * 4096; i += 512)
		{
			/*
			 * Directory
			 */
			if (inData[i + 1] == 0x0a)
			{
				int len = 0;
				for (int k = 0; k < 37; k++)
				{
					if (UnsignedByte.intValue(inData[i + 0x1db + k]) == 0xe2)
					{
						len = k;
						// System.err.println("Found end at: "+k);
					}
				}
				String filename = OfficeSys6Util.toAscii(inData, i + 0x1db, len);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				// System.err.println("DEBUG: found file: " + filename);
				int trackIndex = 0;
				for (j = indexOffset; j < 0x597d; j++)
				{
					if ((UnsignedByte.intValue(inData[j]) == 0xca))
					{
						// System.err.println("DEBUG: Found a file track index...");
						for (trackIndex = j + 1; trackIndex < 0x597d; trackIndex++)
						{
							// System.err.println("DEBUG: Found a track: "+UnsignedByte.toString(inData[trackIndex]));
							if ((UnsignedByte.intValue(inData[trackIndex]) == 0xca) || (UnsignedByte.intValue(inData[trackIndex]) == 0xc0))
							{
								break;
							}
							else
							{
								out.write(inData, trackToOffset(inData[trackIndex]), 4096);
							}
						}
						j = trackIndex;
						break;
					}
				}
				parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename + fileSuffix);
				// System.err.println("DEBUG: Setting indexOffset to: " + j);
				indexOffset = j;
			}
		}
	}
	public static void dumpTrack(byte[] inData, int offset, ByteArrayOutputStream out)
	{

		try
		{
			out.write(inData, offset, 4096);
			out.flush();
			// System.err.println("DEBUG: Dumped track:  "+UnsignedByte.toString(offsetToTrack(offset))+" from 0x"+Integer.toHexString(offset)+"...appended: "+append);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static int trackToOffset(int track)
	{
		return (track - 1) * 4096 + 0xd00;
	}

	public static int offsetToTrack(int offset)
	{
		return (offset - 0xd00) / 4096 + 1;
	}

}
