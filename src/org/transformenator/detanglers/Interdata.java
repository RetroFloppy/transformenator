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

/*
 * Pull the files off of the virtual file system of an 8" Interdata disk image.
 * 
 * Disk geometry: standard IBM 8" - FM encoded, single sided, 
 * 128 bytes x 26 sectors x 77 tracks - but the OS deals in 256 byte chunks.
 * Files in the FAT include a couple of pointers (first block, last block) 
 * and an LRECL specification.  Pulling text data out of blocks involves 
 * re-casting text into record-length chunks, and removing anything after a
 * 0x0d byte (newline).  Binary interpretation skips the newline truncation.
 * 
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Interdata extends ADetangler
{
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
  {
    /*
     * Filesystem is based on 4-byte pointers to "blocks" (256 byte chunks). 
     * The very first pointer to a block of interest is the FAT - which sits 0x08 bytes in.
     */
    int nextFatLink = numAt(inData, 0x08);
    // System.out.println("DEBUG: Head pointer to FAT table: 0x"+Integer.toHexString(nextFatLink));
    while (nextFatLink != 0x00)
    {
      int ixFAT = nextFatLink * 0x100;
      nextFatLink = numAt(inData, ixFAT);
      // There is at most 5 file entries per 256 byte FAT "block"  
      for (int i = 0; i < 0x5; i++)
      {
        // Keep track of both a text and binary file name
        String filename = "";
        String filename2 = "";
        // We may have a suffix, or may not - so either it's "bob.txt" or "bob"
        String suffix = "";
        if (UnsignedByte.intValue(inData[ixFAT + (i * 0x30) + 4]) != 0x00)
        {
          // FAT entry looks reasonable; collect file name
          for (int j = ixFAT + (i * 0x30) + 4; j < ixFAT + (i * 0x30) + 12; j++) // Filename length is 8
          {
            filename += (char) inData[j];
            filename2 += (char) inData[j];
          }
          filename = filename.trim();
          filename2 = filename.trim();
          for (int j = ixFAT + (i * 0x30) + 12; j < ixFAT + (i * 0x30) + 15; j++) // File suffix length is 3
          {
            suffix += (char) inData[j];
          }
          suffix = suffix.trim();
          if (suffix.length() > 0)
          {
            filename = filename + "." + suffix;
            filename2 = filename2 + "." + suffix;
          }
          if (fileSuffix.length() > 0)
            filename2 = filename2 + fileSuffix;
          // Discover more about the file - where it starts, and its LRECL
          int ixFileHead = numAt(inData, ixFAT + (i * 0x30) + 0x10) * 0x100;
          int lrecl = numAt(inData, ixFAT + (i * 0x30) + 0x18);
          if (ixFileHead > 0)
          {
            // System.out.println("DEBUG: found file: "+filename + " with lrecl "+lrecl+" at 0x"+Integer.toHexString(ixFileHead));
            ByteArrayOutputStream outBinary, outText;
            outBinary = new ByteArrayOutputStream();
            outText = new ByteArrayOutputStream();
            // Respect the LRECL - we don't know if a file is supposed to be text or binary, so try both
            followFile(inData, ixFileHead, lrecl, outBinary);
            byte intermediate[] = outBinary.toByteArray();
            outBinary = new ByteArrayOutputStream();
            for (int j = 0; j <= intermediate.length - lrecl; j += lrecl)
            {
              // For text file type, for each record, truncate at 0x0d (newline)
              int textEnd = lrecl;
              for (int k = 0; k < lrecl; k++)
              {
                if (intermediate[j + k] == 0x0d)
                {
                  textEnd = k + 1;
                  break;
                }
              }
              try
              {
                // System.out.println("DEBUG: range is copying from 0x"+Integer.toHexString(j)+" to 0x"+Integer.toHexString(j+end-1));
                // Dump file in binary, which is the full LRECL for each record
                byte rangeBinary[] = Arrays.copyOfRange(intermediate, j, j + lrecl);
                outBinary.write(rangeBinary);
                outBinary.flush();
                // Dump file in text, which truncates each record at newline
                byte rangeText[] = Arrays.copyOfRange(intermediate, j, j + textEnd);
                outText.write(rangeText);
                outText.flush();
              }
              catch (IOException e)
              {
                System.out.println(e);
              }
            }
            parent.emitFile(outBinary.toByteArray(), outDirectory,
                inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())),
                filename + ".bin");
            parent.emitFile(outText.toByteArray(), outDirectory,
                inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())),
                filename2);
          }
          else
            System.out.println("Found empty file: " + filename);
        }
      }
      // System.out.println(" next FAT block: 0x"+Integer.toHexString(nextFatLink));
    }
  }

  int numAt(byte inData[], int index)
  {
    /*
     * Given an index to a 4-byte number, return the number there (ignoring top two MSBs, we're lazy)
     */
    int result = UnsignedByte.intValue(inData[index + 3], inData[index + 2]);
    return result;
  }

  void followFile(byte inData[], int index, int lrecl, ByteArrayOutputStream out)
  {
    /*
     * Given an index to a file chunk, follow it and send it out
     */
    int ixPrev = numAt(inData, index) * 0x100;
    int ixNext = numAt(inData, index + 0x04) * 0x100;
    // System.out.println(" arrived at file extent, index 0x" + Integer.toHexString(index) + "; next link: 0x" + Integer.toHexString(ixNext) + "; prev: 0x" + Integer.toHexString(ixPrev));

    dumpExtent(inData, index, lrecl, out);

    if (ixNext >= inData.length)
      System.out.println("*** FILE OVERRAN IMAGE ***");
    if ((ixNext != 0x00) && (ixNext < inData.length))
      followFile(inData, ixNext, lrecl, out);
    // And if it's zero, we're done
  }

  void dumpExtent(byte inData[], int index, int lrecl, ByteArrayOutputStream out)
  {
    int ixPrev = numAt(inData, index) * 0x100;
    int ixNext = numAt(inData, index + 0x04) * 0x100;
    byte range[];
    // System.out.println(" next block: 0x"+Integer.toHexString(ixNext)+"; prev: 0x"+Integer.toHexString(ixPrev));
    for (int i = 0x08; i < 0x100; i += 0x04)
    {
      int ixBlock = numAt(inData, index + i) * 0x100;
      if (ixBlock < inData.length)
      {
        if (ixBlock > 0)
        {
          // System.out.println(" dumping block at 0x"+Integer.toHexString(ixBlock));
          try
          {
            // System.out.println("range is copying from 0x"+Integer.toHexString(ixBlock)+" to 0x"+Integer.toHexString(ixBlock + 0x100));
            range = Arrays.copyOfRange(inData, ixBlock, ixBlock + 0x100);
            out.write(range);
            out.flush();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
      }
      // else
      //   System.err.println("*** FILE OVERRAN IMAGE ***");
    }
  }
}