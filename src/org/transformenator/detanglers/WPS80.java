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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

/*
 * ConvertWps80File
 * 
 * Convert the block structure of DEC Rainbow WPS-80 files.
 * (WPS-80 was marketed by Exceptional Business Solutions (EBS) running on CP/M on the Rainbow.)
 * They have a forward-and-backward linked list structure to 256-byte blocks with inner used-length,
 * very much like WANG did.
 */
public class WPS80 extends ADetangler
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String outDirectory, String inFile, String fileSuffix)
	{
		ByteBuffer bb = ByteBuffer.allocate(inData.length);
		if ((inData.length % 256) > 0)
		{
			System.err.println("Warning: file size is not an integral of 256, this may not be a WPS-80 file.");
		}
		/*
		 * The WPS-80 word processor leaves a set of 256-byte chunks, each of which has
		 * a preamble:
		 * next-pointer (2 bytes)
		 * prev-pointer (2 bytes)
		 * unknown (1 byte)
		 * bytes-used (1 byte) 
		 */
		int nextPage = nextWpsPage(inData, 1);
		while (nextPage > 0)
		{
			try {
				nextPage = dumpWpsPage(bb, inData, nextPage);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		inData = bb.array();
		interpreter.emitFile(inData, outDirectory, "", inFile + fileSuffix);
	}

	static int nextWpsPage(byte[] inData, int thisPage)
	{
		int thisOffset = (thisPage - 1) * 256;
		return UnsignedByte.intValue(inData[thisOffset + 1]) + UnsignedByte.intValue(inData[thisOffset]);
	}

	static int dumpWpsPage(ByteBuffer bb, byte[] inData, int thisPage) throws IOException
	{
		int nextPage = 0;
		int thisOffset = (thisPage - 1) * 256;
		if (thisOffset < inData.length)
		{
			nextPage = nextWpsPage(inData,thisPage);
			int numBytes = UnsignedByte.intValue(inData[thisOffset + 5]);
			if (numBytes > 0)
			{
				bb.put(inData, thisOffset + 6, numBytes);
			}
		}
		return nextPage;
	}
}