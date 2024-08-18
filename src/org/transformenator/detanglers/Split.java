/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2024 by David Schmidt
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

/*
 * Split - separate a file into bite-sized chunks
 */

public class Split extends ADetangler
{
    int CHUNK_SIZE = 512; // Really need to somehow parameterize this
    @Override
    public void detangle(FileInterpreter interpreter, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
    {
        ByteArrayOutputStream out = null;
        out = new ByteArrayOutputStream();
        int remaining;
        int i;
        for (i = 0; i <= inData.length; i+=CHUNK_SIZE)
        {
            remaining = inData.length - i;
            if (remaining >= CHUNK_SIZE)
                remaining = CHUNK_SIZE;
            // System.out.println("total="+inData.length+" i "+i+" remaining="+remaining);
            try
            {
              out.write(("\n[]\n[Sector boundary]\n[]\n").getBytes());
              out.write(inData, i, remaining);
            }
            catch (IOException e)
            {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
        }
        interpreter.emitFile(out.toByteArray(), outDirectory, "", inFile + fileSuffix);
    }
}
