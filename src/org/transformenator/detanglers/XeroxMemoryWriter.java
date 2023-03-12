/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2014 - 2018 by David Schmidt
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
 * Pull the files off of the virtual file system of a Seawell 8" disk image.
 * 
 * Disk geometry: 2 sides, 256 bytes per sector, 26 sectors per track, 76 tracks.
 *
 */
package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class XeroxMemoryWriter extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		/* Now pull the files out of the image. */
		for (int i = 0x10100 + 25; i < 0x10100 + 0x100 + 25; i += 19)
		{
			/*
			 * Directory entry structure: 0x00: 3 bytes file number
			 * 0x03: 10 bytes of name 0x0d: 2 bytes something 0x15: 2
			 * bytes first sector pointer 0x17: 2 bytes something
			 */
			int fileNumber = (inData[i] * 1024) + (inData[i + 1] * 256) + inData[i + 2];
			if (fileNumber > 0)
			{
				byte fnb[] = new byte[10];
				/* Extract this file */
				int firstOffset = (inData[i + 15] - 1) * 0x1000 + ((inData[i + 16] - 1) * 256);
				// System.err.print("DEBUG: file number found: " + fileNumber + " first track/sector: " + UnsignedByte.toString(inData[i + 15]) + "/" + UnsignedByte.toString(inData[i + 16]));
				// System.err.println(" offset:" + Integer.toHexString(firstOffset));
				fnb = Arrays.copyOfRange(inData, i + 3, i + 12);
				String fileName = new String(fnb).trim();
				ByteArrayOutputStream out;
				if (!fileName.isEmpty())
				{
					try
					{
						out = new ByteArrayOutputStream();
						decodeFile(inData, out, firstOffset);
						out.flush();
						parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void decodeFile(byte[] inData, ByteArrayOutputStream out, int sectorOffset) throws IOException
	{
		/*
		 * A sector has its first few bytes listing the following: 0x00
		 */
		int nextOffset = ((UnsignedByte.intValue(inData[sectorOffset + 2]) - 1) * 0x10 + (UnsignedByte.intValue(inData[sectorOffset + 3]) - 1)) * 0x100;
		int realOffset = ((UnsignedByte.intValue(inData[sectorOffset + 2])) * 0x10 + (UnsignedByte.intValue(inData[sectorOffset + 3]))) * 0x100;
		int rangeEnd = UnsignedByte.intValue(inData[sectorOffset + 4]) - 1;
		// System.err.println("rangeEnd: " + rangeEnd);
		if (rangeEnd > 0)
		{
			byte range[] = Arrays.copyOfRange(inData, sectorOffset + 8, sectorOffset + rangeEnd + 9);
			out.write(range);
		}

		if (realOffset > 0)
			decodeFile(inData, out, nextOffset);
	}

	public static String offsetToTS(int offset)
	{
		int track = offset / 0x100;
		int sector = UnsignedByte.loByte(offset / 0x10) / 0x010;
		return Integer.toHexString(track) + "/" + sector;
	}

}
