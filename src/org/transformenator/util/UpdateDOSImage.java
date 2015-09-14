/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2015 by David Schmidt
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
import org.transformenator.internal.UnsignedByte;

/*
 * UpdateDOSImage
 * 
 * The intent of this helper app is to add a BIOS Parameter Block (BPB) to a FAT12
 * disk image that lacks one (i.e. DOS 2.0 and earlier).  This makes a disk image
 * mountable on systems that don't pay attention to the media descriptor byte, but 
 * instead require the BPB to tell them what the disk geometry is.
 *
 * It also checks for and removes the Stoned virus:
 *   http://en.wikipedia.org/wiki/Stoned_%28computer_virus%29
 *
 */
public class UpdateDOSImage
{

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 2) || (args.length == 3))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			int force = 0;
			if (args.length == 3)
			{
				if (args[2].equalsIgnoreCase("force160"))
				{
					force = 160;
				}
				else if (args[2].equalsIgnoreCase("force180"))
				{
					force = 180;
				}
				else if (args[2].equalsIgnoreCase("force320"))
				{
					force = 320;
				}
				else if (args[2].equalsIgnoreCase("force360"))
				{
					force = 360;
				}
				else if (args[2].equalsIgnoreCase("force360a")) // For Atari 3.5" single sided disks
				{
					force = 361;
				}
			}

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
					if (isFixable(inData) || (force > 0))
					{
						if (modifyImage(inData, force))
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
	public static boolean modifyImage(byte inData[], int force)
	{
		if (is160k(inData, force))
		{
			return true;
		}
		else if (is180k(inData, force))
		{
			return true;
		}
		else if (is320k(inData, force))
		{
			return true;
		}
		else if (is360k(inData, force))
		{
			return true;
		}
		else if (is1200k(inData, force))
		{
			return true;
		}
		return false;
	}

	public static boolean is160k(byte[] inData, int force)
	{
		byte bpb_160k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x01, 0x01, 0x00,
			0x02, 0x40, 0x00, 0x40, 0x01, (byte)0xfe, 0x01, 0x00,
			0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
	        };

		if (((inData[512] == (byte)0xfe) && inData.length == 160*1024) || (force == 160))
		{
			for (int i = 0; i < bpb_160k.length; i++)
				inData[i] = bpb_160k[i];
			// System.out.println("is160k is true!");
			return true;
		}
		// System.out.println("is160k is false.");
		return false;
	}

	public static boolean is180k(byte[] inData, int force)
	{
		byte bpb_180k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x01, 0x01, 0x00,
			0x02, 0x40, 0x00, 0x68, 0x01, (byte)0xfc, 0x02, 0x00,
			0x09, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};

		if (((inData[512] == (byte)0xfc) && inData.length == 180*1024) || (force == 180))
		{
			for (int i = 0; i < bpb_180k.length; i++)
				inData[i] = bpb_180k[i];
			// System.out.println("is180k is true!");
			return true;
		}
		// System.out.println("is180k is false.");
		return false;
	}
	
	public static boolean is320k(byte[] inData, int force)
	{
		byte bpb_320k[]={
			(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
			0x33, 0x2e, 0x32, 0x00, 0x02, 0x01, 0x01, 0x00,
			0x02, 0x70, 0x00, (byte)0x80, 0x02, (byte)0xfa, 0x02, 0x00,
			0x08, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};

		if (((inData[512] == (byte)0xfa) && inData.length == 320*1024) || (force == 320))
		{
			for (int i = 0; i < bpb_320k.length; i++)
				inData[i] = bpb_320k[i];
			// System.out.println("is320k is true!");
			return true;
		}
		// System.out.println("is320k is false.");
		return false;
	}
	
	public static boolean is360k(byte[] inData, int force)
	{
		byte bpb_360k[]={
				(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
				0x33, 0x2e, 0x32, 0x00, 0x02, 0x02, 0x01, 0x00,
				0x02, 0x70, 0x00, (byte)0xd0, 0x02, (byte)0xfd, 0x02, 0x00,
				0x09, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			};
		byte bpb_360k_atari[]={ /* 3.5", single sided, 360k */
				(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
				0x33, 0x2e, 0x32, 0x00, 0x02, 0x02, 0x01, 0x00,
				/* (Offset 0x05) */
				0x02, 0x70, 0x00, (byte)0xd0, 0x02, (byte)0xf8, 0x05, 0x00,
				/* (Offset 0x0d) */
				0x09, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			};
		// Check for stoned virus
		if (hasStonedVirus(inData) && (inData.length == 360*1024))
		{
			// Copy out the stashed boot block
			for (int i = 0; i < 512; i++)
			{
				inData[i] = inData[i+0x1600];
				inData[i+0x1600] = 0x00;
			}	
			System.err.println("Removed Stoned virus from 360K image.");
			return true;
		}
		else if (((inData[512] == (byte)0xfd) && inData.length == 360*1024) || (force == 360))
		{
			for (int i = 0; i < bpb_360k.length; i++)
				inData[i] = bpb_360k[i];
			// System.err.println("is360k is true!");
			return true;
		}
		else if (((inData[512] == (byte)0xfd) && inData.length == 360*1024) || (force == 361))
		{
			for (int i = 0; i < bpb_360k_atari.length; i++)
				inData[i] = bpb_360k_atari[i];
			System.err.println("is360k Atari is true!");
			return true;
		}
		// System.err.println("is360k is false.");
		return false;
	}
	
	/**
	 * is1200k - the only thing a 1.2MB disk can do is remove the Stoned virus.
	 */
	public static boolean is1200k(byte[] inData, int force)
	{
		// Check for stoned virus
		if (hasStonedVirus(inData) && (inData.length == 1200*1024))
		{
			// Copy out the stashed boot block
			for (int i = 0; i < 512; i++)
			{
				inData[i] = inData[i+0x2200];
				inData[i+0x2200] = 0x00;
			}	
			System.err.println("Removed Stoned virus from 1.2MB image.");
			return true;
		}
		// System.err.println("is1200k is false.");
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
		else if ((hasStonedVirus(inData) && (inData.length == 360*1024)) ||
			(hasStonedVirus(inData) && (inData.length == 1200*1024)))
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
		int firstByte;
		/*
		 * First check: is the first byte a jmp?
		 */
		firstByte = UnsignedByte.loByte(inData[0]);
		if (firstByte == UnsignedByte.loByte(0xe9) ||
			firstByte == UnsignedByte.loByte(0xeb) ||
			firstByte == UnsignedByte.loByte(0x69))
		{
			// System.err.println("isFAT12(): first test passed: first byte is a jmp instruction.");
			/*
			 * Second check: do we have a valid media descriptor?
			 */
			retval = isMediaDescriptor(UnsignedByte.loByte(inData[0x200]));
		}
		return retval;
	}

	public static boolean hasStonedVirus(byte inData[])
	{
		boolean retval = false;
		if (UnsignedByte.loByte(inData[0]) == UnsignedByte.loByte(0xea)) // Stoned virus
		{
			if (((inData[0x199] == 'S') && 
					(inData[0x19a] == 't') &&
					(inData[0x19b] == 'o') &&
					(inData[0x19c] == 'n') &&
					(inData[0x19d] == 'e') &&
					(inData[0x19e] == 'd') &&
					(inData[0x19f] == '!')) &&
					(isMediaDescriptor(UnsignedByte.loByte(inData[0x200]))))
			{
				retval = true;
			}
			
		}
		return retval;
	}

	/**
	 * isMediaDescriptor - is this a valid media descriptor byte?
	 */
	public static boolean isMediaDescriptor(byte mediaDescriptor)
	{
		boolean retval = false;
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
			// System.err.println("isMediaDescriptor(): media descriptor is one we like.");
			retval = true;
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
		 * First check: do we have a valid media descriptor inside the BPB?
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
			// System.err.println("hasBPB(): first test passed: media descriptor smells like BPB.");
			/*
			 * Second check: do we have the end-of-sector signature?
			 */
			if (
				UnsignedByte.loByte(inData[510]) == UnsignedByte.loByte(0x55) &&
				UnsignedByte.loByte(inData[511]) == UnsignedByte.loByte(0xaa))
			{
				// System.err.println("hasBPB(): second test passed: end-of-sector marker smells like BPB.  Calling it a BPB.");
				retval = true;
			}
		}
		return retval;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("UpdateDOSImage "+Version.VersionString+" - Update the BIOS Parameter Block of a PC DOS disk image.");
		System.err.println();
		System.err.println("Usage: UpdateDOSImage infile outfile");
	}
}