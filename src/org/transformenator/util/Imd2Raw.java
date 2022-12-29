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

package org.transformenator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;

import org.transformenator.internal.Version;
import org.transformenator.internal.ImageDisk;

/*
 * Convert Dave Dunfield's IMD format to raw binary data; the majority of the work
 * happens in the org.transformenator.internal.ImageDisk class.  Output matches the
 * original IMDU /b command output. 
 *
 */
public class Imd2Raw
{
  public static void main(java.lang.String[] args)
  {
    BufferedOutputStream out = null;
    String outputFile = "";
    if (args.length == 2)
    {
      byte[] inData = null;
      System.err.println("Reading input file: " + args[0]);
      File file = new File(args[0]);
      byte[] result = new byte[(int) file.length()];
      try
      {
        // Read in the entire imd file
        InputStream input = null;
        try
        {
          int totalBytesRead = 0;
          input = new BufferedInputStream(new FileInputStream(file));
          while (totalBytesRead < result.length)
          {
            int bytesRemaining = result.length - totalBytesRead;
            // input.read() returns -1, 0, or more:
            int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
            if (bytesRead > 0)
            {
              totalBytesRead = totalBytesRead + bytesRead;
            }
          }
          inData = result;
        }
        finally
        {
          if (input != null)
            input.close();
        }
      }
      catch (FileNotFoundException ex)
      {
        System.err.println("Input file \"" + file + "\" not found.");
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
      if (inData != null)
      {
        // We have good data; get ready to deal with it.
        System.err.println("Read " + inData.length + " bytes.");
        outputFile = args[1];
        // Ready to go.  Time to face the music.
        byte[] outData = null;
        outData = ImageDisk.imd2raw(inData);
        if (outData != null)
        {
          try
          {
            out = new BufferedOutputStream(new FileOutputStream(outputFile));
          }
          catch (FileNotFoundException e1)
          {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
          System.err.println("Creating output file: " + outputFile);
          try
          {
            out.write(outData, 0, outData.length);
            out.flush();
            out.close();
          }
          catch (IOException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        else
        {
          System.err.println("File does not appear to be in ImageDisk format.");
        }
      }
    }
    else
    {
      // wrong args
      help();
    }
  }

  public static String describe(boolean verbose)
  {
    return "Convert ImageDisk .IMD disk image file to raw, linear data." + (verbose ? "" : "");
  }

  public static void help()
  {
    System.err.println();
    System.err.println("Imd2Raw " + Version.VersionString + " - " + describe(true));
    System.err.println();
    System.err.println("Usage: Imd2Raw infile outfile");
  }
}