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
 * ExtractCTOSArchive
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of
 * CTOS NGEN disk images; specifically, the ones that are part of an NGEN archive.
 * 
 * Weaknesses: 
 *  - The end-of-file isn't detected except on 512-byte boundaries, meaning
 *    each file has extra cruft at the end that is not really part of the file.
 *  - Archives probably span disks, and there's not a way to concatenate these
 *    together and make better sense of them. 
 *
 */
public class ExtractCTOSArchive
{

	public static void main(java.lang.String[] args)
	{
		int indexSearch;
		byte[] fileIndex = new byte[4];

		if ((args.length == 1) || (args.length == 2))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			try
			{
				/*
				 * Read in the entire disk into memory
				 */
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
				if (args.length == 2)
				{
					/*
					 * If they wanted an output directory, go ahead and make it.
					 */
					File baseDirFile = new File(args[1]);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("./"+args[1]);
					}
					baseDirFile.mkdir();
				}
				/*
				 * We have a disk's worth of data.  Start hunting for start-of-file
				 * indices at the beginning of the disk.
				 */
				for (indexSearch = 5; indexSearch < inData.length - 7; indexSearch++)
				{
					if (/*(inData[indexSearch + 0] == 0x00) &&*/ 
							(inData[indexSearch + 1] == 0x0c) &&
							(inData[indexSearch + 2] == 0x01) &&
							(inData[indexSearch + 3] == 0x10))
					{
						// System.out.println("Found a file index at "+indexSearch+" ...");
						// byte 12 is the length of the filename
						int fnlen = inData[indexSearch + 12];
						String fn = "";
						boolean stillFinding = true;
						FileOutputStream out;
						for (int fnc = 1; fnc <= fnlen; fnc++)
						{
							char ch = (char) inData[indexSearch + 12 + fnc];
							/* Strip out some obviously bad filename characters */
							if ((ch == '>') || (ch == '<') || (ch == '\\') || (ch == '/') || (ch == '`') || (ch == '*'))
								ch = '_';
							fn = fn + ch;
						}
						/* Open the file named "fn" */
						try
						{
							if (args.length == 2)
								fn = args[1] + File.separator + fn;
							out = new FileOutputStream(fn);

							fileIndex[0] = inData[indexSearch + 4];
							fileIndex[1] = inData[indexSearch + 5];
							fileIndex[2] = inData[indexSearch + 6];
							fileIndex[3] = inData[indexSearch + 7];
							do
							{
								/*
								 * Search the entire disk for file segments from the beginning
								 * for each one, since they aren't necessarily on disk in order.
								 * This of course could be sped/smartened up by finding them all
								 * in order with one pass and keeping a table.
								 */
								stillFinding = false;
								fileIndex[2]++;
								if (fileIndex[2] == 0)
									fileIndex[3]++;
								for (int segmentSearch = 5; segmentSearch < inData.length - 7; segmentSearch++)
								{
									if (/*(inData[segmentSearch + 0] == 0x00) &&*/
											(inData[segmentSearch + 1] == 0x0c) &&
											(inData[segmentSearch + 2] == 0x02) &&
											(inData[segmentSearch + 3] == 0x10) &&
											(inData[segmentSearch + 4] == fileIndex[0]) &&
											(inData[segmentSearch + 5] == fileIndex[1]) &&
											(inData[segmentSearch + 6] == fileIndex[2]) &&
											(inData[segmentSearch + 7] == fileIndex[3]))
									{
										// System.out.println("Found a segment at "+segmentSearch);
										byte range[] = Arrays.copyOfRange(inData, segmentSearch + 8, segmentSearch + 8 + 512);
										out.write(range);
										stillFinding = true;
									}
								}
								//if (stillFinding == false)
								//	System.out.println("Stopped finding indexes at "+(fileIndex[2]+(fileIndex[3]*256)));
							} while (stillFinding);
							System.out.println("Extracted file "+fn);
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
		System.err.println("ExtractCTOSArchive "+Version.VersionString+" - Extract files from CTOS archives.");
		System.err.println();
		System.err.println("Usage: ExtractCTOSArchive infile [out_directory]");
	}
}