/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2023 by David Schmidt
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

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Linolex extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		/*
		 * Catalog starts on the 0th track and runs for 20 entries
		 * 
		 * Catalog entries are 64 (0x40) bytes long:
		 *   bytes 0x00-0x01: Numeric ("01", "02", etc.)
		 *   bytes 0x02-0x03: Starting sector
		 *   bytes 0x04-0x05: Ending sector
		 *   bytes 0x06: 0x00
		 *   bytes 0x07-0x3f: Filename
		 *
		 */
		for (int i = 0x80; i < 0x800; i += 0x40)
		{
			String filename = "";
			if (((UnsignedByte.intValue(inData[i + 0x02])) + UnsignedByte.intValue(inData[i + 0x03])) > 0)
			{
        filename = filename + (char)inData[i + 0] + (char)inData[i + 1] + " ";
				for (int j = 0x00; j < 0x39; j++)
				{
				  char proposedChar = (char)inData[j + i + 0x07];
				  switch (proposedChar)
				  {
				    case '\\':
				      proposedChar = '-';
				      break;
				    case '/':
              proposedChar = '-';
              break;
            default:
              break;
				  }
          filename += proposedChar;
				}
				filename = filename.trim() + fileSuffix;
        int startSector = UnsignedByte.intValue(inData[i + 0x02]) * 256 + UnsignedByte.intValue(inData[i + 0x03]);
        int endSector = UnsignedByte.intValue(inData[i + 0x04]) * 256 + UnsignedByte.intValue(inData[i + 0x05]);
        System.out.println("Found file: "+filename + " Start offset: 0x"+Integer.toHexString(startSector * 0x80)+ " End offset: 0x"+Integer.toHexString(endSector * 0x80));
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream();
        out.write(inData, startSector * 0x80, (endSector - startSector + 1)*0x80);
        try
        {
          out.flush();
        }
        catch (IOException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename);
			}
		}
	}
}
