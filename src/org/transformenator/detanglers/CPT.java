/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2014 - 2018 by David Schmidt
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

public class CPT extends ADetangler
{
	public void detangle(FileInterpreter parent, byte skewedData[], String outDirectory, String inFile, String fileSuffix)
	{
		if (skewedData.length % (256 * 16) == 0)
		{
			byte[] inData = new byte[skewedData.length];
			int skew[] = {0, 3, 6, 9, 12, 15, 2, 5, 8, 11, 14, 1, 4, 7, 10, 13};
			// De-skew the disk image in memory
			for (int i = 0; i < skewedData.length / 4096; i++) // for each track...
			{
				for (int j = 0; j < 16; j++) // for each sector...
				{
					System.arraycopy( skewedData, (skew[j]*256)+i*4096, inData, (j*256)+i*4096, 256 );
				}
			}

			// Ok, we have good sectors.
			int trackLen = 256 * 16;
			int i, startSector = -1;
			String currentFile = "";
			ByteArrayOutputStream outFile = null;
			String continuationFile = "";
			try
			{
				String fileName = "";
				for (i = 0; i < 77; i++)
				{
					if (!fileName.equals(""))
					{
						// System.err.println("DEBUG: ...started a new track, but we haven't finished file " + fileName + " yet.");
						continuationFile = fileName;
					}
					// Hunt for a logical beginning of a track
					for (int sectorNum = 0; sectorNum < 16; sectorNum++)
					{
						int sectorStart = sectorNum * 256 + (i * trackLen);
						byte startByte = inData[sectorStart];
						boolean pad = false;
						if (startByte == 0x01)
						{
							fileName = "";
							startSector = sectorNum;
							for (int k2 = 1; k2 < 17; k2++)
							{
								if (inData[sectorStart + k2] == 0x02)
									pad = true;
								if (!pad)
									fileName += (char) inData[sectorStart + k2];
							}
							// System.err.println("DEBUG: Found a new file ["+fileName+"] in track "+i);
						}
					}
					if (startSector == 0)
					{
						if (!currentFile.equals(""))
						{
							startSector = 0;
							System.err.println("ERROR: a file was in progress, but we don't know where to start now in the next track.");
						}
					}
					if (startSector > -1)
					{
						for (int sectorNum = startSector; sectorNum < startSector + 16; sectorNum++)
						{
							fileName = "";
							boolean pad = false;
							int offset = sectorNum;
							if (offset > 15)
								offset -= 16;
							int firstByteOfSector = UnsignedByte.intValue(inData[offset * 256 + (i * trackLen)]);
							if (firstByteOfSector == 0x00)
							{
								if (outFile != null)
								{
									outFile.flush();
									outFile.close();
								}
								currentFile = "";
								break;
							}
							else if (firstByteOfSector == 0x01)
							{
								int fnLen = 0;
								for (int k2 = 1; k2 < 17; k2++)
								{
									if (inData[offset * 256 + (i * trackLen) + k2] == 0x02)
									{
										pad = true;
										fnLen = k2 + 1;
									}
									if (!pad)
										fileName += (char) inData[offset * 256 + (i * trackLen) + k2];
								}
								// System.err.println("DEBUG: Found file [" + fileName + "] in track " + i + ", sector "+sectorNum+" ("+offset+")");
								if (outFile != null)
								{
									outFile.flush();
									parent.emitFile(outFile.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
								}
								currentFile = fileName;
								outFile = new ByteArrayOutputStream();

								for (int x = fnLen; x < 256; x++)
								{
									outFile.write((char) inData[offset * 256 + (i * trackLen) + x]);
									// System.err.print((char) inData[offset * 256 + (i * trackLen) + x]);
								}
								// System.err.println();
							}
							else if (!currentFile.equals(""))
							{
								// System.err.println("  Dump of sector "+sectorNum+" ("+offset+"), offset 0x"+Integer.toHexString(offset * 256 + (i * trackLen))+" for file " + currentFile);
								for (int x = 0; x < 256; x++)
								{
									if (inData[offset * 256 + (i * trackLen) + x] == 0x03)
									{
										if (outFile != null)
										{
											outFile.flush();
											parent.emitFile(outFile.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), currentFile+fileSuffix);
										}
										currentFile = "";
										break;
									}
									else
									{
										outFile.write((char) inData[offset * 256 + (i * trackLen) + x]);
										// System.err.print((char) inData[offset * 256 + (i * trackLen) + x]);
									}
								}
								// System.err.println();
							}
							else
							{
								if (!continuationFile.equals(""))
								{
									currentFile = continuationFile;
									continuationFile = "";
									if (outFile != null)
									{
										outFile.flush();
										parent.emitFile(outFile.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
									}
									outFile = new ByteArrayOutputStream();
									// System.err.println("  (restart) Dump of sector for file " + currentFile);
									for (int x = 0; x < 256; x++)
									{
										if (inData[offset * 256 + (i * trackLen) + x] == 0x03)
										{
											currentFile = "";
											if (outFile != null)
											{
												outFile.flush();
												parent.emitFile(outFile.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
											}
											break;
										}
										else
										{
											outFile.write((char) inData[offset * 256 + (i * trackLen) + x]);
											// System.err.print((char) inData[offset * 256 + (i * trackLen) + x]);
										}
									}
								}
								// System.err.println();
							}
						}
					}
					else
						currentFile = "";
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}