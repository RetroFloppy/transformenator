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

/*
 * Abstract class for detangler types.  Descendants of this class need to
 * take in a byte[] and send back a byte[] after having done whatever
 * detangling work is necessary on it.  They can optionally return a new
 * file name, potentially derived from the contents of the incoming data.
 * If there is no file name modification required, getNewName() can return
 * null.
 */
package org.transformenator.detanglers;

import org.transformenator.internal.FileInterpreter;

public abstract class ADetangler
{
	/*
	 * Given a byte stream, do whatever work is necessary to "flatten" or
	 * detangle the file.  This may include creating multiple files (in 
	 * the case of a disk image containing zero or more) or discovering
	 * a new file name that should be used instead of the original name.
	 */
	public abstract void detangle(FileInterpreter parent, byte inData[], String inFile, String outDirectory, String fileSuffix);
}