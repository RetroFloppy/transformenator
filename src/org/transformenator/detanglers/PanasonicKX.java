/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2015 - 2018 by David Schmidt
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
import org.transformenator.internal.UnsignedByte;

public class PanasonicKX extends ADetangler
{
	static int SIZE_SS = 0;
	static int SIZE_DS = 1;
	static int zeroesBegin[] = { 0x0c, 0x0c }; // + 0x09
	static int fileHeadOffset[] = { 0x1110, 0x1510 };

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		ByteArrayOutputStream currentOut = null;
		// Panasonic disks came in SS and DS sizes.  Inspect the image size; if it's larger than SS * 1.5, it's DS.
		int diskSize = inData.length > (360 * 1024 * 1.5)? SIZE_DS: SIZE_SS;
		for (int i = 0x100; i < 0x1c00; i+=0x20)
		{
			String filename = "";
			int j, result = 0;
			for (j = zeroesBegin[diskSize]; j < zeroesBegin[diskSize] + 0x09; j++)
				result += inData[i+j];
			filename = "";
			for (j = 0x00; j < 0x0a; j++)
			{
				if (UnsignedByte.intValue(inData[i+j]) != 0xe5)
					filename += (char)inData[i+j];
			}
			filename = filename.trim();
			if ((filename.length() > 0) && (result == 0))
			{
				try
				{
					currentOut = new ByteArrayOutputStream();
					int fileHead = UnsignedByte.intValue(inData[i+0x1a],inData[i+0x1b]);
					int fileLength = UnsignedByte.intValue(inData[i+0x1c],inData[i+0x1d]);
					fileHead = (fileHead * 0x400) + fileHeadOffset[diskSize];
					fileLength = fileLength - 0x100;
					for (j = fileHead; j < fileHead+fileLength; j++)
					{
						if (UnsignedByte.intValue(inData[j]) != 0xc5)
							currentOut.write(inData[j]);
						else
							break;
					}
					currentOut.flush();
					parent.emitFile(currentOut.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename + fileSuffix);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	static byte[] followFile(byte[] inData, byte fileNumber)
	{
		/*
		 * Make one contiguous array of data out of a file number
		 */
		byte[] fileContents = null;
		// System.err.println("Following file 0x" + UnsignedByte.toString(fileNumber));
		int count = 0;
		for (int i = 0; i < 640; i++)
		{
			if (inData[i] == fileNumber)
			{
				count++;
			}
		}
		if (count > 0)
		{
			fileContents = new byte[count * 256];
			count = 0; // Start over counting
			for (int i = 0; i < 640; i++)
			{
				if (inData[i] == fileNumber)
				{
					for (int j = 0; j < 256; j++)
					{
						fileContents[(count * 256) + j] = inData[(i * 256) + j];
					}
					count++;
				}
			}
		}
		return fileContents;
	}

}