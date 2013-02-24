package org.transformenator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;

import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.Vector;

public class BinaryTransform
{

	public static void main(java.lang.String[] args)
	{
		boolean rc = true;
		if (args.length > 0)
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
				help();
			}
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
				try
				{
					while ((byteCount = System.in.read(inb)) > 0)
					{
						buf.write(inb, 0, byteCount);
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
				for (int i = 0; i < inData.length; i++)
				{
					bytesForward = evaluateTransforms(inData, outBuf, i, inData.length);
					// System.err.println("i=" + i + "; bytesForward=" + bytesForward);
					if (bytesForward > 0)
						i = i + bytesForward - 1;
				}
				if (preamble != null)
				{
					System.out.write(preamble.getBytes(),
							0, preamble.length());
				}
				String tempStr = outBuf.toString();
				for (int i = 0; i < regReplace.size(); i++)
				{
					// System.err.println("Replacing ["+regPattern.elementAt(i)+"] with ["+regReplace.elementAt(i)+"].");
					tempStr = tempStr
							.replaceAll(regPattern
									.elementAt(i),
									regReplace.elementAt(i));
				}
				byte[] stdout = tempStr.getBytes();
				System.out.write(stdout, 0, stdout.length);
				if (postamble != null)
				{
					System.out.write(postamble.getBytes(),
							0, postamble.length());
				}
			}
		}
	}

	public static int evaluateTransforms(byte[] inData,
			ByteArrayOutputStream outBuf, int location, int max)
	{
		int i = 0;
		int bytesMatched = 0;
		int currLeftLength = 0;
		boolean match = false;
		for (i = 0; i < leftSide.size(); i++)
		{
			currLeftLength = leftSide.elementAt(i).length;
			if (location + currLeftLength > max)
				continue;
			byte[] compLeft = leftSide.elementAt(i);
			byte[] replRight = rightSide.elementAt(i);
			match = true;
			for (int j = 0; j < leftSide.elementAt(i).length; j++)
			{
				if ((compLeft[j]) != inData[location + j])
				{
					match = false;
				}
			}
			if (match == true)
			{
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
			outBuf.write(inData[location]);
			bytesMatched = 1;
		}
		return bytesMatched;
	}

	public static byte transformOneByte(byte datum)
	{
		byte newDatum = datum;
		for (int i = 0; i < leftSide.size(); i++)
		{
			byte compare[] = leftSide.elementAt(i);
			if (datum == compare[0])
			{
				newDatum = rightSide.elementAt(i)[0];
			}
		}
		return newDatum;
	}

	public static boolean readTransform(String[] args)
	{
		boolean isOK = true;
		FileReader fr = null;
		try
		{
			if (args.length < 2)
			{
				isOK = false;
				help();
			}
			else
			{
				// System.err.println("Reading file " + args[1] + ".");
				fr = new FileReader(args[1]);
				parseTransforms(fr);
				// System.err.println("Completed reading.");
			}
		}
		catch (FileNotFoundException ex)
		{
			isOK = false;
			System.err.println("Unable to read transform file \""
					+ args[1] + "\".");
		}
		return isOK;
	}

	public static void help()
	{
		System.err.println("Syntax: BinaryTransform -t transformFile < in.foo > out.foo");
		System.err.println("        BinaryTransform -t transformFile in.foo > out.foo");
	}

	public static void parseTransforms(FileReader fr)
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
					if (leftTemp.equals("head")
							|| leftTemp.equals("tail")
							|| leftTemp.trim()
									.charAt(0) == (';'))
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
						byte[] leftBytes = asBytes(leftTemp);
						// System.err.println("Left bytes length: " + leftBytes.length);
						leftSide.add(leftBytes);
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
							rightTemp = rightTemp
									+ st.nextToken()
									+ " ";
						}
						if (rightTemp.trim().charAt(0) == '"')
						{
							String newString = "";
							// System.err.println("Found a string...");
							rightTemp.trim();
							rightTemp = rightTemp
									.substring(1,
											rightTemp.length() - 2);
							newString = rightTemp
									.replace("\\\\r",
											"\r")
									.replace("\\\\n",
											"\n");
							rightBytes = newString
									.getBytes();
						}
						else if (leftTemp
								.equals("regex"))
						{
							String delim = rightTemp
									.substring(0,
											1);
							// System.err.println("Regex replacement: "+rightTemp+" Delimiter: "+delim);
							String[] rxTokens = rightTemp
									.split(delim);
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
							rightBytes = asBytes(rightTemp
									.trim());
						}

						// System.err.println("Right side token: ["+rightTemp+"]");
						if (leftTemp.equals("head"))
						{
							preamble = rightTemp;
						}
						else if (leftTemp
								.equals("tail"))
						{
							postamble = rightTemp;
						}
						else if (leftTemp
								.equals("regex"))
						{
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
			byte b = Byte.parseByte(String.valueOf(c), 16);
			buf[i / 2] |= (b << (((i % 2) == 0) ? 4 : 0));
			i++;
		}

		return buf;
	}

	static Vector<String> regPattern = new Vector<String>();
	static Vector<String> regReplace = new Vector<String>();
	static Vector<byte[]> leftSide = new Vector<byte[]>();
	static Vector<byte[]> rightSide = new Vector<byte[]>();
	static String preamble;
	static String postamble;

}