/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2023 by David Schmidt
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

import java.io.ByteArrayOutputStream;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Exxon extends ADetangler
{
  /*
   * 
   *
   * 
   */

  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
  {
    ByteArrayOutputStream out = null;
    /* Extant examples had exactly 28 entries */
    for (int cursor = 0x1113; cursor < 0x1113 + (0x42 * 28); cursor+=0x42)
    {
      String filename = "";
      for (int name = 0; name < 0x26; name++)
        filename += (char)inData[cursor + name];
      System.out.println("Found filename: ["+filename.trim()+"]");
    }
    for (int sector = 0x1100; sector < inData.length / 256; sector++)
    {
      //			parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
    }
  }
}
