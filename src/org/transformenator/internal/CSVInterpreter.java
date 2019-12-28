/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2016 - 2019 by David Schmidt
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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.transformenator.Transform;

import java.util.Iterator;

public class CSVInterpreter
{
	public CSVInterpreter(String transform_name)
	{
		transformName = transform_name;
		isOK = readTransform(transform_name);
	}

	public boolean createOutput(String inFile, String outDirectory)
	{
		return createOutput(inFile, outDirectory, "csv");
	}

	public boolean createOutput(String inFile, String outDirectory, String fileSuffix)
	{
		inData = null;
		System.err.println("Reading CSV input file \"" + inFile + "\".");
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
				if (isOK)
				{
					// If they wanted an output directory, go ahead and make it.
					File baseDirFile = new File(outDirectory);
					if (!baseDirFile.isAbsolute())
					{
						baseDirFile = new File("." + File.separator + outDirectory);
					}
					// System.err.println("Making directory: ["+baseDirFile+"]");
					baseDirFile.mkdir();

					String outFile = outDirectory + File.separator + file.getName() + fileSuffix;
					System.err.println("Creating CSV file: \"" + outFile + "\"");
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
					Iterator<FieldSpec> header = fields.iterator();
					FieldSpec fs;
					String hf;
					boolean isFirst = true;
					while (header.hasNext())
					{
						fs = header.next();
						hf = new String("\"" + fs.fieldName + "\"");
						if (isFirst)
						{
							out.write(hf.getBytes());
							isFirst = false;
						}
						else
						{
							hf = "," + hf;
							out.write(hf.getBytes());
						}
					}
					hf = "\n";
					out.write(hf.getBytes());
					/*
					 * Iterate over the entire file, so far only doing fixed-size/fixed-location records.
					 * If there comes a time where records must be searched for, the outer loop construct
					 * will have to change to accommodate that.
					 */
					for (int i = firstRec; i < (result.length - nextRec); i += nextRec)
					{
						/*
						 * First, check if the data qualifies as a record.
						 */
						Iterator<SelectSpec> it = selection.iterator();
						SelectSpec selection;
						boolean shouldPrint = false;
						if (it.hasNext() == false)
						{
							// System.err.println("DEBUG no hasNext on selection iterator.");
							shouldPrint = true;
						}
						else
						{
							shouldPrint = true;
							while (it.hasNext())
							{
								selection = it.next();
								for (int j = 0; j < selection.rightCompare.length; j++)
								{
									if (UnsignedByte.intValue(result[i + j + selection.offset]) == UnsignedByte.intValue(selection.rightCompare[j]))
									{
										// System.err.println("DEBUG Found one! "+UnsignedByte.toString(result[i+j+selection.offset]) + "=" + UnsignedByte.toString(selection.rightCompare[j]));
										continue;
									}
									else
									{
										shouldPrint = false;
										// System.err.println("DEBUG mismatch: "+UnsignedByte.toString(result[i+j+selection.offset]) + "!=" + UnsignedByte.toString(selection.rightCompare[j]));
									}
								}
							}
						}
						if (shouldPrint)
						{
							// System.err.println("DEBUG Found one!");
							Iterator<FieldSpec> it2 = fields.iterator();
							FieldSpec field;
							boolean isFirstField = true;
							while (it2.hasNext())
							{
								String fieldString = new String("");
								field = it2.next();
								// System.err.println("DEBUG Field name:   " + field.fieldName);
								// System.err.println("DEBUG Field origin: " + field.fieldOrigin);
								// System.err.println("DEBUG Field length: " + field.fieldLength);
								byte[] fieldBytes = new byte[field.fieldLength];
								System.arraycopy(result, i + field.fieldOrigin, fieldBytes, 0, field.fieldLength);
								String temp;
								if (field.interp == 1) // ASCII text
								{
									temp = new String(fieldBytes);
									if (isFirstField)
									{
										if (field.csvLiteral)
											fieldString += "\"=\"\"" + temp + "\"\"\"";
										else
											fieldString += "\"" + temp + "\"";
										isFirstField = false;
									}
									else
									{
										if (field.csvLiteral)
											fieldString += ",\"=\"\"" + temp + "\"\"\"";
										else
											fieldString += ",\"" + temp + "\"";
									}
								}
								else if (field.interp == 2) // EBCDIC text
								{
									fieldString = new String("");
									if (isFirstField)
									{
										if (field.csvLiteral)
											fieldString += "\"=\"\"" + EbcdicUtil.toAscii(fieldBytes, 0, fieldBytes.length) + "\"\"\"";
										else
											fieldString += "\"" + EbcdicUtil.toAscii(fieldBytes, 0, fieldBytes.length) + "\"";
										isFirstField = false;
									}
									else
									{
										if (field.csvLiteral)
											fieldString += ",\"=\"\"" + EbcdicUtil.toAscii(fieldBytes, 0, fieldBytes.length) + "\"\"\"";
										else
											fieldString += ",\"" + EbcdicUtil.toAscii(fieldBytes, 0, fieldBytes.length) + "\"";
									}
								}
								else if (field.interp == 3) // Hex data
								{
									fieldString = new String("");
									if (isFirstField)
									{
										if (field.csvLiteral)
											fieldString += "\"=\"\"";
										else
											fieldString += "\"0x";
										isFirstField = false;
									}
									else
									{
										if (field.csvLiteral)
											fieldString += ",\"=\"\"";
										else
											fieldString += ",\"0x";
									}
									for (int k = 0; k < fieldBytes.length; k++)
									{
										fieldString += UnsignedByte.toString(fieldBytes[k]);
									}
									if (field.csvLiteral)
										fieldString += "\"\"\"";
									else
										fieldString += "\"";
								}
								out.write(fieldString.getBytes());
							}
							out.write('\n');
						}
					}
					out.flush();
					out.close();
				}
			}
			catch (Exception ex)
			{
				System.err.println(ex.getMessage());
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
		return isOK;
	}

	public boolean readTransform(String filename)
	{
		FileReader fr = null;
		try
		{
			fr = new FileReader(filename);
			if (fr != null)
			{
				parseTransforms(fr);
				if (nextRec > 0)
					isOK = true;
				else
					isOK = false;
			}
		}
		catch (Exception e)
		{
			isOK = false;
		}
		return isOK;
	}

	public void parseTransforms(Reader fr)
	{
		String line;
		StringBuffer messageBuffer = new StringBuffer();
		StringBuffer layoutBuffer = new StringBuffer();
		try
		{
			BufferedReader br = new BufferedReader(fr);
			StringTokenizer st;
			FieldSpec currentFieldSpec = null;
			boolean skip = false;
			while ((line = br.readLine()) != null)
			{
				skip = false;
				// System.err.println("DEBUG Line before: ["+line+"]");
				if (line.indexOf(";") > 0)
				{
					line = line.substring(0, line.indexOf(";"));
					// System.err.println("DEBUG 1 Line after : ["+line+"]");
				}
				else if (line.indexOf(";") == 0)
				{
					line = "";
					skip = true;
					// System.err.println("DEBUG 2 Line after : ["+line+"]");
				}
				String[] equalsSplits = line.split("=");
				String[] result;
				String leftTemp = "";
				String rightTemp1 = "";
				byte[] rightBytes = null;
				int fieldOrigin = 0, fieldLength = 0;
				st = new StringTokenizer(equalsSplits[0]);
				result = equalsSplits;
				// System.err.println("Splits on '=': "+equalsSplits.length);
				if (st != null && st.hasMoreTokens())
				{
					leftTemp = st.nextToken();
					skip = false;
					// System.err.println("DEBUG Left side token: ["+leftTemp+"]");
				}
				if (result != null && (result.length > 1) && !skip)
				{
					rightTemp1 = line.substring(line.indexOf("=") + 1);
					{
						if (rightTemp1.trim().length() > 0 && rightTemp1.trim().charAt(0) == '"')
						{
							// System.err.println("DEBUG Found a right side string: ["+rightTemp1.trim()+"]");
							String newString = "";
							rightTemp1 = rightTemp1.trim();
							if (rightTemp1.length() > 1)
								rightTemp1 = rightTemp1.substring(1, rightTemp1.length() - 1);
							newString = rightTemp1.replace("\\\\r", "\r").replace("\\\\n", "\n");
							rightBytes = newString.getBytes();
						}
						else
						{
							// System.err.println("DEBUG Found right side bytes: "+rightTemp1.trim());
							rightBytes = asBytes(rightTemp1.trim());
						}

						// System.err.println("DEBUG Right side token: ["+rightTemp1+"]");
						if (leftTemp.equalsIgnoreCase("FIRSTREC"))
						{
							firstRec = fromByteArray(rightBytes);
							// System.err.println("DEBUG First record starts at: " + firstRec);
						}
						else if (leftTemp.equalsIgnoreCase("NEXTREC"))
						{
							nextRec = fromByteArray(rightBytes);
							// System.err.println("DEBUG Next record increment is: " + nextRec);
						}
						else if (leftTemp.equalsIgnoreCase("SELECTIF"))
						{
							String[] commaSplits = rightTemp1.trim().split(",");
							if (commaSplits.length == 2)
							{
								SelectSpec newSelectionSpec = new SelectSpec();
								newSelectionSpec.offset = fromByteArray(asBytes(commaSplits[0]));
								newSelectionSpec.rightCompare = asBytes(commaSplits[1]);
								selection.addElement(newSelectionSpec);
								// System.err.println("DEBUG Added selection Spec: " + newSelectionSpec.offset + ", " + commaSplits[1]);
							}
							else
							{
								messageBuffer.append("ERROR: Incomplete CSV selection criteria.");
							}
						}
						else if (leftTemp.equalsIgnoreCase("NAME"))
						{
							if (currentFieldSpec != null)
							{
								messageBuffer.append("ERROR: found a new CSV field before completing the prior one.  Abandoning: " + currentFieldSpec.fieldName);
								currentFieldSpec = null;
							}
							currentFieldSpec = new FieldSpec();
							currentFieldSpec.fieldName = new String(rightBytes);
							// System.err.println("DEBUG Creating new field: " + currentFieldSpec.fieldName);
						}
						else if (leftTemp.equalsIgnoreCase("ORIGIN"))
						{
							if (currentFieldSpec != null)
							{
								fieldOrigin = fromByteArray(rightBytes);
								currentFieldSpec.fieldOrigin = fieldOrigin;
								// System.err.println("DEBUG Origin of field is: " + fieldOrigin);
							}
							else
							{
								messageBuffer.append("ERROR: CSV field origin found with no prior name to associate it with.");
							}
						}
						else if (leftTemp.equalsIgnoreCase("LENGTH"))
						{
							if (currentFieldSpec != null)
							{
								fieldLength = fromByteArray(rightBytes);
								// System.err.println("DEBUG Length of field is: " + fieldLength);
								currentFieldSpec.fieldLength = fieldLength;
								if (fieldLength < 1)
								{
									messageBuffer.append("ERROR: CSV field named \"" + currentFieldSpec.fieldName + "\" must have a length greater than zero.  Abandoning it.");
									currentFieldSpec = null;
								}
							}
							else
							{
								messageBuffer.append("ERROR: CSV field length found with no prior field name to associate it with.");
							}
						}
						else if (leftTemp.equalsIgnoreCase("LAYOUT"))
						{
							int fieldNumber = 1;
							char previousChar = 0, currentChar;
							int fieldStart = 0;
							FieldSpec autoFieldSpec = null;
							String layout = new String(rightBytes);
							// System.err.println("DEBUG: Layout: [" + layout +"]");
							for (int i = 0; i < layout.length(); i++)
							{
								currentChar = layout.charAt(i);
								if (currentChar != previousChar)
								{
									// System.err.println("DEBUG: We have a field transition!");
									if (autoFieldSpec != null)
									{
										autoFieldSpec.fieldOrigin = fieldStart;
										autoFieldSpec.fieldLength = i - fieldStart;
										nextRecGuess = autoFieldSpec.fieldOrigin + i;
										autoFieldSpec.interp = 1; // ASCII by default
										fieldStart = i;
										autoFields.addElement(autoFieldSpec);
										autoFieldSpec = null;
									}
									autoFieldSpec = new FieldSpec();
									autoFieldSpec.fieldName = "Field" + fieldNumber;
									previousChar = currentChar;
									fieldNumber++;
								}
							}
							autoFieldSpec.fieldOrigin = fieldStart;
							autoFieldSpec.fieldLength = layout.length() - fieldStart;
							autoFieldSpec.interp = 1; // ASCII by default
							nextRecGuess = autoFieldSpec.fieldOrigin + autoFieldSpec.fieldLength;
							autoFields.addElement(autoFieldSpec);
							autoFieldSpec = null;
							Iterator<FieldSpec> af = autoFields.iterator();
							while (af.hasNext())
							{
								autoFieldSpec = af.next();
								// System.err.println("DEBUG: Auto field name: "+autoFieldSpec.fieldName+" start: 0x"+Integer.toHexString(autoFieldSpec.fieldOrigin)+" length: 0x"+Integer.toHexString(autoFieldSpec.fieldLength));
							}

						}
						else if (leftTemp.equalsIgnoreCase("INTERP") || leftTemp.equalsIgnoreCase("INTERPLITERAL"))
						{
							String typeString = new String(rightBytes).trim();
							if (currentFieldSpec != null)
							{
								if (leftTemp.equalsIgnoreCase("INTERPLITERAL"))
									currentFieldSpec.csvLiteral = true;
								if ((typeString.equalsIgnoreCase("ASCII") || rightTemp1.trim().equalsIgnoreCase("ASCII")))
									currentFieldSpec.interp = 1;
								else if ((typeString.equalsIgnoreCase("EBCDIC") || rightTemp1.trim().equalsIgnoreCase("EBCDIC")))
									currentFieldSpec.interp = 2;
								else if ((typeString.equalsIgnoreCase("HEX") || rightTemp1.trim().equalsIgnoreCase("HEX")))
									currentFieldSpec.interp = 3;
								else
								{
									messageBuffer.append("ERROR: unexpected value for CSV field \"" + currentFieldSpec.fieldName + "\" interpretation: " + rightTemp1.trim());
									currentFieldSpec.interp = 0;
								}
								fields.addElement(currentFieldSpec);
								nextRecGuess = currentFieldSpec.fieldOrigin + currentFieldSpec.fieldLength;
								// System.err.println("DEBUG Added field " + currentFieldSpec.fieldName + " with interpretation: " + currentFieldSpec.interp);
								currentFieldSpec = null;
							}
							else
							{
								messageBuffer.append("ERROR: CSV field interpretation found with no prior name to associate it with.");
							}
						}
						else
						{
							messageBuffer.append("ERROR: unknown specification encountered in CSV transform file: " + leftTemp);
						}
					}
				}
				else
				{

				}
			}
			if (!autoFields.isEmpty())
			{
				int i, j;
				// System.err.println("DEBUG: Found "+autoFields.size()+" auto fields, and there are "+fields.size() +" manual fields.");
				for (i = 0; i < fields.size(); i++)
				{
					if (autoFields.size() > i)
					{
						fields.elementAt(i).fieldOrigin = autoFields.elementAt(i).fieldOrigin;
						fields.elementAt(i).fieldLength = autoFields.elementAt(i).fieldLength;
						/*
						System.err.println("DEBUG: NAME=\""+fields.elementAt(i).fieldName+"\"");
						System.err.println("DEBUG: ORIGIN="+Integer.toHexString(fields.elementAt(i).fieldOrigin));
						System.err.println("DEBUG: LENGTH="+Integer.toHexString(fields.elementAt(i).fieldLength));
						if (fields.elementAt(i).csvLiteral)
							System.err.println("DEBUG: INTERPLITERAL="+FieldSpec.interpString(fields.elementAt(i).interp));
						else
							System.err.println("DEBUG: INTERP="+FieldSpec.interpString(fields.elementAt(i).interp));
						*/
					}
				}
				j = fields.size();
				// Are there more auto fields than manual ones?  Add them in!
				for (i = 0; i < (autoFields.size() - j); i++)
				{
					// System.err.println("DEBUG: Adding autoField " + (j + i + 1) + ": "+autoFields.elementAt(j+i).fieldName + ", 0x"+Integer.toHexString(autoFields.elementAt(j+i).fieldOrigin));
					fields.add(autoFields.elementAt(j + i));
				}
			}
			if (!autoFields.isEmpty())
			{
				layoutBuffer.append(System.lineSeparator()+"; Fields automatically defined by layout:"+System.lineSeparator());
				if (nextRec == 0)
				{
					layoutBuffer.append("NEXTREC="+Integer.toHexString(nextRecGuess)+System.lineSeparator());
					layoutBuffer.append(";"+System.lineSeparator());
				}
				for (int i = 0; i < fields.size(); i++)
				{
					layoutBuffer.append("NAME=\"" + fields.elementAt(i).fieldName + "\""+System.lineSeparator());
					layoutBuffer.append("ORIGIN=" + Integer.toHexString(fields.elementAt(i).fieldOrigin)+System.lineSeparator());
					layoutBuffer.append("LENGTH=" + Integer.toHexString(fields.elementAt(i).fieldLength)+System.lineSeparator());
					if (fields.elementAt(i).csvLiteral)
						layoutBuffer.append("INTERPLITERAL=" + FieldSpec.interpString(fields.elementAt(i).interp)+System.lineSeparator());
					else
						layoutBuffer.append("INTERP=" + FieldSpec.interpString(fields.elementAt(i).interp)+System.lineSeparator());
					if (i + 1 < fields.size())
						layoutBuffer.append(";"+System.lineSeparator());
				}
				layoutBuffer.append("; End of automatic field definition."+System.lineSeparator());
			}
			if (nextRec == 0)
				nextRec = nextRecGuess;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		messages = new String(messageBuffer);
		autoLayout = new String(layoutBuffer);
	}

	public byte[] asBytes(String str)
	{
		str = str.trim();
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

	int fromByteArray(byte[] digits)
	{
		int result = 0;
		int j = 0;
		int multiplier = 1;
		for (j = digits.length - 1; j > -1; j--)
		{
			result += (UnsignedByte.intValue(digits[j]) * multiplier);
			multiplier *= 256;
		}
		return result;
	}

	public void emitStatus()
	{
		if (isOK == false)
			System.err.println("Unable to find or use CSV transform file named \"" + transformName + "\".");
		else
			System.err.println("Using CSV transform file \"" + transformName + "\".");
		if (messages.length() > 0)
			System.err.println(messages);
		if (autoLayout.length() > 0)
			System.out.println(autoLayout);
	}

	public static void listExamples()
	{
		InputStream is = null;
		is = Transform.class.getResourceAsStream("/org/transformenator/help-csv.txt");

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
					sb.append((char)buffer[i]);
				}
				System.err.println(sb.toString());
			}
			catch (IOException e)
			{
				System.err.println("Unable to access CSV examples.");
			}
		}
		else
		{
			System.err.println("Unable to access CSV examples.");
		}
	}

	byte inData[] = null;
	Vector<FieldSpec> fields = new Vector<FieldSpec>();
	Vector<FieldSpec> autoFields = new Vector<FieldSpec>();
	Vector<SelectSpec> selection = new Vector<SelectSpec>();
	String inFile, outFile, transformName, messages = null, autoLayout = null;
	public int firstRec = 0, nextRec = 0, nextRecGuess = 0;
	public boolean isOK = false;
}
