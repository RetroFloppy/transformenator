package org.transformenator.detanglers;

import org.transformenator.internal.UnsignedByte;

public class JustWrite extends ADetangler
{

	@Override
	public byte[] detangle(byte[] inData)
	{
		// Re-assemble the file based on index before starting
		/*
		 * Pull out the file chain indices. Indices start at 0x010A.
		 * 
		 * Each index is a pointer to a chain of sectors of 4096 bytes each, starting at
		 * n*1024+512.
		 */
		byte[] newBuf = new byte[inData.length];
		int newBufCursor = 0;
		int nextBlock = 0;
		for (int i = 0x10a; i < 0x200; i += 2)
		{
			int block = UnsignedByte.intValue(inData[i], inData[i + 1]);
			int index = block * 1024;
			if (block != 65535)
			{
				nextBlock = UnsignedByte.intValue(inData[index + 1022], inData[index + 1023]);
				/*
				 * System.err.println("DEBUG: Chain head: 0x"+Integer.toHexString(block)
				 * +" at file offset: 0x"+Integer.toHexString(index)+" to 0x"+Integer.
				 * toHexString(index+1023)
				 * +" next block ptr: 0x"+Integer.toHexString(nextBlock));
				 */
			}
			if (block == 0)
				break;
			if (block < 65535)
			{
				if (index + 1023 < inData.length)
				{
					// System.err.println("Trailing byte:
					// "+UnsignedByte.intValue(inData[index+1023]));
					int j;
					// Find out where the block really ends - remove trailing zeroes
					if (UnsignedByte.intValue(inData[index + 1023]) == 0xff)
					{
						for (j = index + 1021; j >= index; j--)
							if (inData[j] == 0x1f)
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
					{
						// The whole block is in use (except the last two)
						for (int k = 0; k < 1022; k++)
						{
							newBuf[newBufCursor++] = inData[index + k];
						}
					}
				}
				nextBlock = UnsignedByte.intValue(inData[index + 1022], inData[index + 1023]);
				while (nextBlock < 65535)
				{
					// System.err.println("DEBUG moar blocks in this chain...");
					int bl = nextBlock;
					if (bl < 65535)
					{
						index = bl * 1024;
						if (index + 1023 < inData.length)
						{
							int j;
							// Find out where the block really ends - remove trailing zeroes
							if (UnsignedByte.intValue(inData[index + 1023]) == 0xff)
							{
								for (j = index + 1021; j >= index; j--)
									if (inData[j] == 0x1f)
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
							{
								// The whole block is in use (except the last two)
								for (int k = 0; k < 1022; k++)
								{
									newBuf[newBufCursor++] = inData[index + k];
								}
							}
						}
						nextBlock = UnsignedByte.intValue(inData[index + 1022], inData[index + 1023]);
					}
				}
			}
		}
		inData = new byte[newBufCursor];
		for (int i = 0; i < newBufCursor; i++)
			inData[i] = newBuf[i];
		// System.err.println("DEBUG: Data length after de-indexing: "+inData.length);
		return inData;
	}

	@Override
	public String getNewName()
	{
		return null;
	}

}
