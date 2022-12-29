/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2022 by David Schmidt
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

public class SWTPCDOS68 extends ADetangler
{
  /*
   *
   */

  int SECTOR_SIZE = 128;
  int TRACK_SIZE = 18 * SECTOR_SIZE;

  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
  {
    String fileName = "";
    int nextTrack = 0;
    int nextSector = 1;
    boolean deleted = false;
    while (nextTrack + nextSector > 0)
    {
      int cursor = (nextTrack * TRACK_SIZE) + (nextSector * SECTOR_SIZE);
      for (int i = 0; i < 4; i++)
      {
        int offset = 0x20 + (i * 0x18);
        // if (there is some indication the file exists)
        {
          // Pull out the file name
          fileName = "";
          for (int name = 0; name < 6; name++)
          {
            char letter = (char) inData[cursor + offset + name];
            if (letter != 0)
            {
              fileName += letter;
            }
            else
              if (name == 0)
              {
                fileName += '_';
                deleted = true;
              }
          }
          if (deleted)
          {
            fileName += "(deleted)";
          }
          // Take care of the file "suffix"
          for (int name = 6; name < 9; name++)
          {
            char letter = (char) inData[cursor + offset + name];
            if (letter != 0)
            {
              if (name == 6)
                fileName += ".";
              fileName += letter;
            }
          }
          int fileType = UnsignedByte.intValue(inData[cursor + offset + 0x09]);
          int fileFirstTrack = UnsignedByte.intValue(inData[cursor + offset + 0x0b]) - 0x80;
          int fileFirstSector = UnsignedByte.intValue(inData[cursor + offset + 0x0c]) - 0x40;
          int fileFirstOffset = (fileFirstTrack * TRACK_SIZE) + (fileFirstSector * SECTOR_SIZE);
          int fileLastTrack = UnsignedByte.intValue(inData[cursor + offset + 0x0d]) - 0x80;
          int fileLastSector = UnsignedByte.intValue(inData[cursor + offset + 0x0e]) - 0x40;
          int fileLastOffset = (fileLastTrack * TRACK_SIZE) + (fileLastSector * SECTOR_SIZE);
          System.out.println("T/S: " + fileFirstTrack + "/" + fileFirstSector + "," + fileLastTrack + "/"
              + fileLastSector + " First: 0x" + Integer.toHexString(fileFirstOffset) + " Last: 0x"
              + Integer.toHexString(fileLastOffset) + " file: " + fileName + " type: " + fileType);
          dumpFile(parent, inData, fileFirstTrack, fileFirstSector, deleted, outDirectory, inFile, fileName);
          deleted = false;
        }
      }
      nextTrack = UnsignedByte.intValue(inData[cursor]) - 0x80;
      nextSector = UnsignedByte.intValue(inData[cursor + 1]) - 0x40;
    }
  }

  public void dumpFile(FileInterpreter interpreter, byte[] inData, int fileFirstTrack, int fileFirstSector,
      boolean deleted, String outDirectory, String inFile, String fileName)
  {
    if (deleted)
    {
      System.out.println("*** Noting deleted file: \"" + fileName + "\"");
    }
    else
    {
      byte buffer[] = null;
      ByteArrayOutputStream out = null;
      try
      {
        out = new ByteArrayOutputStream();
        int nextTrack = fileFirstTrack;
        int nextSector = fileFirstSector;
        while (nextTrack + nextSector > 0)
        {
          int cursor = (nextTrack * TRACK_SIZE) + (nextSector * SECTOR_SIZE);
          // System.out.println("  Writing T/S " + nextTrack + "/" + nextSector);
          buffer = new byte[SECTOR_SIZE - 4];
          System.arraycopy(inData, cursor + 4, buffer, 0, SECTOR_SIZE - 4);
          out.write(buffer);
          nextTrack = UnsignedByte.intValue(inData[cursor]) - 0x80;
          nextSector = UnsignedByte.intValue(inData[cursor + 1]) - 0x40;
        }
        out.flush();
        interpreter.emitFile(out.toByteArray(), outDirectory,
            inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())), fileName);
      }
      catch (IOException io)
      {
        io.printStackTrace();
      }
      catch (ArrayIndexOutOfBoundsException ai)
      {
        System.err.println("Error: seek requested outside bounds of disk image " + inFile + " for file " + fileName);
      }
    }
  }
}