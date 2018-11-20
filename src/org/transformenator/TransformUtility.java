/*
 * Transformenator - perform transformation operations on binary files
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

package org.transformenator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.Version;

public class TransformUtility
{

	public static void main(String[] args)
	{
		if (args.length > 0)
		{
			if (args[0].trim().toLowerCase().equals("describe"))
				help(true);
			else
			{
				try
				{
					Class<?> utility = Class.forName("org.transformenator.util." + args[0]);
					Method main = utility.getMethod("main", String[].class);
					String[] newArgs = new String[args.length -1];
					for (int i = 1; i < args.length; i++)
						newArgs[i-1] = args[i];
					main.invoke(null, (Object) newArgs); // static method doesn't have an instance
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else help(false);
	}

	public static void help(boolean describe)
	{
		System.err.println();
		System.err.println("TransformUtility " + Version.VersionString + " - perform transformation utility functions.");
		System.err.println();
		System.err.println("Usage: TransformUtility function parameter [parameter...]");
		System.err.println("       TransformUtility describe");
		System.err.println();
		System.err.println("  For more details, see:");
		System.err.println("  https://github.com/RetroFloppy/transformenator/wiki/Utility-Functions");
		System.err.println();
		FileInterpreter.listUtilities(describe);
	}
}
