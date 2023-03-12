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

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Brother120k extends ADetangler
{

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		// Brother disks came in 120k and 240k sizes. Inspect the image size; we only
		// want to work on the 120k ones.
		// System.out.println("inData.length: "+inData.length);
		if (inData.length == 119808)
		{
			// First 0x800 bytes is the directory
			for (int i = 0; i < 0x800; i += 0x10)
			{
					byte[] fileNameBytes = {inData[i],inData[i+1],inData[i+2],inData[i+3],inData[i+4],inData[i+5],inData[i+6],inData[i+7]};
					String fileName = new String(fileNameBytes).trim();
					if ((inData[i]) != (byte)0xf0)
					{
						if (inData[i+8] != 0x01)
						{
							// System.out.print("Found file: "+fileName);
							int startSector = UnsignedByte.intValue(inData[i+0x0a], inData[i+0x09]);
							// System.out.print("  Start sector: 0x"+Integer.toHexString(startSector));
							int lengthSector = UnsignedByte.intValue(inData[i+0x0b]);
							// System.out.println("  Length in sectors: 0x"+Integer.toHexString(lengthSector));
							byte[] tempFile = new byte[lengthSector * 256];
							System.arraycopy(inData, startSector*256, tempFile, 0, lengthSector * 256);
							// Remove the (last) file suffix, if one exists, from the image file name before sending to emitFile()
							parent.emitFile(tempFile, outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName + fileSuffix);
						}
						// else
							// System.out.println("Found deleted file: "+fileName);
					}
					// First byte of 0xf0 = empty directory entry
			}
		}
		else
			parent.emitFile(inData, outDirectory, "", inFile + fileSuffix);
	}

}
