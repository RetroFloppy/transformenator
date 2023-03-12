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

public class JPGScrape extends ADetangler
{

  @Override
  public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
  {
    System.out.println("Here!");
    // FF D8 FF E0 00 10 4A 46 49 46
    byte jfifheader1[] =
    { -0x01, -0x28, -0x01, -0x20, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46 }; // JFIF header
    // FF D9 FF E0 00 10 4A 46 49 46
    byte jfifheader2[] =
    { -0x01, -0x27, -0x01, -0x20, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46 }; // JFIF header
    // FF D8 FF DB 00 84 00 06 04 05
    byte jfifheader3[] =
    { -0x01, -0x28, -0x01, -0x25, 0x00, -0x7c, 0x00, 0x06, 0x04, 0x05 }; // JFIF header
    // FF D8 FF E1 0F EF 45 78 69 66
    byte exifheader[] =
    { -0x01, -0x28, -0x01, -0x1f, 0x0f, -0x11, 0x45, 0x78, 0x69, 0x66 }; // Exif header
    int begin = 0, end = 0;
    int filenum = 1;
    for (int i = 0; i < inData.length - jfifheader1.length; i++)
    {
      byte range1[] = Arrays.copyOfRange(inData, i, i + jfifheader1.length);
      byte range2[] = Arrays.copyOfRange(inData, i, i + jfifheader2.length);
      byte range3[] = Arrays.copyOfRange(inData, i, i + jfifheader3.length);
      byte range4[] = Arrays.copyOfRange(inData, i, i + exifheader.length);
      if ((Arrays.equals(range1, jfifheader1)) || (Arrays.equals(range2, jfifheader2))
          || (Arrays.equals(range3, jfifheader3)) || (Arrays.equals(range4, exifheader))) // Is the JPG eyecatcher in the disk image?
      {
        System.out.println("DEBUG: Found JFIF header at offset 0x" + Integer.toHexString(i));
        if (begin == 0)
          begin = i;
        else
          end = i;
        if (begin > 0 && end > 0)
        {
          byte[] out = new byte[end - begin];
          System.arraycopy(inData, begin, out, 0, end - begin);
          parent.emitFile(out, outDirectory,
              inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())),
              "" + filenum++);
          System.out.println(
              "DEBUG: Write JFIF file from 0x" + Integer.toHexString(begin) + " to 0x" + Integer.toHexString(end));
        }
        end = 0;
        begin = i;
      }
    }
    if (begin > 0)
    {
      byte[] out = new byte[inData.length - begin];
      System.arraycopy(inData, begin, out, 0, inData.length - begin);
      System.out.println("DEBUG: Write JFIF file from 0x" + Integer.toHexString(begin) + " to 0x"
          + Integer.toHexString(inData.length));
      parent.emitFile(out, outDirectory,
          inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())),
          "" + filenum++);
    }
  }
}
