/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 by David Schmidt
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
import java.util.Arrays;

import org.transformenator.UnsignedByte;
import org.transformenator.Version;

/*
 * ExtractDisplaywriterFiles
 * 
 * The intent of this helper app is to pull the files off of the virtual file system of Displaywriter
 * word processor disks.  Recall that text will need to be interpreted as EBCDIC.
 *
 */
public class ExtractDisplaywriterFiles
{

	public static void main(java.lang.String[] args)
	{
		if ((args.length == 1) || (args.length == 2))
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
				int track0Offset = 3328;
				String docName = "";
				System.err.println("Read " + inData.length + " bytes.");
				if (inData.length > 1000000)
				{
					track0Offset = 3328 * 3;
				}
				for (int i = track0Offset; i < inData.length; i+=256)
				{
					if (inData[i] == 0x00)
					{
						int recType = UnsignedByte.intValue(inData[i+2]);
						int mid = UnsignedByte.intValue(inData[i+1]);
						if (((recType != 0x00) && (recType != 0xe0)) &&
							(!((mid == 0x05) && recType == 0xe1)))
						{
							int intOffset = i-track0Offset;
							int xsb = intOffset / 65536;
							if (intOffset > 65535)
								intOffset -= 65536;
							String recordSignature = "0x00"+UnsignedByte.toString(mid)+UnsignedByte.toString(recType);
							String recordEyecatcher = "    ";
							if (recType == 0x20)
								recordEyecatcher = "EHL1";
							else if (recType == 0x40)
								recordEyecatcher = "ABM ";
							else if (recType == 0x60)
							{
								recordEyecatcher = "DSL2";
								docName = EbcdicUtil.toAscii(inData,i+46,44);
							}
							else if (recType == 0x80)
								recordEyecatcher = "NAME";
							else if (recType == 0xC0)
								recordEyecatcher = "DOCS";
							else if (recType == 0xE0)
								recordEyecatcher = "DEXT";
							else if (recType == 0xE1)
							{
								recordEyecatcher = "STUF";
								String texty = EbcdicUtil.toAscii(inData, i+0x90, 256-0x90);
								System.err.println("Text: "+texty);
							}
							System.err.print("Found a 0x"+UnsignedByte.toString(recType)+" record ("+recordSignature+") "+recordEyecatcher+" at 0x"+UnsignedByte.toString(xsb)+UnsignedByte.toString(UnsignedByte.hiByte(intOffset))+UnsignedByte.toString(UnsignedByte.loByte(intOffset)));
							if (recType == 0x60)
							{
								System.err.println(" Filename: ["+docName.trim()+"]");
							}
							else
								System.err.println();
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

	public static void unwindDOSFile(byte[] inData, String fileName)
	{
		/*
		 * This is probably a file from a DOS-ish WANG word processor
		 */
		FileOutputStream out;
		try
		{
			out = new FileOutputStream(fileName);
			System.err.println("Creating file: " + fileName);

			/*
			 * These word processors have a list of pointers to the starts of pages, in order.
			 * Each pointer points to a 1k chunk of data that ends in another pointer (or xffff for the end). 
			 */
			for (int i = 258; i < 512; i+= 2)
			{
				int pageNumber = UnsignedByte.intValue(inData[i+1]) * 256 + UnsignedByte.intValue(inData[i]);
				if ((pageNumber > 0) && (pageNumber < 65535))
				{
					//System.err.println("pageNumber: "+pageNumber);
					dumpDOSPage(out, inData, pageNumber);
				}
			}
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}		
	}

	public static void dumpDOSPage(FileOutputStream out, byte[] inData, int pageNumber) throws IOException
	{
		int next = 0;
		int endMarker = 1022;
		int offset = pageNumber * 1024;
		// Find the end of data in this block
		for (int i = 0; i < 1022; i++)
		{
			if (inData[i+offset] == 0x1f)
				endMarker = i;
		}
		//System.out.println("Start of page; block: "+UnsignedByte.toString(UnsignedByte.hiByte(pageNumber))+""+UnsignedByte.toString(UnsignedByte.loByte(pageNumber))+" offset: "+UnsignedByte.toString(UnsignedByte.loByte(offset))+""+UnsignedByte.toString(UnsignedByte.hiByte(offset)));
		out.write(inData, offset, endMarker);
		next = UnsignedByte.intValue(inData[offset+1023]) * 256 + UnsignedByte.intValue(inData[offset+1022]);
		if (next < 65535)
			dumpDOSPage(out, inData, next);
		//else
			//System.out.println("End.");
	}

	public static void decodeFile(byte[] inData, String shortName, int fileHeaderSector, int preambleOffset)
	{
		/*
		 * Incoming, we have the sector address of the file header; use that to
		 * find the rest of the file.
		 */
		int fileHeaderOffset = fileHeaderSector * 256 + preambleOffset;
		// System.err.println("fileHeaderSector: "+fileHeaderSector+" fileHeaderOffset: "+fileHeaderOffset);
		int firstFileChainSector = UnsignedByte.intValue((byte) inData[fileHeaderOffset + 193 /* 0xc1 */], (byte) inData[fileHeaderOffset + 192 /* 0xc0 */]);
		byte fnb[] = new byte[64];
		fnb = Arrays.copyOfRange(inData, fileHeaderOffset + 64, fileHeaderOffset + 128);
		String longName = new String(fnb).trim().replace("\\", "-").replace("/", "-").replace("?", "-"), fullName;
		// System.err.println("Long name: "+longName);
		FileOutputStream out;
		try
		{
			if (longName.equals(""))
				fullName = shortName;
			else
				fullName = shortName + "-" + longName;
			out = new FileOutputStream(fullName);
			System.err.println("Creating file: " + fullName);
			dumpFileChain(out, inData, firstFileChainSector, preambleOffset);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void dumpFileChain(FileOutputStream out, byte[] inData, int fileChainSector, int preambleOffset) throws IOException
	{
		// System.err.println("dumpFileChain: fileChainSector: " + fileChainSector);
		int textSector = 0, i = 0, textRealOffset;
		if ((fileChainSector > 0) && ((fileChainSector+1)*256 < inData.length))
		{
			do
			{
				textSector = UnsignedByte.intValue((byte) inData[realAddress(fileChainSector, preambleOffset) + 0x41 + i], (byte) inData[realAddress(fileChainSector, preambleOffset) + 0x40 + i]);
				if ((textSector > 0) && ((textSector+1)*256 < inData.length))
				{
					// System.err.println("dumpFileChain: textSector: " + textSector);
					textRealOffset = realAddress(textSector, preambleOffset);
					// System.err.println("dumpFileChain: textRealOffset: "+textRealOffset);
					byte range[] = Arrays.copyOfRange(inData, textRealOffset, textRealOffset + 256);
					out.write(range);
					i += 2;
				}
			} while ((textSector > 0) && ((textSector+1)*256 < inData.length));
			int nextFileChainSector = UnsignedByte.intValue((byte) inData[realAddress(fileChainSector, preambleOffset) + 9], (byte) inData[realAddress(fileChainSector, preambleOffset) + 8]);
			// System.err.println("dumpFileChain: nextFileChainSector: "+nextFileChainSector);
			if (nextFileChainSector != 0)
				dumpFileChain(out, inData, nextFileChainSector, preambleOffset);
		}
	}

	public static void decodeWPFile(byte[] inData, String fullName, int preambleOffset, int track, int sector, int fileID, int skew)
	{
		FileOutputStream out;
		try
		{
			out = new FileOutputStream(fullName);
			System.err.println("Creating file: " + fullName);
			dumpWPFileChain(out, inData, track, sector, fileID, preambleOffset, skew);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void dumpWPFileChain(FileOutputStream out, byte[] inData, int track, int sector, int fileID, int preambleOffset, int skew) throws IOException
	{
		int nextTrack, nextSector, dataOffset;

		dataOffset = realWPAddress(track, sector, preambleOffset, skew);
		nextTrack = UnsignedByte.intValue(inData[dataOffset]);
		nextSector = UnsignedByte.intValue(inData[dataOffset+1]);
		byte range[] = Arrays.copyOfRange(inData, dataOffset + 7, dataOffset + 256);
		out.write(range);
		nextTrack = UnsignedByte.intValue(inData[dataOffset]);
		nextSector = UnsignedByte.intValue(inData[dataOffset+1]);
		if ((nextTrack > 0) && (nextTrack*4096 <= inData.length))
		{
			if (nextTrack != 0)
				dumpWPFileChain(out, inData, nextTrack, nextSector, fileID, preambleOffset, skew);
		}
	}

	public static int mapSector(int sectorIn, int skew)
	{
		int skewedSectorMap[] = {0,4,8,0x0c,1,5,9,0x0d,2,6,0x0a,0x0e,3,7,0x0b,0x0f};
		if (skew != 0)
			return skewedSectorMap[sectorIn];
		else
			return sectorIn;
	}
	
	public static int realAddress(int sector, int offset)
	{
		return sector * 256 + offset;
	}

	public static int realWPAddress(int track, int sector, int offset, int skew)
	{
		int newSector = mapSector(sector, skew);
		return track * 4096 + newSector * 256 + offset;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractDisplaywriterFiles "+Version.VersionString+" - Extract files from Displaywriter word processor disk images.");
		System.err.println();
		System.err.println("Usage: ExtractDisplaywriterFiles infile [out_directory]");
	}
}