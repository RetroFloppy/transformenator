/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2015 by David Schmidt
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

/*
 * ADTPro - Apple Disk Transfer ProDOS
 * Copyright (C) 2006 by David Schmidt
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

public class UnsignedByte
{

	private UnsignedByte()
	{
	}

	/**
	 * Returns the value of the specified byte in unsigned form as an integer.
	 * 
	 * @param b
	 *                the byte to be returned as an int.
	 * @return unsigned b represented as an integer.
	 */

	public static int intValue(byte b)
	{
		if (b >= 0)
			return b;
		else
			return 256 + b;
	}

	public static int intValue(byte lo, byte hi)
	{
		return UnsignedByte.intValue(hi) * 256 + UnsignedByte.intValue(lo);
	}

	public static byte loByte(int value)
	{
		return (byte) (value & 0x00ff);
	}

	public static byte hiByte(int value)
	{
		return (byte) ((value & 0xff00) >> 8);
	}

	public static byte loByte(long value)
	{
		return (byte) (loByte((int) value));
	}

	public static byte hiByte(long value)
	{
		return (byte) (hiByte((int) value));
	}

	/**
	 * Returns the value of the specified four bytes in unsigned form as a long.
	 * 
	 * @param b1    the least significant byte
	 * @param b2    the next most significant byte after b1
	 * @param b3    the next most significant byte after b2
	 * @param b4    the most significant byte
	 * @return long b1 through b4 represented as a long.
	 */

	public static long longValue(byte b1, byte b2, byte b3, byte b4)
	{
		return UnsignedByte.intValue(b4) * (long)4294967296.0 + UnsignedByte.intValue(b3) * 65536 + UnsignedByte.intValue(b2) * 256 + UnsignedByte.intValue(b1);
	}

	/**
	 * Returns the string representation of the specified byte in unsigned form as a two-digit hex value.
	 * 
	 * @param b
	 *                the byte to represent as a hex string
	 * @return b represented as a hex string.
	 */

	public static String toString(byte b)
	{
		int i = intValue(b);
		char c;
		if (i / 16 < 10)
			c = (char) (i / 16 + 48);
		else
			c = (char) (i / 16 + 55);

		char c1;
		if (i % 16 < 10)
			c1 = (char) (i % 16 + 48);
		else
			c1 = (char) (i % 16 + 55);

		return ("" + c + "" + c1);
	}

	/**
	 * Returns the string representation of the specified integer in unsigned form as a hex value.
	 * 
	 * @param b
	 *                the byte to represent as a hex string
	 * @return b represented as a hex string.
	 */

	public static String toString(int i)
	{
		char c;
		if (i / 16 < 10)
			c = (char) (i / 16 + 48);
		else
			c = (char) (i / 16 + 55);

		char c1;
		if (i % 16 < 10)
			c1 = (char) (i % 16 + 48);
		else
			c1 = (char) (i % 16 + 55);

		return ("" + c + "" + c1);
	}

}
