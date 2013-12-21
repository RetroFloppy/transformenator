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
					if (isFixable(inData))
					{
						if (modifyImage(inData))
						{
							FileOutputStream out;
							try
							{
								out = new FileOutputStream(args[1]);
								out.write(inData);
								out.flush();
								out.close();
								System.err.println("Image "+args[1]+" saved.");
							}
							catch (IOException e)
        						{
								e.printStackTrace();
        						}
						}
						else
						{
							System.err.println("Image is not of the expected format or size; unable to modify.");
						}
					}
					else
					{
						System.err.println("Image failed validty checks; unable to modify.");
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
		}
		else
		{
			// wrong args
			help();
		}
	}

	/*
	 * modifyImage - make bpb changes to image
	 */
	public static boolean modifyImage(byte inData[])
	{
		if (is160k(inData))
		{
			return true;
		}
		else if (is180k(inData))
		{
			return true;
		}
		else if (is320k(inData))
		{
			return true;
		}
		else if (is360k(inData))
		{
			return true;
		}
		return false;
	}

	public static boolean is160k(byte[] inData)
	{
		byte bpb_160k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x01, 0x01, 0x00,
			0x02, 0x40, 0x00, 0x40, 0x01, (byte)0xfe, 0x01, 0x00,
			0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
	        };

		if ((inData[512] == (byte)0xfe) && inData.length == 163840)
		{
			for (int i = 0; i < bpb_160k.length; i++)
				inData[i] = bpb_160k[i];
			// System.out.println("is160k is true!");
			return true;
		}
		// System.out.println("is160k is false.");
		return false;
	}

	public static boolean is180k(byte[] inData)
	{
		byte bpb_180k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x01, 0x01, 0x00,
			0x02, 0x40, 0x00, 0x68, 0x01, (byte)0xfc, 0x02, 0x00,
			0x09, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};

		if ((inData[512] == (byte)0xfc) && inData.length == 184320)
		{
			for (int i = 0; i < bpb_180k.length; i++)
				inData[i] = bpb_180k[i];
			// System.out.println("is180k is true!");
			return true;
		}
		// System.out.println("is180k is false.");
		return false;
	}
	
	public static boolean is320k(byte[] inData)
	{
		byte bpb_320k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x01, 0x01, 0x00,
			0x02, 0x70, 0x00, (byte)0x80, 0x02, (byte)0xfa, 0x02, 0x00,
			0x08, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};

		if ((inData[512] == (byte)0xfa) && inData.length == 327680)
		{
			for (int i = 0; i < bpb_320k.length; i++)
				inData[i] = bpb_320k[i];
			// System.out.println("is320k is true!");
			return true;
		}
		// System.out.println("is320k is false.");
		return false;
	}
	
	public static boolean is360k(byte[] inData)
	{
		byte bpb_360k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x02, 0x01, 0x00,
			0x02, 0x70, 0x00, (byte)0xd0, 0x02, (byte)0xfd, 0x02, 0x00,
			0x09, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};
		if ((inData[512] == (byte)0xfd) && inData.length == 360*1024)
		{
			for (int i = 0; i < bpb_360k.length; i++)
				inData[i] = bpb_360k[i];
			// System.err.println("is360k is true!");
			return true;
		}
		// System.err.println("is360k is false.");
		return false;
	}
	
	/**
	 * isFixable - determine if we can fix this image at all
	 */
	public static boolean isFixable(byte inData[])
	{
		if (isFAT12(inData) && !hasBPB(inData))
		{
			return true;
		}
		else return false;
	}

	/**
	 * isFAT12 - determine if we're looking at a FAT12 image
	 */
	public static boolean isFAT12(byte inData[])
	{
		boolean retval = false;
		int firstByte, mediaDescriptor;
		/*
		 * First check: is the first byte a jmp?
		 */
		firstByte = UnsignedByte.loByte(inData[0]);
		if (firstByte == UnsignedByte.loByte(0xe9) || firstByte == UnsignedByte.loByte(0xeb) || firstByte == UnsignedByte.loByte(0x69))
		{
			// System.err.println("isFAT12(): first test passed: first byte is a jmp instruction.");
			/*
			 * Second check: do we have a valid media descriptor?
			 */
			mediaDescriptor = UnsignedByte.loByte(inData[0x200]);
			if (mediaDescriptor == UnsignedByte.loByte(0xe5) || mediaDescriptor == UnsignedByte.loByte(0xed) || mediaDescriptor == UnsignedByte.loByte(0xf0) || mediaDescriptor == UnsignedByte.loByte(0xf8) || mediaDescriptor == UnsignedByte.loByte(0xf9) || mediaDescriptor == UnsignedByte.loByte(0xfa) || mediaDescriptor == UnsignedByte.loByte(0xfb) || mediaDescriptor == UnsignedByte.loByte(0xfc) || mediaDescriptor == UnsignedByte.loByte(0xfd) || mediaDescriptor == UnsignedByte.loByte(0xfe) || mediaDescriptor == UnsignedByte.loByte(0xff))
			{
				// System.err.println("isFAT12(): second test passed: media descriptor is one we like.");
				retval = true;
			}
		}
		return retval;
	}

	/**
	 * hasBPB - determine if the image likely already has a valid BPB
	 */
	public static boolean hasBPB(byte inData[])
	{
		boolean retval = false;
		int mediaDescriptor;
		/*
		 * First check: is the first byte a jmp?
		 */
		// System.err.println("hasBPB(): first test passed: first byte is a jmp instruction.");
		if (UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0xe9) || UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0xeb) || UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0x69))
		{
			/*
			 * Second check: do we have a valid media descriptor?
			 */
			mediaDescriptor = UnsignedByte.loByte(inData[0x15]);
			if (
				mediaDescriptor == UnsignedByte.loByte(0xe5) ||
				mediaDescriptor == UnsignedByte.loByte(0xed) ||
				mediaDescriptor == UnsignedByte.loByte(0xf0) ||
				mediaDescriptor == UnsignedByte.loByte(0xf8) ||
				mediaDescriptor == UnsignedByte.loByte(0xf9) ||
				mediaDescriptor == UnsignedByte.loByte(0xfa) ||
				mediaDescriptor == UnsignedByte.loByte(0xfb) ||
				mediaDescriptor == UnsignedByte.loByte(0xfc) ||
				mediaDescriptor == UnsignedByte.loByte(0xfd) ||
				mediaDescriptor == UnsignedByte.loByte(0xfe) ||
				mediaDescriptor == UnsignedByte.loByte(0xff))
			{
				// System.err.println("hasBPB(): second test passed: media descriptor smells like bpb.");
				/*
				 * Third check: do we have the end-of-sector signature?
				 */
				if (
					UnsignedByte.loByte(inData[510]) == UnsignedByte.loByte(0x55) &&
					UnsignedByte.loByte(inData[511]) == UnsignedByte.loByte(0xaa))
				{
					// System.err.println("hasBPB(): third test passed: end-of-sector marker smells like bpb.  Calling it a bpb.");
					retval = true;
				}
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