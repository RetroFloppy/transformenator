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

/*
 * UpdateDOSImage
 * 
 * The intent of this helper app is to add a BIOS Parameter Block (BPB) to a FAT12
 * disk image that lacks one (i.e. DOS 2.0 and earlier).  This makes a disk image
 * mountable on systems that don't pay attention to the media descriptor byte, but 
 * instead require the BPB to tell them what the disk geometry is.
 *
 */
public class UpdateDOSImage
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
					if (isFAT12Image(inData))
					{
						
					}
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
				FileOutputStream out;
				try
				{
					out = new FileOutputStream(args[1]);
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
	 * isFAT12Image - determine if we're looking at a FAT12 image
	 */
	public static boolean isFAT12Image(byte inData[])
	{
		boolean retval = false;
		/*
		 * First check: is the first byte a jmp?
		 */
		System.out.println("Ok, first test passed: first byte is a jmp instruction.");
		if (UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0xe9) || UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0xeb) || UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0x69))
		{
			/*
			 * Second check: do we have a valid media descriptor?
			 */
			if (UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xe5) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xed) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xf0) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xf8) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xf9) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xfa) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xfb) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xfc) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xfd) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xfe) || UnsignedByte.loByte(inData[512+21]) == UnsignedByte.loByte(0xff))
			{
				System.out.println("Ok, second test passed: media descriptor is one we like.");
			}
		}
		return retval;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("UpdateDOSImage v1.6 - Update the BIOS Parameter Block of a DOS disk image.");
		System.err.println();
		System.err.println("Syntax: UpdateDOSImage infile outfile");
	}
}