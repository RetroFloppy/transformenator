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

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

/*
 * CTOS/BTOS image extractor
 * 
 * CTOS has a flat-ish inode-ish file system.  There are "directories" that are exactly one
 * level deep; they are represented as a "DIRNAME." on the front of a file.  The filesystem is
 * case insensitive, but case is preserved (similar to the way OSX deals with case).
 */

public class CTOS extends ADetangler
{
	public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix)
	{
		int pagesFH;
		long lfaFileHeadersBase, lfaWorkingVHB;
		// Look at the initial VHB
		lfaWorkingVHB = UnsignedByte.longValue(inData[46],inData[47],inData[48],inData[49]);
		lfaFileHeadersBase = UnsignedByte.longValue(inData[(int)lfaWorkingVHB+78], inData[(int)lfaWorkingVHB+79], inData[(int)lfaWorkingVHB+80], inData[(int)lfaWorkingVHB+81]);
		pagesFH = UnsignedByte.intValue(inData[(int)lfaWorkingVHB+82], inData[(int)lfaWorkingVHB+83]);
		// System.out.println("pagesFH: "+pagesFH);
		// System.out.println("alternatePageOffset: "+alternatePageOffset);
		// Fish out the volume name
		System.out.println("Volume name: "+pascalString(inData,20,false));
		for (int i = 0; i < pagesFH; i++)
		{
			// Rumble through all the File Headers and pull out files
			// We may duplicate output files based on alternatePageOffset > 0.
			int newBase = (int)lfaFileHeadersBase + (i*512);
			int filenameLength = UnsignedByte.intValue(inData[(int)newBase + 4]);
			if (filenameLength > 0)
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream(); 
				int fileLength = (int) UnsignedByte.longValue(inData[newBase+111],inData[newBase+112],inData[newBase+113],inData[newBase+114]);
				int outputAccumulator = 0;
				String directoryName = pascalString(inData,(int)lfaFileHeadersBase + (i*512) + 68, true);
				String fileName = directoryName+"."+pascalString(inData,(int)lfaFileHeadersBase + (i*512) + 4, true);
				// System.out.println(fileName+"\t "+fileLength);
				for (int j = 0; j < 32; j++)
				{
					// For each extent, check out its length for usage info...
					int extentLen = (int)UnsignedByte.longValue(inData[newBase + 249 + (j*4)],inData[newBase + 250 + (j*4)],inData[newBase + 251 + (j*4)],inData[newBase + 252 + (j*4)]);
					if (extentLen != 0)
					{
						// This extent is in use... address it
						int lfaExtent = (int)UnsignedByte.longValue(inData[newBase + 121 + (j*4)],inData[newBase + 122 + (j*4)],inData[newBase + 123 + (j*4)],inData[newBase + 124 + (j*4)]);
						int copyLength = 0;
						// If this extent fits fully within the file size, output the whole thing; else, trim it off
						if (extentLen + outputAccumulator <= fileLength)
							copyLength = extentLen;
						else
							copyLength = fileLength - outputAccumulator;
						// System.out.println("lfaExtent: "+lfaExtent+" this extent len: "+extentLen+" accumulated: "+outputAccumulator+" Copying: "+copyLength);
						out.write(inData,lfaExtent,copyLength);
						outputAccumulator += copyLength;
					}
				}
				parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
			}
		}
	}

	String pascalString(byte[] inData, int offset, boolean sanitize)
	{
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < inData[offset]; i++)
		{
			char c = (char)inData[offset + i + 1];
			if (sanitize)
			{
				switch (c)
				{
					case '<':
					case '>':
					case '/':
					case '\\':
					case '`':
					case '*': c = '_'; break;
					default: break;
				}
				if ((c > 127) || (c < 32)) c = '_';
			}
			s.append(c);
		}
		return s.toString();
	}
}