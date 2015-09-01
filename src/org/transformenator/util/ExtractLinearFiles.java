/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 - 2015 by David Schmidt
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

import org.transformenator.Version;

/*
 * ExtractLinearFiles
 * 
 * Helper app to pull the files off of the virtual file system of some kind of archival disk image - likely a 
 * proprietary archival/tape format.
 * 
 */
public class ExtractLinearFiles
{

	public static void main(java.lang.String[] args)
	{
		String outputDirectory = "";
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
						baseDirFile = new File("." + File.separator + args[1]);
					}
					// System.out.println("Making directory: ["+baseDirFile+"]");
					baseDirFile.mkdirs();
					outputDirectory = args[1];
				}
				else
				{
					if (file.getName().endsWith(".img"))
					{
						// System.out.println("Making image directory: ["+"." + File.separator + file.getName().substring(0, file.getName().length() - 4)+"]");
						outputDirectory = "." + File.separator + file.getName().substring(0, file.getName().length() - 4);
						File baseDirFile = new File(outputDirectory);
						baseDirFile.mkdirs();
					}
				}
				/*
				 * OK, it's all over but the cryin'.
				 */
				byte[] fileMarker = {0,0x5c, 0x55, 0x53, 0x45, 0x52, 0x5c };
				for (int i = 0; i < inData.length-10; i++)
				{
					String fileName = null;
					/* Search for the production "\0/USER" */
					if ((inData[i]==fileMarker[0]) &&
						(inData[i+1]==fileMarker[1]) &&
						(inData[i+2]==fileMarker[2]) &&
						(inData[i+3]==fileMarker[3]) &&
						(inData[i+4]==fileMarker[4]) &&
						(inData[i+5]==fileMarker[5]) &&
						(inData[i+6]==fileMarker[6]))
					{
						//System.out.println("Woohoo!  Found a file!");
						int j = 0;
						for (j = i+1; j<inData.length; j++)
						{
							if(inData[j] == 0x00)
								break;
						}
						fileName = "";
						for (int k = i+1; k < j; k++)
						{
							if (inData[k] == 0x5c)
							{
								fileName += File.separatorChar;
							}
							else
								fileName += (char)inData[k];
						}
						System.out.println(outputDirectory+fileName);
						FileOutputStream out;
						try
						{
							String fullname = new String(outputDirectory + fileName);
							out = new FileOutputStream(fullname);
							System.err.println("Creating file: " + fullname);
							int x = j+1;
							char ch = (char)inData[x];
							while ((ch != 0x00) || (ch == 0x00 && (char)inData[x+1] != 0x5c)) 
							{
								// Check if this is a blank sector - skip if so
								if (ch == 'm' && (char)inData[x+1] == 'm' && x % 512 == 0)
								{
									x+= 512;
								}
								else
								{
									out.write(ch);
									x++;
								}
								if (x < inData.length)
									ch = (char)inData[x];
								else
									break;
							}
							out.flush();
							out.close();
						}
						catch (IOException io)
						{
							io.printStackTrace();
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
		System.err.println("ExtractLinearFiles " + Version.VersionString + " - Extract files from linear disk images.");
		System.err.println();
		System.err.println("Usage: ExtractLinearFiles infile [out_directory]");
	}

}