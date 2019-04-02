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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Kermit
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		/*
		 * Ostensibly a "Kermit" filesystem - maybe it's a tar-like archive.
		 * Disk is 2-sided, 10x256 for the first track, then 18x256 for the rest. 
		 */
		ByteArrayOutputStream out = null;

		for (int n = 0; n < 6; n++)
		{
			for (int i = 0x0400 + (0x100 * n); i < 0x4e1 + (0x100 * n); i += 0x1f)
			{
				String filename = "";
				int j;
				for (j = 0x04; j <= 0x18; j++)
				{
					filename += (char) inData[j + i];
				}
				filename = filename.trim();
				if (filename.length() > 0)
				{
					byte fnb[];
					int msb = UnsignedByte.intValue(inData[i + 0x1a]);
					int lsb = UnsignedByte.intValue(inData[i + 0x1b]);
					int startAddress = ((msb * 0x24 + lsb) * 256) - 4352;
					msb = UnsignedByte.intValue(inData[i + 0x1c]);
					lsb = UnsignedByte.intValue(inData[i + 0x1d]);
					int endAddress = ((msb * 0x24 + lsb) * 256) - 4352;
					try
					{
						out = new ByteArrayOutputStream();
						fnb = Arrays.copyOfRange(inData, startAddress + 8, endAddress);
						out.write(fnb);
						out.flush();
						// Remove the (last) file suffix, if one exists, from
						// the
						// image file name before sending to emitFile()
						parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())), filename);
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
