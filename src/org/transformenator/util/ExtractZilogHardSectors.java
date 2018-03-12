package org.transformenator.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.transformenator.internal.UnsignedByte;
import org.transformenator.internal.Version;

/*
 * ExtractZilogHardSectors
 * 
 * This helper app pulls the files off of the virtual file system of a hard-
 * sectored disk that was from a Zilog trainer.  The expected data comes
 * from the FC5025 (fcdumpwang), with two copies of each track: one as extracted
 * and FM-decoded, and one that has been bit-shifted one bit and then FM-decoded.
 * This app will first find good copies of each sector (or substitute zeroes) and
 * then retrieve the files from the expected filesystem.
 * 
 * The disk geometry is 128 bytes per sector, 32 sectors per track, 77 tracks per side, one sided.
 *
 */
public class ExtractZilogHardSectors
{
	public static void main(java.lang.String[] args)
	{
		if ((args.length == 2) || (args.length == 3))
		{
			byte[] inData = null;
			System.err.println("Reading input file " + args[0]);
			File file = new File(args[0]);
			byte[] result = new byte[(int) file.length()];
			/*
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
					out = new FileOutputStream(args[1]);
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
			if ((inData != null) && (out != null))
			{
				int i, j, len = inData.length * 17, offset;
				byte shifts[] = new byte[(int)len];
				int byte0, byte1, bit;
				System.err.println("Read " + inData.length + " bytes.");
				for (i = 0; i < inData.length-1; i++)
				{
					// Seed the first (unshifted) version with the original data
					shifts[i] = inData[i];
				}
				for (j = 0; j < 16; j++)
				{
					for (i = 0; i < inData.length-1; i++)
					{
						// System.err.println("i = "+i);
						byte0 = UnsignedByte.intValue(shifts[i+(j*inData.length)]);
						byte1 = UnsignedByte.intValue(shifts[i+1+(j*inData.length)]);
						bit = byte1 >> 7 & 0x01;
						byte0 <<= 1;
						byte0 |= bit;
						byte0 &= 0xff;
						shifts[i+((j+1)*inData.length)] = (byte)byte0;
					}
				}
				try
				{
					for (int track = 0; track < 77; track++)
					{
						// System.err.println("====== Track "+track+" ======");
						for (int sector = 0; sector < 32; sector++)
						{
							offset = seekSector(shifts, track, sector);
							if (offset > -1)
							{
								for (int k = 0; k < 128; k++)
									out.write(shifts[k+offset]);
								out.flush();
							}
							else
							{
								for (int k = 0; k < 128; k++)
									out.write(0x00);
							}
						}
					}
					out.close();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			*/
			System.err.println("Reading input file " + args[1]);
			file = new File(args[1]);
			result = new byte[(int) file.length()];
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
				/*
				 * Catalog sits in a few sectors:
				 * 16280 (starts with the word "DIRECTORY"
				 * 16500 (+5 sectors (0x280 bytes))
				 * 16780
				 * 16a00
				 * 16c80
				 * [16f00 is EOF]
				 * (wraps around to:)
				 * 16300
				 * 16580
				 * 16800
				 */
				interpretCatalog(inData, 0x16280);
				interpretCatalog(inData, 0x16500);
				interpretCatalog(inData, 0x16780);
				interpretCatalog(inData, 0x16a00);
				interpretCatalog(inData, 0x16c80);
				interpretCatalog(inData, 0x16300);
				interpretCatalog(inData, 0x16580);
				interpretCatalog(inData, 0x16800);
				
			}

		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void interpretCatalog(byte[] inData, int offset)
	{
		int dirOffset = 0;
		int firstByte = UnsignedByte.intValue(inData[offset]);
		if (firstByte == 0x89) // Directory
		{
			System.out.println("Directory:");
			dirOffset = 0x0c;
		}
		int j;
		for (int i = dirOffset; i < 128; i++)
		{
			int nameLen = UnsignedByte.intValue(inData[i + offset]);
			if (nameLen < 255) 
			{
				// System.out.println("Dumping file name at offset 0x"+Integer.toHexString(i + offset)+" for 0x"+Integer.toHexString(nameLen)+" bytes.");
				for (j = 1; j <= nameLen; j++)
				{
					System.out.print((char)inData[i+j+offset]);
				}
				if (nameLen < 8)
					System.out.print("\t");
				System.out.println("\t Indices: 0x"+UnsignedByte.toString(inData[i+j+offset+0])+" 0x"+UnsignedByte.toString(inData[i+j+offset+1]));
				i += j + 1;
			}
			else
				break;
		}
	}

	public static int seekSector(byte[] shifts, int track, int sector)
	{
		int cursor, ret = -1;
		for (int segment = 0; segment < 16; segment ++)
		{
			// System.err.println("Scanning segment #"+segment);
			for (int i = 0; i < 1253376 - 256; i++) // Stop looking 256 bytes from the end
			{
				cursor = i + (segment * 1253376);
				if ((
					 UnsignedByte.intValue(shifts[cursor]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+1]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+2]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+3]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+4]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+5]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+6]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+7]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+8]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+9]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+10]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+11]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+12]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+13]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+14]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+15]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+16]) >= 0x80))
				{
					int foundSector = UnsignedByte.intValue(shifts[cursor+16]) - 128;
					int foundTrack = UnsignedByte.intValue(shifts[cursor+17]);
					// System.err.println("Found a sector marker.  Track: "+UnsignedByte.toString(UnsignedByte.intValue(shifts[cursor+17]))+" ("+foundTrack+") Sector: "+UnsignedByte.toString(shifts[cursor+16])+" ("+foundSector+") Cursor: "+Integer.toHexString(cursor));
					if ((foundSector == sector) && (foundTrack == track))
					{
						// System.err.println("Found track "+foundTrack+", sector "+foundSector+" in segment "+segment);
						ret = cursor + 18; 
						break;
					}
				}
				if (ret > -1)
					break;				
			}
			if (ret > -1)
				break;				
		}
		if (ret == -1)
			System.err.println("Missing track "+track+" sector "+sector);
		return ret;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractZilogHardSectors " + Version.VersionString + " - Extract files from a Zilog trainer (maybe) disk image from FC5025 hard-sector disk capture.");
		System.err.println();
		System.err.println("Usage: ExtractZilogHardSectors infile outfile [out_directory]");
	}
}