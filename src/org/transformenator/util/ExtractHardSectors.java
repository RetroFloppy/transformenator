package org.transformenator.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.transformenator.Version;
import org.transformenator.internal.UnsignedByte;

/*
 * ExtractHardSectors
 * 
 * This helper app pulls the files off of the virtual file system of a hard=
 * sectored word processor disk of some unknown origin.  The expected data comes
 * from the FC5025 (fcdumpwang), with two copies of each track: one as extracted
 * and FM-decoded, and one that has been bit-shifted one bit and then FM-decoded.
 * This app will first find good copies of each sector (or substitute zeroes) and
 * then retrieve the files from the expected filesystem.
 * 
 * The disk geometry is 128 bytes per sector, 26 sectors per track, 77 tracks per side, one sided.
 *
 */
public class ExtractHardSectors
{
	public static void main(java.lang.String[] args)
	{
		String outputDirectory = "";
		FileOutputStream out = null;
		if ((args.length == 2) || (args.length == 3))
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
						for (int sector = 1; sector <= 26; sector++)
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
						if (args.length > 2)
						{
							// If they wanted an output directory, go ahead and make it.
							File baseDirFile = new File(args[2]);
							if (!baseDirFile.isAbsolute())
							{
								baseDirFile = new File("." + File.separator + args[2]);
							}
							// System.out.println("Making directory: ["+baseDirFile+"]");
							baseDirFile.mkdirs();
							outputDirectory = args[2];
						}
						else
						{
							// System.out.println("Making directory: ["+"." + File.separator + file.getName().substring(0, file.getName().length() - 4)+"]");
							outputDirectory = "." + File.separator + file.getName().substring(0, file.getName().length() - 4);
							File baseDirFile = new File(outputDirectory);
							baseDirFile.mkdirs();
						}
						/*
						 * Catalog starts on track 0, sector 1 (0x80).
						 * 
						 * Catalog entries are 0x2a bytes long; stuff we know/care
						 * about: 
						 * bytes 0x00-0x13: Filename (space padded)
						 * byte 0x14: starting track
						 * byte 0x16: ending track
						 */
						int fileStart = 0, fileEnd = 0;
						for (int sectors = 1; sectors < 26; sectors ++)
						{
							for (int files = 0; files < 3; files++)
							{
								String filename = "";
								int cursor = (sectors * 128) + (files * 42);
								if (inData[cursor + 0x14] != inData[cursor + 0x16])
								{
									fileStart = UnsignedByte.intValue(inData[cursor + 0x14]);
									fileEnd = UnsignedByte.intValue(inData[cursor + 0x16]);
									for (j = 0; j < 20; j++)
									{
										if (inData[cursor + j] != 0x00)
										{
											filename += (char) inData[cursor + j];
										}
										else
											break;
									}
									System.out.println("Found file: "+filename+" Start: 0x"+Integer.toHexString(fileStart)+" End: 0x"+Integer.toHexString(fileEnd));
									filename = filename.trim();
									try
									{
										String fullname = new String(outputDirectory + File.separator + filename);
										if (fileEnd * 128 * 26 < inData.length)
										{
											out = new FileOutputStream(fullname);
											System.err.println("Creating file: " + fullname);
											out.write(inData, fileStart * 128 * 26 + 128, (fileEnd - fileStart)  * 128 * 26);
											out.flush();
											out.close();
										}
										else
											System.err.println("Error: file " + fullname + " would exceed the capacity of the disk image.");
									}
									catch (IOException io)
									{
										io.printStackTrace();
									}

								}
								else
									break;								
							}
						}
					}
				} 
				catch (IOException e)
				{
					// TODO Auto-generated catch block
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

	public static int seekSector(byte[] shifts, int track, int sector)
	{
		int cursor, ret = -1;
		for (int segment = 0; segment < 16; segment ++)
		{
			// System.err.println("Scanning segment #"+segment);
			for (int i = 0; i < TRACK_SIZE - 256; i++) // Stop looking 256 bytes from the end
			{
				cursor = (i + (track * TRACK_SIZE)) + (segment * DISK_SIZE);
				if ((UnsignedByte.intValue(shifts[cursor]) == 0xff) &&
					(UnsignedByte.intValue(shifts[cursor+1]) == 0xff) &&
					(UnsignedByte.intValue(shifts[cursor+2]) == 0xfe) &&
					(UnsignedByte.intValue(shifts[cursor+3]) == 0x00) &&
					(UnsignedByte.intValue(shifts[cursor+8]) == 0x01) &&
					(UnsignedByte.intValue(shifts[cursor+9]) == 0xfc))
				{
					// System.err.println("Found a sector marker.  Track: "+UnsignedByte.toString(UnsignedByte.intValue(shifts[cursor+10])/2)+" ("+UnsignedByte.toString(UnsignedByte.intValue(shifts[cursor+10]))+") Sector: "+UnsignedByte.toString(shifts[cursor+12]/2)+" ("+UnsignedByte.toString(shifts[cursor+12])+") Cursor: "+Integer.toHexString(cursor));
					if (UnsignedByte.intValue(shifts[cursor+12])/2 == sector)
					{
						// System.err.println("Found sector "+sector);
						ret = cursor + 34; 
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
		System.err.println("ExtractHardSectors " + Version.VersionString + " - Extract files from an otherwise unknown word processor disk image from FC5025 hard-sector disk capture.");
		System.err.println();
		System.err.println("Usage: ExtractHardSectors infile outfile [out_directory]");
	}
	
	public static final int DISK_SIZE = 1261568;
	public static final int TRACK_SIZE = DISK_SIZE/77;
	public static final int SECTOR_SIZE = TRACK_SIZE/26;
}