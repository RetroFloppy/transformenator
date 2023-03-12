/*
 * Transformenator - perform transformation operations on files Copyright (C)
 * 2020 by David Schmidt 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class EightInch8x512 extends ADetangler
{
	/*
	 * These 8" disks have one track of 26 sectors of 128 bytes each, followed
	 * by 76 tracks of 8 sectors of 512 bytes each.
	 * 
	 * Sector structure (complete image should be 314,624 bytes):
	 *
	 * Offset Length Meaning
	 * ====== ====== ============== 
	 * 0x00 1 Sector type (4=data (file or directory)) 
	 * 0x01 1 Previous sector's track 
	 * 0x02 1 Previous sector's sector 
	 * 0x03 1 Next sector's track 
	 * 0x04 1 Next sector's sector
	 * 
	 * Text sectors are zero-terminated.
	 * 
	 */

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		int key = 0;

		ByteArrayOutputStream out = null;
		int offset;
		for (int track = 1; track < (inData.length - 3328) / 4096; track++)
		{
			for (int sector = 1; sector < 9; sector++)
			{
				offset = calcOffset(track, sector);
				if ((UnsignedByte.intValue(inData[offset + 0]) == 0x04) && (UnsignedByte.intValue(inData[offset + 1]) == 0x00) && (UnsignedByte.intValue(inData[offset + 2]) == 0x00))
				{
					String fileName = new String("recovered_file_" + ++key);
					System.err.println("Found the start "+fileName+" offset 0x" + Integer.toHexString(offset));
					ArrayList<Integer> visitedSectors = new ArrayList<Integer>();
					out = new ByteArrayOutputStream();
					dumpSectorChain(offset, inData, visitedSectors, true, out);
					parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())), fileName + fileSuffix);
				}
				else
					if (UnsignedByte.intValue(inData[offset + 0]) == 0x04)
					{
						// dumpSectorHeader(offset, inData);
					}
			}
		}
	}

	public static int calcOffset(int track, int sector)
	{
		int offset = 3328 + ((track - 1) * 4096) + ((sector - 1) * 512);
		// System.err.println("calcOffset
		// (0x"+Integer.toHexString(track)+","+sector+"): returning
		// 0x"+Integer.toHexString(offset));
		return offset;
	}

	public static int calcTrack(int offset)
	{
		int track = (offset - 3328) / 4096 + 1;
		// System.err.println("calcTrack: returning track "+track);
		return track;
	}

	public static int calcSector(int offset)
	{
		// System.err.println("calcSector entry; offset:
		// 0x"+Integer.toHexString(offset));
		// System.err.println("calcOffset(calcTrack(offset),1):
		// 0x"+Integer.toHexString(calcOffset(calcTrack(offset),1)));
		int sector = ((offset - calcOffset(calcTrack(offset), 1)) / 512) + 1;
		// System.err.println("calcSector of 0x"+Integer.toHexString(offset)+"
		// returning sector "+sector);
		return sector;
	}

	static void dumpSectorChain(int offset, byte[] inData, ArrayList<Integer> visitedSectors, Boolean isFirst, ByteArrayOutputStream out)
	{
		int thisTrack = calcTrack(offset);
		int thisSector = calcSector(offset);
		String offsetStr = "0x" + (Integer.toHexString(offset).toUpperCase()) + " this: (0x" + Integer.toHexString(thisTrack) + "," + thisSector + ")";
		// System.err.println();
		int newKey = offset;
		if (!visitedSectors.contains(newKey))
		{
			visitedSectors.add(newKey);
			int prevTrack = UnsignedByte.intValue(inData[offset + 1]);
			int prevSector = UnsignedByte.intValue(inData[offset + 2]);
			int nextTrack = UnsignedByte.intValue(inData[offset + 3]);
			int nextSector = UnsignedByte.intValue(inData[offset + 4]);
			System.err.println(offsetStr + " prev: (0x" + Integer.toHexString(prevTrack) + "," + prevSector + ") " + "next: (0x" + Integer.toHexString(nextTrack) + "," + nextSector + ")");
			if ((prevTrack + prevSector + nextTrack + nextSector != 0) || (isFirst))
			{
				if ((prevTrack + prevSector > 0) || (isFirst))
				{
					System.err.println("writing: (0x" + Integer.toHexString(thisTrack) + "," + thisSector + ")");
					out.write(inData, offset+8, 512-8);
				}
			}
			if ((nextTrack > 0) && ((nextSector > 0) && nextSector < 9))
			{
				System.err.println("dumping: next: (0x" + Integer.toHexString(nextTrack) + "," + nextSector + ")");
				dumpSectorChain(calcOffset(nextTrack, nextSector), inData, visitedSectors, false, out);
			}
		}
		else
		{
			System.err.println("Uh oh, we've visited this sector before!");
		}
	}

	static void dumpSectorHeader(int offset, byte[] inData)
	{
		int thisTrack = calcTrack(offset);
		int thisSector = calcSector(offset);
		String offsetStr = "0x" + (Integer.toHexString(offset).toUpperCase()) + " this: (0x" + Integer.toHexString(thisTrack) + "," + thisSector + ")";
		// System.err.println();
		int prevTrack = UnsignedByte.intValue(inData[offset + 1]);
		int prevSector = UnsignedByte.intValue(inData[offset + 2]);
		int nextTrack = UnsignedByte.intValue(inData[offset + 3]);
		int nextSector = UnsignedByte.intValue(inData[offset + 4]);
		System.err.println(offsetStr + " prev: (0x" + Integer.toHexString(prevTrack) + "," + prevSector + ") " + "next: (0x" + Integer.toHexString(nextTrack) + "," + nextSector + ")");
	}

}
