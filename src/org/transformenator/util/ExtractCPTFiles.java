/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2014 by David Schmidt
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
 * ExtractWangFiles
 * 
 * This helper app pulls the files off of the virtual file system of CPT
 * word processor disks.
 *
 */
public class ExtractCPTFiles
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
				System.err.print("Skew: ");
				for (int i = 0; i < 16; i++)
					System.err.print(mapSector(i, 1)+" ");
				System.err.println();
				if (inData.length % (256*16) == 0)
				{
					// Ok, we have good sectors.
					int trackLen = 256*16;
					String intname = "";
					byte fnb[] = new byte[256];
					FileOutputStream out;
					System.err.println("Creating file: " + args[1]);
					try
					{
						out = new FileOutputStream(args[1]);
						for (int i = 0; i < 77; i++)
						{
							for (int j = 0; j < 16; j++)
							{
								int offset = mapSector(j, 1);
								fnb = Arrays.copyOfRange(inData, offset*256 + (i * trackLen), (offset+1)*256 + (i * trackLen));
								out.write(fnb);
								System.err.println("*** CPT track "+i+" sector "+UnsignedByte.toString(offset)+" ***");
								for (int x = 0; x < 256; x++)
								{
									if (UnsignedByte.intValue(fnb[x]) == 0x80)
										System.err.print(' ');
									else if (UnsignedByte.intValue(fnb[x]) == 0x00)
									{
										//System.err.println("[EOF]");
										break;
									}
									else if (UnsignedByte.intValue(fnb[x]) == 0x03)
									{
										System.err.println("[EOF]");
										break;
									}
									else
										System.err.print((char)fnb[x]);
								}
								System.err.println();
								if (fnb[0] == 0x30)
								{
									// System.err.println("Found directory sector at track "+i+" sector "+j);
									for (int k = 0; k < 11; k++)
									{
										intname = "";
										if (fnb[(k*23)+1] != 0x00)
										{
											// Pull out the file name
											int l;
											for (l = 0; l < 16; l++)
											{
												if (fnb[(k*23)+1+l] != 0x20)
													intname += (char)fnb[(k*23)+1+l];
												System.err.print((char)fnb[(k*23)+1+l]);
											}
											byte fnb2[] = new byte[256];
											String intname2 = "";
											int cptlocation = 0;
											int sect = 0;
											int nativesect = 0;
											int mappedsect = 0;
											// Look for the file name later on disk
											for (int trk = 0; trk < 77; trk++)
											{
												for (sect = 0; sect < 16; sect++)
												{
													int offset2 = mapSector(sect, 1);
													fnb2 = Arrays.copyOfRange(inData, offset2*256 + (trk * trackLen), (offset2+1)*256 + (trk * trackLen));
													if (fnb2[0] == 0x01)
													{
														intname2 = "";
														boolean pad = false;
														for (int k2 = 1; k2 < 17; k2++)
														{
															if (fnb2[k2] == 0x02)
																pad = true;
															if (!pad)
																intname2 += (char)fnb2[k2];
														}
														// Is this the file we were looking for?
														if (intname.equals(intname2))
														{
															cptlocation = trk; // (trk*4096); //+(sect*256);
															nativesect = sect;
															mappedsect = offset2; //(trk*4096) + (offset2);
														}
													}
												}
											}
											// Dump out the data bytes of this directory entry
											for (l = 16; l < 23; l++)
											{
												System.err.print(UnsignedByte.toString(fnb[(k*23)+1+l]));
											}
											System.err.print(' ');
											// Dump out the data bytes of this directory entry
											for (l = 21; l < 23; l++)
											{
												System.err.print(UnsignedByte.toString(fnb[(k*23)+1+l]));
											}
											System.err.println(" "+Integer.toHexString(0x1000 | cptlocation).substring(1)+
													" "+Integer.toHexString(nativesect) +
													" "+Integer.toHexString(mappedsect));
										}
									}
								}
/*
								if (fnb[0] == 0x01)
								{
									boolean pad = false;
									for (int k = 1; k < 17; k++)
									{
										if (fnb[k] == 0x02)
											pad = true;
										if (pad)
											System.err.print(' ');
										else
											System.err.print((char)fnb[k]);
									}
									System.err.println(Integer.toHexString((i*4096) + (j*256)));
								}
*/
							}
						}
						out.flush();
						out.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static int mapSector(int sectorIn, int trackIn)
	{
//		int  skewedSectorMap[] = { 0, 3, 6, 9, 12, 15, 2, 5, 8, 11, 14, 1, 4, 7, 10, 13};

		int  skewedSectorMap[] = { 2, 5, 8, 11, 14, 1, 4, 7, 10, 13, 0, 3, 6, 9, 12, 15, 2, 5, 8, 11, 14, 1, 4, 7, 10, 13 , 0, 3, 6, 9, 12, 15, 2, 5, 8, 11, 14, 1, 4, 7, 10, 13 };
//		int  skewedSectorMap[] = { 8, 11, 14, 1, 4, 7, 10, 13, 0, 3, 6, 9, 12, 15, 2, 5 };
//		int  skewedSectorMap[] = { 10, 13, 0, 3, 6, 9, 12 , 15, 2, 5, 8, 11, 14, 1, 4, 7 };
//		int  skewedSectorMap[] = { 8, 11, 14, 1, 4, 7, 10, 13, 0, 3, 6, 9, 12 , 15, 2, 5 };
		return skewedSectorMap[sectorIn];
/*
 * Sector skew went from 1/f to 2/4
 */
	}

	public static int realAddress(int sector, int offset)
	{
		return sector * 256 + offset;
	}

	public static int realWPAddress(int track, int sector, int offset, int skew)
	{
		int newSector = mapSector(sector, skew);
		return track * 4096 + newSector * 256 + offset;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractCPTFiles "+Version.VersionString+" - Extract files from CPT word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractCPTFiles infile [outfile|out_directory]");
	}
}