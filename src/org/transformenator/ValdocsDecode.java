package org.transformenator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;

import java.io.InputStream;
import java.util.Arrays;

public class ValdocsDecode
{

	public static void main(java.lang.String[] args)
	{
		boolean rc = false;
		if (args.length == 1)
		{
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
			byte[] inb = new byte[10240];
			byte[] inData = null;
			int byteCount = 0;
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

	public static byte[] asBytes(String str)
	{
		if ((str.length() % 2) == 1)
			str = "0" + str; // pad leading 0 if needed
		byte[] buf = new byte[str.length() / 2];
		int i = 0;

		for (char c : str.toCharArray())
		{
			byte b = Byte.parseByte(String.valueOf(c), 16);
			buf[i / 2] |= (b << (((i % 2) == 0) ? 4 : 0));
			i++;
		}

		return buf;
	}

	static int fromByteArray(byte[] digits)
	{
		// Thunk everything to six hex digits
		int result = 0;
		byte bytes[] = { 0x00,0x00,0x00,0x00,0x00,0x00 };
		int len = digits.length;
		int j = 0;
		// Right-justify digits
		for (int i = len-1; i > -1; i--)
		{
			bytes[j] = digits[i];
			j++;
		}
		result = (bytes[0] & 0xFF)
				| (bytes[1] & 0xFF) * 256
				| (bytes[2] & 0xFF) * 512
				| (bytes[3] & 0xFF) * 1024
				| (bytes[4] & 0xFF) * 2048
				| (bytes[5] & 0xFF) * 4096;
		return result;
	}

}