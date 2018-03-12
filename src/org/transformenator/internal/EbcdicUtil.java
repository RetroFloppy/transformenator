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



public class EbcdicUtil
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
		int myByte = UnsignedByte.intValue(inByte);
		switch (myByte)
		{
			case 0x06: return '\n';

			case 0x15: return ' ';

			case 0x2d: return '-';

			case 0x40: return ' ';

			case 0x4b: return '.';
			case 0x4c: return '<';
			case 0x4d: return '(';
			case 0x4e: return '+';
			case 0x4f: return '|';

			case 0x50: return '&';

			case 0x5a: return '!';
			case 0x5b: return '$';
			case 0x5c: return '*';
			case 0x5d: return ')';
			case 0x5e: return ';';
			case 0x5f: return '^';

			case 0x60: return '-';
			case 0x61: return '/';

			case 0x6b: return ',';
			case 0x6c: return '%';
			case 0x6d: return '_';
			case 0x6e: return '>';
			case 0x6f: return '?';
			case 0x70: return ';';

			case 0x7a: return ':';
			case 0x7b: return '#';
			case 0x7c: return '@';
			case 0x7d: return '\'';
			case 0x7e: return '=';
			case 0x7f: return '"';

			case 0x81: return 'a';
			case 0x82: return 'b';
			case 0x83: return 'c';
			case 0x84: return 'd';
			case 0x85: return 'e';
			case 0x86: return 'f';
			case 0x87: return 'g';
			case 0x88: return 'h';
			case 0x89: return 'i';

			case 0x91: return 'j';
			case 0x92: return 'k';
			case 0x93: return 'l';
			case 0x94: return 'm';
			case 0x95: return 'n';
			case 0x96: return 'o';
			case 0x97: return 'p';
			case 0x98: return 'q';
			case 0x99: return 'r';

			case 0xa1: return '~';
			case 0xa2: return 's';
			case 0xa3: return 't';
			case 0xa4: return 'u';
			case 0xa5: return 'v';
			case 0xa6: return 'w';
			case 0xa7: return 'x';
			case 0xa8: return 'y';
			case 0xa9: return 'z';

			case 0xc0: return '{';
			case 0xc1: return 'A';
			case 0xc2: return 'B';
			case 0xc3: return 'C';
			case 0xc4: return 'D';
			case 0xc5: return 'E';
			case 0xc6: return 'F';
			case 0xc7: return 'G';
			case 0xc8: return 'H';
			case 0xc9: return 'I';

			case 0xd0: return '}';
			case 0xd1: return 'J';
			case 0xd2: return 'K';
			case 0xd3: return 'L';
			case 0xd4: return 'M';
			case 0xd5: return 'N';
			case 0xd6: return 'O';
			case 0xd7: return 'P';
			case 0xd8: return 'Q';
			case 0xd9: return 'R';

			case 0xe0: return '\\';
			case 0xe2: return 'S';
			case 0xe3: return 'T';
			case 0xe4: return 'U';
			case 0xe5: return 'V';
			case 0xe6: return 'W';
			case 0xe7: return 'X';
			case 0xe8: return 'Y';
			case 0xe9: return 'Z';

			case 0xf0: return '0';
			case 0xf1: return '1';
			case 0xf2: return '2';
			case 0xf3: return '3';
			case 0xf4: return '4';
			case 0xf5: return '5';
			case 0xf6: return '6';
			case 0xf7: return '7';
			case 0xf8: return '8';
			case 0xf9: return '9';

		}
		return (char)myByte;
	}
}