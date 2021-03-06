/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2019 by David Schmidt
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
 * RevealDirectoryEntries
 * 
 * The intent of this helper app is to "un-hide" the CP/M directory entries of files that 
 * have a leading byte of 0x60.  These typically come from Epson QX-10 TPM-II Valdocs disks.  
 *
 */
public class RevealValdocsEntries
{

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
				boolean foundSome = false, isIMGDISK = false;
				// System.err.println("Read " + inData.length + " bytes.");
				// Disks captured with FC5025 will have entries starting at offset 0x5000
				for (int i = 0x5000; i < 0x5400; i += 0x20)
				{
					if (isValdocsFile(inData, i))
					{
						// Make the directory entry visible
						inData[i] = 0x00;
						foundSome = true;
					}
				}
				if (foundSome == false)
				{
					// Disks captured with IMGDISK may have entries starting at offset 0x4c00
					for (int i = 0x4c00; i < 0x5000; i += 0x20)
					{
						if (isValdocsFile(inData, i))
						{
							// Make the directory entry visible
							inData[i] = 0x00;
							isIMGDISK = true;
							foundSome = true;
						}
					}
				}
				if (foundSome == false)
				{
					// Disks captured with IMGDISK may have entries starting at offset 0x4000
					for (int i = 0x4000; i < 0x4400; i += 0x20)
					{
						// System.err.println("Checking at offset "+Integer.toHexString(i)+"...");
						if (isValdocsFile(inData, i))
						{
							// Make the directory entry visible
							inData[i] = 0x00;
							isIMGDISK = true;
							foundSome = true;
						}
					}
				}
				if (foundSome)
					System.err.println("Found some Valdocs entries.");
				else
					System.err.println("Did not find any Valdocs entries.");
				// Cleaned up the directory - now write the resulting image
				BufferedOutputStream out;
				try
				{
					out = new BufferedOutputStream(new FileOutputStream(args[1]));

					if (isIMGDISK)
					{
						// Since these were (short) IMGDISK-produced images, fatten them out for cpmtools
						out.write(inData, 0, 409600 - inData.length);
					}
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
	 * isValdocsFile - determine if a directory entry is likely to be a Valdocs file
	 */
	public static boolean isValdocsFile(byte inData[], int offset)
	{
		boolean retval = false;
		int i;
		/*
		 * First check: is the first byte a 0x60?
		 */
		if ((inData[offset] == 0x60) || (inData[offset] == 0x00)) 
		{
			/*
			 * Second check: are the first two bytes numeric? All Valdocs files have the form NNxxxnnnVAL, where NN are numeric. Generally all the rest of the x elements are numeric as well, but not always. The n elements are probably always numeric, as they are counters (001, 002, 003, etc.)
			 */
			for (i = 1; i < 3; i++)
			{
				if ((inData[offset + i] < 0x30) || (inData[offset + i] > 0x39))
				{
					/*
					 * A non-numeric value found in the first two bytes, so bail out now.
					 */
					retval = false;
					break;
				}
				retval = true;
			}
			if (retval == true)
			{
				/*
				 * Are the final three bytes "VAL?"
				 */
				if ((inData[offset + 9] == 0x56) && (inData[offset + 10] == 0x41) && (inData[offset + 11] == 0x4c))
					retval = true; // Yes, this assignment is redundant... but I didn't want to tangle the logic more
				else
					retval = false; // No match
			}
		}
		return retval;
	}

	public static String describe(boolean verbose)
	{
		return "Un-hide CP/M directory entries of Valdocs files in disk image files."+
				(verbose?"  These typically come from Epson QX-10 TPM-II Valdocs disks and have a leading byte of 0x60.":"");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("RevealValdocsEntries " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: RevealValdocsEntries infile outfile");
	}
}