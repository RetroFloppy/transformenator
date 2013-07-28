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
import java.io.FileOutputStream;
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

	public static void help()
	{
		System.err.println();
		System.err.println("Transformenator v1.5 - perform transformation operations on binary files.");
		System.err.println();
		System.err.println("Syntax: Transformenator [transform] [infile] [outfile]");
		System.err.println();
		System.err.println("  See http://transformenator.sourceforge.net/ for transform file specification.");
		System.err.println("  If using a valdocs transform, outfile specifies an output directory.");
		listInternalTransforms();
	}

	public static void main(java.lang.String[] args)
	{
		boolean rc = false;
		foundSOF = false;
		String valdocsName = null;
		if (args.length == 3)
		{
			try
			{
				rc = readTransform(args[0]);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			if (rc == true)
			{
				ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
				inData = null;
				// System.err.println("Reading input file " + args[1]);
				File file = new File(args[1]);
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
				int bytesForward = 0;
				if (inData != null)
				{
					// System.err.println("Incoming data length: "+inData.length);
					if (args[0].toUpperCase().contains("VALDOCS"))
					{
						// If they are using a Valdocs transform, let's pick apart the file first.
						System.err.println("De-indexing valdocs file " + file + ".");
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
						valdocsName = new String(name).trim();
						// System.err.println("Found file: \"" + valdocsName+"\"");
						/*
						 * Pick apart the file hunk indices.  The first few indices seem to be non-useful... 
						 * so start in at 0x80a.  It's unclear how deep the indices can go.  
						 * It's possible it should look deeper than it does, but the field of 
						 * 0xFFs has some noise near the end.
						 * 
						 * Each index is a pointer to the next 512 bytes (a sector) of data in the file.
						 */
						for (int i = 0x80a; i < 0xa00; i += 2)
						{
							int idx = UnsignedByte.intValue(inData[i],inData[i + 1]);
							if (idx < 32768)
							{
								// System.err.println("idx: "+idx);
								if (((idx*512) + 1) < inData.length)
								{
									// Chunks may start with a pointer to skip over blank space
									int offset = UnsignedByte.intValue(inData[(idx * 512)],inData[(idx * 512) + 1]);
									// Pull out the data in the chunk
									for (int j = offset + 4; j < 0x200; j++)
									{
										newBuf[newBufCursor++] = inData[(idx * 512) + j];
									}
								}
								// else
									// System.err.println("Found an index out of bounds: "+idx);
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
						backupBytes = 0;
						bytesForward = evaluateTransforms(outBuf, i, inData.length);
						// System.err.println("i=" + i + "; bytesForward=" + bytesForward+"; backupBytes="+backupBytes);
						if (bytesForward > 0)
							i = i + bytesForward - 1;
						if (backupBytes > 0)
							i = i - backupBytes;
						if (bytesForward == -1)
							// EOF reached
							break;
					}
					try
					{
						String outfile = args[2];
						if (valdocsName != null)
						{
							outfile = outfile + File.separator + valdocsName + ".txt";
							System.err.println("Creating file: \"" + outfile+"\"");
						}
						FileOutputStream out = new FileOutputStream(outfile);
						if (prefix != null)
						{
							out.write(prefix.getBytes(), 0, prefix.length());
						}
						String tempStr = outBuf.toString();
						for (int i = 0; i < regReplace.size(); i++)
						{
							// System.err.println("DEBUG Replacing ["+regPattern.elementAt(i)+"] with ["+regReplace.elementAt(i)+"].");
							tempStr = tempStr.replaceAll(regPattern.elementAt(i), regReplace.elementAt(i));
						}
						byte[] stdout = tempStr.getBytes();
						out.write(stdout, 0, stdout.length);
						if (suffix != null)
						{
							out.write(suffix.getBytes(), 0, suffix.length());
						}
						out.flush();
						out.close();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
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

	public static int evaluateTransforms(ByteArrayOutputStream outBuf, int offset, int max)
	{
		int i = 0, k = 0;
		int bytesMatched = 0;
		int currLeftLength = 0;
		boolean match = false;
		backupBytes = 0;
		for (i = 0; i < leftSide.size(); i++) // For each left specification
		{
			RegSpec currentSpec = leftSide.elementAt(i);
			currLeftLength = currentSpec.leftCompare.length;
			if (offset + currLeftLength > max)
				continue;
			byte[] compLeft = leftSide.elementAt(i).leftCompare;
			byte[] maskLeft = leftSide.elementAt(i).leftMask;
			byte[] replRight = rightSide.elementAt(i);
			byte[] replRightToggle = rightToggle.elementAt(i);
			match = true;
			for (int j = 0; j < compLeft.length; j++)
			{
				if (((compLeft[j]) != inData[offset + j]) && (maskLeft[j] == 0))
				{
					// If the byte doesn't match and there's no mask...
					// Then it's not a match.
					match = false;
				}
				else if ((inData[offset + j] == 0) && (maskLeft[j] == 2))
				{
					// System.err.println("Found a non-zero byte at "+j);								
					match = false;
				}
			}
			if (match == true)
			{
				// System.err.println("Found a match at offset "+offset+"; left length = "+currLeftLength+" command: "+currentSpec.command+" backtrack: "+currentSpec.backtrack);
				if (currentSpec.command == 0)
				{
					try
					{
						// send out new data
						if ((replRight != null) && (currentSpec.backtrack == true))
						{
							// Push the replacement back onto incoming
							int calc = offset + compLeft.length - replRight.length;
							// System.err.println("calc: "+calc+" offset: "+offset);								
							if (calc < 0)
							{
								// System.err.println("calc: "+calc+" offset: "+offset);								
								int bump = Math.abs(calc);
								byte newInData[] = new byte[inData.length - calc];
								for (int q = 0; q < inData.length; q++)
								{
									newInData[q+bump] = inData[q];
								}
								inData = newInData;
								offset += replRight.length - compLeft.length;
							}
							for (k = 0; k < replRight.length; k++)
							{
								// System.err.println("Pushing byte: "+k);								
								inData[offset + compLeft.length - replRight.length + k] = replRight[k];
							}
							backupBytes = replRight.length;
							if (calc < 0)
							{
								backupBytes += calc;
							}
							// System.err.println("Backing up "+replRight.length+" bytes.");
						}
						else if ((replRight != null) && (currentSpec.backtrack == false))
						{
							if (currentSpec.toggle == true)
							{
								if (currentSpec.toggleState)
									outBuf.write(replRight);
								else
									outBuf.write(replRightToggle);
								currentSpec.toggleState = !currentSpec.toggleState;
							}
							else
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
				else if (currentSpec.command == 1)
				{
					// EOF reached
					bytesMatched = -1;
					break;
				}
				else if (currentSpec.command == 2)
				{
					// SOF reached
					bytesMatched = currLeftLength;
					if (foundSOF == false)
						outBuf.reset();
					foundSOF = true;
					break;
				}
				else if (currentSpec.command == 3)
				{
					// SOF (greedy) reached
					bytesMatched = currLeftLength;
					outBuf.reset();
					break;
				}
			}
		}
		if (bytesMatched == 0)
		{
			// System.err.println("Writing out original byte, no comparisons to make.");
			outBuf.write(inData[offset]);
			bytesMatched = 1;
		}
		return bytesMatched;
	}

	public static boolean readTransform(String filename)
	{
		boolean isOK = true;
		FileReader fr = null;
		// First try to load an external transform file. That should take
		// precedence over an internal one.
		try
		{
			fr = new FileReader(filename);
			if (fr != null)
			{
				isOK = true;
				parseTransforms(fr);
				System.err.println("Using external transform file \"" + filename + "\".");
			}
		}
		catch (Exception e)
		{
			isOK = false;
		}
		if (isOK == false)
		{
			// Didn't find an external one; how about that same specification as an internal one?
			try
			{
				InputStream is;
				is = Transformenator.class.getResourceAsStream("/org/transformenator/transforms/" + filename);
				if (is != null)
				{
					InputStreamReader isr = new InputStreamReader(is);
					parseTransforms(isr);
					System.err.println("Using internal transform file \"" + filename + "\".");
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
		if (isOK == false)
			System.err.println("Unable to locate transform file named \"" + filename + "\".");
		return isOK;
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
			StringTokenizer st;
			while ((line = br.readLine()) != null)
			{
				RegSpec newRegSpec = new RegSpec();
				boolean addLeft = false;
				boolean addedRight = false;
				boolean skip = false;
				String[] hashSplits = line.split("#");
				String[] equalsSplits = line.split("=");
				String[] toggleSplits = line.split("%");
				String[] result;
				String leftTemp = "";
				String rightTemp1 = "";
				String rightTemp2 = "";
				byte[] rightBytes = null;
				// System.err.println("Splits on '=': "+equalsSplits.length+" splits on '#': "+hashSplits.length+" splits on '%': "+toggleSplits.length+"\n line.indexOf('='): "+line.indexOf("=")+ " line.indexOf('#'): "+line.indexOf("#")+ " line.indexOf('%'): "+line.indexOf("%"));
				if ((line.indexOf("%") > 0 && line.indexOf("=") < 0 && line.indexOf("#") < 0) || 
					(line.indexOf("%") > 0 && (line.indexOf("%") < line.indexOf("=")) && (line.indexOf("%") < line.indexOf("#"))))
				{
					// System.err.println("DEBUG This is a toggle production.");
					st = new StringTokenizer(toggleSplits[0]);
					result = toggleSplits;
					newRegSpec.backtrack = false;					
					newRegSpec.toggle = true;					
				}
				else if ((line.indexOf("=") > 0 && line.indexOf("%") < 0 && line.indexOf("#") < 0) || 
					(line.indexOf("=") > 0 && (line.indexOf("=") < line.indexOf("%")) && (line.indexOf("=") < line.indexOf("#"))))
				{
					// System.err.println("DEBUG This is an equals production.");
					st = new StringTokenizer(equalsSplits[0]);
					result = equalsSplits;
					newRegSpec.backtrack = false;
					newRegSpec.toggle = false;					
				}
				else if ((line.indexOf("#") > 0 && line.indexOf("=") < 0 && line.indexOf("%") < 0) || 
					(line.indexOf("#") > 0 && (line.indexOf("#") < line.indexOf("=")) && (line.indexOf("#") < line.indexOf("%"))))
						
				{
					// System.err.println("DEBUG This is a hash production.");
					st = new StringTokenizer(hashSplits[0]);
					result = hashSplits;
					newRegSpec.backtrack = true;
					newRegSpec.toggle = false;					
				}
				else
				{
					// System.err.println("DEBUG Don't know what this is.");
					st = new StringTokenizer(equalsSplits[0]);
					result = equalsSplits;
					newRegSpec.backtrack = false;
					newRegSpec.toggle = false;					
				}
				if (st != null && st.hasMoreTokens())
				{
					leftTemp = st.nextToken();
					skip = false;
					// System.err.println("DEBUG Left side token: ["+leftTemp+"]");
					if (leftTemp.equals("head") || leftTemp.equals("tail") || leftTemp.equals("trim_leading") || leftTemp.trim().charAt(0) == (';'))
					{
						if (leftTemp.trim().charAt(0) == (';'))
						{
							skip = true;
						}
					}
					else if (leftTemp.equals("regex"))
					{
						// System.err.println("DEBUG Left side token: ["+leftTemp+"]");
					}
					else if ((leftTemp.trim().startsWith("[")) && leftTemp.trim().endsWith("]") && leftTemp.trim().length() == 8)
					{
						// We have a translation (i.e. [41..5a] = 61)
						addLeft = false;
						skip = true;
						// Ok, we have opening and closing braces.  Check for two digits.
						// String firstByte, endByte;
						byte firstByte, endByte;
						int firstInt, endInt;
						int anchorByte = 0;
						firstByte = asByte(leftTemp.substring(1, 3));
						endByte = asByte(leftTemp.substring(5, 7));
						firstInt = UnsignedByte.intValue(firstByte);
						endInt = UnsignedByte.intValue(endByte);
						if (endInt > firstInt)
						{
							// System.err.println("We have a range: 0x"+UnsignedByte.toString(firstByte)+" through 0x"+UnsignedByte.toString(endInt));
							// Take care of adding specs right here...
							if (result.length > 1)
							{
								st = new StringTokenizer(result[1]);
								if (st.hasMoreTokens())
								{
									// Add a pile of left sides with incrementing right sides
									rightTemp1 = st.nextToken();
									anchorByte = asByte(rightTemp1);
									// System.err.println("Right anchor: 0x"+UnsignedByte.toString(anchorByte));
									for (int i = firstByte; i <= endByte; i++)
									{
										newRegSpec.leftCompare = asBytes(UnsignedByte.loByte(i));
										// No mask is possible with ranges
										newRegSpec.leftMask = asBytes(0);
										leftSide.add(newRegSpec);
										// System.err.println("Adding spec 0x"+UnsignedByte.toString(UnsignedByte.loByte(i))+" = 0x"+UnsignedByte.toString(anchorByte)+" (decimal "+anchorByte+")");
										byte b[] = new byte[1];
										b[0] = UnsignedByte.loByte(anchorByte);
										rightSide.add(b);
										rightToggle.add(null); // Keep up with the toggle side
										boolean backtrack = newRegSpec.backtrack;
										newRegSpec = new RegSpec();
										newRegSpec.backtrack = backtrack;
										anchorByte++;
									}
								}
							}
							else
							{
								// Add a pile of left sides with null right sides
								for (int i = firstByte; i <= endByte; i++)
								{
									newRegSpec.leftCompare = asBytes(i);
									// No mask is possible with ranges
									newRegSpec.leftMask = asBytes(0);
									leftSide.add(newRegSpec);
									// System.err.println("Adding null spec 0x"+UnsignedByte.toString(UnsignedByte.loByte(i)));
									// System.err.println("Right side token is null.");
									rightSide.add(null);
									rightToggle.add(null); // Keep up with the toggle side
									boolean backtrack = newRegSpec.backtrack;
									newRegSpec = new RegSpec();
									newRegSpec.backtrack = backtrack;
								}
							}
						}
					}
					else
					{
						// System.err.println("DEBUG leftCompare: ["+leftTemp+"]");
						newRegSpec.leftCompare = asBytes(leftTemp);
						newRegSpec.leftMask = maskBytes(leftTemp);
						addLeft = true;
					}
				}
				if (result != null && (result.length > 1) && !skip)
				{
					if (result == equalsSplits) // We are using '=' as separator
					{
						rightTemp1 = line.substring(line.indexOf("=")+1);
					}
					else if (result == hashSplits) // We are using '#' as separator
					{
						rightTemp1 = line.substring(line.indexOf("#")+1);
					}
					else // We are using '%' as separator
					{
						String rightTemp = line.substring(line.indexOf("%")+1);
						// System.err.println("DEBUG toggling... right side is: "+rightTemp);
						rightTemp1 = rightTemp.substring(1,rightTemp.indexOf(","));
						rightTemp2 = rightTemp.substring(rightTemp.indexOf(",")+1);
						// System.err.println("DEBUG Toggle on : ["+rightTemp1+"]");
						// System.err.println("DEBUG Toggle off: ["+rightTemp2+"]");
					}
					{
						// System.err.println("DEBUG Found a right side string: ["+rightTemp1.trim()+"]");
						if (rightTemp1.trim().equals("\"{@@<FiLe_EoF>@@}\""))
						{
							// System.err.println("Found an EOF specification...");
							// Need to add an EOF command to the left side spec.
							newRegSpec.command = 1;
						}
						else if (rightTemp1.trim().equals("\"{@@<FiLe_SoF>@@}\""))
						{
							// System.err.println("Found an SOF specification...");
							// Need to add an SOF command to the left side spec.
							newRegSpec.command = 2;
						}
						else if (rightTemp1.trim().equals("\"{@@<FiLe_SoF_GrEeDy>@@}\""))
						{
							// System.err.println("Found a greedy SOF specification...");
							// Need to add an SOF command to the left side spec.
							newRegSpec.command = 3;
						}
						else if (rightTemp1.trim().length() > 0 && rightTemp1.trim().charAt(0) == '"')
						{
							String newString = "";
							rightTemp1 = rightTemp1.trim();
							rightTemp1 = rightTemp1.substring(1, rightTemp1.length() - 1);
							newString = rightTemp1.replace("\\\\r", "\r").replace("\\\\n", "\n");
							rightBytes = newString.getBytes();
						}
						else if (leftTemp.equals("regex"))
						{
							String delim = rightTemp1.trim().substring(0, 1);
							// System.err.println("DEBUG Delimeter: "+delim);
							String[] rxTokens = rightTemp1.split(delim);
							if (rxTokens.length > 1)
							{
								regPattern.add(rxTokens[1]);
								// System.err.println("DEBUG Regex replacement token 1: "+rxTokens[1]);
							}
							if (rxTokens.length > 2)
							{
								regReplace.add(rxTokens[2]);
								// System.err.println("Regex replacement token 2: "+rxTokens[2]);
							}
							else
							{
								regReplace.add("");
								// System.err.println("DEBUG Regex replacement token 2 is blank.");
							}
						}
						else
						{
							rightBytes = asBytes(rightTemp1.trim());
						}

						// System.err.println("DEBUG Right side token: ["+rightTemp1+"]");
						if (leftTemp.equals("head"))
						{
							rightTemp1 = result[1];
							for (int j = 2; j < result.length;j++)
							{
								// System.err.println("Token: ["+result[j]+"]");
								rightTemp1 = rightTemp1 + "=" + result[j];
							}
							if (rightTemp1.trim().charAt(0) == '"')
							{
								String newString = "";
								// System.err.println("Found a string...");
								rightTemp1 = rightTemp1.trim();
								rightTemp1 = rightTemp1.substring(1, rightTemp1.length() - 1);
								newString = rightTemp1.replace("\\\\r", "\r").replace("\\\\n", "\n");
								prefix = newString;
							}
							else
								prefix = rightTemp1;
						}
						else if (leftTemp.equals("tail"))
						{
							suffix = rightTemp1;
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
							// Add toggle, if necessary
							if (rightTemp2.trim().length() > 0)
							{
								rightTemp2 = rightTemp2.trim();
								if (rightTemp2.trim().charAt(0) == '"')
								{
									// This production is surrounded by quotes
									// System.err.println("DEBUG Adding toggle ["+rightTemp2+"]");
									String newString = "";
									rightTemp2 = rightTemp2.substring(1, rightTemp2.length() - 1);
									newString = rightTemp2.replace("\\\\r", "\r").replace("\\\\n", "\n");
									rightBytes = newString.getBytes();
									rightToggle.add(rightBytes);
								}
								else
								{
									// No quotes surrounding this production
									rightBytes = asBytes(rightTemp2);
									rightToggle.add(rightBytes);
								}
							}
							else
								rightToggle.add(null);
						}
					}
				}
				if (addLeft)
					leftSide.add(newRegSpec);
				if (addLeft & !addedRight)
				{
					// System.err.println("Right side token is null.");
					rightSide.add(null);
					rightToggle.add(null); // Keep up with the toggle side
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

	public static byte[] asBytes(int val)
	{
		byte[] buf = new byte[1];
		buf[0] = UnsignedByte.loByte(val);
		return buf;
	}

	public static byte asByte(String str)
	{
		if ((str.length() % 2) == 1)
			str = "0" + str; // pad leading 0 if needed
		byte buf = 0;
		int i = 0;
		for (char c : str.toCharArray())
		{
			try
			{
				byte b = Byte.parseByte(String.valueOf(c), 16);
				buf |= (b << (((i % 2) == 0) ? 4 : 0));
			}
			catch (java.lang.NumberFormatException ex)
			{
				buf = 0; // Going to need a "don't care" here, probably
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
				if (c == '!')
				{
					// System.err.println("Byte at position "+i/2+" must be non-zero.");
					buf[i / 2] = 2;
				}
				else
				{
					// System.err.println("Ignoring byte at position "+i/2);
					buf[i / 2] = 1;
				}
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
		result = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) * 256
				| (bytes[2] & 0xFF) * 512 | (bytes[3] & 0xFF) * 1024
				| (bytes[4] & 0xFF) * 2048 | (bytes[5] & 0xFF) * 4096;
		return result;
	}

	static byte inData[] = null;
	static Vector<String> regPattern = new Vector<String>();
	static Vector<String> regReplace = new Vector<String>();
	static Vector<RegSpec> leftSide = new Vector<RegSpec>();
	static Vector<byte[]> rightSide = new Vector<byte[]>();
	static Vector<byte[]> rightToggle = new Vector<byte[]>();
	static String prefix;
	static String suffix;
	static int trimLeading;
	static boolean foundSOF;
	static int backupBytes;

}