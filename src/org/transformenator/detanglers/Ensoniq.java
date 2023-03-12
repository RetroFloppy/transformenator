/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2018 by David Schmidt
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
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Ensoniq extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
	{
		byte[] pad = new byte[448];

		// We have good data; get ready to deal with it.
		if (!(((inData[0x21f] == 0x54) && (inData[0x220] == 0x53) && (inData[0x221] == 0x44))
				|| ((inData[0x21f] == 0x56) && (inData[0x220] == 0x41) && (inData[0x221] == 0x4c))))
		{
			System.err.println("Warning: no \"TSD\" or \"VAL\" signature found.");
		}
		int numSectors = UnsignedByte.intValue(inData[0x211], inData[0x210]);
		int catalogStart = 0;
		if (numSectors == 3200)
		{
			// System.err.println("On-disk format is high density.");
			if (inData.length != 1638400)
			{
				System.err.println("Warning: disk size does not match size definition on disk.");
			}
			catalogStart = 0x3000;
		}
		else if (numSectors == 1600)
		{
			// System.err.println("On-disk format is double density.");
			if (inData.length != 819200)
			{
				System.err.println("Warning: disk size does not match size definition on disk.");
			}
			catalogStart = 0x1E00;
		}
		else
		{
			System.err.println("Error: disk size definition recorded on-disk [" + numSectors
					+ "] is neither double nor high density.");
		}
		if (catalogStart > 0)
		{
			int len, startAddr;
			byte fnb[];
			for (int q = 0; q < 2; q++)
			{
				for (int i = (catalogStart + (q * 0x400)); i < (catalogStart + (q * 0x400) + 0x400); i += 26)
				{
					String filename = "";
					ByteArrayOutputStream out;
					if (inData[i + 2] != 0x00)
					{
						for (int j = 2; j < 13; j++)
						{
							if (inData[j + i] != 0x00)
							{
								filename += (char) inData[j + i];
							}
							else
								break;
						}
					}
					if (!filename.equals(""))
					{
						if (((inData[i + 0] == 0) && (inData[i + 1] == 0x29)))
						{
							// Sequence file
						}
						else if (((inData[i + 0] == 0) && (inData[i + 1] == 0x22)))
						{
							// Program file
						}
						else
						{
							// System.err.println("The following file is not a typical file.");
						}
						len = UnsignedByte.intValue(inData[i + 0x0f], inData[i + 0x0e]) * 512;
						startAddr = UnsignedByte.intValue(inData[i + 0x15], inData[i + 0x14]) * 512;
						filename = filename.trim().replace("\\", "-").replace("/", "-").replace("?", "_")
								.replace("*", "_").replace("$", "-").replace(" ", "-").replace("QUENCE", "Q");
						String fullname;
						fullname = filename + ".EFT";
						try
						{
							String header = "\r\nTS-10 File:     " + filename;
							for (int k = 0; k < 11 - filename.length(); k++)
								header = header + " ";
							header = header + "\0    1 Sequence   \r\n";
							out = new ByteArrayOutputStream();
							fnb = Arrays.copyOfRange(inData, startAddr, startAddr + len);
							out.write(header.getBytes()); // Textual portion of header
							out.write(0x1a); // End-of-file for folks catting the file
							out.write(inData[i + 1]); // File type, presumably
							out.write(inData, i + 0x0d, 13); // Send out the gorpy gorp after the filename
							out.write(pad); // Round out the sector with zeroes
							out.write(fnb, 0, len);
							// All done with all files
							out.flush();
							parent.emitFile(out.toByteArray(), outDirectory, inFile, fullname);
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
}
