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

import java.util.Arrays;
import org.transformenator.internal.UnsignedByte;

public class Canon extends ADetangler
{
	@Override
	public byte[] detangle(byte[] inData) {

		/*
		 * Canon Starwriter (not the early ETW one, but the later proto-WYSIWYG one.)
		 * We still don't know how to order the segments.
		 */
		byte[] newBuf = new byte[inData.length];
		byte fileIdentifier[] = { -57, 0x45, -63, 0x53 }; // Identifier bytes: 0xc745c153
		byte textIdentifier[] = { 0x01, 0x00, 0x00, 0x00, 0x08 }; // Text is coming
		int length, newBufCursor = 0;
		if (Arrays.equals(Arrays.copyOfRange(inData, 0, fileIdentifier.length), fileIdentifier))
		{
			int i;
			// Pick out a new file name - from byte 4 until the first 0x00
			for (i = 4; i < 40; i++)
			{
				if (inData[i] == 0x00)
				{
					newName = new String(Arrays.copyOfRange(inData, 4, i)).trim();
					break;
				}
			}
			for (i = 4; i < inData.length - 8; i ++)
			{
				byte range[] = Arrays.copyOfRange(inData, i, i + textIdentifier.length);
				int outerLength = UnsignedByte.intValue(inData[i - 4]) + 256 * (UnsignedByte.intValue(inData[i - 3]));
				length = UnsignedByte.intValue(inData[i - 2]) + 256 * (UnsignedByte.intValue(inData[i - 1]));
				if (Arrays.equals(range, textIdentifier) && length == outerLength - 10)
				{
					System.err.println("Found text segment @ $" + Integer.toHexString(i) + " for length " + length);
					if (length > 0)
					{
						/*
						newBuf[newBufCursor++] = '<';
						newBuf[newBufCursor++] = 'S';
						newBuf[newBufCursor++] = 'E';
						newBuf[newBufCursor++] = 'G';
						newBuf[newBufCursor++] = '>';
						newBuf[newBufCursor++] = 0x0d;
						newBuf[newBufCursor++] = 0x0a;
						*/
						// Copy this text segment out
						for (int k = 0; k < length; k++)
						{
							newBuf[newBufCursor++] = inData[i + textIdentifier.length + 1 + k];
						}
						// System.err.println("cursor after text: "+newBufCursor);
						/*
						newBuf[newBufCursor++] = 0x0d;
						newBuf[newBufCursor++] = 0x0a;
						newBuf[newBufCursor++] = '<';
						newBuf[newBufCursor++] = '/';
						newBuf[newBufCursor++] = 'S';
						newBuf[newBufCursor++] = 'E';
						newBuf[newBufCursor++] = 'G';
						newBuf[newBufCursor++] = '>';
						*/
					}
					i += length; // Move past this text segment
					newBuf[newBufCursor++] = 0x0d;
					newBuf[newBufCursor++] = 0x0a;
					// System.err.println("cursor after newl: "+newBufCursor);
				}
			}
			inData = Arrays.copyOfRange(newBuf, 0, newBufCursor);
		}
		else
			System.err.println("Not a Canon Starwriter file.");
		return inData;
	}

	@Override
	public String getNewName() {
		return newName;
	}

	String newName = null;
}
