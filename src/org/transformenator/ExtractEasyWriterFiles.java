/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 by David Schmidt
 * david__schmidt at users.sourceforge.net
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

package org.transformenator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

/*
 * UnpackWangFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of
 * AppleWriter word processor disks in D13 format.    
 *
 */
public class ExtractEasyWriterFiles
{

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
				InputStream input = null;
				try
				{
					int totalBytesRead = 0;
					input = new BufferedInputStream(new FileInputStream(file));
					while (totalBytesRead < result.length)
					{
						int bytesRemaining = result.length - totalBytesRead;
						// input.read() returns -1, 0, or more:
						int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
						if (bytesRead > 0)
						{
							totalBytesRead = totalBytesRead + bytesRead;
						}
					}
					inData = result;
				}
				finally
				{
					if (input != null)
						input.close();
				}
			}
			catch (FileNotFoundException ex)
			{
				System.err.println("Input file \"" + file + "\" not found.");
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
			if (inData != null)
			{
				System.err.println("Read " + inData.length + " bytes.");
				if (args.length == 2)
				{
					/*
					 * If they wanted an output directory, go ahead and make it.
					 */
					try
					{
						// System.err.println("mkdir "+args[1]);
						Runtime.getRuntime().exec("mkdir "+args[1]);
					}
					catch (IOException e)
					{
						// e.printStackTrace();
						/*
						 *  The natural course of events will 
						 *  be to have errors reported by the 
						 *  attempt to eventually write the file.
						 *  No need to complain here if, say, the
						 *  directory already exists.
						 */
					}
				}
				int fat = 8 * (256 * 13); // FAT starts at track 8, sector 0
				int num = 0;
				int nextFile;

				do
				{
					nextFile = UnsignedByte.intValue(inData[fat + 32 + (32 * num)]);
					if (nextFile == 255)
					{
						String fileName;
						if (args.length == 2)
							fileName = new String(args[1]) + File.separator;
						else
							fileName = "";
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
							FileOutputStream out;
							try
							{
								byte range[];
								out = new FileOutputStream(fileName);
								System.err.println("Creating file: " + fileName);
								int fileSize = (UnsignedByte.intValue(inData[fat + 32 + (32*num)+17]) * 256) + UnsignedByte.intValue(inData[fat + 32 + (32*num)+16]);
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
								out.close();
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
		else
		{
			// wrong args
			help();
		}
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractAppleWriterFiles v1.6 - Extract files from AppleWriter word processor disk image.");
		System.err.println();
		System.err.println("Syntax: ExtractAppleWriterFiles infile [out_directory]");
	}
}