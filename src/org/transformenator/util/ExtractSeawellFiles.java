/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 by David Schmidt
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

package org.transformenator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.transformenator.Version;

/*
 * ExtractSeawellFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of a Seawell
 * 8" disk image.
 *
 */
public class ExtractSeawellFiles
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
				/*
				 * If they wanted an output directory, go ahead and make it.
				 */
				File baseDirFile = new File(args[1]);
				if (!baseDirFile.isAbsolute())
				{
					baseDirFile = new File("."+File.separator+args[1]);
				}
				baseDirFile.mkdir();
				// System.out.println("Making directory: ["+baseDirFile+"]");
				for (int i = 0x3de00; i < inData.length; i+=35)
				{
					String filename = "";
					if (inData[i+16] != 0x00)
					{
						int j;
						for (j = 0; j < 18; j++)
						{
							if (inData[j+i+16] != 0x00)
							{
								filename += (char) inData[j+i+16];
							}
							else
								break;
						}
						int nodeAddr = (UnsignedByte.intValue(inData[i+33]))*13312 + (UnsignedByte.intValue(inData[i+34]))*256;
						// Now pull up the info block for that file
						if ((inData[nodeAddr+8] == 0x34) && (inData[nodeAddr+9] == 0x00))
						{
							byte fnb[];
							/*
							System.out.println("Extracting file: "+filename);
							System.out.print("Node Addr: 0x"+Integer.toHexString(0x1000000 |nodeAddr).substring(1).toUpperCase());
							System.out.print(" Node Data: 0x");
							for (int k = 8; k < 20; k++)
								System.out.print(UnsignedByte.toString(inData[nodeAddr+k]));
							*/
							int dataAddr1 = (UnsignedByte.intValue(inData[nodeAddr+0x0a]))*13312 + (UnsignedByte.intValue(inData[nodeAddr+0x0b])-1)*256;
							int dataLen1 = inData[nodeAddr+0x0c]*256;
							int dataAddr2;
							if (inData[nodeAddr+0x0e] == 0x00)
								dataAddr2 = 0;
							else
								dataAddr2 = (UnsignedByte.intValue(inData[nodeAddr+0x0d]))*13312 + (UnsignedByte.intValue(inData[nodeAddr+0x0e])-1)*256;
							int dataLen2 = inData[nodeAddr+0x0f]*256;
							/*
							System.out.println();
							System.out.println("Data add1: 0x"+Integer.toHexString(0x1000000 | dataAddr1).substring(1).toUpperCase()+" length: 0x"+ Integer.toHexString(0x10000 | dataLen1).substring(1).toUpperCase());
							if (dataLen2 > 0)
								System.out.println(Data add2: 0x"+Integer.toHexString(0x1000000 | dataAddr2).substring(1).toUpperCase()+" length: 0x"+ Integer.toHexString(0x10000 | dataLen2).substring(1).toUpperCase());
							System.out.println();
							*/
							FileOutputStream out;
							fnb = Arrays.copyOfRange(inData, dataAddr1, dataAddr1+dataLen1);
							for (j = fnb.length - 1; j > 0; j--)
							{
								if (fnb[j] != 0x00)
								{
									dataLen1 = j;
									break;
								}
							}
							try
							{
								String fullname;
								if (args.length == 2)
								{
									fullname = new String(args[1]) + File.separator + filename;
								}
								else
									fullname = filename;
								out = new FileOutputStream(fullname);
								System.err.println("Creating file: " + fullname);
								out.write(fnb, 0, dataLen1);
								if (dataLen2 > 0)
								{
									fnb = Arrays.copyOfRange(inData, dataAddr2, dataAddr2+dataLen2);
									for (j = fnb.length - 1; j > 0; j--)
									{
										if (fnb[j] != 0x00)
										{
											dataLen2 = j;
											break;
										}
									}
									out.write(fnb, 0, dataLen2);
								}
								out.flush();
								out.close();
							}
							catch (IOException io)
							{
								io.printStackTrace();
							}
						}
						// else System.out.println(" tag byte at index block: 0x"+UnsignedByte.toString(inData[nodeAddr+8]));
					}
					else break;
				}
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
		System.err.println("ExtractSeawellFiles "+Version.VersionString+" - Extract files from Seawell disk images.");
		System.err.println();
		System.err.println("Usage: ExtractSeawellFiles infile [out_directory]");
	}

}