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

public class EasyWriterA2 extends ADetangler
{
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		int fat = 8 * (256 * 13); // FAT starts at track 8, sector 0
		int num = 0;
		int nextFile;

		do
		{
			nextFile = UnsignedByte.intValue(inData[fat + 32 + (32 * num)]);
			if (nextFile == 255)
			{
				String fileName = "";
				for (int i = 1; i < 10; i++)
				{
					int ic = UnsignedByte.intValue(inData[fat + 32 + (32 * num) + i]);
					if (ic > 0)
					{
						if (ic == 47) // Exchange Slash for...
							ic = 45; // Dash
						if (ic == 92) // Exchange backslash for...
							ic = 45; //Dash
						if (ic > 127) // Exchange anything too high...
							ic = 45; // for a dash.
						if (ic < 32) // Exchange anything too low...
							ic = 45; // for a dash.
						fileName += (char) ic;
					}
				}
				// System.err.println("File Name: [" + fileName + "]");
				if (!fileName.equals(""))
				{
					ByteArrayOutputStream out;
					try
					{
						byte range[];
						out = new ByteArrayOutputStream();
						int fileSize = (UnsignedByte.intValue(inData[fat + 32 + (32 * num) + 17]) * 256) + UnsignedByte.intValue(inData[fat + 32 + (32 * num) + 16]);
						// System.err.println("File size: "+fileSize);
						int fatOffset = fat + 32 + (32 * num) + 18;
						int bytesWritten = 0;
						int bytesToWrite = 1024;
						// Interpret the file
						for (int j = 0; j < 13; j++)
						{
							int allocValue = UnsignedByte.intValue(inData[fatOffset + j]);
							// System.err.print(UnsignedByte.toString(inData[fatOffset+j])+" ");
							if (allocValue > 0)
							{
								/*
								 * Ok, we are going to look at: bit
								 * position * 1024 (0x400) + byte
								 * position * 8192 (0x2000)
								 */
								int trackOffset = (8192 * j);
								int sectorOffset = 0;
								if ((allocValue & 0x01) == 0x01)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 0));
									sectorOffset = 0;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 0x02) == 0x02)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 1));
									sectorOffset = 1024 * 1;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 0x04) == 0x04)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 2));
									sectorOffset = 1024 * 2;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 0x08) == 0x08)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 3));
									sectorOffset = 1024 * 3;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 0x10) == 0x10)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 4));
									sectorOffset = 1024 * 4;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 0x20) == 0x20)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 5));
									sectorOffset = 1024 * 5;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 0x40) == 0x40)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 6));
									sectorOffset = 1024 * 6;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
								if ((allocValue & 128) == 128)
								{
									// System.err.println("Print 1k from " + (trackOffset + 1024 * 7));
									sectorOffset = 1024 * 7;
									if (bytesWritten + 1024 > fileSize)
										bytesToWrite = fileSize - bytesWritten;
									else
										bytesToWrite = 1024;
									range = Arrays.copyOfRange(inData, trackOffset + sectorOffset, trackOffset + sectorOffset + bytesToWrite);
									bytesWritten += bytesToWrite;
									out.write(range);
								}
							}
						}
						out.flush();
						parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					// System.out.println();
				}
				num++;
			}
		} while (nextFile == 255);
	}
}