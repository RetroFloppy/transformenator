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

public class DECmate extends ADetangler
{
  // Based on disk geometry and math from Kevin Handy in his dmprocess.c:
  // "dmprocess.c - Program to convert Decmate II DecWord files to WordPerfect"
  // Kevin Handy (kth@srv.net)
  // Software Solutions, Inc.
  // Idaho Falls, Idaho  83401

	// Making these global because it's a pain to pass them around everywhere
	static FileInterpreter _parent = null;
	static String _outDirectory = null;
	static String _inFile = null;
	static String _fileSuffix = null;

	public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
	{
	  byte[] inData8bit = new byte[inData.length];
		// Save off our globals
		_parent = parent;
		_outDirectory = outDirectory;
		_inFile = inFile;
		_fileSuffix = fileSuffix;
    if (inData.length == 409600)
    {
      // First, swap around the 12-bit bytes into 8-bit bytes of the disk image
      int harrisByte; // The DECmate (II) came with a Harris/Intersil 6100 12-bit processor
      for (int i = 0; i < inData.length; i+=2)
      {
        harrisByte = UnsignedByte.intValue(inData[i]);
        harrisByte += (UnsignedByte.intValue(inData[i+1]) & 15) * 256;
        // fscking octal because DEC
        inData8bit[i] = UnsignedByte.loByte((harrisByte & 07700) / 64 + 31);
        inData8bit[i+1] = UnsignedByte.loByte((harrisByte & 077) + 31);
      }
      // So now we've 8-bit-ized the disk image in the inData8bit[] array, let's start interpreting that.
      
      ByteArrayOutputStream out;
      try
      {
        String filename = "dump.img";
        out = new ByteArrayOutputStream();
        out.write(inData8bit, 0, inData8bit.length);
        System.err.println("Creating file: " + filename);
        out.flush();
        out.close();
        parent.emitFile(out.toByteArray(), outDirectory, inFile, filename);
      }
      catch (IOException io)
      {
        io.printStackTrace();
      }
    }
    else
    {
      System.err.println("Probably not a DECmate RX50K disk image.");
    }
	}
}