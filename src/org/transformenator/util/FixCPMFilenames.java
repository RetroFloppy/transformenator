/*
 * Transformenator - perform transformation operations on binary files Copyright
 * (C) 2019 by David Schmidt
 * 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.transformenator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;

import org.transformenator.internal.Version;

/*
 * FixCPMFilenames
 * 
 * The intent of this helper app is to fix some "illegal" characters in CP/M
 * directory entries of files that are in CP/M disk directories.
 *
 */
public class FixCPMFilenames
{
	int j;
	public static void main(java.lang.String[] args)
	{
		if (args.length == 2)
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
				boolean foundSome = false;
				// System.err.println("Read " + inData.length + " bytes.");
				for (int i = 0x1a00; i < 0x4d00; i += 0x20)
				{
					boolean thisOne = false;
					if (isDirectoryEntry(inData, i))
					{
						String ofn="";
						String oft="";
						String nfn="";
						String nft="";
						String obuffer="";
						// Sanitize the directory entry
						for (int j = 1; j < 0x0b; j++ )
						{
							if (j < 9)
							{
								if(inData[i+j] == 0x20)
								{
									obuffer += " ";
								}
								else
									ofn = ofn + (char)inData[i+j];
							}
							if (j >= 9)
								oft = oft + (char)+inData[i+j];
							if (inData[i+j] == 0x2f)
							{
								inData[i+j] = '-';
								foundSome = true;
								thisOne = true;
							}
							if (j < 8)
							{
								if(inData[i+j] != 0x20)
								{
									nfn = nfn + (char)inData[i+j];
								}
							}
							if (j > 8)
								nft = nft + (char)inData[i+j];
						}
						if (thisOne)
						{
							System.out.println("Changed filename "+ofn+"."+oft+obuffer+" to "+nfn+"."+nft);
						}
					}
				}
				if (!foundSome)
					System.err.println("Did not find any names to change.");
				// Cleaned up the directory - now write the resulting image
				BufferedOutputStream out;
				try
				{
					out = new BufferedOutputStream(new FileOutputStream(args[1]));
					out.write(inData);
					out.flush();
					out.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	/**
	 * isDirectoryEntry - determine if we have a directory entry
	 */
	public static boolean isDirectoryEntry(byte inData[], int offset)
	{
		boolean retval = false;
		/*
		 * First check: is the first byte a 0x00?
		 */
		if (inData[offset] == 0x00)
		{
			/*
			 * Second check: are there three nulls after the file name and extension?
			 */
			if ((inData[offset + 0x0c] == 0x00) &&
				(inData[offset + 0x0d] == 0x00) &&
				(inData[offset + 0x0e] == 0x00))
			{
				/*
				 * Probably a directory entry!
				 */
				retval = true;
			}
		}
		return retval;
	}

	public static String describe(boolean verbose)
	{
		return "Sanitize CP/M directory entries in disk image files." + (verbose ? "" : "");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("FixCPMFilenames " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: FixCPMFilenames infile outfile");
	}
}