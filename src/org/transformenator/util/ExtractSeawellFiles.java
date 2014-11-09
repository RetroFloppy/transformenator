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
 * Helper app to pull the files off of the virtual file system of a Seawell
 * 8" disk image.
 * 
 * Disk geometry: 2 sides, 256 bytes per sector, 26 sectors per track, 76 tracks
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
				// Read in the entire disk image
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
				// We have good data; get ready to deal with it.
				System.err.println("Read " + inData.length + " bytes.");
				if (args.length > 1)
				{
					// If they wanted an output directory, go ahead and make it.
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("."+File.separator+args[1]);
					}
					// System.out.println("Making directory: ["+baseDirFile+"]");
					baseDirFile.mkdir();
				}
				/*
				 * Catalog starts on the 19th track - no idea how far it can stretch,
				 * so be conservative about what we identify as a valid file entry
				 * 
				 * Catalog entries are 36 (0x24) bytes long:
				 * bytes 0x00-0x0f: Data (unknown origin)
				 * bytes 0x10-0x21: Filename (zero padded, generally)
				 * bytes 0x22-0x23: Track, sector of the index entry
				 *
				 */
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
						// System.out.println("Found file: "+filename);
						int indexEntry = (UnsignedByte.intValue(inData[i+33]))*13312 + (UnsignedByte.intValue(inData[i+34])-1)*256;
						/*
						 * Now pull up the index entry for that file
						 *  
						 * Index entries start in 0x08 bytes.  It starts with a leading 0x34, 0x00 (likely a track/
						 * sector multiplier?), followed by one ore more pointers to sector chains.  Sector chain
						 * pointers look like this:
						 * 0xnn - starting track
						 * 0xnn - starting sector
						 * 0xnn - sector count (runs from 0x01 to 0x34; if there are 0x34 sectors, there may be another chain)
						 *
						 * We currently only look for two chains, as no file was found that had more than that.
						 *
						 */
						if ((inData[indexEntry+8] == 0x34) && (inData[indexEntry+9] == 0x00))
						{
							byte fnb[];
							/*
							System.out.println("Extracting file: "+filename);
							System.out.print("Node Addr: 0x"+Integer.toHexString(0x1000000 |indexEntry).substring(1).toUpperCase());
							System.out.print(" Node Data: 0x");
							for (int k = 8; k < 20; k++)
								System.out.print(UnsignedByte.toString(inData[indexEntry+k]));
							*/
							int dataAddr1 = (UnsignedByte.intValue(inData[indexEntry+0x0a]))*13312 + (UnsignedByte.intValue(inData[indexEntry+0x0b])-1)*256;
							int dataLen1 = inData[indexEntry+0x0c]*256;
							int dataAddr2;
							if (inData[indexEntry+0x0e] == 0x00)
								dataAddr2 = 0;
							else
								dataAddr2 = (UnsignedByte.intValue(inData[indexEntry+0x0d]))*13312 + (UnsignedByte.intValue(inData[indexEntry+0x0e])-1)*256;
							int dataLen2 = inData[indexEntry+0x0f]*256;
							/*
							System.out.println();
							System.out.println("Data Add1: 0x"+Integer.toHexString(0x1000000 | dataAddr1).substring(1).toUpperCase()+" length: 0x"+ Integer.toHexString(0x10000 | dataLen1).substring(1).toUpperCase());
							if (dataLen2 > 0)
								System.out.println("Data Add2: 0x"+Integer.toHexString(0x1000000 | dataAddr2).substring(1).toUpperCase()+" length: 0x"+ Integer.toHexString(0x10000 | dataLen2).substring(1).toUpperCase());
							System.out.println();
							*/
							FileOutputStream out;
							// Copy data from first sector chain
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
								// Dump out the first chain
								out.write(fnb, 0, dataLen1);
								if (dataLen2 > 0)
								{
									// Copy data from second sector chain, if one exists
									fnb = Arrays.copyOfRange(inData, dataAddr2, dataAddr2+dataLen2);
									for (j = fnb.length - 1; j > 0; j--)
									{
										if (fnb[j] != 0x00)
										{
											dataLen2 = j;
											break;
										}
									}
									// Dump out the second chain
									out.write(fnb, 0, dataLen2);
								}
								// All done with all files
								out.flush();
								out.close();
							}
							catch (IOException io)
							{
								io.printStackTrace();
							}
						}
						// else System.out.println(" tag byte at index block: 0x"+UnsignedByte.toString(inData[indexEntry+8]));
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