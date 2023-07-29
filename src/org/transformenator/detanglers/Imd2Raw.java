/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2022 - 2023 by David Schmidt
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
import org.transformenator.internal.ImageDisk;

/*
 * Convert Dave Dunfield's IMD format to raw binary data; the majority of the work
 * happens in the org.transformenator.internal.ImageDisk class.  Output matches the
 * original IMDU /b command output. 
 *
 */
public class Imd2Raw extends ADetangler
{
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean debugMode)
  {
    String fileName = inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length()));
    if (fileSuffix.equals(""))
      fileName += ".img";
    else
      fileName += fileSuffix;
    if (inData != null)
    {
      // Ready to go.  Time to face the music.
      byte[] outData = null;
      outData = ImageDisk.imd2raw(inData);
      if (outData != null)
      {
        parent.emitFile(outData, outDirectory, "", fileName);
      }
      else
      {
        System.err.println("File does not appear to be in ImageDisk format.");
      }
    }
  }
}