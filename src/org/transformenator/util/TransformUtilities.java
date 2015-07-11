/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2015 by David Schmidt
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

import org.transformenator.Transformation;
import org.transformenator.Version;

public class TransformUtilities
{

	public static void main(String[] args)
	{
		System.err.println();
		System.err.println("TransformUtilities " + Version.VersionString + " - perform transformation utility functions.");
		System.err.println();
		System.err.println("Usage: TransformUtilities function parameter [parameter...]");
		System.err.println();
		System.err.println("  See http://transformenator.sourceforge.net/#Utility for more details.");
		System.err.println();
		Transformation.listUtilities();
	}
}
