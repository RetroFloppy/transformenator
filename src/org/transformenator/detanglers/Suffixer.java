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

import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;

public class Suffixer extends ADetangler
{

  @Override
  public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
  {
    byte pdfheader1[] =
    // PDF header
    { 0x25, 0x50, 0x44, 0x46 };
    byte range1[] = Arrays.copyOfRange(inData, 0, pdfheader1.length);
    String suffix="";
    if ((Arrays.equals(range1, pdfheader1)))
    {
      // System.out.println("DEBUG: Found PDF header: "+inFile);
      if (!inFile.toUpperCase().endsWith("PDF"))
      {
        // System.out.println("Didn't end with PDF");
        suffix = ".PDF";
      }
    }      
    byte[] out = new byte[inData.length];
    System.arraycopy(inData, 0, out, 0, inData.length);
    parent.emitFile(out, outDirectory, "", inFile + suffix);
  }
}
