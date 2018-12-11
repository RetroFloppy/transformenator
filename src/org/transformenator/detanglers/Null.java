/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2018 by David Schmidt
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

import org.transformenator.internal.FileInterpreter;

/*
 * Null detangler - i.e. do nothing to the input; just send it back out as-is.
 */
public class Null extends ADetangler
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String outDirectory, String inFile, String fileSuffix)
	{
		interpreter.emitFile(inData, outDirectory, "", inFile + fileSuffix);
	}
}