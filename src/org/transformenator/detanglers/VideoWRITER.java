/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2020 by David Schmidt
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
import java.io.IOException;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class VideoWRITER extends ADetangler
{
  public static ByteArrayOutputStream out = null;

  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
  {
    // Philips/Magnavox VideoWRITER disks are 368,640 bytes in length.
    // System.err.println("DEBUG: inData.length: " + inData.length);
    if (inData.length == 368640)
    {
      // Directory starts at 0x100, each entry is 0x40 long
      for (int i = 0x100; i < 0x1000; i += 0x40)
      {
        if (inData[i + 6] > 0)
        {
          byte[] fileNameBytes = new byte[20];
          for (int j = 0x06; j < 0x1a; j++)
          {
            fileNameBytes[j - 6] = inData[i + j];
          }
          String fileName = normalizeName(new String(fileNameBytes).trim());
          // System.err.println("DEBUG: Found file at offset: 0x" + Integer.toHexString(i) + " " + fileName);
          // System.out.println("DEBUG: Found file: " + fileName);
          out = new ByteArrayOutputStream();
          dumpFile(inData, i + 0x1a, out);
          parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName+fileSuffix);
          try
          {
            out.close();
          }
          catch (IOException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
    else
      parent.emitFile(inData, outDirectory, "", inFile + fileSuffix);
  }

  void dumpFile(byte[] inData, int index, ByteArrayOutputStream out)
  {
    // System.err.println("  DEBUG: dumpFile entry, index=0x"+Integer.toHexString(index));
    for (int i = 0; i < 0x26; i += 0x02)
    {
      int startIndex = UnsignedByte.intValue(inData[index + i]) * 0x1200;
      startIndex += UnsignedByte.intValue(inData[index + i + 1]) * 0x100;
      if (startIndex > 0)
      {
        // System.err.println("  DEBUG: dumpFile calling dumpSegment for 0x"+Integer.toHexString(UnsignedByte.intValue(inData[index + i]))+"/0x"+Integer.toHexString(UnsignedByte.intValue(inData[index + i + 1])));
        dumpSegment(inData, startIndex, out);
      }
    }
  }

  int dumpSegment(byte[] inData, int startIndex, ByteArrayOutputStream out)
  {
      // System.err.println("    DEBUG: dumpSegment entry, startIndex = 0x"+Integer.toHexString(startIndex));
      for (int i = 0; i < 0x100; i += 0x40)
      {
        dumpLine(inData, startIndex + i, out);
      }
    return 0;
  }

  int dumpLine(byte[] inData, int index, ByteArrayOutputStream out)
  {
    int lineCapacity = UnsignedByte.intValue(inData[index], inData[index + 1]);
    // System.err.println("      DEBUG: dumpLine entry; offset = 0x" + Integer.toHexString(index)+" length: 0x"+Integer.toHexString(lineCapacity));
    int emittedBytes = 0;
    int track, sector;
    if (lineCapacity == 0)
        return 0;
    int bytesToEmit = 256;
    for (int i = 0x02; i < 0x40; i += 2)
    {
      track = UnsignedByte.intValue(inData[index + i]);
      sector = UnsignedByte.intValue(inData[index + i + 1]);
      if (track + sector == 0)
        continue;
      if (track > 0x4f)
        continue;
      if (sector > 0x12)
        continue;
      int chunkOffset = (track * 0x1200) + (sector * 0x100);
      /*
      System.err.println("        DEBUG: Chunk offset: 0x" + Integer.toHexString(chunkOffset) + " Track/Sector: 0x"
          + Integer.toHexString(UnsignedByte.intValue(inData[index + i])) + "/0x"
          + Integer.toHexString(UnsignedByte.intValue(inData[index + i + 1])));
      */
      if (emittedBytes + 256 > lineCapacity)
        bytesToEmit = lineCapacity - emittedBytes;
      else
        bytesToEmit = 256;
      dumpSector(inData, chunkOffset, bytesToEmit, out);
      emittedBytes += bytesToEmit;
    }
    // System.err.println("        DEBUG: Emitted: 0x" + Integer.toHexString(emittedBytes));
    return emittedBytes;
  }

  void dumpSector(byte inData[], int offset, int limit, ByteArrayOutputStream out)
  {
    byte[] tempFile = new byte[limit];
    System.arraycopy(inData, offset, tempFile, 0, limit);
    // String newString = new String(tempFile);
    // System.out.println(newString);
    try
    {
      out.write(tempFile);
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static String normalizeName(String name)
  {
    /*
     * Some characters that might come out in a filename are illegal these days - so modify those
     */
    char[] newName = name.toCharArray();
    if (name != null)
    {
      char c;
      for (int i = 0; i < newName.length; i++)
      {
        c = newName[i];
        switch (c)
        {
        case '/':
          newName[i] = '-';
          break;
        case ':':
          newName[i] = '-';
          break;
        case '&':
          newName[i] = '_';
          break;
        case '?':
          newName[i] = '_';
          break;
        }
      }
    }
    return new String(newName).trim();
  }
}
