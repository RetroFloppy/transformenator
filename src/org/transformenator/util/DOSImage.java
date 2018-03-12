/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2017 by David Schmidt
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
import java.io.InputStream;

import org.transformenator.internal.UnsignedByte;
import org.transformenator.internal.Version;

/*
 * DOSImage
 * 
 * The intent of this helper app is to view or add a BIOS Parameter Block (BPB) to a FAT12
 * disk image that lacks one (i.e. DOS 2.0 and earlier).  This makes a disk image
 * mountable on systems that don't pay attention to the media descriptor byte, but 
 * instead require the BPB to tell them what the disk geometry is.
 *
 * With 'update' parameter, also checks for and removes the Stoned and Michelangelo viruses:
 *   https://en.wikipedia.org/wiki/Stoned_(computer_virus)
 *   https://en.wikipedia.org/wiki/Michelangelo_(computer_virus)
 *
 */
public class DOSImage
{

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 2) || (args.length == 3) || (args.length == 4))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[1]);
			File file = new File(args[1]);
			int force = 0;
			if (args.length == 4)
			{
				if (args[3].equalsIgnoreCase("force160"))
				{
					force = 160;
				}
				else if (args[3].equalsIgnoreCase("force180"))
				{
					force = 180;
				}
				else if (args[3].equalsIgnoreCase("force320"))
				{
					force = 320;
				}
				else if (args[3].equalsIgnoreCase("force360"))
				{
					force = 360;
				}
				else if (args[3].equalsIgnoreCase("force360a")) // For Atari 3.5" single sided disks
				{
					force = 361;
				}
				else if (args[3].equalsIgnoreCase("force1200"))
				{
					force = 1200;
				}
			}

			byte[] result = new byte[(int) file.length()];
			// Load up the input file
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
					if (args.length > 2)
					{
						if (args[0].equalsIgnoreCase("update"))
						{
							if (isFixable(inData) || (force > 0))
							{
								if (modifyImage(inData, force))
								{
									FileOutputStream out;
									try
									{
										out = new FileOutputStream(args[2]);
										out.write(inData);
										out.flush();
										out.close();
										System.err.println("Image "+args[2]+" saved.");
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
						else
							help();
					}
					else
					{
						if (args[0].equalsIgnoreCase("display"))
						{
							System.out.println("Signature - typically 0x000055AA: 0x"+UnsignedByte.toString(result[0x1fc])+UnsignedByte.toString(result[0x1fd])+UnsignedByte.toString(result[0x1fe])+UnsignedByte.toString(result[0x1ff]));
							System.out.println("Jump byte [0x00]: "+UnsignedByte.toString(result[0]));
							System.out.println("OEM name [0x03-0x0a]: "+(char)result[3]+(char)result[4]+(char)result[5]+(char)result[6]+(char)result[7]+(char)result[8]+(char)result[9]+(char)result[10]);
							System.out.println("Bytes per logical sector: "+(UnsignedByte.intValue(result[0x0b])+UnsignedByte.intValue(result[0x0c])*256));
							System.out.println("Logical sectors per cluster [0x0d]: "+UnsignedByte.intValue(result[0x0d]));
							System.out.println("Count of reserved logical sectors [0x0e-0x0f]: "+(UnsignedByte.intValue(result[0x0e])+UnsignedByte.intValue(result[0x0f])*256));
							System.out.println("Number of File Allocation Tables [0x10]: "+UnsignedByte.intValue(result[0x10]));
							System.out.println("Maximum number of FAT12 or FAT16 root directory entries [0x11-0x12]: "+(UnsignedByte.intValue(result[0x11])+UnsignedByte.intValue(result[0x12])*256));
							System.out.println("Total logical sectors [0x13-0x14]: "+(UnsignedByte.intValue(result[0x13])+UnsignedByte.intValue(result[0x14])*256));
							System.out.println("Media descriptor [0x15]: 0x"+UnsignedByte.toString(result[0x15]));
							System.out.println("Description(s):");
							System.out.println(mediaDescriptor(result[0x15]));
							System.out.println("Logical sectors per File Allocation Table [0x16-0x17]: "+(UnsignedByte.intValue(result[0x16])+UnsignedByte.intValue(result[0x17])*256));
							if (hasStonedVirus(inData)) System.out.println("Stoned virus detected");
							if (hasMichelangeloVirus(inData)) System.out.println("Michelangelo virus detected");
						}
						else
						{
							help();
						}
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
				System.err.println("Input file \"" + file + "\" not accessible.");
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
		byte bpb_360k[]={ /* 5.25", double sided, 40 track, 360k */
				(byte)0xeb, 0x34, (byte)0x90, 0x4d, 0x53, 0x44, 0x4f, 0x53,
				0x33, 0x2e, 0x32, 0x00, 0x02, 0x02, 0x01, 0x00,
				0x02, 0x70, 0x00, (byte)0xd0, 0x02, (byte)0xfd, 0x02, 0x00,
				0x09, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			};
		byte bpb_360k_atari[]={ /* 3.5", single sided, 80 track, 360k */
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
		if (hasMichelangeloVirus(inData) && (inData.length == 360*1024))
		{
			// Copy out the stashed boot block
			for (int i = 0; i < 512; i++)
			{
				inData[i] = inData[i+0x0c00];
				inData[i+0x0c00] = 0x00;
			}	
			System.err.println("Removed Michelangelo virus from 360K image.");
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
			// System.err.println("is360k Atari is true!");
			return true;
		}
		// System.err.println("is360k is false.");
		return false;
	}
	
	/**
	 * is1200k - the only thing a 1.2MB disk can do is remove boot block viruses.
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
		else if (hasMichelangeloVirus(inData) && (inData.length == 1200*1024))
		{
			// Copy out the stashed boot block
			for (int i = 0; i < 512; i++)
			{
				inData[i] = inData[i+0x3800];
				inData[i+0x3800] = 0x00;
			}	
			System.err.println("Removed Michelangelo virus from 1.2MB image.");
			return true;
		}
		// System.err.println("DEBUG: is1200k is false.");
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
		else if ((hasMichelangeloVirus(inData) && (inData.length == 360*1024)) ||
				(hasMichelangeloVirus(inData) && (inData.length == 1200*1024)))
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

	public static boolean hasMichelangeloVirus(byte inData[])
	{
		boolean retval = false;
		if (UnsignedByte.loByte(inData[0x15]) != UnsignedByte.loByte(0xf9)) // Illogical media descriptor - might be Michelangelo
		{
			// Look for a good BPB at 0x3800 - 1.2MB infection
			if ((UnsignedByte.intValue(inData[0x3800]) == 0xeb) && 
					(UnsignedByte.intValue(inData[0x3815]) == 0xf9) && // 1.2MB media descriptor... at the stash location
					(UnsignedByte.intValue(inData[0x39fe]) == 0x55) &&
					(UnsignedByte.intValue(inData[0x39ff]) == 0xaa))
			{
				retval = true;
			}
			// Look for a good BPB at 0xc00 - 360KB infection
			else if ((UnsignedByte.intValue(inData[0x0c00]) == 0xeb) && 
					(UnsignedByte.intValue(inData[0x0c15]) == 0xfd) && // 350KB media descriptor... at the stash location
					(UnsignedByte.intValue(inData[0x0dfe]) == 0x55) &&
					(UnsignedByte.intValue(inData[0x0dff]) == 0xaa))
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

	public static String mediaDescriptor(byte md)
	{
		String ret = null;
		switch (UnsignedByte.intValue(md))
		{
			case 0xe5:
				ret = "8-inch SS, 77 tracks/side, 26 sectors/track, 128 bytes/sector (250.25 KiB) (DR-DOS only)";
				break;
			case 0xed:
				ret = "5.25-inch DS, 80 tracks/side, 9 sectors/track, 720 KiB";
				break;
			case 0xf0:
				ret = "3.5-inch DS, 80 tracks/side, 18 or 36 sectors/track (1440 KiB/\"1.44 MB\" or 2880 KiB/\"2.88 MB\")\n"+
						"Designated for use with custom floppy and superfloppy formats where the geometry is defined in the BPB";
				break;
			case 0xf8:
				ret = "Fixed disk (i.e., typically a partition on a hard disk)\n"+
						"Designated to be used for any partitioned fixed or removable media, where the geometry is defined in the BPB\n"+
						"3.5-inch SS, 80 tracks/side, 9 sectors/track (360 KiB)\n"+
						"5.25-inch DS, 80 tracks/side, 9 sectors/track (720 KiB)\n";
				break;
			case 0xf9:
				ret="3.5-inch DS, 80 tracks/side, 9 sectors/track (720 KiB)\n"+
						"3.5-inch DS, 80 tracks/side, 18 sectors/track (1440 KiB)\n"+
						"5.25-inch DS, 80 tracks/side, 15 sectors/track (1200 KiB/\"1.2 MB\")";
				break;
			case 0xfa:
				ret="3.5-inch and 5.25-inch SS, 80 tracks/side, 8 sectors/track (320 KiB)\n"+
					    "Used also for RAM disks and ROM disks\n"+
					    "Hard disk (Tandy MS-DOS only)";
				break;
			case 0xfb:
				ret = "3.5-inch and 5.25-inch DS, 80 tracks/side, 8 sectors/track (640 KiB)";
				break;
			case 0xfc:
				ret = "5.25-inch SS, 40 tracks/side, 9 sectors/track (180 KiB)";
				break;
			case 0xfd:
				ret = "5.25-inch DS, 40 tracks/side, 9 sectors/track (360 KiB)\n"+
						"8-inch DS, 77 tracks/side, 26 sectors/track, 128 bytes/sector (500.5 KiB)\n"+
						"8-inch DS, SD/DD";
				break;
			case 0xfe:
				ret = "5.25-inch SS, 40 tracks/side, 8 sectors/track (160 KiB)\n"+
						"8-inch SS, 77 tracks/side, 26 sectors/track, 128 bytes/sector (250.25 KiB)\n"+
						"8-inch DS, 77 tracks/side, 8 sectors/track, 1024 bytes/sector (1232 KiB)\n"+
						"8-inch SS, SD/DD";
				break;
			case 0xff:
				ret = "5.25-inch Double sided, 40 tracks per side, 8 sectors per track (320 KiB)\n"+
						"Hard disk (Sanyo 55x DS-DOS 2.11 only)";
				break;
			default:
				ret = "(unknown/invalid media descriptor)";
		}
		return ret;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("DOSImage "+Version.VersionString+" - View or update the BIOS Parameter Block of a PC DOS disk image.");
		System.err.println();
		System.err.println("Usage: DOSImage display infile");
		System.err.println("       DOSImage update infile outfile [force{160|180|320|360|360a|1200}]");
	}
}