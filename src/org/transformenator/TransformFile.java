/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013 - 2014 by David Schmidt
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

public class TransformFile
{

	public static void help()
	{
		System.err.println();
		System.err.println("TransformFile " + Version.VersionString + " - perform transformation operations on files.");
		System.err.println();
		System.err.println("Usage: TransformFile transform infile outfile");
		System.err.println();
		System.err.println("See transform file specification documentation here:");
		System.err.println("   https://github.com/RetroFloppy/transformenator/wiki/Transform-Specification");
		System.err.println();
		GenericInterpreter.listInternalTransforms();
	}

	public static void main(String[] args)
	{
		if (args.length > 0)
		{
			GenericInterpreter transform = new GenericInterpreter(args[0]);
			if (args.length == 3)
			{
				if (transform != null)
				{
					transform.createOutput(args[1], args[2]);
				}
			}
			else if ((args.length == 1) && (transform.isOK))
			{
				String description = transform.describe();
				if (description != null)
				{
					System.err.println();
					System.err.println("Description: ");
					System.out.println(description);
				}
				else
					System.out.println("No description available for transform \""+args[0]+"\".");
			}
			else
			{
				// two args
				help();
			}

		}
		else
		{
			// No args
			help();
		}
	}
}
