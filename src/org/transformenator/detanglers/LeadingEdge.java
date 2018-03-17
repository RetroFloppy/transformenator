package org.transformenator.detanglers;

import org.transformenator.internal.UnsignedByte;

public class LeadingEdge extends ADetangler
{

	@Override
	public byte[] detangle(byte[] inData)
	{
		/*
		 * Pick apart the file hunk indices. Hunk indices start at 0x1200.
		 * 
		 * Each index is a pointer to a hunk at 512 bytes * the index number in the
		 * file.
		 */
		byte[] newBuf = new byte[inData.length];
		int newBufCursor = 0;
		boolean found9Yet = false;
		for (int indexIndex = 0x400; indexIndex < 0x500; indexIndex += 2)
		{
			int indexStart = UnsignedByte.intValue(inData[indexIndex], inData[indexIndex + 1]);
			// System.err.println("DEBUG: Index start value: "+indexStart);
			if ((indexStart >= 65520) || (indexStart < 9))
				continue;
			if (indexStart == 9)
				found9Yet = true;
			if (!found9Yet)
				continue;
			indexStart *= 512;
			// System.err.println("DEBUG: Found index table
			// 0x"+UnsignedByte.toString(inData[indexIndex+1])+UnsignedByte.toString(inData[indexIndex])+"
			// at "+indexStart);
			for (int i = indexStart; i < indexStart + 256; i += 2)
			{
				int block = UnsignedByte.intValue(inData[i], inData[i + 1]);
				int index = block * 512;
				// System.err.println("DEBUG: block:
				// 0x"+UnsignedByte.toString(inData[i+1])+UnsignedByte.toString(inData[i])
				// +" at file offset:
				// 0x"+UnsignedByte.toString(UnsignedByte.hiByte(index))+UnsignedByte.toString(UnsignedByte.loByte(index)));
				if (block == 0)
					break;
				if (block < 32768)
				{
					if (index + 1 < inData.length)
					{
						int j;
						// Find out where the block really ends - remove trailing zeroes
						for (j = index + 511; j >= index; j--)
							if (inData[j] != 0x00)
								break;
						// System.err.println("DEBUG: Found end of chunk at "+j+", or length
						// "+(j-index)+".");
						// Pull out the data in the chunk
						for (int k = 0; k < (j - index + 1); k++)
						{
							newBuf[newBufCursor++] = inData[index + k];
						}
					}
					else
						System.err.println("Found an index out of bounds: " + block);
				}
			}
			inData = new byte[newBufCursor];
			for (int i = 0; i < newBufCursor; i++)
				inData[i] = newBuf[i];
			// System.err.println("DEBUG: Data length after de-indexing: "+inData.length);
		}
		return inData;
	}

	@Override
	public String getNewName()
	{
		// No name change associated with LEWP
		return null;
	}

}
