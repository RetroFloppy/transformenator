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
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class ProWrite extends ADetangler
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String inFile, String outDirectory, String fileSuffix)
	{
		byte[] newBuf = new byte[inData.length];
		byte textEyecatcher[] = { 0x54, 0x45, 0x58, 0x54 }; // "TEXT"
		byte paraEyecatcher[] = { 0x50, 0x41, 0x52, 0x41 }; // "PARA"
		byte formEyecatcher[] = { 0x46, 0x4f, 0x52, 0x4d }; // "FORM"
		int length, newBufCursor = 0;
		if (Arrays.equals(Arrays.copyOfRange(inData, 0, 4), formEyecatcher))
		{
			for (int i = 0; i < inData.length; i ++)
			{
				if (inData.length - i > 8)
				{
					byte range[] = Arrays.copyOfRange(inData, i, i+0x04);
					length = UnsignedByte.intValue(inData[i+7]) + 256*(UnsignedByte.intValue(inData[i+6]));
					if (Arrays.equals(range, textEyecatcher))
					{
						// System.err.println("Found text segment @ $"+Integer.toHexString(i)+" for length "+length);
						if (length > 0)
						{
							for (int k = 0; k < length; k++)
							{
								newBuf[newBufCursor++] = inData[i + 8 + k];
							}
							i += length; // Move past this text segment
							// System.err.println("cursor after text: "+newBufCursor);
						}
						newBuf[newBufCursor++] = 0x0d;
						newBuf[newBufCursor++] = 0x0a;
						// System.err.println("cursor after newl: "+newBufCursor);
					}
					else if (Arrays.equals(range, paraEyecatcher))
					{
						// System.err.println("Found para segment @ $"+Integer.toHexString(i)+" for length "+length);
					}
				}
			}
			inData = new byte[newBufCursor];
			for (int i = 0; i < newBufCursor; i++)
				inData[i] = newBuf[i];
			interpreter.emitFile(inData, outDirectory + File.separator + inFile + fileSuffix);
		}
		else
			System.err.println("Not a ProWrite file.");
	}
}