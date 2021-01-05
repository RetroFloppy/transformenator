/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2021 by David Schmidt
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
 * Pull the files off of the virtual file system of a Microdata 8" disk image.
 * 
 * Disk geometry: FM, 1 side, 256 bytes per sector, 16 sectors per track, 77 tracks.
 *
 */
package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Microdata extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		/*
		 * Catalog starts 2 tracks in; first sector is informational, actual
		 * entries start at sector 33
		 * 
		 * Catalog entries are 24 (0x18) bytes long.
		 *   bytes 0x00-0x0c: Filename (space padded, generally) 
		 *   bytes 0x0d-0x0e: track, sector of first sector
		 *   byte 0x0f: file length in sectors
		 */
		for (int i = 0x2108; i < inData.length; i += 0x18)
		{
		  int startAddr = 0;
			String filename = "";
			if (UnsignedByte.intValue(inData[i]) != 0xe5)
			{
				int j;
				for (j = 0; j < 13; j++)
				{
					filename += (char) inData[j + i];
				}
				filename = filename.trim();
				startAddr = UnsignedByte.intValue(inData[i+13])*0x1000 + UnsignedByte.intValue(inData[i+14]) * 0x100;
        // System.out.println("Found file: "+filename+" at 0x"+Integer.toHexString(startAddr)+" for 0x"+Integer.toHexString(UnsignedByte.intValue(inData[i+15]))+" sectors");
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream();
        followFile(out, startAddr, inData);
        parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename+fileSuffix);
	    }
			else
			  break;
    }
	}
	
	void followFile(ByteArrayOutputStream out, int startAddr, byte[] inData)
	{
	  // System.out.println("  Writing sector from 0x"+Integer.toHexString(startAddr));
    out.write(inData, startAddr+2, 254);
    startAddr = UnsignedByte.intValue(inData[startAddr])*0x1000 + UnsignedByte.intValue(inData[startAddr+1]) * 0x100; 
    if ((startAddr > 0) && (startAddr < inData.length))
      followFile(out, startAddr, inData);
    //else
      //System.out.println("  End of file marker: 0x"+Integer.toHexString(startAddr));
	}
}