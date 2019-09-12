/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2019 by David Schmidt
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

/*
 * SuperSCRIPTSIT file interpreter
 * 
 */

public class SuperSCRIPSIT extends ADetangler
{
	public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix)
	{
		if (UnsignedByte.intValue(inData[0]) == 0xe0)
		{
			// System.err.println("DEBUG: Found a SuperSCRIPSIT file.");
			ByteArrayOutputStream out = null;
			out = new ByteArrayOutputStream();
			int numHeaders = UnsignedByte.intValue(inData[256]);
			for (int i = 0; i < numHeaders; i++)
			{
				int index = i * 5 + 1;
				int blockNum = UnsignedByte.intValue(inData[256+index]);
				int numBytes = UnsignedByte.intValue(inData[256+index+1],inData[256+index+2]);
				int filePos = blockNum * 0x400 + 0x600;
				// System.out.println("Block: 0x"+Integer.toHexString(blockNum)+"  Bytes: 0x"+Integer.toHexString(numBytes)+" File position: 0x"+Integer.toHexString(filePos));
				if (filePos + 12 + numBytes < inData.length)
				{
					// System.out.println("filepos + 12: 0x"+Integer.toHexString(filePos + 12)+" numBytes: 0x"+Integer.toHexString(numBytes));
					out.write(inData, filePos + 12, numBytes);
				}
			}
			parent.emitFile(out.toByteArray(), outDirectory, "", inFile + fileSuffix);
		}
		else
		{
			System.err.println("Probably not a SuperSCRIPSIT file.");
		}
	}
}