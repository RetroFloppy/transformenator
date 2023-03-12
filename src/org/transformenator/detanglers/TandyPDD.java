/*
 * Transformenator - perform transformation operations on files Copyright (C) 2022 by David Schmidt
 * 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class TandyPDD extends ADetangler
{
  /*
   * A Tandy PDD (PDD1 or PDD2) filesystem has beginning and ending sector
   * indicators, as well as a file length.  Sometimes they disagree with one
   * another; sometimes they are in sync.  It may have to do with deleted
   * files and reused sectors, but I can't seem to find any indication of how
   * that would work.
   */
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
  {
    int num_catalog_sectors = 1;
    int i, j, k;
    ByteArrayOutputStream out = null;

    if (inData.length > 102400)
    {
      num_catalog_sectors = 2;
    }
    for (i = 0; i < num_catalog_sectors; i++)
    {
      // There are a max of 40 file entries in each PDD directory sector.
      for (j = 0; j < 40; j++)
      {
        // Test for file existence by checking the file head pointer
        if (UnsignedByte.intValue(inData[(i * 1280) + (j * 31 + 29)]) == 0x00)
        {
          // System.out.println("(" + j + ": Zero catalog entry)");
          continue;
        }
        // We have a likely file; filename is at most 24 bytes, space-padded
        String filename = "";
        for (k = 0; k < 23; k++)
        {
          char bob = (char) inData[(i * 1280) + (j * 31 + k)];
          if (bob != 0x20)
          {
            filename += bob;
          }
          else
            break;
        }
        int file_size = UnsignedByte.intValue(inData[(j * 31) + (i * 1280) + 26], inData[(j * 31) + (i * 1280) + 25]);
        int start_sector = UnsignedByte.intValue(inData[(j * 31) + (i * 1280) + 29]);
        int end_sector = UnsignedByte.intValue(inData[(j * 31) + (i * 1280) + 30]);
        int num_sectors = end_sector - start_sector + 1;
        int bytes_copied = 0;
        System.out.println("File " + j + ": [" + filename + "] sectors: " + num_sectors + " size: " + file_size
            + " start sector: 0x" + Integer.toHexString(start_sector * 1280) + "");
        out = new ByteArrayOutputStream();

        for (k = 0; k < num_sectors; k++)
        {
          int lim = 1280;
          //if (bytes_copied + 1280 > file_size)
          //  lim = file_size - bytes_copied;
          for (int x = 0; x < lim; x++)
          {
            out.write(inData[(start_sector * 1280) + (k * 1280) + x]);
          }
          bytes_copied += lim;
          //if (lim <= 0)
          //  System.out.println("Hmmm...");
          parent.emitFile(out.toByteArray(), outDirectory,
              inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length())),
              filename + fileSuffix);
        }
      }
    }
  }
}
