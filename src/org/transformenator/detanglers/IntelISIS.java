/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2026 by David Schmidt
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

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.ImageDisk;
import org.transformenator.internal.UnsignedByte;

public class IntelISIS extends ADetangler
{
	/*
	 * Based on code from OS2Museum which was placed in the public domain:
	 *   https://www.os2museum.com/wp/the-ibm-pc-bios-and-intel-isis-ii/
	 *   https://www.os2museum.com/files/i2img.c
	 */

	static final int SECT_SZ = 128;
	static final int NUM_TRACKS = 77;
	static final int SECTS_PER_TRACK_SS = 26;
	static final int SECTS_PER_TRACK_DS = 52;
	static final int DISK_SIZE_SS = NUM_TRACKS * SECTS_PER_TRACK_SS * SECT_SZ;
	static final int DISK_SIZE_DS = NUM_TRACKS * SECTS_PER_TRACK_DS * SECT_SZ;
	static final int DBLK_PER_LBLK = 62;
	static final int MAX_DIR_ENTRIES = 128;

	static final int DS_VALID = 0x00;

	int sectsPerTrack;

	@Override
	public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean debugMode)
	{
		byte[] rawData = ImageDisk.imd2raw(inData, false, false);
		if (rawData != null)
			inData = rawData;

		if (inData.length == DISK_SIZE_SS)
			sectsPerTrack = SECTS_PER_TRACK_SS;
		else if (inData.length == DISK_SIZE_DS)
			sectsPerTrack = SECTS_PER_TRACK_DS;
		else
		{
			System.err.println("Incorrect ISIS-II image size: expected " + DISK_SIZE_SS + " or " + DISK_SIZE_DS + ", got " + inData.length);
			return;
		}

		byte[] dirData = new byte[MAX_DIR_ENTRIES * 16];
		int dirSectCount = MAX_DIR_ENTRIES * 16 / SECT_SZ;
		for (int i = 0; i < dirSectCount; i++)
		{
			byte[] sector = readFileBlock(inData, 1, 1, i);
			if (sector != null)
				System.arraycopy(sector, 0, dirData, i * SECT_SZ, SECT_SZ);
		}

		for (int i = 0; i < MAX_DIR_ENTRIES; i++)
		{
			int base = i * 16;
			int stat = UnsignedByte.intValue(dirData[base]);
			if (stat != DS_VALID)
				continue;

			String name = extractString(dirData, base + 1, 6);
			String ext = extractString(dirData, base + 7, 3);
			String fileName;
			if (ext.length() > 0)
				fileName = name + "." + ext;
			else
				fileName = name;

			int lastSz = UnsignedByte.intValue(dirData[base + 11]);
			int numBlks = UnsignedByte.intValue(dirData[base + 12], dirData[base + 13]);
			int firstSector = UnsignedByte.intValue(dirData[base + 14]);
			int firstTrack = UnsignedByte.intValue(dirData[base + 15]);

			if (numBlks == 0)
				continue;

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			for (int blk = 0; blk < numBlks; blk++)
			{
				byte[] blockData = readFileBlock(inData, firstTrack, firstSector, blk);
				if (blockData != null)
				{
					int nbytes;
					if (blk == numBlks - 1)
						nbytes = lastSz;
					else
						nbytes = SECT_SZ;
					out.write(blockData, 0, nbytes);
				}
			}
			parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())), fileName + fileSuffix);
		}
	}

	byte[] readFileBlock(byte[] inData, int anchorTrack, int anchorSector, int blockNo)
	{
		int lblkTrack = anchorTrack;
		int lblkSector = anchorSector;

		while (blockNo >= DBLK_PER_LBLK)
		{
			byte[] linkBlk = readSector(inData, lblkTrack, lblkSector);
			if (linkBlk == null)
				return null;
			lblkSector = UnsignedByte.intValue(linkBlk[2]);
			lblkTrack = UnsignedByte.intValue(linkBlk[3]);
			blockNo -= DBLK_PER_LBLK;
		}

		byte[] linkBlk = readSector(inData, lblkTrack, lblkSector);
		if (linkBlk == null)
			return null;

		int dataSector = UnsignedByte.intValue(linkBlk[4 + blockNo * 2]);
		int dataTrack = UnsignedByte.intValue(linkBlk[4 + blockNo * 2 + 1]);

		return readSector(inData, dataTrack, dataSector);
	}

	byte[] readSector(byte[] inData, int track, int sector)
	{
		if (sector < 1)
			return null;
		int offset = (track * sectsPerTrack + sector - 1) * SECT_SZ;
		if (offset + SECT_SZ > inData.length)
			return null;
		byte[] result = new byte[SECT_SZ];
		System.arraycopy(inData, offset, result, 0, SECT_SZ);
		return result;
	}

	String extractString(byte[] data, int offset, int maxLen)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < maxLen; i++)
		{
			int ch = UnsignedByte.intValue(data[offset + i]);
			if (ch == 0)
				break;
			if (ch >= 0x20 && ch <= 0x7E)
				sb.append((char) ch);
			else
			  sb.append('_');
		}
		return sb.toString().trim();
	}
}
