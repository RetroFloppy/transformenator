/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2025 by David Schmidt
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
 * HP 4145 file extractor
 * 
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.ImageDisk;
import org.transformenator.internal.UnsignedByte;

public class HP4145 extends ADetangler
{
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix,
      boolean isDebugMode)
  {
    // First, is our data stream in ImageDisk (i.e. .IMD) format?
    byte imdData[] = null;
    imdData = ImageDisk.imd2raw(inData, false, false);
    if (imdData != null)
      inData = imdData;
    // Now we know we have a linear disk image in memory
    int fileStart = 0, fileLength = 0, fileType = 0;
    if (inData.length == 92160) // If it's not the size we expect, bail
    {
      /*
       * Catalog starts on track 0, second sector and stretches to the
       * end of the track.
       * 
       * Catalog entries are 20 (0x14) bytes long; stuff we know/care
       * about: bytes 0x00-0x05: Filename (space padded) bytes
       * 0x07-0x07: File start (in sectors) bytes 0x0a: File
       * length (in sectors)
       */
      for (int i = 0xc980; i < 0xd180; i += 0x14)
      {
        String filename = "";
        if (inData[i] != 0x00)
        {
          // System.out.println("Location: "+Integer.toHexString(i));
          fileStart = UnsignedByte.intValue(inData[i + 0x08], inData[i + 0x07]) * 256;
          fileLength = UnsignedByte.intValue(inData[i + 0x0a]) * 256;
          if (fileStart + fileLength > inData.length)
            fileLength = inData.length - fileStart;

          int j;
          for (j = 0; j < 6; j++)
          {
            if (inData[j + i] != 0x00)
            {
              filename += (char) inData[j + i];
            }
            else
              break;
          }
          // Find the end-of-file marker, trim down to that length
          boolean foundEOF = false;
          for (j = fileStart + fileLength - 1; j > fileStart; j--)
          {
            if ((inData[j] == -1) && (inData[j + 1] == -1))
            {
              // System.err.println("Found final 0xffff at "+(j-fileStart));
              foundEOF = true;
              break;
            }
            // System.err.println(Integer.toHexString(UnsignedByte.intValue(inData[j])));
            if (UnsignedByte.intValue(inData[j]) == 0xff)
            {
              continue;
            }
            if (UnsignedByte.intValue(inData[j]) == 0xef)
            {
              // System.err.println("Found a trailing 0xef at "+(j-fileStart));
              foundEOF = true;
              break;
            }
          }
          if (foundEOF)
            fileLength = j - fileStart;
          // System.out.println("Found file: "+filename+" Start: 0x"+Integer.toHexString(fileStart)+" End: 0x"+Integer.toHexString(fileStart+fileLength-1)+" Length: 0x"+Integer.toHexString(fileLength));
          filename = filename.trim();
          if ((filename.length() > 0) && (fileLength > 0))
          {
            ByteArrayOutputStream out;
            try
            {
              if (fileStart + fileLength < inData.length)
              {
                out = new ByteArrayOutputStream();
                out.write(inData, fileStart, fileLength);
                out.flush();
                parent.emitFile(out.toByteArray(), outDirectory, inFile, filename + fileSuffix);
              }
              else
                System.err.println("Error: file " + filename + " would exceed the capacity of the disk image.");
            }
            catch (IOException io)
            {
              io.printStackTrace();
            }
          }
        }
        else
          break;
      }
    }
    else
    // Don't know what this is
    {
      System.err.println("Likely not an HP 4145 disk image; file length of "+inData.length+" bytes is not expected.");
    }
  }

  public static void dumpFileChain(ByteArrayOutputStream out, byte[] inData, int currentSector, int preambleOffset,
      int firstSectorComp) throws IOException
  {
    int realOffset = currentSector * 0x1000 + preambleOffset;
    int nextSector = UnsignedByte.intValue((byte) inData[realOffset + 0xfff], (byte) inData[realOffset + 0xffe]);
    /*
    System.err.println("dumpFileChain: currentSector: " + Integer.toHexString(currentSector) + 
    		" nextSector: "+ Integer.toHexString(nextSector) + 
    		" nS pointer address: " + Integer.toHexString(realOffset + 0xfff));
    */
    if (realOffset < inData.length)
    {
      // System.err.println("dumpFileChain: realOffset: "+Integer.toHexString(realOffset));
      byte range[] = Arrays.copyOfRange(inData, realOffset + 2 + firstSectorComp, realOffset + 0xffe);
      out.write(range);
      // System.err.println("dumpFileChain: nextSector: "+ Integer.toHexString(nextSector));
      if ((nextSector != 0xffff) && // Not standard end of sector chain
          (nextSector != 0) && // Not zero, which is almost certainly an error
          (nextSector * 0x1000 + preambleOffset < inData.length) && // Lies within the image
          (nextSector != currentSector)) // Not the sector we just came from
        // Still have to worry about loops... those won't be detected
        dumpFileChain(out, inData, nextSector, preambleOffset, 0);
    }
  }

}
