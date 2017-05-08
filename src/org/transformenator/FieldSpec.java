/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2016 by David Schmidt
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

package org.transformenator;

public class FieldSpec
{
	// Struct-ifying field specifications
	public String fieldName = "";
	public int fieldOrigin = 0;
	public int fieldLength = 0;
	/*
	 * Interpretation of field:
	 * 0 = error
	 * 1 = ASCII text
	 * 2 = EBCDIC text
	 * 3 = Hex dump
	 */
	public int interp = 0;
	public boolean csvLiteral = false;
	
	public static String interpString(int interp)
	{
		switch (interp)
		{
			case 1: return "ASCII";
			case 2: return "EBCDIC";
			case 3: return "HEX";
		}
		return "<undefined>";
	}
}