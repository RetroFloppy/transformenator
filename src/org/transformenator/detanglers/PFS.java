/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2016 - 2018 by David Schmidt
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

public class PFS extends ADetangler
{
	public void detangle(FileInterpreter parent, byte skewed[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		byte[] inData = new byte[skewed.length];
		int skew[] = {0,14,13,12,11,10,9,8,7,6,5,4,3,2,1,15};
		// De-skew the disk image in memory
		for (int i = 0; i < skewed.length / 4096; i++)
		{
			for (int j = 0; j < 16; j++)
			{
				System.arraycopy( skewed, (j*256)+i*4096, inData, (skew[j]*256)+i*4096, 256 );
			}
		}
		/* Now pull the files out of the image. */
		for (int i = 0x2e; i < 0x1ff; i += 18)
		{
			/*
			 * Directory sector
			 */
			if ((inData[i] != 0x00) && (UnsignedByte.intValue(inData[i]) < 16))
			{
				// System.err.println("DEBUG: found file name length: " + inData[i]);
				String filename = "";
				// Build the filename
				for (int j = 1; j < UnsignedByte.intValue(inData[i]) + 1; j++)
				{
					filename += (char) inData[i + j];
				}
				// Find file's starting "block"
				int fileStart = ((UnsignedByte.intValue(inData[i + 17]) * 256) + UnsignedByte.intValue(inData[i + 16])) * 512;
				// System.err.println("DEBUG: found file: " + filename + " Starting at offset: 0x"+Integer.toHexString(fileStart));
				ByteArrayOutputStream out;
				try
				{
					out = new ByteArrayOutputStream();
					int j, block;
					// Run through the sector map, skipping the first couple of blocks
					for (j = fileStart+4; j < fileStart+512; j += 2 )
					{
						block = (UnsignedByte.intValue(inData[j+1])*256)+UnsignedByte.intValue(inData[j]);
						if (block <= 0)
							break;
						// System.err.println("Exporting block: 0x"+Integer.toHexString(block));
						out.write(inData, block*512, 512);
					}
					out.flush();
					parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
