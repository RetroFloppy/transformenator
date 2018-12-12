/*
 * Transformenator - perform transformation operations on files Copyright (C) 2018 by David Schmidt
 * 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.transformenator.internal.EbcdicUtil;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class AdministrativeSystem extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		int track0Offset = 3328;
		if (inData.length > 1000000)
		{
			track0Offset = 3328 * 3;
		}
		if (inData.length == 1177344)
		{
			// We have an IBM 5520 Administrative System disk
			// Track 0, side 0: 26 sectors @ 128 bytes
			// Track 0, side 1: 26 sectors @ 256 bytes
			// The rest: 15 sectors @ 512 bytes
		}
		Hashtable<Integer, TextRecord> textRecordCollection = new Hashtable<Integer, TextRecord>();
		TextRecord tr;
		int i = track0Offset;
		while (i < inData.length)
		{
			if (inData[i] == 0x00)
			{
				int mid = UnsignedByte.intValue(inData[i + 1]);
				int recType = UnsignedByte.intValue(inData[i + 2]);
				int end = UnsignedByte.intValue(inData[i + 3]);
				if (((end == 0x04) && (recType != 0xe0)) && (!((mid == 0x05) && recType == 0xe1)))
				{
					int intOffset = i - track0Offset;
					if (intOffset > 65535)
						intOffset -= 65536;
					if (recType == 0xE1)
					{
						String marker = EbcdicUtil.toAscii(inData, i + 5, 6);
						int scratchIndicator = inData[i + 0x0c];
						int markerInt = Integer.parseInt(marker);
						int j;
						if (markerInt == 100)
							i += 0x146;
						else
							i += 0x9c;
						int beginOffset = i;
						int endOffset = beginOffset;
						for (j = i; j < inData.length; j++)
						{
							if (inData[j] == 0x0c)
							{
								endOffset = j;
								break;
							}
							if ((j % 256 == 0))
							{
								if (inData[j] == 0x00)
								{
									// System.err.println("Ok, we hit the end of a sector without a 0x0c - breaking.  j="+Integer.toHexString(j));
									endOffset = j;
									break;
								}
							}
						}
						i = j;
						// System.err.println("Text Record: "+markerInt+" Scratch indicator: 0x"+Integer.toHexString(scratchIndicator)+" Offsets: "+ Integer.toHexString(beginOffset) + " - "+Integer.toHexString(endOffset));
						if (scratchIndicator != 0x05)
						{
							tr = new TextRecord(beginOffset, endOffset);
							textRecordCollection.put(markerInt, tr);
						}
					}
					else
						i++;
				}
				else
					i++;
			}
			else
				i++;
		}
		// System.err.println("Text records found: " + textRecordCollection.size());
		TextRecord tr2;
		Enumeration<Integer> recs = textRecordCollection.keys();
		List<Integer> list = Collections.list(recs); // create list from enumeration 
		Collections.sort(list);
		recs = Collections.enumeration(list);
		if (recs.hasMoreElements())
		{
			try
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				while (recs.hasMoreElements())
				{
					Integer marker = (Integer) recs.nextElement();
					tr2 = textRecordCollection.get(marker);
					// System.err.println("Text record "+marker+": "+Integer.toHexString(tr2.beginOffset)+"-"+Integer.toHexString(tr2.endOffset)+":");
					// System.err.println(EbcdicUtil.toAscii(inData, tr2.beginOffset, tr2.endOffset));
					byte range2[] = Arrays.copyOfRange(inData, tr2.beginOffset, tr2.endOffset);
					out.write(range2);
					out.flush();
				}
				parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length()))+fileSuffix);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

    public static class TextRecord
    {
            public TextRecord(int begin, int end)
            {
                    beginOffset = begin;
                    endOffset = end;
            }

            public int beginOffset;
            public int endOffset;
    }

}