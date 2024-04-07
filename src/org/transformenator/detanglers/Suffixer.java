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
    byte pdfheader[] = // PDF header
    { 0x25, 0x50, 0x44, 0x46 };
    // FF D8 FF E0 00 10 4A 46 49 46
    byte jfifheader1[] = // JPG header
    { -0x01, -0x28, -0x01, -0x20, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46 }; // JFIF header
    // FF D9 FF E0 00 10 4A 46 49 46
    byte jfifheader2[] = // JPG header
    { -0x01, -0x27, -0x01, -0x20, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46 }; // JFIF header
    // FF D8 FF DB 00 84 00 06 04 05
    byte jfifheader3[] = // JPG header
    { -0x01, -0x28, -0x01, -0x25, 0x00, -0x7c, 0x00, 0x06, 0x04, 0x05 }; // JFIF header
    // FF D8 FF E1 0F EF 45 78 69 66
    byte exifheader[] = // JPG header
    { -0x01, -0x28, -0x01, -0x1f, 0x0f, -0x11, 0x45, 0x78, 0x69, 0x66 }; // Exif header
    byte mscomheader[] = // MS COM header
    // D0 CF 11 E0 A1 B1 1A E1
    { -0x30, -0x31, 0x11, -0x20, -0x5f, -0x4f, 0x1a, -0x1f }; // MS COM header
    byte comtable_doc1[] = // MS COM DOC header
    // EC A5 C1 00
    { -0x14, -0x5b, -0x3f, 0x00 };
    byte comtable_doc2[] = // MS COM DOC header
    // 57 00 6F 00 72 00 64 00 44 00 6F 00 63 00 75 00 6D 00 65 00 6E 00 74 00
    { 0x57, 0x00, 0x6F, 0x00, 0x72, 0x00, 0x64, 0x00, 0x44, 0x00, 0x6F, 0x00, 0x63, 0x00, 0x75, 0x00, 0x6D, 0x00, 0x65, 0x00, 0x6E, 0x00, 0x74, 0x00 };
    byte comtable_doc3[] = // MS COM DOC header
    // DC A5 68 00 63
    { -0x24, -0x5b, 0x68, 0x00, 0x63 };
    byte comtable_xls1[] = // MS COM XLS header
    // FD FF FF FF nn 00
    { -0x03, -0x01, -0x01, -0x01, 0x00, 0x00 };
    byte comtable_xls2[] = // MS COM XLS header
    // 09 08 nn 00 00
    { 0x09, 0x08, 0x00, 0x00, 0x00 };
    byte comtable_ppt1[] = // MS COM PPT header
    // A0 46 1D F0
    { -0x60, 0x46, 0x1d, -0x10 };
    byte comtable_ppt2[] = // MS COM PPT header
    // 00 6E 1E F0
    { 0x00, 0x6e, 0x1e, -0x10 }; 
    byte comtable_ppt3[] = // MS COM PPT header
    // FD FF FF FF nn nn 00 00
    { -0x03, -0x01, -0x01, -0x01, 0x00, 0x00, 0x00, 0x00 };
    byte comtable_ppt4[] = // MS COM PPT header
    // 60 21 1B F0
    { 0x60, 0x21, 0x1b, -0x10 }; 
    byte comtable_ppt5[] = // MS COM PPT header
    // 40 3D 1A F0 38 0B 00 00
    { 0x40, 0x3d, 0x1a, -0x10, 0x38, 0x0b, 0x00, 0x00 };
    byte comtable_ppt6[] = // MS COM PPT header
    // 03 00 00 00 FF FF
    { 0x03, 0x00, 0x00, 0x00, -0x01, -0x01 }; 
    byte range1[] = Arrays.copyOfRange(inData, 0, pdfheader.length);
    byte range2[] = Arrays.copyOfRange(inData, 0, jfifheader1.length);
    byte range3[] = Arrays.copyOfRange(inData, 0, jfifheader2.length);
    byte range4[] = Arrays.copyOfRange(inData, 0, jfifheader3.length);
    byte range5[] = Arrays.copyOfRange(inData, 0, exifheader.length);
    byte range6[] = Arrays.copyOfRange(inData, 0, mscomheader.length);
    String suffix="";
    if ((Arrays.equals(range1, pdfheader)))
    {
      if (!inFile.toUpperCase().endsWith("PDF"))
      {
        suffix = ".PDF";
      }
    }
    else if ((Arrays.equals(range2, jfifheader1)) || (Arrays.equals(range3, jfifheader2)) ||
        (Arrays.equals(range4, jfifheader3)) || (Arrays.equals(range5, exifheader)))
    {
      if ((!inFile.toUpperCase().endsWith("JPG")) && (!inFile.toUpperCase().endsWith("JPEG")))
      {
        suffix = ".JPG";
      }
    }
    else if (Arrays.equals(range6, mscomheader))
    {
      // System.out.println("Got an MS COM header");
      byte comrange1[] = Arrays.copyOfRange(inData, 512, 512+comtable_doc1.length);
      byte comrange2[] = Arrays.copyOfRange(inData, 512, 512+comtable_xls1.length);
      comrange2[4] = 0x00; // Don't care byte
      byte comrange3[] = Arrays.copyOfRange(inData, 512, 512+comtable_xls2.length);
      comrange3[2] = 0x00; // Don't care byte
      byte comrange4[] = Arrays.copyOfRange(inData, 512, 512+comtable_ppt1.length);
      byte comrange5[] = Arrays.copyOfRange(inData, 512, 512+comtable_ppt2.length);
      byte comrange6[] = Arrays.copyOfRange(inData, 512, 512+comtable_ppt3.length);
      comrange6[4] = 0x00;
      comrange6[5] = 0x00; // Don't care bytes
      byte comrange7[] = Arrays.copyOfRange(inData, 512, 512+comtable_ppt4.length);
      byte comrange8[] = Arrays.copyOfRange(inData, 512, 512+comtable_doc2.length);
      byte comrange9[] = Arrays.copyOfRange(inData, 512, 512+comtable_ppt5.length);
      byte comrange10[] = Arrays.copyOfRange(inData, 512, 512+comtable_doc3.length);
      byte comrange11[] = Arrays.copyOfRange(inData, 512, 512+comtable_ppt6.length);
      if ((Arrays.equals(comrange1, comtable_doc1)) ||
          (Arrays.equals(comrange8, comtable_doc2)) ||
          (Arrays.equals(comrange10, comtable_doc3)))
      {
        //System.out.println("   ...and it's a DOC");
        suffix = ".DOC";
      }
      else if ((Arrays.equals(comrange4, comtable_ppt1)) ||
          (Arrays.equals(comrange5, comtable_ppt2)) ||
          (Arrays.equals(comrange6, comtable_ppt3)) ||
          (Arrays.equals(comrange7, comtable_ppt4)) ||
          (Arrays.equals(comrange9, comtable_ppt5)) ||
          (Arrays.equals(comrange11, comtable_ppt6)))
      {
        //System.out.println("   ...and it's a PPT");
        suffix = ".PPT";
      }
      // Check for XLS after checking for PPT because they overlap a bit
      else if ((Arrays.equals(comrange2, comtable_xls1)) ||
          (Arrays.equals(comrange3, comtable_xls2)))
      {
        //System.out.println("   ...and it's an XLS");
        suffix = ".XLS";
      }
      // else System.out.println ("Didn't find an MS match");
    }
    
    byte[] out = new byte[inData.length];
    System.arraycopy(inData, 0, out, 0, inData.length);
    parent.emitFile(out, outDirectory, "", inFile + suffix);
  }
}
