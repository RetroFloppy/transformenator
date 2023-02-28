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

public class Exxon extends ADetangler
{
  /*
   * Exxon 5xx disks have two giant (hard) sectors
   *
   * There's a catalog area, consisting of 28 entries starting at 0x1100:
   *   First filename entry starts at 0x13 bytes in; filenames run for 0x26 bytes
   *   Filename entries start 0x42 bytes later, one right after the other
   */

  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
  {
    ByteArrayOutputStream out = null;
    /* Extant examples had exactly 28 entries */
    int num = 0;
    String fileName = "";
    for (int cursor = 0x1113; cursor < 0x1113 + (0x42 * 28); cursor += 0x42)
    {
      fileName = "";
      num++;
      for (int name = 0; name < 0x26; name++)
        fileName += (char)inData[cursor + name];
      fileName = fileName.trim();
      System.out.println("Found filename "+num+": ["+fileName.trim()+"]");
    }
    num = 0;
    for (int cursor = 0x1900; (cursor + 0x100) < inData.length; cursor+=0x100)
    {
      if (inData[cursor] == 0x00 && inData[cursor+1] == 0x60)
      {
        num++;
        System.out.println("File "+num+" found at 0x"+Integer.toHexString(cursor));
        int fileCursor = 0;
        for (fileCursor = 0; cursor + fileCursor < inData.length; fileCursor++)
          if (inData[cursor + fileCursor] != 0x1a)
            continue;
          else
            break;
        out = new ByteArrayOutputStream();
        fileName = Integer.toString(num);
        out.write(inData, cursor, fileCursor);
        parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
      }
      //			parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
    }
  }
}
