/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2016 by David Schmidt
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

package org.transformenator;

import org.transformenator.internal.Version;

/*
 * ExtractCSV
 * 
 * The intent of this helper app is to take a file and interpret it as 
 * records and fields, extracting a .csv file in the process.
 *
 */
public class TransformCSV
{

	public static void main(java.lang.String[] args)
	{
		if (args.length == 3)
		{
			CSVInterpreter csvi = new CSVInterpreter(args[0]);
			csvi.createOutput(args[1], args[2]);
		}
		else
		{
			// wrong args
			help();
		}
	}

	public static void help()
	{
		System.err.println();
		System.err.println("ExtractCSV " + Version.VersionString + " - extract fixed-length records from a file as comma seperated values.");
		System.err.println();
		System.err.println("Usage: ExtractCSV csv-transform infile outfile.csv");
	}
}