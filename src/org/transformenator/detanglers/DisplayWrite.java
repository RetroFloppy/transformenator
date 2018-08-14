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

import java.io.File;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class DisplayWrite extends ADetangler
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String inFile, String outDirectory, String fileSuffix)
	{
		if ((inData.length > 0x66) && ((UnsignedByte.intValue(inData[0x64]) == 0xaa) && (UnsignedByte.intValue(inData[0x65]) == 0xaa) && (UnsignedByte.intValue(inData[0x66]) == 0xaa)))
		{
			/*
			 * Make the output directory
			 */
			File baseDirFile = new File(outDirectory);
			if (!baseDirFile.isAbsolute())
			{
				baseDirFile = new File("." + File.separator + outDirectory);
			}
			baseDirFile.mkdirs();
			/*
			 * Pick apart the file chunk indices. Chunk indices start at 0x6b and follow 3 bytes of 0xaa. There are a maximum of 59 indices.
			 * 
			 * Each index is a pointer to a hunk at 512 bytes * the index number in the file.
			 */
			byte[] newBuf = new byte[inData.length];
			int newBufCursor = 0, bytesFound;
			for (int i = 0x6b; i < 0x200; i += 7)
			{
				// System.err.println("DEBUG: Asking for chunk #"+(1+(i-0x6b)/7));
				bytesFound = grabDisplayWriteChunk(inData, newBuf, i, newBufCursor);
				// System.err.println("DEBUG: Got chunk, pulled "+bytesFound+" bytes.");
				newBufCursor += bytesFound;
			}
			if (inData.length > 0x11200)
			{
				for (int i = 0x11010; i < 0x11200; i += 7)
				{
					bytesFound = grabDisplayWriteChunk(inData, newBuf, i, newBufCursor);
					// System.err.println("DEBUG: Got chunk, pulled "+bytesFound+" bytes.");
					newBufCursor += bytesFound;
				}
				if (inData.length > 0x25100)
					for (int i = 0x25010; i < 0x25100; i += 7)
					{
						bytesFound = grabDisplayWriteChunk(inData, newBuf, i, newBufCursor);
						// System.err.println("DEBUG: Got chunk, pulled "+bytesFound+" bytes.");
						newBufCursor += bytesFound;
					}
			}
			inData = new byte[newBufCursor];
			for (int i = 0; i < newBufCursor; i++)
				inData[i] = newBuf[i];
			// System.err.println("DEBUG: Data length after de-indexing: "+inData.length);
			interpreter.emitFile(inData, outDirectory + File.separator + inFile + fileSuffix);
		}
		else
		{
			System.err.println("Probably not a DisplayWrite file.");
		}
	}

	public int grabDisplayWriteChunk(byte[] inData, byte[] newBuf, int i, int newBufCursor)
	{
		int len = 0;
		if (((UnsignedByte.intValue(inData[i]) == 0xaa) && (UnsignedByte.intValue(inData[i + 1]) == 0xaa) && (UnsignedByte.intValue(inData[i + 2]) == 0xaa)) || ((UnsignedByte.intValue(inData[i]) == 0x00) && (UnsignedByte.intValue(inData[i + 1]) > 0x00) && (UnsignedByte.intValue(inData[i + 2]) == 0x00)))
		{
			int idx = UnsignedByte.intValue(inData[i + 4], inData[i + 3]);
			len = UnsignedByte.intValue(inData[i + 6], inData[i + 5]) + 6;
			// System.err.println("DEBUG: idx: 0x"+UnsignedByte.toString(idx)+" length: "+len);
			if (idx < 32768)
			{
				if (((idx * 512) + 1) < inData.length)
				{
					// System.err.println("DEBUG: Pulling data from "+idx*512+" to "+((idx*512)+len)+".");
					/*
					 * Need to hunt for the SOT. It will be 3 bytes: 0xe80700.
					 */
					int offset = 0;
					for (int j = 0; j < 0xff; j++)
					{
						if ((UnsignedByte.intValue(inData[(idx * 512) + j + 0]) == 0xe8) && (UnsignedByte.intValue(inData[(idx * 512) + j + 1]) == 0x07) && (UnsignedByte.intValue(inData[(idx * 512) + j + 2]) == 0x00))
						{
							offset = j + 3;
							// System.err.println("DEBUG: Found start of text at offset 0x"+UnsignedByte.toString(offset));
						}
					}
					if (offset == 0)
					{
						System.err.println("No SOT found for index 0x" + UnsignedByte.toString(idx) + ".");
					}
					// Pull out the data in the chunk
					else
					{
						if (idx * 512 + len > inData.length)
							len = inData.length - (idx * 512);
						for (int k = offset; k < len; k++)
						{
							newBuf[newBufCursor++] = inData[(idx * 512) + k];
						}
					}
				}
				else
					System.err.println("Found an index out of bounds: " + idx);
			}
		}
		return len;
	}

}
