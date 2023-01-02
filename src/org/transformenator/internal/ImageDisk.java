/*
 * Transformenator - perform transformation operations on binary files
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

package org.transformenator.internal;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/*
 * Conversion from Dave Dunfield's ImageDisk (.IMD) format to raw binary,
 * respecting the logical sector ordering specified in the original.
 * Much of this code is a direct translation into Java from a utility
 * found here:
 * http://bitsavers.org/bits/Convergent/ngen/imd2raw/
 * and corrections have been made to respect sector ordering.  Also see:
 * https://github.com/RetroFloppy/imd2raw
 */

/*
 * IMD image format
 * 
 * IMD v.v: dd/mm/yyy hh:mm:ss (ascii header)
 * comment terminated with 0x1a
 * for each track
 *  byte mode (0-5)
 *  byte cylinder
 *  byte head
 *  byte sector count
 *  byte sector size (0-6)
 *  sector numbering map
 *  optional cylinder map
 *  optional head map
 *  sector data records (type) (val, or data)
 */

public class ImageDisk
{
  static String[] modetbl =
  { "500K FM", "300K FM", "250K FM", "500K MFM", "300K MFM", "250K MFM" };

  // Do the conversion noisily by default
  public static byte[] imd2raw(byte[] inData)
  {
    // Default verbosity is true
    return imd2raw(inData, true);
  }

  // Convert an incoming byte array from IMD to raw format
  //  - Return a new byte array if successful
  //  - Return null if not
  // Be verbose or not verbose (i.e. silent) based on boolean
  public static byte[] imd2raw(byte[] inData, boolean verbose)
  {
    byte[] outData = null;
    ByteArrayOutputStream out = null;

    // First test: does it start with an 'IMD '?
    if (((char) inData[0] == 'I') && ((char) inData[1] == 'M') && ((char) inData[2] == 'D')
        && ((char) inData[3] == ' '))
    {
      int cursor = 0;
      int c, mode, cyl, hd, seccnt;
      int secsiz = 0;
      int[] sectormap;
      int[] sectormapsorted;
      byte[][] secdata = new byte[64][8192];
      char[] secdisp = new char[32];
      String cylpad = " ";
      out = new ByteArrayOutputStream();

      while (true)
      {
        c = UnsignedByte.intValue(inData[cursor]);
        if (c == 0x1a)
          break;
        cursor++;
      }

      while (true)
      {
        cursor++;
        mode = UnsignedByte.intValue(inData[cursor]);
        if (mode > 6)
        {
          if (verbose) System.err.println("Unexpected mode, got " + mode + ", expecting 1-5.");
          return null;
        }

        cursor++;
        cyl = UnsignedByte.intValue(inData[cursor]);
        if (cyl > 80)
        {
          if (verbose) System.err.println("Unexpected cylinder count, got " + cyl + ", expecting <= 80.");
          return null;
        }

        cursor++;
        hd = UnsignedByte.intValue(inData[cursor]);
        if (hd > 1)
        {
          if (verbose) System.err.println("Unexpected head count, got " + hd + ", expecting 0-1.");
          return null;
        }

        cursor++;
        seccnt = UnsignedByte.intValue(inData[cursor]);

        cursor++;
        c = UnsignedByte.intValue(inData[cursor]);

        switch (c)
        {
          case 0:
            secsiz = 128;
            break;
          case 1:
            secsiz = 256;
            break;
          case 2:
            secsiz = 512;
            break;
          case 3:
            secsiz = 1024;
            break;
          case 4:
            secsiz = 2048;
            break;
          case 5:
            secsiz = 4096;
            break;
          case 6:
            secsiz = 8192;
            break;
          default:
            if (verbose) System.err.println("Unknown sector size indicator " + c);
            break;
        }
        // System.err.println(
        //   "Cyl:" + cyl + " Hd:" + hd + " " + (String) modetbl[mode] + " " + seccnt + " sectors size " + secsiz);

        // copy sector/interleave map
        sectormap = new int[seccnt];
        sectormapsorted = new int[seccnt];
        for (int i = 0; i < seccnt; i++)
        {
          cursor++;
          sectormap[i] = UnsignedByte.intValue(inData[cursor]);
          sectormapsorted[i] = UnsignedByte.intValue(inData[cursor]);
        }
        Arrays.sort(sectormapsorted);
        /*
        System.err.print("Tbl ");
        for (int num : sectormap)
          System.err.print(num + " ");
        System.err.println();
        System.err.print("Srt ");
        for (int num : sectormapsorted)
          System.err.print(num + " ");
        System.err.println();
        */

        // copy sector information indexed by the sector number
        for (int i = 0; i < seccnt; i++)
        {
          cursor++;
          c = UnsignedByte.intValue(inData[cursor]);
          switch (c)
          {
            case 0: // Sector data unavailable - could not be read
            case 5:
            case 7:
              secdisp[i] = 'X';
              for (int j = 0; j < secsiz; j++)
              {
                if (c > 0)
                  cursor++;
                secdata[sectormap[i]][j] = (byte) 0xE5;
              }
              break;
            case 1: // Normal data 'secsiz' bytes to follow
              secdisp[i] = '.';
              for (int j = 0; j < secsiz; j++)
              {
                cursor++;
                secdata[sectormap[i]][j] = (byte) UnsignedByte.intValue(inData[cursor]);
              }
              break;
            case 3: // Data with 'deleted data' address mark
              secdisp[i] = 'd';
              for (int j = 0; j < secsiz; j++)
              {
                cursor++;
                secdata[sectormap[i]][j] = (byte) UnsignedByte.intValue(inData[cursor]);
              }
              break;
            case 2: // Compressed with value in next byte
            case 4:
            case 6:
            case 8:
              secdisp[i] = 'C';
              cursor++;
              byte value = inData[cursor];
              for (int j = 0; j < secsiz; j++)
                secdata[sectormap[i]][j] = value;
              break;
            default:
              if (verbose) System.err.println("Unexpected sector data flags, got "+c+", expected 0-8");
              return null;
          }
        }
        // Pad out the output so it lines up nicely
        if (cyl < 10)
          cylpad = "  ";
        else
          cylpad = " ";
        if (verbose) System.err.print("Cyl" + cylpad + cyl + " Hd " + hd + " " + secsiz + " ");
        for (int i = 0; i < seccnt; i++)
        {
          if (verbose) System.err.print(secdisp[i]);
          for (int j = 0; j < secsiz; j++)
            out.write(secdata[sectormapsorted[i]][j]);
        }
        for (int i = 0; i < seccnt; i++)
          if (verbose) System.err.print(" " + sectormap[i]);
        if (verbose) System.err.println();
        // Process until the end of the file data
        if (cursor == inData.length - 1)
          break;
      }
      outData = out.toByteArray();
    }
    return outData;
  }
}
