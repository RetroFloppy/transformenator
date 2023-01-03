/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2023 by David Schmidt
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
import org.transformenator.internal.UnsignedByte;

/*
 * Convert spreadsheets from the Brother Word Processor to comma separated values 
 *
 */
public class ConvertBrotherSpreadsheet
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
        int cursor = 0x0a50;
        while (true)
        {
          boolean hex = true;
          String cell;
          int increment = UnsignedByte.intValue(inData[cursor]);
          if (increment + cursor > inData.length)
            break;
          if (increment == 0)
            break;
          if (inData[cursor + 1] != 0)
            hex = false;
          cell = "";
          for (int i = 1; i < increment; i++)
          {
            if (hex == false)
            {
              if ((inData[cursor + i] != 0) && (inData[cursor + i] != 0x0d) && (inData[cursor + i] != 0x0a)) 
                cell += (char)inData[cursor + i];
            }
            if (i == 1)
            {
              if (UnsignedByte.intValue(inData[cursor]) < 0x10)
                System.out.print("(0x0");
              else
                System.out.print("(0x");
              System.out.print(Integer.toHexString(UnsignedByte.intValue(inData[cursor]))+")");
            }
            if (UnsignedByte.intValue(inData[cursor+i]) < 0x10)
              System.out.print(" 0x0" + Integer.toHexString(UnsignedByte.intValue(inData[cursor + i])));
            else
              System.out.print(" 0x" + Integer.toHexString(UnsignedByte.intValue(inData[cursor + i])));
          }
          if (cell != "")
            System.out.println(" ["+cell+"]");
          else
            System.out.println();
          cursor += increment;
        }
        // System.err.println("After devectorization, cursor = "+cursor);
        if (cursor + 4 != inData.length)
          System.out.println("*** Halted early");
        byte[] outData = null;
        if (inData != null)
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
            out.write(inData, 0, inData.length);
            out.flush();
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
    {
      // wrong args
      help();
    }
  }

  public static String describe(boolean verbose)
  {
    return "Convert Brother Word Processor spreadsheet files to text." + (verbose ? "" : "");
  }

  public static void help()
  {
    System.err.println();
    System.err.println("ConvertBrotherSpreadsheet " + Version.VersionString + " - " + describe(true));
    System.err.println();
    System.err.println("Usage: ConvertBrotherSpreadsheet infile outfile");
  }
}