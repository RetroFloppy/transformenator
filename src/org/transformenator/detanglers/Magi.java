/*
 * Transformenator - perform transformation operations on files Copyright (C) 2018 by David Schmidt
 * 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Magi extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		/*
		 * Catalog resides on the first track only.
		 * 
		 * Catalog entries are 16 (0x10) bytes long: bytes 0x00-0x01: File length (in
		 * 128 byte sectors) bytes 0x02-0x03: Starting sector (MSB * track + LSB *
		 * sector) byte 0x04: 0x81 - system file; 0x01 - regular file bytes 0x05-0x0c:
		 * Filename bytes 0x0d-0x0f: File suffix File suffix of "KIL" likely means
		 * deleted file
		 *
		 */
		ByteArrayOutputStream out = null;

		for (int i = 0x20; i < 0xd00; i += 0x10)
		{
			String filename = "";
			String suffix = "";
			if (inData[i + 04] == 0x01)
			{
				int j;
				for (j = 0x05; j <= 0x0c; j++)
				{
					filename += (char) inData[j + i];
				}
				filename = filename.trim();
				for (j = 0x0d; j <= 0x0f; j++)
				{
					suffix += (char) inData[j + i];
				}
				if (!suffix.equals("KIL"))
				{
					byte fnb[];
					filename = filename + "." + suffix;
					int startAddress = (UnsignedByte.intValue(inData[i + 2]) - 1) * 128
							+ (UnsignedByte.intValue(inData[i + 3]) * 128 * 26);
					int length = UnsignedByte.intValue(inData[i], inData[i + 1]);
					try
					{
						out = new ByteArrayOutputStream();
						fnb = Arrays.copyOfRange(inData, startAddress, startAddress + length * 128);
						out.write(fnb);
						out.flush();
						// Remove the (last) file suffix, if one exists, from the image file name before sending to emitFile()
						parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename);
					}
					catch (IOException io)
					{
						io.printStackTrace();
					}
				}
			}
		}
	}
}