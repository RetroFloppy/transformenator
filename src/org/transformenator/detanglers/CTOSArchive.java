package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.transformenator.internal.FileInterpreter;

public class CTOSArchive extends ADetangler
{
	public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix)
	{
		int indexSearch;
		byte[] fileIndex = new byte[4];
		/*
		 * We have a disk's worth of data. Start hunting for
		 * start-of-file indices at the beginning of the disk.
		 */
		for (indexSearch = 5; indexSearch < inData.length - 7; indexSearch++)
		{
			if (/* (inData[indexSearch + 0] == 0x00) && */
			(inData[indexSearch + 1] == 0x0c) && (inData[indexSearch + 2] == 0x01) && (inData[indexSearch + 3] == 0x10))
			{
				// System.out.println("Found a file index at "+indexSearch+" ...");
				// byte 12 is the length of the filename
				boolean stillFinding = true;
				ByteArrayOutputStream out;
				String fn = pascalString(inData,indexSearch + 12,true);
				if (fn.trim().length() > 0)
				{
					try
					{
						out = new ByteArrayOutputStream();

						fileIndex[0] = inData[indexSearch + 4];
						fileIndex[1] = inData[indexSearch + 5];
						fileIndex[2] = inData[indexSearch + 6];
						fileIndex[3] = inData[indexSearch + 7];
						do
						{
							/*
							 * Search the entire disk for file segments from
							 * the beginning for each one, since they aren't
							 * necessarily on disk in order. This of course
							 * could be sped/smartened up by finding them
							 * all in order with one pass and keeping a
							 * table.
							 */
							stillFinding = false;
							fileIndex[2]++;
							if (fileIndex[2] == 0)
								fileIndex[3]++;
							for (int segmentSearch = 5; segmentSearch < inData.length - 7; segmentSearch++)
							{
								if ((inData[segmentSearch + 1] == 0x0c) && (inData[segmentSearch + 2] == 0x02) && (inData[segmentSearch + 3] == 0x10) && (inData[segmentSearch + 4] == fileIndex[0]) && (inData[segmentSearch + 5] == fileIndex[1]) && (inData[segmentSearch + 6] == fileIndex[2]) && (inData[segmentSearch + 7] == fileIndex[3]))
								{
									// System.out.println("Found a segment at "+segmentSearch);
									byte range[] = Arrays.copyOfRange(inData, segmentSearch + 8, segmentSearch + 8 + 512);
									out.write(range);
									stillFinding = true;
								}
							}
							//if (stillFinding == false)
							//	System.out.println("Stopped finding indexes at "+(fileIndex[2]+(fileIndex[3]*256)));
						} while (stillFinding);
						out.flush();
						parent.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fn);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	String pascalString(byte[] inData, int offset, boolean sanitize)
	{
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < inData[offset]; i++)
		{
			char c = (char)inData[offset + i + 1];
			if (sanitize)
			{
				switch (c)
				{
					case '<':
					case '>':
					case '/':
					case '\\':
					case '`':
					case '*': c = '_'; break;
					default: break;
				}
				if ((c > 127) || (c < 32)) c = '_';
			}
			s.append(c);
		}
		return s.toString();
	}

}
