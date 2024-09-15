/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2024 by David Schmidt
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
 * Pull the files off of the virtual file system of a Rockwell AIM-65 disk image.
 * 
 * Disk geometry: 1 side, 128 bytes per sector, 18 sectors per track, 35 tracks.
 * FM encoded, approx. 80k per disk.  Multiple variants may exist.
 *
 */
package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Rockwell extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		/*
		 * Catalog starts on the 0th track - no idea how far it can stretch, so be
		 * conservative about what we identify as a valid file entry
		 * 
		 * Catalog entries are 10 (0x0a) bytes long: 
		 *   byte 0x00 is some sort of indicator (typically 0x01)
		 *   bytes 0x01-0x06: Filename (0x20 padded)
		 *   byte 0x07: offset to the next file's start sector (two decimal digits represented as hex)
		 *   bytes 0x08-0x09: unknown
		 *
		 */
	  int startSector = 0x05;
		for (int i = 0x00; i < 0x280; i += 0x0a)
		{
			String filename = "";
      // Binary coded decimal, I guess... offset to next file in sectors
      int lengthSectors = UnsignedByte.hiNibble(inData[i+7]) * 10 + UnsignedByte.loNibble(inData[i+7]);
			if ((inData[i] == 0x01) || (inData[i] == 0x02))
			{
				int j;
				for (j = 0; j < 6; j++)
				{
				  filename += (char) inData[j + i + 1];
				}
				filename = filename.trim();
				// Initial byte of 0x02 may indicate duplicate filename (may also indicate deleted, we don't know yet)
				if (inData[i] == 0x02)
				{
				  filename += "(1)";
				}
        // System.out.println("Found file: "+filename+ " for length "+lengthSectors);
				ByteArrayOutputStream out;
				// Copy data from first sector chain
				out = new ByteArrayOutputStream();
        // Dump out the file
				out.write(inData,startSector * 0x80, lengthSectors * 0x80);
				parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename);
			}
			startSector += lengthSectors;
		}
	}
}
