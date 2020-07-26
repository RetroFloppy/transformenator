/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2015 - 2018 by David Schmidt
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

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class SmithCorona extends ADetangler
{
	public static ByteArrayOutputStream out = null;
	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
		byte[] systemChain = null;
		systemChain = followFile(inData, (byte) 0x00);
		if (systemChain != null)
		{
			// Follow the sectors of names
			for (int j = 768; j < systemChain.length; j += 256)
			{
				for (int i = 0; i < 12; i++)
				{
					String filename = normalizeName(toAscii(systemChain, j + (i * 21), 20));
					byte filenumber = systemChain[j + (i * 21) + 20];
					if (filenumber != 0)
					{
						// System.err.print("Filename: " + filename + "  File number: " + UnsignedByte.toString(filenumber));
						// System.err.println();
						byte[] fileChain = followFile(inData, filenumber);
						if (fileChain != null)
						{
							out = new ByteArrayOutputStream();
							for (int k = 43; k < fileChain.length; k++)
								out.write(fileChain[k]);
						}
						parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), filename+fileSuffix);
					}
				}
			}
		}
	}

	static String normalizeName(String name)
	{
		/*
		 * Some characters that might come out in a filename are illegal these days - so modify those
		 */
		char[] newName = name.toCharArray();
		if (name != null)
		{
			char c;
			for (int i = 0; i < newName.length; i++)
			{
				c = newName[i];
				switch (c)
				{
				case '/':
					newName[i] = '-';
					break;
				case ':':
					newName[i] = '-';
					break;
				case '&':
					newName[i] = '_';
					break;
				case '?':
					newName[i] = '_';
					break;
				}
			}
		}
		return new String(newName).trim();
	}

	static byte[] followFile(byte[] inData, byte fileNumber)
	{
		/*
		 * Make one contiguous array of data out of a file number
		 */
		byte[] fileContents = null;
		// System.err.println("Following file 0x" + UnsignedByte.toString(fileNumber));
		int count = 0;
		for (int i = 0; i < 640; i++)
		{
			if (inData[i] == fileNumber)
			{
				count++;
			}
		}
		if (count > 0)
		{
			fileContents = new byte[count * 256];
			count = 0; // Start over counting
			for (int i = 0; i < 640; i++)
			{
				if (inData[i] == fileNumber)
				{
					for (int j = 0; j < 256; j++)
					{
						fileContents[(count * 256) + j] = inData[(i * 256) + j];
					}
					count++;
				}
			}
		}
		return fileContents;
	}

	public static String toAscii(byte inData[])
	{
		if (inData != null)
		{
			return toAscii(inData, 0, inData.length);
		}
		else
			return null;
	}

	public static String toAscii(byte inData[], int offset, int length)
	{
		/*
		 * Convert a section of a byte array from Smith-Corona data to ASCII representation
		 */
		String resultString = "";
		for (int i = offset; i < offset + length; i++)
		{
			resultString += asciiChar(inData[i]);
		}
		return resultString;
	};

	public static char asciiChar(byte inByte)
	{
		/*
		 * Convert a Smith-Corona spin value to ASCII character
		 */
		int myByte = UnsignedByte.intValue(inByte);
		switch (myByte)
		{
		case 0x22:
			return 'A';
		case 0x19:
			return 'B';
		case 0x2b:
			return 'C';
		case 0x48:
			return 'D';
		case 0x40:
			return 'E';
		case 0x08:
			return 'F';
		case 0x0c:
			return 'G';
		case 0x31:
			return 'H';
		case 0x0e:
			return 'I';
		case 0x09:
			return 'J';
		case 0x2d:
			return 'K';
		case 0x11:
			return 'L';
		case 0x4a:
			return 'M';
		case 0x24:
			return 'N';
		case 0x0f:
			return 'O';
		case 0x10:
			return 'P';
		case 0x34:
			return 'Q';
		case 0x33:
			return 'R';
		case 0x38:
			return 'S';
		case 0x16:
			return 'T';
		case 0x0b:
			return 'U';
		case 0x05:
			return 'V';
		case 0x14:
			return 'W';
		case 0x2f:
			return 'X';
		case 0x12:
			return 'Y';
		case 0x1C:
			return 'Z';

		case 0x06:
			return 'a';
		case 0x27:
			return 'b';
		case 0x43:
			return 'c';
		case 0x25:
			return 'd';
		case 0x02:
			return 'e';
		case 0x0d:
			return 'f';
		case 0x1e:
			return 'g';
		case 0x46:
			return 'h';
		case 0x03:
			return 'i';
		case 0x1b:
			return 'j';
		case 0x42:
			return 'k';
		case 0x39:
			return 'l';
		case 0x4e:
			return 'm';
		case 0x36:
			return 'n';
		case 0x0a:
			return 'o';
		case 0x26:
			return 'p';
		case 0x3e:
			return 'q';
		case 0x44:
			return 'r';
		case 0x3b:
			return 's';
		case 0x04:
			return 't';
		case 0x13:
			return 'u';
		case 0x07:
			return 'v';
		case 0x18:
			return 'w';
		case 0x4c:
			return 'x';
		case 0x28:
			return 'y';
		case 0x3c:
			return 'z';

		case 0x47:
			return '0';
		case 0x4f:
			return '1';
		case 0x3d:
			return '2';
		case 0x3a:
			return '3';
		case 0x17:
			return '4';
		case 0x1a:
			return '5';
		case 0x15:
			return '6';
		case 0x21:
			return '7';
		case 0x3f:
			return '8';
		case 0x23:
			return '9';
		case 0x1f:
			return '-';
		case 0x35:
			return '(';
		case 0x37:
			return ')';
		case 0x41:
			return '.';
		case 0x45:
			return 'q';
		case 0x4b:
			return '\'';
		case 0x2a:
			return ',';
		case 0x2c:
			return '*';
		case 0x2e:
			return '+';
		case 0x30:
			return '@';
		case 0x50:
			return '_';
		case 0x51:
			return ';';
		case 0x53:
			return '_';
		case 0x54:
			return '#';
		case 0x1d:
			return ':';
		case 0x32:
			return '?';
		case 0x52:
			return '$';
		case 0x29:
			return '&';
		case 0x55:
			return '/';
		case 0x68:
			return ' ';

		case 0x00:
			return ' ';
		}
		return '~'; //(char)myByte;
	}
}