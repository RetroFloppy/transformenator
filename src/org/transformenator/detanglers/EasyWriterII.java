/*
 * Transformenator - perform transformation operations on files Copyright (C)
 * 2019 by David Schmidt 32302105+RetroFloppySupport@users.noreply.github.com
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

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EasyWriterII extends ADetangler
{
	/*
	 * EasyWriter II word processor by Information Unlimited Software (IUS)
	 * 
	 * A single file is really a "folder" of child files - there's a "directory"
	 * of file names, and the content is in 0x200 byte "sectors" within that file,
	 * all indexed with a file identifier in the sector header.  The file identifier  
	 * is actually that file's position in the directory.  It's very much
	 * like a disk image in and of itself: it's a little filesystem buried within
	 * the file.
	 */
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		/* Pull the files out of the image. */
		int fileNum = 0;
		String fileName = "";
		fileName = "";
		for (int k = 6; k < 36; k++)
			fileName += (char) inData[k];
		if (fileName.trim().length() > 0)
			System.out.println("Folder description: "+fileName.trim());
		for (int i = 0x400; i < 0x1800; i += 0x200)
		{
			// 50 bytes per entry; only room for 1/2 entry in the last position of a sector
			for (int j = 0; j < 0x1c3; j += 50)
			{
				fileName = "";
				for (int k = 0; k < 30; k++)
					fileName += (char) inData[i + j + k];
				if (fileName.trim().length() > 0)
				{
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					// System.err.print("Found file name: [" + fileName + "] 0x" + Integer.toHexString(i) + "  ");
					// for (int k = 30; k < 50; k++) System.err.print(UnsignedByte.toString(inData[i + j + k]));
					// System.err.println();
					// System.err.println("Adding file num: " + fileNum);
					for (int b = 0; b < inData.length; b += 0x200)
					{
						/* 0x01 in the zeroeth byte of a "sector" signifies a data sector; the file identifier is the 4th byte */
						if ((inData[b] == 0x01) && (inData[b + 9] == 0x00) && (UnsignedByte.intValue(inData[b + 4]) == fileNum))
						{
							try
							{
								out.write(inData, (b + 10), (512 - 10));
							}
							catch (Throwable t)
							{
								System.err.println(t);
							}
						}
					}
					try
					{
						out.flush();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					parent.emitFile(out.toByteArray(), outDirectory, inFile, fileName.trim()+fileSuffix);
				}
				fileNum++;
			}
		}
	}
}
