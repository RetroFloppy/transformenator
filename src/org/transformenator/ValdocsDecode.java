package org.transformenator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedInputStream;

import java.io.InputStream;

public class ValdocsDecode
{

	public static void main(java.lang.String[] args)
	{
		if (args.length == 1)
		{
			byte[] inData = null;
			if (args.length > 0)
			{
				System.err.println("Reading input file " + args[0]);
				File file = new File(args[0]);
				byte[] result = new byte[(int) file.length()];
				try
				{
					InputStream input = null;
					try
					{
						int totalBytesRead = 0;
						input = new BufferedInputStream(new FileInputStream(file));
						while (totalBytesRead < result.length)
						{
							int bytesRemaining = result.length - totalBytesRead;
							// input.read() returns -1, 0, or more :
							int bytesRead = input.read(result,totalBytesRead,bytesRemaining);
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
					System.err.println("Input file "+file+" not found.");
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
			else
			{
				help();
			}
			if (inData != null)
			{
				// Figure out the original file name
				byte[] name = new byte[110];
				for (int i = 0;i<110;i++)
				{
					name[i] = inData[i+4];
				}
				String s1 = new String(name).trim()+".val";
				System.err.println("Filename: ["+s1+"]");
				// Pick apart the file hunk indices
				for (int i = 0x802;i<0x90f;i+=2)
				{
					int idx = (int)inData[i]+(int)inData[i+1]*256;
					if (idx > 0)
					{
						// System.err.print(" "+idx);
						for (int j = 4;j<0x200;j++)
						{
							// Each hunk starts with a header of 0x00002020, so skip first 4 bytes.
							System.out.write(inData[(idx * 512) + j]);
						}
					}
				}
				// System.err.println();
			}
		}
		else
		{
			// No args
			help();
		}
	}

	public static void help()
	{
		System.err.println("Syntax: Valdocs in.foo > out.foo");
	}

}