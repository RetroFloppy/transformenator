/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2013 - 2018 by David Schmidt
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

package org.transformenator.internal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.transformenator.Transform;
import org.transformenator.detanglers.ADetangler;

public class FileInterpreter
{
	public boolean isOK = false;
	public boolean isInternal = false;

	public FileInterpreter(String transform_name)
	{
		transformName = transform_name;
		isOK = readTransform(transform_name);
	}

	public String describe()
	{
		return description;
	}

	public String detanglerName()
	{
		if (detangler != null)
			return detangler.getName();
		else
			return null;
	}

	public boolean process(String inFile, String outDirectory)
	{
		return process(inFile, outDirectory, "txt");
	}

	public boolean process(String inFile, String outDirectory, String fileSuffix)
	{
		if (isOK)
		{
			foundSOF = false;
			inData = null;
			// System.err.println("DEBUG Reading input file " + inFile);
			File file = new File(inFile);
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
			if ((inData != null) && (inData.length > 0))
			{
				// System.err.println("Incoming data length: "+inData.length);
				// Did they ask for a EOF to be calculated from inside the file? Get it!
				if (eofLo + eofMid + eofHi + eofOffset > 0)
				{
					int calculatedEOF = eofOffset;
					if (eofLo > 0)
						calculatedEOF = calculatedEOF + UnsignedByte.intValue(inData[eofLo]);
					if (eofMid > 0)
						calculatedEOF = calculatedEOF + (256 * UnsignedByte.intValue(inData[eofMid]));
					if (eofHi > 0)
						calculatedEOF = calculatedEOF + (65536 * UnsignedByte.intValue(inData[eofHi]));
					// System.err.println("DEBUG: After dereference, calculated EOF: "+calculatedEOF);
					trimTrailing = inData.length - calculatedEOF;
				}
				if (detangler != null)
				{
					// Run the detangler if the transform specifies one
					try
					{
						detangle.invoke(t, this, inData, file.getName(), outDirectory, fileSuffix);
					}
					catch (IllegalAccessException e)
					{
						e.printStackTrace();
					}
					catch (IllegalArgumentException e)
					{
						e.printStackTrace();
					}
					catch (InvocationTargetException e)
					{
						e.printStackTrace();
					}
				}
				else
					emitFile(inData, outDirectory + File.pathSeparator + file.getName() + fileSuffix);
			}
		}
		return isOK;
	}

	public boolean emitFile(byte[] data, String filename)
	{
		if (isOK)
		{
			ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
			foundSOF = false;
			inData = data;
			int bytesForward = 0;
			// System.err.println("Incoming data length: "+inData.length);
			// Did they ask for a EOF to be calculated from inside the file? Get it!
			if (eofLo + eofMid + eofHi + eofOffset > 0)
			{
				int calculatedEOF = eofOffset;
				if (eofLo > 0)
					calculatedEOF = calculatedEOF + UnsignedByte.intValue(inData[eofLo]);
				if (eofMid > 0)
					calculatedEOF = calculatedEOF + (256 * UnsignedByte.intValue(inData[eofMid]));
				if (eofHi > 0)
					calculatedEOF = calculatedEOF + (65536 * UnsignedByte.intValue(inData[eofHi]));
				// System.err.println("DEBUG: After dereference, calculated EOF: "+calculatedEOF);
				trimTrailing = inData.length - calculatedEOF;
			}
			// System.err.println("DEBUG: Trimming leading "+trimLeading+" and "+ trimTrailing +" trailing bytes.");
			trimmedEnd = inData.length - trimTrailing;
			// Clean out the toggle states
			for (int i = 0; i < leftSide.size(); i++) // For each left specification
			{
				RegSpec currentSpec = leftSide.elementAt(i);
				currentSpec.toggleState = false;
			}
			for (int i = trimLeading; i < trimmedEnd; i++)
			{
				backupBytes = 0;
				bytesForward = evaluateTransforms(outBuf, i, trimmedEnd);
				// System.err.println("DEBUG: i=" + i + "; bytesForward=" + bytesForward+"; backupBytes="+backupBytes);
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
				/*
				 * We have a directory - so just append the newly discovered filename.
				 */
				System.out.println("Creating file: \"" + filename + "\"");
				FileOutputStream out = new FileOutputStream(filename);
				if (prefix != null)
				{
					out.write(prefix.getBytes(), 0, prefix.length());
				}
				String tempStr = outBuf.toString();
				for (int i = 0; i < regReplace.size(); i++)
				{
					// System.err.println("DEBUG Replacing ["+regPattern.elementAt(i)+"] with ["+regReplace.elementAt(i)+"].");
					tempStr = tempStr.replaceAll("(?m)" + regPattern.elementAt(i), regReplace.elementAt(i));
				}
				byte[] stdout = tempStr.getBytes();
				out.write(stdout, 0, stdout.length);
				if (suffix != null)
				{
					out.write(suffix.getBytes(), 0, suffix.length());
				}
				out.flush();
				out.close();
				//newName = null;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		return isOK;
	}

	public boolean readTransform(String filename)
	{
		isOK = true;
		isInternal = true;
		FileReader fr = null;
		// First try to load an external transform file. That should take
		// precedence over an internal one.
		try
		{
			fr = new FileReader(filename);
			if (fr != null)
			{
				isOK = true;
				isInternal = false;
				parseTransforms(fr);
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
				is = Transform.class.getResourceAsStream("/org/transformenator/transforms/" + filename);
				if (is != null)
				{
					InputStreamReader isr = new InputStreamReader(is);
					parseTransforms(isr);
					isOK = true;
					isInternal = true;
				}
				else
					isOK = false;
			}
			catch (Exception e)
			{
				isOK = false;
			}
		}
		if (detangler != null)
		{
			try
			{
				t = detangler.newInstance();
				detangle = detangler.getDeclaredMethod("detangle", FileInterpreter.class, byte[].class, String.class, String.class, String.class);
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
			}
			catch (SecurityException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
		}
		return isOK;
	}

	@SuppressWarnings("unchecked")
	public void parseTransforms(Reader fr)
	{
		StringBuffer messageBuffer = new StringBuffer();
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
				if ((line.indexOf("%") > 0 && line.indexOf("=") < 0 && line.indexOf("#") < 0) || (line.indexOf("%") > 0 && (line.indexOf("%") < line.indexOf("=")) && (line.indexOf("%") < line.indexOf("#"))))
				{
					// System.err.println("DEBUG This is a toggle production.");
					st = new StringTokenizer(toggleSplits[0]);
					result = toggleSplits;
					newRegSpec.backtrack = false;
					newRegSpec.toggle = true;
				}
				else if ((line.indexOf("=") > 0 && line.indexOf("%") < 0 && line.indexOf("#") < 0) || (line.indexOf("=") > 0 && (line.indexOf("=") < line.indexOf("%")) && (line.indexOf("=") < line.indexOf("#"))))
				{
					// System.err.println("DEBUG This is an equals production.");
					st = new StringTokenizer(equalsSplits[0]);
					result = equalsSplits;
					newRegSpec.backtrack = false;
					newRegSpec.toggle = false;
				}
				else if ((line.indexOf("#") > 0 && line.indexOf("=") < 0 && line.indexOf("%") < 0) || (line.indexOf("#") > 0 && (line.indexOf("#") < line.indexOf("=")) && (line.indexOf("#") < line.indexOf("%"))))

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
					// If you add a left side keyword that will get consumed, be sure to add it here, otherwise the fall through processing will try to eat it:
					if (leftTemp.toLowerCase().equals("detangler") || leftTemp.toLowerCase().equals("description") || leftTemp.equals("head") || leftTemp.equals("tail") || leftTemp.equals("trim_leading") || leftTemp.equals("trim_trailing") || leftTemp.equals("eof_lo") || leftTemp.equals("eof_mid") || leftTemp.equals("eof_hi") || leftTemp.trim().charAt(0) == (';'))
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
						// Ok, we have opening and closing braces. Check for two digits.
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
							// System.err.println("DEBUG We have a range: 0x"+UnsignedByte.toString(firstByte)+" through 0x"+UnsignedByte.toString(endInt));
							// Take care of adding specs right here...
							if (result.length > 1)
							{
								st = new StringTokenizer(result[1]);
								if (st.hasMoreTokens())
								{
									// Add a pile of left sides with incrementing right sides
									rightTemp1 = st.nextToken();
									anchorByte = asByte(rightTemp1);
									// System.err.println("DEBUG Right anchor: 0x"+UnsignedByte.toString(anchorByte));
									for (int i = firstByte; i <= endByte; i++)
									{
										newRegSpec.leftCompare = asBytes(UnsignedByte.loByte(i));
										// No mask is possible with ranges
										newRegSpec.leftMask = asBytes(0);
										leftSide.add(newRegSpec);
										// System.err.println("DEBUG Adding spec 0x"+UnsignedByte.toString(UnsignedByte.loByte(i))+" = 0x"+UnsignedByte.toString(anchorByte)+" (decimal "+anchorByte+")");
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
									// System.err.println("DEBUG Adding null spec 0x"+UnsignedByte.toString(UnsignedByte.loByte(i)));
									// System.err.println("DEBUG Right side token is null.");
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
						rightTemp1 = line.substring(line.indexOf("=") + 1);
					}
					else if (result == hashSplits) // We are using '#' as separator
					{
						rightTemp1 = line.substring(line.indexOf("#") + 1);
					}
					else
					// We are using '%' as separator
					{
						String rightTemp = line.substring(line.indexOf("%") + 1);
						// System.err.println("DEBUG toggling... right side is: "+rightTemp);
						rightTemp1 = rightTemp.substring(1, rightTemp.indexOf(","));
						rightTemp2 = rightTemp.substring(rightTemp.indexOf(",") + 1);
						// System.err.println("DEBUG Toggle on : ["+rightTemp1+"]");
						// System.err.println("DEBUG Toggle off: ["+rightTemp2+"]");
					}
					{
						// System.err.println("DEBUG Found a right side string: ["+rightTemp1.trim()+"]");
						if (rightTemp1.trim().equals("\"{@@<FiLe_EoF>@@}\""))
						{
							// System.err.println("DEBUG Found an EOF specification...");
							// Need to add an EOF command to the left side spec.
							newRegSpec.command = 1;
						}
						else if (rightTemp1.trim().equals("\"{@@<FiLe_SoF>@@}\""))
						{
							// System.err.println("DEBUG Found an SOF specification...");
							// Need to add an SOF command to the left side spec.
							newRegSpec.command = 2;
						}
						else if (rightTemp1.trim().equals("\"{@@<FiLe_SoF_GrEeDy>@@}\""))
						{
							// System.err.println("DEBUG Found a greedy SOF specification...");
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
								// System.err.println("DEBUG Regex replacement token 2: "+rxTokens[2]);
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
						if (leftTemp.toLowerCase().equals("description"))
						{
							description = rightTemp1;
						}
						else if (leftTemp.toLowerCase().equals("detangler"))
						{
							try
							{
								detangler = (Class<ADetangler>) java.lang.Class.forName(rightTemp1);
							}
							catch (ClassNotFoundException e)
							{
								try
								{
									detangler = (Class<ADetangler>) java.lang.Class.forName("org.transformenator.detanglers." + rightTemp1.trim());
								}
								catch (ClassNotFoundException e2)
								{
									// No detangler for YOU!
									messageBuffer.append("No detangler code " + rightTemp1.trim() + " found.");
								}
							}
						}
						else if (leftTemp.equals("head"))
						{
							rightTemp1 = result[1];
							for (int j = 2; j < result.length; j++)
							{
								// System.err.println("DEBUG Token: ["+result[j]+"]");
								rightTemp1 = rightTemp1 + "=" + result[j];
							}
							if (rightTemp1.trim().charAt(0) == '"')
							{
								String newString = "";
								// System.err.println("DEBUG Found a string...");
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
						else if (leftTemp.equals("trim_trailing"))
						{
							trimTrailing = fromByteArray(rightBytes);
						}
						else if (leftTemp.equals("eof_hi"))
						{
							eofHi = fromByteArray(rightBytes);
							// System.err.println("DEBUG eof_hi found: "+eofHi);
						}
						else if (leftTemp.equals("eof_mid"))
						{
							eofMid = fromByteArray(rightBytes);
							// System.err.println("DEBUG eof_mid found: "+eofMid);
						}
						else if (leftTemp.equals("eof_lo"))
						{
							eofLo = fromByteArray(rightBytes);
							// System.err.println("DEBUG eof_lo found: "+eofLo);
						}
						else if (leftTemp.equals("eof_offset"))
						{
							eofOffset = fromByteArray(rightBytes);
							// System.err.println("DEBUG eof_offset found: "+eofOffset);
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
				{
					leftSide.add(newRegSpec);
				}
				if (addLeft & !addedRight)
				{
					// System.err.println("DEBUG Right side token is null.");
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

	public byte[] asBytes(String str)
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

	public byte[] asBytes(int val)
	{
		byte[] buf = new byte[1];
		buf[0] = UnsignedByte.loByte(val);
		return buf;
	}

	public byte asByte(String str)
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

	public byte[] maskBytes(String str)
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
					// System.err.println("DEBUG Byte at position "+i/2+" must be non-zero.");
					buf[i / 2] = 2;
				}
				else
				{
					// System.err.println("DEBUG Ignoring byte at position "+i/2);
					buf[i / 2] = 1;
				}
			}
			i++;
		}
		return buf;
	}

	private int evaluateTransforms(ByteArrayOutputStream outBuf, int offset, int max)
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
					// System.err.println("DEBUG Found a non-zero byte at "+j);
					match = false;
				}
			}
			if (match == true)
			{
				// System.err.println("DEBUG Match @ offset 0x"+Integer.toHexString(offset)+"; left length = "+currLeftLength+" command: "+currentSpec.command+" backtrack: "+currentSpec.backtrack);
				if (currentSpec.command == 0)
				{
					try
					{
						// send out new data
						if ((replRight != null) && (currentSpec.backtrack == true))
						{
							// Push the replacement back onto incoming
							int calc = offset + compLeft.length - replRight.length;
							// System.err.println("DEBUG calc: "+calc+" offset: "+offset);
							if (calc < 0)
							{
								// System.err.println("DEBUG calc: "+calc+" offset: "+offset);
								int bump = Math.abs(calc);
								byte newInData[] = new byte[trimmedEnd - calc];
								for (int q = 0; q < trimmedEnd; q++)
								{
									newInData[q + bump] = inData[q];
								}
								inData = newInData;
								offset += replRight.length - compLeft.length;
							}
							for (k = 0; k < replRight.length; k++)
							{
								// System.err.println("DEBUG Pushing byte: "+k);
								inData[offset + compLeft.length - replRight.length + k] = replRight[k];
							}
							backupBytes = replRight.length;
							if (calc < 0)
							{
								backupBytes += calc;
							}
							// System.err.println("DEBUG Backing up "+replRight.length+" bytes.");
						}
						else if ((replRight != null) && (currentSpec.backtrack == false))
						{
							if (currentSpec.toggle == true)
							{
								if (currentSpec.toggleState == false) // Toggle state was "false"
									outBuf.write(replRight); // Write out "on" value
								else
									// Toggle state was "true"
									outBuf.write(replRightToggle); // Write out "off" value
								currentSpec.toggleState = !currentSpec.toggleState; // Toggle the state
								// System.err.println("DEBUG currentSpec id: "+currentSpec.id+" ("+temp1+"),("+temp2+") toggleState: "+currentSpec.toggleState);
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
					if (foundSOF == false)
					{
						bytesMatched = currLeftLength;
						outBuf.reset();
						foundSOF = true;
						break;
					} // Else we already found (non-greedy) SOF, so keep matching
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
			// System.err.println("DEBUG Writing out original byte, no comparisons to make.");
			outBuf.write(inData[offset]);
			bytesMatched = 1;
		}
		return bytesMatched;
	}

	int fromByteArray(byte[] digits)
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

	public static void listExamples()
	{
		InputStream is = null;
		is = Transform.class.getResourceAsStream("/org/transformenator/help.txt");

		if (is != null)
		{
			int bytesAvailable;
			try
			{
				bytesAvailable = is.available();
				byte[] buffer = new byte[bytesAvailable];
				int bytesRead = is.read(buffer);
				while (bytesRead < bytesAvailable)
				{
					bytesRead += is.read(buffer, bytesRead, bytesAvailable - bytesRead);
				}
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < buffer.length; i++)
				{
					sb.append((char) buffer[i]);
				}
				System.err.println(sb.toString());
			}
			catch (IOException e)
			{
				System.err.println("Unable to access examples.");
			}
		}
		else
		{
			System.err.println("Unable to access examples.");
		}
	}

	public static void listInternalTransforms()
	{
		String prefix = "org/transformenator/transforms/";
		CodeSource src = FileInterpreter.class.getProtectionDomain().getCodeSource();
		Vector<String> transforms = new Vector<String>();

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
					if (entryName.startsWith(prefix))
					{
						String finalName = entryName.substring(prefix.length());
						if (finalName.length() > 0)
							transforms.add(finalName);
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			File path = new File(jar.getPath() + prefix);
			File[] listOfFiles = path.listFiles();
			if (listOfFiles != null)
			{
				int offset = path.toString().length() + 1;
				String name;
				for (int i = 0; i < listOfFiles.length; i++)
				{
					name = listOfFiles[i].toString().substring(offset).trim();
					if (!name.equals("") && !name.equals(".gitignore") && !name.equals(".cvsignore"))
						transforms.add(name);
				}
			}
			System.err.println("Available internal transforms:");
			printElements(transforms);
		}
	}

	public static void listUtilities()
	{
		boolean printedHeaderYet = false;
		String prefix = "org/transformenator/util/";
		CodeSource src = FileInterpreter.class.getProtectionDomain().getCodeSource();
		Vector<String> utilities = new Vector<String>();

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
					if (entryName.startsWith(prefix))
					{
						String finalName = entryName.substring(prefix.length());
						if (finalName.length() > 0)
						{
							if (printedHeaderYet == false)
							{
								System.err.println("Available utilities:");
								printedHeaderYet = true;
							}
							if ((finalName.indexOf('$') == -1) && (!finalName.equals("TransformUtility.class")))
							{
								utilities.add(finalName.substring(0, finalName.length() - 6));
							}
						}
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			if (!printedHeaderYet)
			{
				File path = new File(jar.getPath() + prefix);
				File[] listOfFiles = path.listFiles();
				if (listOfFiles != null)
				{
					int offset = path.toString().length() + 1;
					System.err.println("Available utilities:");
					for (int i = 0; i < listOfFiles.length; i++)
					{
						String finalName = listOfFiles[i].toString().substring(offset);
						if ((finalName.indexOf('$') == -1) && (!finalName.equals("TransformUtility.class")))
						{
							utilities.add(finalName.substring(0, finalName.length() - 6));
						}
					}
				}
			}
			printElements(utilities);
		}
	}

	public String getPrefix()
	{
		return prefix;
	}

	public String getSuffix()
	{
		return suffix;
	}

	public static void printElements(Vector<String> elements)
	{
		/*
		 *  Print out a vector of elements in two columns
		 *   - is not smart enough to know terminal width; assumes 80 
		 *   - is not smart enough to know if two columns actually fit; assumes 37 chars max name length
		 */
		if (!elements.isEmpty())
		{
			int halfway = (int) elements.size() / 2;
			if (elements.size() % 2 > 0)
				halfway += 1;
			for (int i = 0; i < halfway; i++)
			{
				System.err.print("  " + elements.get(i));
				if (elements.size() > i + halfway) // If there is a final element (even number of elements)
				{
					for (int j = 0; j < 40 - (elements.get(i).length()); j++)
					{
						System.err.print(" ");
					}
					System.err.println("  " + elements.get(i + halfway));
				}
				else
					System.err.println();
			}
		}
	}

	public void emitStatus()
	{
		if (isOK == false)
			System.err.println("Unable to locate transform file named \"" + transformName + "\".");
		else
		{
			if (isInternal)
				System.err.println("Using internal transform file \"" + transformName + "\".");
			else
				System.err.println("Using external transform file \"" + transformName + "\".");
		}

		if (description != null)
		{
			System.out.println();
			System.out.println("Description: ");
			System.out.println(description);
		}
		if (detanglerName() != null)
		{
			// Only bother reporting the detangler if it's not the default one
			System.out.println();
			System.out.println("Detangler: ");
			System.out.println(detanglerName());
		}

	}

	// Dynamic class loading for detangler
	Object t;
	Method detangle;
	Method getNewName;

	// Global variables
	byte inData[] = null;
	Vector<String> regPattern = new Vector<String>();
	Vector<String> regReplace = new Vector<String>();
	Vector<RegSpec> leftSide = new Vector<RegSpec>();
	Vector<byte[]> rightSide = new Vector<byte[]>();
	Vector<byte[]> rightToggle = new Vector<byte[]>();
	String description, prefix, suffix, messages = null;
	Class<ADetangler> detangler = null;
	String inFile, transformName;
	int trimLeading = 0, trimTrailing = 0, trimmedEnd;
	int eofHi = 0, eofMid = 0, eofLo = 0, eofOffset = 0;
	boolean foundSOF;
	int backupBytes;
}