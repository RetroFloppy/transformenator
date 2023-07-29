/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2017 - 2023 by David Schmidt
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

import java.io.IOException;
import java.io.ByteArrayOutputStream;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

/*
 * Dmk2Raw - convert DMK format to raw, linear data
 *
 */
public class Dmk2Raw extends ADetangler
{
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean debugMode)
	{
    ByteArrayOutputStream out = new ByteArrayOutputStream();
		Boolean rx01Mode = false;
    String fileName = inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length()));
		if (fileSuffix.equals(""))
			fileName += ".img";
		else
      fileName += fileSuffix;
		// Ready to go.  Time to face the music.
		int numTracks = inData[1];
		int trackLength = UnsignedByte.intValue(inData[2], inData[3]);
		int zeroOffset = 0x10;
		int trackOffset = 0;
		int dataIndex = 0;
		int sectorOffset = 0;
		int i;
		sectorOffset = UnsignedByte.intValue(inData[trackOffset],inData[trackOffset+1]);
		dataIndex = trackOffset + sectorOffset + 50;
		long accum1 = 0, accum2 = 0;
		boolean hasData = false;
		// Search for non-zero data, and see if it repeats 000011112222 (i.e. rx01)
		// or differs: 00112233 (i.e. rx02)
		for (i = 4; i < 8; i++)
		{
			trackOffset = zeroOffset + i * trackLength;
			sectorOffset = UnsignedByte.intValue(inData[trackOffset],inData[trackOffset+1]);
			int index = 0;
			dataIndex = 0;
			while (sectorOffset > 0)
			{
				dataIndex = trackOffset + sectorOffset + 50;
				for (int j = 0; j < 256; j += 2)
				{
					accum1 += UnsignedByte.intValue(inData[dataIndex + j]);
					accum2 += UnsignedByte.intValue(inData[dataIndex + j + 1]);
					if (accum1 > 0)
						hasData = true;
				}
				index += 2;
				sectorOffset = UnsignedByte.intValue(inData[trackOffset+index],inData[trackOffset+index+1]);
			}
		}
		if ((accum1 == accum2) && hasData)
		{
			System.out.println("Repeating data found; assuming RX01 format.");
			rx01Mode = true;
		}
		for (i = 0; i < numTracks; i++)
		{
			trackOffset = zeroOffset + i * trackLength;
			// System.err.println("Track "+i+" offset: "+Integer.toHexString(trackOffset)+" Value there: 0x"+Integer.toHexString(UnsignedByte.intValue(result[trackOffset],result[trackOffset+1])));
			sectorOffset = UnsignedByte.intValue(inData[trackOffset],inData[trackOffset+1]);
			int index = 0;
			dataIndex = 0;
			while (sectorOffset > 0)
			{
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
					out.flush();
				} 
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				index += 2;
				sectorOffset = UnsignedByte.intValue(inData[trackOffset+index],inData[trackOffset+index+1]);
			}
    }
    parent.emitFile(out.toByteArray(), outDirectory, "", fileName);
	}
}