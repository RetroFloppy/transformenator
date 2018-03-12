/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2014 - 2015 by David Schmidt
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

package org.transformenator.internal;

public class OfficeSys6Util
{
	public static String toAscii(byte inData[], int offset, int length)
	{
		String resultString = "";
		for (int i = offset; i < offset+length; i++)
		{
			resultString += asciiChar(inData[i]);
		}
		return resultString;
	};

	public static char asciiChar(byte inByte)
	{
		// Really basic transform for readabilty of filenames only
		int myByte = UnsignedByte.intValue(inByte);
		switch (myByte)
		{
			case 0x02: return '-';
			case 0x0c: return ',';
			case 0x15: return '\'';
			case 0x38: return ' ';

			case 0x30: return '9';
			case 0x31: return '0';
			case 0x34: return '6';
			case 0x35: return '5';
			case 0x36: return '2';
			case 0x39: return '4';
			case 0x3c: return '8';
			case 0x3d: return '7';
			case 0x3e: return '3';
			case 0x3f: return '1';

			case 0x41: return 'Y';
			case 0x44: return 'Q';
			case 0x45: return 'P';
			case 0x47: return 'J';
			case 0x4e: return 'F';
			case 0x4f: return 'G';
			case 0x50: return 'W';
			case 0x51: return 'S';
			case 0x54: return 'I';
			case 0x59: return 'O';
			case 0x5c: return 'A';
			case 0x5d: return 'R';
			case 0x5e: return 'V';
			case 0x5f: return 'M';
			case 0x60: return 'B';
			case 0x61: return 'H';
			case 0x64: return 'K';
			case 0x65: return 'E';
			case 0x66: return 'N';
			case 0x67: return 'T';
			case 0x69: return 'L';
			case 0x6c: return 'C';
			case 0x6d: return 'D';
			case 0x6e: return 'U';
			case 0x6f: return 'X';
			case 0x77: return 'Z';

			case 0x01: return 'y';
			case 0x04: return 'q';
			case 0x05: return 'p';
			case 0x07: return 'j';
			case 0x0e: return 'f';
			case 0x0f: return 'g';
			case 0x10: return 'w';
			case 0x11: return 's';
			case 0x14: return 'i';
			case 0x19: return 'o';
			case 0x1c: return 'a';
			case 0x1d: return 'r';
			case 0x1e: return 'v';
			case 0x1f: return 'm';
			case 0x20: return 'b';
			case 0x21: return 'h';
			case 0x24: return 'k';
			case 0x25: return 'e';
			case 0x26: return 'n';
			case 0x27: return 't';
			case 0x29: return 'l';
			case 0x2c: return 'c';
			case 0x2d: return 'd';
			case 0x2e: return 'u';
			case 0x2f: return 'x';
			case 0x37: return 'z';
		}
		return '_';
	}
}