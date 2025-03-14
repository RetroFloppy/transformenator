/*
 * Transformenator - perform transformation operations on binary files Copyright
 * (C) 2025 by David Schmidt
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
 * DeSkew
 * 
 * The intent of this helper app is to de-skew data in an image file.  Given
 * details of the disk geometry, re-arrange sector-sized chunks by regular
 * offsets.
 *
 */

public class DeSkew
{
	int j;
	public static void main(java.lang.String[] args)
	{
		if (args.length == 5)
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
				int sectorLength, sectorsPerTrack, skewSectors = 0;
				sectorLength = Integer.parseInt(args[2]);
				sectorsPerTrack = Integer.parseInt(args[3]);
				skewSectors = Integer.parseInt(args[4]);
				if (skewSectors == 0)
					skewSectors = 1; // Skew sectors is now the number of sector lengths to advance to the next sector
				// Check math - are we square?
				if (inData.length % (sectorLength * sectorsPerTrack) == 0)
				{
					byte[] outData = new byte[inData.length];
					int trackLength = sectorLength * sectorsPerTrack; // For convience and readability
					// For each track
					for (int track = 0; track < inData.length / trackLength; track++)
					{
						// For each sector in a track, skew it
						for (int physicalSector = 0; physicalSector < sectorsPerTrack; physicalSector++)
						{
							int logicalSector = physicalSector * skewSectors % sectorsPerTrack;
							// System.out.print("Physical Sector: "+physicalSector+" logical sector: "+logicalSector+" ("+(physicalSector * sectorLength + (track * trackLength))+" to "+(logicalSector * sectorLength + (track * trackLength))+")\n");
							int destIndex = 0;
							System.arraycopy(inData, logicalSector * sectorLength + (track * trackLength), outData, physicalSector * sectorLength + (track * trackLength), sectorLength);
						}
					}
					BufferedOutputStream out;
					try
					{
						out = new BufferedOutputStream(new FileOutputStream(args[1]));
						out.write(outData);
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
					System.err.println("SectorLength * SectorsPerTrack does not divide evenly into image size.  Exiting.");
				}
			}
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static String describe(boolean verbose)
	{
		return "De-skew sector data in an image file." + (verbose ? "" : "");
	}

	public static void help()
	{
		System.err.println();
		System.err.println("DeSkew " + Version.VersionString + " - " + describe(true));
		System.err.println();
		System.err.println("Usage: DeSkew infile outfile SectorLengthInBytes SectorsPerTrack SkewSectors");
		System.err.println();
		System.err.println("Notes:");
		System.err.println("   SkewSectors is the number of (physical) sectors to skip to the next (logical) sector.");
		System.err.println("   SkewSectors of 0 or 1 is the same as no skew.");
	}
}