/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2012 - 2013 by David Schmidt
 * david__schmidt at users.sourceforge.net
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

package org.transformenator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.transformenator.RegSpec;

public class Transformenator
{

	public static void main(java.lang.String[] args)
	{
		boolean rc = false;
		if (args.length > 1)
		{
			try
			{
				if ("-t".equalsIgnoreCase(args[0]))
				{
					rc = readTransform(args);
				}
				else
				{
					help();
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			if (rc == true)
			{
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
				byte[] inb = new byte[10240];
				byte[] inData = null;
				int byteCount = 0;
				if (args.length > 2)
				{
					// System.err.println("Reading input file " + args[2]);
					File file = new File(args[2]);
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
								int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
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
						System.err.println("Input file \"" + file + "\" not found.");
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
					}
				}
				else
				{
					try
					{
						while ((byteCount = System.in.read(inb)) > 0)
						{
							buf.write(inb, 0, byteCount);
							buf.close();
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					inData = buf.toByteArray();
				}
				int bytesForward = 0;
				if (inData != null)
				{
					// System.err.println("Incoming data length: "+inData.length);
					if (args[1].toUpperCase().contains("VALDOCS"))
					{
						// If they are using a Valdocs transform, let's pick
						// apart the file first.
						// System.err.println("De-indexing a valdocs file.");
						// Figure out the original file name
						char[] name = new char[110];
						byte[] newBuf = new byte[inData.length];
						int newBufCursor = 0;
						for (int i = 0; i < 110; i++)
						{
							name[i] = (char) inData[i + 4];
						}
						String s1 = new String(name).trim();
						System.err.println("Original filename: " + s1);
						// Pick apart the file hunk indices
						for (int i = 0x802; i < 0x90f; i += 2)
						{
							int idx = (int) inData[i] + (int) inData[i + 1] * 256;
							if (idx > 0)
							{
								// System.err.print(" "+idx);
								for (int j = 4; j < 0x200; j++)
								{
									// Each hunk starts with a header of
									// 0x00002020, so skip first 4 bytes.
									newBuf[newBufCursor++] = inData[(idx * 512) + j];
								}
							}
						}
						inData = new byte[newBufCursor];
						for (int i = 0; i < newBufCursor; i++)
							inData[i] = newBuf[i];
						// System.err.println("Data length after de-indexing: "+inData.length);
					}
					// System.err.println("Trimming leading "+trimLeading+" bytes.");
					for (int i = trimLeading; i < inData.length; i++)
					{
						bytesForward = evaluateTransforms(inData, outBuf, i, inData.length);
						// System.err.println("i=" + i + "; bytesForward=" +
						// bytesForward);
						if (bytesForward > 0)
							i = i + bytesForward - 1;
					}
					if (preamble != null)
					{
						System.out.write(preamble.getBytes(), 0, preamble.length());
					}
					String tempStr = outBuf.toString();
					for (int i = 0; i < regReplace.size(); i++)
					{
						// System.err.println("Replacing ["+regPattern.elementAt(i)+"] with ["+regReplace.elementAt(i)+"].");
						tempStr = tempStr.replaceAll(regPattern.elementAt(i), regReplace.elementAt(i));
					}
					byte[] stdout = tempStr.getBytes();
					System.out.write(stdout, 0, stdout.length);
					if (postamble != null)
					{
						System.out.write(postamble.getBytes(), 0, postamble.length());
					}
				}
			}
		}
		else
		{
			// No args
			help();
		}
	}

	public static int evaluateTransforms(byte[] incoming, ByteArrayOutputStream outBuf, int offset, int max)
	{
		int i = 0;
		int bytesMatched = 0;
		int currLeftLength = 0;
		boolean match = false;
		for (i = 0; i < leftSide.size(); i++) // For each left specification
		{
			RegSpec currentSpec = leftSide.elementAt(i);
			currLeftLength = currentSpec.leftCompare.length;
			if (offset + currLeftLength > max)
				continue;
			byte[] compLeft = leftSide.elementAt(i).leftCompare;
			byte[] maskLeft = leftSide.elementAt(i).leftMask;
			boolean care = true;
			byte[] replRight = rightSide.elementAt(i);
			match = true;
			for (int j = 0; j < compLeft.length; j++)
			{
				if (maskLeft[j] == 1)
				{
					// System.err.println("Don't care byte - it will match");
					care = false;
				}
				else
				{
					// System.err.println("Comparing left byte "+compLeft[j]+" to right byte "+incoming[offset
					// + j]);
					care = true;
				}
				if (((compLeft[j]) != incoming[offset + j]) && (care == true))
				{
					match = false;
				}
			}
			if (match == true)
			{
				// System.err.println("Found a match at offset "+offset);
				try
				{
					// send out new data
					if (replRight != null)
					{
						outBuf.write(replRight);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				bytesMatched = currLeftLength;
				break;
			}
		}
		if (bytesMatched == 0)
		{
			// System.err.println("Writing out original byte, no comparisons to make. location="+location);
			outBuf.write(incoming[offset]);
			bytesMatched = 1;
		}
		return bytesMatched;
	}

	public static boolean readTransform(String[] args)
	{
		boolean isOK = true;
		FileReader fr = null;
		if (args.length < 2)
		{
			isOK = false;
		}
		else
		{
			// First try to load an external transform file. That should take
			// precedence over an internal one.
			try
			{
				fr = new FileReader(args[1]);
				if (fr != null)
				{
					isOK = true;
					parseTransforms(fr);
					System.err.println("Using external transform file \"" + args[1] + "\".");
				}
			}
			catch (FileNotFoundException ex)
			{
				isOK = false;
			}
			if (isOK == false)
			{
				// Didn't find an external one; how about that same
				// specification as an internal one?
				try
				{
					InputStream is;
					is = Transformenator.class.getResourceAsStream("/org/transformenator/transforms/" + args[1]);
					if (is != null)
					{
						InputStreamReader isr = new InputStreamReader(is);
						parseTransforms(isr);
						System.err.println("Using internal transform file \"" + args[1] + "\".");
						isOK = true;
					}
					else
						isOK = false;
				}
				catch (Exception e)
				{
					isOK = false;
				}
			}
		}
		if (isOK == false)
			System.err.println("Unable to locate transform file named \"" + args[1] + "\".");
		return isOK;
	}

	public static void help()
	{
		System.err.println();
		System.err.println("Transformenator v1.0 - perform transformation operations on binary files.");
		System.err.println();
		System.err.println("Syntax: Transformenator -t transformFile [<] in.foo > out.foo");
		listInternalTransforms();
	}

	public static void listInternalTransforms()
	{
		boolean printedHeaderYet = false;
		CodeSource src = Transformenator.class.getProtectionDomain().getCodeSource();

		if (src != null)
		{
			URL jar = src.getLocation();
			ZipInputStream zip;
			try
			{
				zip = new ZipInputStream(jar.openStream());
				ZipEntry ze = null;

				while ((ze = zip.getNextEntry()) != null)
				{
					String entryName = ze.getName();
					String prefix = "org/transformenator/transforms/";
					if (entryName.startsWith(prefix))
					{
						String finalName = entryName.substring(prefix.length());
						if (finalName.length() > 0)
						{
							if (printedHeaderYet == false)
							{
								System.err.println("Available internal transform files:");
								printedHeaderYet = true;
							}
							System.err.println("  " + finalName);
						}
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void parseTransforms(Reader fr)
	{
		String line;
		try
		{
			BufferedReader br = new BufferedReader(fr);
			StringTokenizer st;// = new StringTokenizer("=");
			while ((line = br.readLine()) != null)
			{
				boolean addedLeft = false;
				boolean addedRight = false;
				boolean skip = false;
				String[] result = line.split("=");
				String leftTemp = "";
				String rightTemp = "";
				byte[] rightBytes = null;
				st = new StringTokenizer(result[0]);
				if (st.hasMoreTokens())
				{
					leftTemp = st.nextToken();
					skip = false;
					// System.err.println("Left side token: ["+leftTemp+"]");
					if (leftTemp.equals("head") || leftTemp.equals("tail") || leftTemp.equals("trim_leading") || leftTemp.trim().charAt(0) == (';'))
					{
						if (leftTemp.trim().charAt(0) == (';'))
						{
							skip = true;
						}
					}
					else if (leftTemp.equals("regex"))
					{
						// System.err.println("Left side token: ["+leftTemp+"]");
					}
					else
					{
						RegSpec newRegSpec = new RegSpec();
						newRegSpec.leftCompare = asBytes(leftTemp);
						newRegSpec.leftMask = maskBytes(leftTemp);
						// System.err.println("Left bytes length: " +
						// leftBytes.length);
						leftSide.add(newRegSpec);
						addedLeft = true;
					}
				}
				if ((result.length > 1) && !skip)
				{
					st = new StringTokenizer(result[1]);
					if (st.hasMoreTokens())
					{
						rightTemp = "";
						while (st.hasMoreTokens())
						{
							rightTemp = rightTemp + st.nextToken() + " ";
						}
						if (rightTemp.trim().charAt(0) == '"')
						{
							String newString = "";
							// System.err.println("Found a string...");
							rightTemp.trim();
							rightTemp = rightTemp.substring(1, rightTemp.length() - 2);
							newString = rightTemp.replace("\\\\r", "\r").replace("\\\\n", "\n");
							rightBytes = newString.getBytes();
						}
						else if (leftTemp.equals("regex"))
						{
							String delim = rightTemp.substring(0, 1);
							// System.err.println("Regex replacement: "+rightTemp+" Delimiter: "+delim);
							String[] rxTokens = rightTemp.split(delim);
							if (rxTokens.length > 1)
							{
								regPattern.add(rxTokens[1]);
								// System.err.println("Regex replacement token 1: "+rxTokens[1]);
							}
							if (rxTokens.length > 2)
							{
								regReplace.add(rxTokens[2]);
								// System.err.println("Regex replacement token 2: "+rxTokens[2]);
							}
							else
							{
								regReplace.add("");
								// System.err.println("Regex replacement token 2 is blank.");
							}
						}
						else
						{
							rightBytes = asBytes(rightTemp.trim());
						}

						// System.err.println("Right side token: ["+rightTemp+"]");
						if (leftTemp.equals("head"))
						{
							preamble = rightTemp;
						}
						else if (leftTemp.equals("tail"))
						{
							postamble = rightTemp;
						}
						else if (leftTemp.equals("regex"))
						{
						}
						else if (leftTemp.equals("trim_leading"))
						{
							trimLeading = fromByteArray(rightBytes);
						}
						else
						{
							rightSide.add(rightBytes);
							addedRight = true;
						}
					}
				}
				if (addedLeft & !addedRight)
				{
					// System.err.println("Right side token is null.");
					rightSide.add(null);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static byte[] asBytes(String str)
	{
		if ((str.length() % 2) == 1)
			str = "0" + str; // pad leading 0 if needed
		byte[] buf = new byte[str.length() / 2];
		int i = 0;

		for (char c : str.toCharArray())
		{
			try
			{
				byte b = Byte.parseByte(String.valueOf(c), 16);
				buf[i / 2] |= (b << (((i % 2) == 0) ? 4 : 0));
			}
			catch (java.lang.NumberFormatException ex)
			{
				buf[i / 2] = 0; // Going to need a "don't care" here, probably
			}
			i++;
		}

		return buf;
	}

	public static byte[] maskBytes(String str)
	{
		if ((str.length() % 2) == 1)
			str = "0" + str; // pad leading 0 if needed
		byte[] buf = new byte[str.length() / 2];
		int i = 0;
		for (char c : str.toCharArray())
		{
			try
			{
				// Try to parse as a byte... if it fails, we know we have an
				// ignorable byte
				Byte.parseByte(String.valueOf(c), 16);
				buf[i / 2] = 0;
			}
			catch (NumberFormatException ex)
			{
				buf[i / 2] = 1;
				// System.err.println("Ignoring byte at position "+i/2);
			}
			i++;
		}
		return buf;
	}

	static int fromByteArray(byte[] digits)
	{
		// Thunk everything to six hex digits
		int result = 0;
		byte bytes[] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		int len = digits.length;
		int j = 0;
		// Right-justify digits
		for (int i = len - 1; i > -1; i--)
		{
			bytes[j] = digits[i];
			j++;
		}
		result = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) * 256 | (bytes[2] & 0xFF) * 512 | (bytes[3] & 0xFF) * 1024 | (bytes[4] & 0xFF) * 2048 | (bytes[5] & 0xFF) * 4096;
		return result;
	}

	static Vector<String> regPattern = new Vector<String>();
	static Vector<String> regReplace = new Vector<String>();
	static Vector<RegSpec> leftSide = new Vector<RegSpec>();
	static Vector<byte[]> rightSide = new Vector<byte[]>();
	static String preamble;
	static String postamble;
	static int trimLeading;

}