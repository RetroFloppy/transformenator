/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2026 by David Schmidt
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

/*
 * PFS:First Choice database (.FOL) to CSV converter
 *
 * Reads PFS:First Choice v3 database files and extracts data to CSV format.
 * The FOL file format uses 128-byte blocks with a special header containing
 * the GERBILDB3 signature and field definitions.
 * 
 * Based on GERBILDB database parser by Paul H. Alfie
 * https://github.com/alfille/firstchoice
 * 
 */

public class PFSDB extends ADetangler
{
  static final int BLOCKSIZE = 128;
  static final byte[] GERB_SIGNATURE = {0x0C, 'G', 'E', 'R', 'B', 'I', 'L', 'D', 'B', '3', ' ', ' ', ' ', 0x00};

  static final int BLOCKTYPE_FORM = 0x82;
  static final int BLOCKTYPE_DATA = 0x81;
  static final int BLOCKTYPE_PROGRAM = 0x84;
  static final int BLOCKTYPE_VIEW = 0x83;
  static final int BLOCKTYPE_EMPTY = 0x00;

  static final int CONTINUATION_FORM = 0x02;
  static final int CONTINUATION_DATA = 0x01;
  static final int CONTINUATION_PROGRAM = 0x04;
  static final int CONTINUATION_VIEW = 0x03;

  public void detangle(FileInterpreter parent, byte[] inData, String outDirectory, String inFile, String fileSuffix,
      boolean isDebugMode)
  {
    try
    {
      FOLDatabase db = new FOLDatabase(inData);
      String csvOutput = db.toCSV();

      String outputName = inFile.substring(0, (inFile.lastIndexOf('.') > 0 ? inFile.lastIndexOf('.') : inFile.length()));
      parent.emitFile(csvOutput.getBytes("UTF-8"), outDirectory, "", outputName + ".csv", null);

      if (isDebugMode)
      {
        System.out.println("Converted " + db.dataRecords.size() + " records with " + db.fieldNames.size() + " fields");
      }
    }
    catch (Exception e)
    {
      System.err.println("Error processing PFS:First Choice database: " + e.getMessage());
      e.printStackTrace();
    }
  }

  static class FOLDatabase
  {
    int formDefBlock;
    int usedBlocks;
    int allocatedBlocks;
    int recordCount;
    int fieldCount;
    int formLength;
    int formRevisions;
    int emptiesListBlock;
    int viewBlock;
    int programBlock;
    int progLines;

    List<String> fieldNames = new ArrayList<>();
    List<List<String>> dataRecords = new ArrayList<>();

    public FOLDatabase(byte[] data) throws Exception
    {
      if (data.length < 512)
      {
        throw new Exception("File too short to be a valid FOL database");
      }

      readHeader(data);
      parseBlocks(data);
    }

    void readHeader(byte[] data) throws Exception
    {
      ByteBuffer bb = ByteBuffer.wrap(data, 0, 512);
      bb.order(ByteOrder.LITTLE_ENDIAN);

      formDefBlock = bb.getShort() & 0xFFFF;
      usedBlocks = bb.getShort() & 0xFFFF;
      allocatedBlocks = bb.getShort() & 0xFFFF;
      recordCount = bb.getShort() & 0xFFFF;

      byte[] gerb = new byte[14];
      bb.get(gerb);
      if (!Arrays.equals(gerb, GERB_SIGNATURE))
      {
        throw new Exception("Invalid GERBILDB3 signature");
      }

      fieldCount = bb.getShort() & 0xFFFF;
      formLength = bb.getShort() & 0xFFFF;
      formRevisions = bb.getShort() & 0xFFFF;
      bb.getShort(); // unknown1
      emptiesListBlock = bb.getShort() & 0xFFFF;
      viewBlock = bb.getShort() & 0xFFFF;
      programBlock = bb.getShort() & 0xFFFF;
      progLines = bb.getShort() & 0xFFFF;
    }

    void parseBlocks(byte[] data) throws Exception
    {
      int offset = 512; // Skip header (4 blocks = 512 bytes)
      offset += 512; // Skip empties list (4 blocks)

      List<BlockRecord> blocks = new ArrayList<>();

      while (offset < data.length)
      {
        if (offset + BLOCKSIZE > data.length)
          break;

        int blockType = UnsignedByte.intValue(data[offset], data[offset + 1]);

        if (blockType == BLOCKTYPE_EMPTY)
        {
          offset += BLOCKSIZE;
          continue;
        }

        int savedOffset = offset;
        byte[] blockData = readBlockWithContinuations(data, offset, blockType);
        blocks.add(new BlockRecord(blockType, blockData));

        int blockCount = UnsignedByte.intValue(data[savedOffset + 2], data[savedOffset + 3]);
        offset = savedOffset + (blockCount * BLOCKSIZE);
      }

      for (BlockRecord block : blocks)
      {
        if (block.type == BLOCKTYPE_FORM)
        {
          parseForm(block.data);
        }
        else if (block.type == BLOCKTYPE_DATA)
        {
          parseData(block.data);
        }
      }
    }

    byte[] readBlockWithContinuations(byte[] data, int startOffset, int blockType) throws Exception
    {
      ByteArrayOutputStream result = new ByteArrayOutputStream();

      int offset = startOffset;
      int continuationType = getContinuationType(blockType);

      if (offset + BLOCKSIZE > data.length)
        return result.toByteArray();

      int blockCount = UnsignedByte.intValue(data[offset + 2], data[offset + 3]);

      result.write(data, offset + 4, 124);
      offset += BLOCKSIZE;

      for (int i = 1; i < blockCount && offset + BLOCKSIZE <= data.length; i++)
      {
        int nextType = UnsignedByte.intValue(data[offset], data[offset + 1]);
        if (nextType != continuationType)
          break;
        result.write(data, offset + 2, 126);
        offset += BLOCKSIZE;
      }

      return result.toByteArray();
    }

    int getContinuationType(int blockType)
    {
      return blockType & 0x7F;
    }

    void parseForm(byte[] formData) throws Exception
    {
      if (formData.length < 4)
        return;

      ByteBuffer bb = ByteBuffer.wrap(formData);
      bb.order(ByteOrder.BIG_ENDIAN);

      int offset = 4;

      for (int i = 0; i < fieldCount && offset < formData.length; i++)
      {
        TextField field = parseTextField(formData, offset);
        if (field == null)
          break;

        String fieldName = field.ftext.trim();
        if (fieldName.isEmpty())
          fieldName = "Scribbles";
        fieldNames.add(fieldName);

        offset += field.totalLength;
      }
    }

    void parseData(byte[] dataData)
    {
      List<String> record = new ArrayList<>();
      int offset = 0;

      for (int i = 0; i < fieldCount && offset < dataData.length; i++)
      {
        TextField field = parseTextField(dataData, offset);
        if (field == null)
          break;

        String value = field.text.trim();
        value = value.replace(" \n", "\n");
        record.add(value);

        offset += field.totalLength;
      }

      if (record.size() == fieldCount)
      {
        dataRecords.add(record);
      }
    }

    TextField parseTextField(byte[] data, int offset)
    {
      if (offset + 2 > data.length)
        return null;

      int fieldLength = UnsignedByte.intValue(data[offset + 1], data[offset]);

      if (offset + 2 + fieldLength > data.length)
        return null;

      TextField field = new TextField();
      field.length = fieldLength;

      int rawStart = offset + 2;
      int rawEnd = rawStart + fieldLength;

      StringBuilder text = new StringBuilder();
      StringBuilder ftext = new StringBuilder();

      boolean postFieldText = false;
      int lengthCount = 0;
      int arrayCount = 0;

      while (lengthCount < fieldLength && rawStart + arrayCount < rawEnd && rawStart + arrayCount < data.length)
      {
        int c = UnsignedByte.intValue(data[rawStart + arrayCount]);
        arrayCount++;
        lengthCount++;

        if (c < 0x80)
        {
          if (c == 0x0d)
          {
            if (postFieldText)
              text.append("\n");
            else
              text.append("\n");
            lengthCount++;
          }
          else
          {
            if (postFieldText)
              text.append((char) c);
            else
              text.append((char) c);
          }
        }
        else
        {
          c &= 0x7F;
          if (rawStart + arrayCount >= data.length)
            break;
          int d = UnsignedByte.intValue(data[rawStart + arrayCount]);
          arrayCount++;
          lengthCount++;

          if (d >= 0xd0 && d <= 0xdf)
          {
            if (rawStart + arrayCount >= data.length)
              break;
            int e = UnsignedByte.intValue(data[rawStart + arrayCount]);
            arrayCount++;
            lengthCount++;

            if ((e & 0x01) == 1)
            {
              if (postFieldText)
                text.append((char) c);
              else
                text.append((char) c);
            }
            else
            {
              ftext.append((char) c);
              postFieldText = true;
            }
          }
          else if (d >= 0x90 && d <= 0x9f)
          {
            if (c != 1 && c != 2 && c != 3 && c != 4 && c != 5)
            {
              ftext.append((char) c);
              postFieldText = true;
            }
          }
          else if (d >= 0x81 && d <= 0x8f)
          {
            if (postFieldText)
              text.append((char) c);
            else
              text.append((char) c);
          }
          else if (d >= 0xc0 && d <= 0xcf)
          {
            if (rawStart + arrayCount >= data.length)
              break;
            arrayCount++;
            lengthCount++;

            if (postFieldText)
              text.append((char) c);
            else
              text.append((char) c);
          }
        }
      }

      field.text = text.toString();
      field.ftext = ftext.toString();
      field.totalLength = 2 + arrayCount;

      return field;
    }

    public String toCSV()
    {
      StringBuilder csv = new StringBuilder();

      csv.append(escapeCSV(fieldNames));
      csv.append("\n");

      for (List<String> record : dataRecords)
      {
        csv.append(escapeCSV(record));
        csv.append("\n");
      }

      return csv.toString();
    }

    String escapeCSV(List<String> fields)
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < fields.size(); i++)
      {
        if (i > 0)
          sb.append(",");

        String field = fields.get(i);
        if (field.contains(",") || field.contains("\"") || field.contains("\n"))
        {
          sb.append("\"");
          sb.append(field.replace("\"", "\"\""));
          sb.append("\"");
        }
        else
        {
          sb.append(field);
        }
      }
      return sb.toString();
    }
  }

  static class BlockRecord
  {
    int type;
    byte[] data;

    BlockRecord(int type, byte[] data)
    {
      this.type = type;
      this.data = data;
    }
  }

  static class TextField
  {
    int length;
    int totalLength;
    String text = "";
    String ftext = "";
  }
}
