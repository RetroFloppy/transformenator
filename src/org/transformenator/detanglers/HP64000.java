/*
 * Transformenator - perform transformation operations on files
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

/*
 * HP 64000 disk image file extractor
 * 
 * Floppy disk geometry: 2 sides, 256 bytes per sector, 16 sectors per track, 35 tracks
 * 
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.ImageDisk;
import org.transformenator.internal.UnsignedByte;

public class HP64000 extends ADetangler
{
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
  {
    /*
     * Catalog starts on track 0, second sector and stretches to the
     * end of the track.
     * 
     * Catalog entries are 32 (0x20) bytes long; stuff we know/care
     * about: bytes 0x00-0x0a: Filename (space padded) bytes
     * 0x0e-0x0f: File start (in sectors) bytes 0x12-0x13: File
     * length (in sectors)
     */
    int startBlock, lengthBlock;
    int flagByte, typeByte;
    // First, is our data stream in ImageDisk (i.e. .IMD) format?
    byte imdData[] = null;
    imdData = ImageDisk.imd2raw(inData, false, false);
    if (imdData != null)
      inData = imdData;
    // Now we know we have a linear disk image in memory
    
    // Start checking for directory entries
    for (int i = 0x22000; i < 0x23800; i += 0x20)
    {
      String filename = "";
      flagByte = UnsignedByte.intValue(inData[i]);
      typeByte = UnsignedByte.intValue(inData[i + 0x11]);
      if ((flagByte != 0x00) && (typeByte != 0x00))
      {
        startBlock = UnsignedByte.intValue(inData[i + 0x13]);
        lengthBlock = UnsignedByte.intValue(inData[i + 0x17]);
        for (int j = 1; j < 16; j++)
        {
          if (inData[j + i] != 0x00)
          {
            char val = (char)inData[j+i];
            if (val < 0x20)
              break;
            filename += val;
          }
          else
            break;
        }
        filename = filename.trim();
        // hp 64000 Logic Development System File Format Reference Manual
        // 64980-90933
        switch (typeByte)
        {
          case 0x01:
            filename += ".system";
            break;
          case 0x02:
            filename += ".source";
            break;
          case 0x03:
            filename += ".reloc";
            break;
          case 0x04:
            filename += ".absolute";
            break;
          case 0x05:
            filename += ".listing";
            break;
          case 0x06:
            filename += ".emul_com";
            break;
          case 0x07:
            filename += ".link_com";
            break;
          case 0x08:
            filename += ".trace";
            break;
          case 0x09:
            filename += ".prom";
            break;
          case 0x0a:
            filename += ".data";
            break;
          case 0x0b:
            filename += ".asmb_db";
            break;
          case 0x0c:
            filename += ".asmb_sym";
            break;
          case 0x0d:
            filename += ".link_sym";
            break;
          default:
            filename += ".temp";
            break;
        }
        // System.out.println("Found file: "+filename+" Type: 0x"+UnsignedByte.toString(typeByte)+" Flag: 0x"+UnsignedByte.toString(flagByte)+" Start: 0x"+Integer.toHexString(startBlock)+" Length: 0x"+UnsignedByte.toString(lengthBlock));
        if ((filename.length() > 0) && (lengthBlock > 0))
        {
          ByteArrayOutputStream out;
          try
          {
            out = new ByteArrayOutputStream();
            dumpFileChain(inData, out, startBlock, typeByte);
            out.flush();
            if (typeByte == 0x02)
            {
              ByteArrayOutputStream textRender = null;
              textRender = dumpTextFile(out.toByteArray());
              if (textRender != null)
                out = textRender;
            }
            parent.emitFile(out.toByteArray(), outDirectory, inFile, filename + fileSuffix);
          }
          catch (IOException io)
          {
            io.printStackTrace();
          }
        }
      }
    }
  }

  ByteArrayOutputStream dumpTextFile(byte inData[])
  {
    /*
     * Text files are bracketed: 0x00 begin and end the file
     * Each "line" has a length (in 2-byte words) at the beginning and end.  Example:
     * "Hello, world!"
     * is 7 words long, so it becomes:
     *(len) H  e  l  l  o  ,     w  o  r  l  d  ! (len) 
     * 07   48 65 6C 6C 6F 2C 20 77 6F 72 6C 64 21 07
     * 
     * So here we just peel off each line and send it to the output stream.
     * If things go off the rails, we return null so we know not to trust it.
     */
    boolean valid = true;
    ByteArrayOutputStream out = null;
    try
    {
      out = new ByteArrayOutputStream();
      int i = 0;
      int j = 0;
      while (true)
      {
        byte b = inData[i];
        if (b > 0x00)
        {
          int run = UnsignedByte.intValue(b);
          i++;
          if (i + (run * 2) > inData.length)
          {
            break;
          }
          if (UnsignedByte.intValue(inData[i + (run * 2)]) == run)
          {
            for (j = 0; j < 2 * run; j++)
              out.write(inData[i+j]);
            i += j + 1;
            out.write(0x0d);
          }
          else
          {
            System.out.println("Run: "+run);
            valid = false;
            break;
          }
        }
        else // b == 0x00
        {
          if (i > 0) // This is the second null, so it terminates the file
            break;
          i++;
        }
        if (i >= inData.length)
          break;
      }
      out.flush();
    }
    catch (IOException io)
    {
      io.printStackTrace();
      valid = false;
    }
    if (!valid) out = null;
    return out;
  }

  void dumpFileChain(byte inData[], ByteArrayOutputStream out, int startBlock, int fileType) throws IOException
  {
    int nextIndex = (startBlock +1) * 0x800 - 2;
    int nextBlock = UnsignedByte.intValue(inData[nextIndex+1],inData[nextIndex]);
    // System.out.println("  this index: 0x"+Integer.toHexString(startBlock * 0x800) +" this block: "+UnsignedByte.toString(startBlock)+" next block: 0x"+Integer.toHexString(nextBlock));
    byte block[] = new byte[0x800-4]; // First 2 bytes of a block is "back" pointer, last 2 bytes  is the "next" pointer
    System.arraycopy(inData, (startBlock * 0x800) + 2, block, 0, 0x800 - 4);
    if (fileType == 0x07) // link_com is only 256 bytes, minus the back pointer
      out.write(block,0,0xfe);
    else
    {
      out.write(block);
      if ((nextBlock != 0xffff) && (nextBlock * 0x800 < inData.length))
        dumpFileChain(inData, out, nextBlock, fileType);
    }
  }
}