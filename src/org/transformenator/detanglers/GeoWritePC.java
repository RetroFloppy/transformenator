/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2018 by David Schmidt
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

import java.util.ArrayList;
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class GeoWritePC extends ADetangler
{
	@Override
	public void detangle(FileInterpreter interpreter, byte[] inData, String outDirectory, String inFile, String fileSuffix) {

		/*
		 * GeoWrite PC/Canon Starwriter 5000/7000
		 */

		byte[] newBuf = new byte[inData.length];
		byte fileIdentifier[] = { -57, 0x45, -63, 0x53 }; // Identifier bytes: 0xc745c153
		byte textIdentifier[] = { 0x01, 0x00, 0x00, 0x00, 0x08 }; // Text is coming
		byte indexIdentifier[] = { 0x08, 0x00, 0x00, 0x00, 0x08, 0x00, -1, -1, -1, -1 }; // Index is coming

		ArrayList<ChunkIndex> chunkIndices = new ArrayList<ChunkIndex>();
		ArrayList<ChunkIndex> localChunkIndices = new ArrayList<ChunkIndex>();
		ArrayList<Chunk> myChunks = new ArrayList<Chunk>();

		int length, newBufCursor = 0;
		if (Arrays.equals(Arrays.copyOfRange(inData, 0, fileIdentifier.length), fileIdentifier))
		{
			int i;
			// Pick out a new file name - from byte 4 until the first 0x00
			for (i = 4; i < 40; i++)
			{
				if (inData[i] == 0x00)
				{
					newName = new String(Arrays.copyOfRange(inData, 4, i)).trim();
					break;
				}
			}
			int priorOffset = -1;
			int indexTableLength = 0;
			ChunkIndex ci = null;
			Chunk c = null;
			boolean badTable = false;
			// Search for text chunk index tables
			for (i = 32; i < inData.length - 32; i ++)
			{
				if ((Arrays.equals(Arrays.copyOfRange(inData, i, i + indexIdentifier.length), indexIdentifier)) &&
						(UnsignedByte.intValue(inData[i-14]) == 0xc8))
				{
					badTable = false;
					// System.err.println("DEBUG: Text chunk index found at file offset: 0x" + Integer.toHexString(i));
					indexTableLength = -1 + UnsignedByte.intValue(inData[i -2]) + 256 * (UnsignedByte.intValue(inData[i - 1]));
					// System.err.println("DEBUG: Number of index entries: "+(indexTableLength));
					for (int j = 0; j < indexTableLength; j++)
					{
						int prev = 0, next = 0;
						int chunkLoc = i + 20 + (j * 8);
						int chunkId = UnsignedByte.intValue(inData[chunkLoc]) + 256 * (UnsignedByte.intValue(inData[chunkLoc+1]));
						// int chunkTblId = UnsignedByte.intValue(inData[chunkLoc-1]) + 256 * (UnsignedByte.intValue(inData[chunkLoc-2]));
						// The table of text chunks has a "next available location (offset)" count, followed by a chunk identifier. 
						int nextFileOffset = UnsignedByte.intValue(inData[chunkLoc - 6]) + (256 * (UnsignedByte.intValue(inData[chunkLoc-5])) + (65536 * (UnsignedByte.intValue(inData[chunkLoc-4]))));
						// System.err.println("Chunk: 0x"+Integer.toHexString(chunkId) + " tId: 0x"+Integer.toHexString(chunkTblId) + " File offset: 0x" +Integer.toHexString(nextFileOffset) + " len: 0x"+Integer.toHexString(nextFileOffset-priorOffset));
						if ((j > 0) && (indexTableLength > 1))
						{
							// Look up the prior table element
							chunkLoc = i + 20 + ((j-1) * 8);
							prev = UnsignedByte.intValue(inData[chunkLoc]) + 256 * (UnsignedByte.intValue(inData[chunkLoc+1]));
						}
						if (j < indexTableLength - 1)
						{
							// Look up the next table element
							chunkLoc = i + 20 + ((j+1) * 8);
							next = UnsignedByte.intValue(inData[chunkLoc]) + 256 * (UnsignedByte.intValue(inData[chunkLoc+1]));
						}
						ci = new ChunkIndex(chunkId, nextFileOffset-priorOffset, prev, next);
						if (nextFileOffset-priorOffset < 0)
						{
							badTable = true;
							// System.err.println("Bad table detected - rejecting");
							break;
						}
						localChunkIndices.add(ci);
						priorOffset = nextFileOffset;
					}
					if (!badTable)
						chunkIndices = localChunkIndices;
				}
			}
			// Search for text chunks
			for (i = 32; i < inData.length - 8; i ++)
			{
				byte range[] = Arrays.copyOfRange(inData, i, i + textIdentifier.length);
				int pPrev = UnsignedByte.intValue(inData[i - 14]) + 256 * (UnsignedByte.intValue(inData[i - 13]));
				int pNext = UnsignedByte.intValue(inData[i - 16]) + 256 * (UnsignedByte.intValue(inData[i - 15]));
				// int pTid = UnsignedByte.intValue(inData[i - 23]) + 256 * (UnsignedByte.intValue(inData[i - 24]));
				int outerLength = UnsignedByte.intValue(inData[i - 4]) + 256 * (UnsignedByte.intValue(inData[i - 3]));
				length = UnsignedByte.intValue(inData[i - 2]) + 256 * (UnsignedByte.intValue(inData[i - 1]));
				if (Arrays.equals(range, textIdentifier) && length == outerLength - 10)
				{
					/*
					System.err.println("Loc: 0x" + Integer.toHexString(i) + "\tlength 0x" + Integer.toHexString(length) +
							"\tpPrev 0x"+Integer.toHexString(pPrev) +
							"\tpNext 0x"+Integer.toHexString(pNext) +
							"");
					 */
					if (length > 0)
					{
						// Now, check if we have an index with this length in it... reject it if not
						for (int j = 0; j < chunkIndices.size(); j++)
						{
							ci = chunkIndices.get(j);
							if (length == ci.len)
							{
								c = new Chunk(i+5, pPrev, pNext, length);
								myChunks.add(c);
								break;
							}
						}
					}
					i += length; // Move past this text segment
				}
			}
			if (chunkIndices.size() != myChunks.size())
			{
				System.err.println("ERROR: Chunk size mismatch: "+chunkIndices.size()+" != "+myChunks.size());
			}
			// Copy out text, in order of chunk indices
			for (i = 0; i < chunkIndices.size(); i++)
			{
				boolean foundIt = false;
				ci = chunkIndices.get(i);
				// System.err.println("DEBUG: Chunk index "+i+": 0x"+Integer.toHexString(ci.id)+" prev: 0x"+Integer.toHexString(ci.prev)+" next: 0x"+Integer.toHexString(ci.next)+" length: 0x"+Integer.toHexString(ci.len));
				for (int j = 0; j < myChunks.size(); j++)
				{
					c = myChunks.get(j);
					if ((c.next == ci.next) && (c.prev == ci.prev))
					{
						if (c.len != ci.len)
							System.err.println("ERROR: lengths differ.");
						foundIt = true;
						// System.err.println("DEBUG: Chunk loc: 0x"+Integer.toHexString(c.loc)+" len: 0x"+Integer.toHexString(c.len));
						for (int k = 0; k < (c.len); k++)
							newBuf[newBufCursor++] = inData[c.loc + 1 + k];
						break;
					}
				}
				if (foundIt == false)
					System.err.println("ERROR: Text chunk missing.");
			}
			inData = Arrays.copyOfRange(newBuf, 0, newBufCursor);
		}
		else
			System.err.println("ERROR: Not a GeoWritePC or Starwriter 5000/7000 file.");
		interpreter.emitFile(inData, outDirectory, "", newName + fileSuffix);
	}

	/*
	 * Struct to hold ordering indices.  An index knows what its current ID is, and by virtue
	 * of its position in the index table it knows what the previous and next neighbors are.
	 */
	public class ChunkIndex {
		int id;
		int next;
		int prev;
		int len;
		ChunkIndex(int newId, int newLen, int newPrev, int newNext)
		{
			id = newId;
			len = newLen;
			prev = newPrev;
			next = newNext;
		}
	}

	/*
	 * Struct to hold info about a text chunk.  A text chunk knows what it's previous
	 * and next neighbors are, but it doesn't know what its own ID is(!).  So we need
	 * to combine information about it along with its position in the index to figure
	 * out who is who, and what order they go in. 
	 */
	public class Chunk {
		int loc;
		int prev;
		int next;
		int len;
		Chunk(int newLoc, int newPrev, int newNext, int newLen)
		{
			loc = newLoc;
			prev = newPrev;
			next = newNext;
			len = newLen;
		}
	}
	String newName = null;
}