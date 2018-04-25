package org.transformenator.detanglers;

import java.io.File;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class Valdocs extends ADetangler 
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String inFile, String outDirectory, String fileSuffix)
	{
		// Figure out the original file name
		char[] name = new char[110];
		byte[] newBuf = new byte[inData.length];
		int newBufCursor = 0;
		for (int i = 0; i < 110; i++)
		{
			char newChar = (char) inData[i + 4];
			if (newChar == ':')
				newChar = '-';
			else if (newChar == '/')
				newChar = '-';
			else if (newChar == '?')
				newChar = 'x';
			name[i] = newChar;
		}
		newName = new String(name).trim();
		// System.err.println("Found Valdocs file: \"" + newName + "\"");
		if (newName.length() > 0)
		{
			/*
			 * Pick apart the file hunk indices. The first few indices seem to be non-useful... so start in at 0x80a. It's unclear how deep the indices can go. It's possible it should look deeper than it does, but the field of 0xFFs has some noise near the end.
			 * 
			 * Each index is a pointer to the next 512 bytes (a sector) of data in the file.
			 */
			for (int i = 0x80a; i < 0xa00; i += 2)
			{
				int idx = UnsignedByte.intValue(inData[i], inData[i + 1]);
				if (idx < 32768)
				{
					// System.err.println("DEBUG: idx: "+idx);
					if (((idx * 512) + 1) < inData.length)
					{
						// Chunks may start with a pointer to skip over blank space
						int offset = UnsignedByte.intValue(inData[(idx * 512)], inData[(idx * 512) + 1]);
						// Pull out the data in the chunk
						for (int j = offset + 4; j < 0x200; j++)
						{
							newBuf[newBufCursor++] = inData[(idx * 512) + j];
						}
					}
					// else
						// System.err.println("DEBUG: Found an index out of bounds: "+idx);
				}
			}
			inData = new byte[newBufCursor];
			for (int i = 0; i < newBufCursor; i++)
			{
				inData[i] = newBuf[i];
			}
		}
		// System.err.println("DEBUG: Data length after de-indexing: "+inData.length);
		interpreter.emitFile(inData, outDirectory + File.separator + newName + fileSuffix);
	}
	String newName = null;
}