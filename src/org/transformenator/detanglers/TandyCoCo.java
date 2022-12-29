/*
 * Transformenator - perform transformation operations on files Copyright (C) 2022 by David Schmidt
 * 32302105+RetroFloppySupport@users.noreply.github.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.transformenator.detanglers;

import java.io.ByteArrayOutputStream;
import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;

public class TandyCoCo extends ADetangler
{
  /*
   * A Tandy Color Computer filesystem
   */
  public void detangle(FileInterpreter parent, byte inData[], String outDirectory, String inFile, String fileSuffix)
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (UnsignedByte.intValue(inData[0]) != 0xff)
    {
      System.out.println("Probably not a CoCo BASIC file.");
    }
    else
    {
      int fileLength = UnsignedByte.intValue(inData[2], inData[1]);

      //int someAddress = UnsignedByte.intValue(inData[cursor + 3], inData[cursor + 2]);
      int cursor = 3;
      while (cursor < (inData.length - 2))
      {
        System.out.println("Top of loop, cursor=0x" + Integer.toHexString(cursor) + " inData.length=0x"
            + Integer.toHexString(inData.length));
        int nextMem = UnsignedByte.intValue(inData[cursor + 1], inData[cursor + 0]) - 0x0e00;
        int lineNumber = UnsignedByte.intValue(inData[cursor + 3], inData[cursor + 2]);
        System.out.println("nextMem: 0x" + Integer.toHexString(nextMem) + " current line: " + lineNumber);
        try
        {
          // Work on a single line
          Boolean inComment = false;
          Boolean inString = false;
          out.write((lineNumber + " ").getBytes());
          for (int i = cursor + 4; i <= nextMem; i++)
          {
            Boolean isExtended = false;
            int currentByte = UnsignedByte.intValue(inData[i]);
            int nextByte = UnsignedByte.intValue(inData[i + 1]);
            if ((!inString && currentByte == 58) && (nextByte == 131 || nextByte == 132))
            {
              // special code for apostrophe remarks and for ELSE statements
              // tokenization always puts a colon in front; detokenization needs to remove it
              continue;
            }
            else
              if ((currentByte > 31) && (currentByte < 128))
              {
                out.write(inData[i]);
                if (inData[i] == '"')
                  inString = !inString;
              }
              else
                if (!inString && !inComment)
                {
                  if (currentByte == 255)
                  {
                    isExtended = true;
                    i++;
                    currentByte = UnsignedByte.intValue(inData[i]);
                  }
                  out.write(toToken(currentByte, isExtended));
                  //inRemark = 1 if $tokens{$byteCode} eq 'REM' || $tokens{$byteCode} eq "'";

                  System.out.println(" 0x" + Integer.toHexString(currentByte));
                  //out.write(Integer.toHexString(currentByte).getBytes());
                }
          }
        }
        catch (Exception e)
        {

        }
        out.write(0x0d);
        out.write(0x0a);
        cursor = nextMem + 2;
        System.out.println("Bottom of loop, cursor=0x" + Integer.toHexString(cursor));
      }
      parent.emitFile(out.toByteArray(), outDirectory, "", inFile + fileSuffix);
    }
  }

  byte[] toToken(int token, boolean isExtended)
  {
    String val = "";
    if (isExtended)
    {
      switch (token)
      {
        case 128:
          val = "SGN";
          break;
        case 129:
          val = "INT";
          break;
        case 130:
          val = "ABS";
          break;
        case 131:
          val = "USR";
          break;
        case 132:
          val = "RND";
          break;
        case 133:
          val = "SIN";
          break;
        case 134:
          val = "PEEK";
          break;
        case 135:
          val = "LEN";
          break;
        case 136:
          val = "STR$";
          break;
        case 137:
          val = "VAL";
          break;
        case 138:
          val = "ASC";
          break;
        case 139:
          val = "CHR$";
          break;
        case 140:
          val = "EOF";
          break;
        case 141:
          val = "JOYSTK";
          break;
        case 142:
          val = "LEFT$";
          break;
        case 143:
          val = "RIGHT$";
          break;
        case 144:
          val = "MID$";
          break;
        case 145:
          val = "POINT";
          break;
        case 146:
          val = "INKEY$";
          break;
        case 147:
          val = "MEM";
          break;
        case 148:
          val = "ATN";
          break;
        case 149:
          val = "COS";
          break;
        case 150:
          val = "TAN";
          break;
        case 151:
          val = "EXP";
          break;
        case 152:
          val = "FIX";
          break;
        case 153:
          val = "LOG";
          break;
        case 154:
          val = "POS";
          break;
        case 155:
          val = "SQR";
          break;
        case 156:
          val = "HEX$";
          break;
        case 157:
          val = "VARPTR";
          break;
        case 158:
          val = "INSTR";
          break;
        case 159:
          val = "TIMER";
          break;
        case 160:
          val = "PPOINT";
          break;
        case 161:
          val = "STRING$";
          break;
        case 162:
          val = "CVN";
          break;
        case 163:
          val = "FREE";
          break;
        case 164:
          val = "LOC";
          break;
        case 165:
          val = "LOF";
          break;
        case 166:
          val = "MKN$";
          break;
        case 167:
          val = "AS";
          break;
        default:
          val = "<e+0x" + Integer.toHexString(token) + ">";
          break;
      }
    }
    else
    {
      switch (token)
      {
        case 128:
          val = "FOR";
          break;
        case 129:
          val = "GO";
          break;
        case 130:
          val = "REM";
          break;
        case 131:
          val = "'";
          break;
        case 132:
          val = "ELSE";
          break;
        case 133:
          val = "IF";
          break;
        case 134:
          val = "DATA";
          break;
        case 135:
          val = "PRINT";
          break;
        case 136:
          val = "ON";
          break;
        case 137:
          val = "INPUT";
          break;
        case 138:
          val = "END";
          break;
        case 139:
          val = "NEXT";
          break;
        case 140:
          val = "DIM";
          break;
        case 141:
          val = "READ";
          break;
        case 142:
          val = "RUN";
          break;
        case 143:
          val = "RESTORE";
          break;
        case 144:
          val = "RETURN";
          break;
        case 145:
          val = "STOP";
          break;
        case 146:
          val = "POKE";
          break;
        case 147:
          val = "CONT";
          break;
        case 148:
          val = "LIST";
          break;
        case 149:
          val = "CLEAR";
          break;
        case 150:
          val = "NEW";
          break;
        case 151:
          val = "CLOAD";
          break;
        case 152:
          val = "CSAVE";
          break;
        case 153:
          val = "OPEN";
          break;
        case 154:
          val = "CLOSE";
          break;
        case 155:
          val = "LLIST";
          break;
        case 156:
          val = "SET";
          break;
        case 157:
          val = "RESET";
          break;
        case 158:
          val = "CLS";
          break;
        case 159:
          val = "MOTOR";
          break;
        case 160:
          val = "SOUND";
          break;
        case 161:
          val = "AUDIO";
          break;
        case 162:
          val = "EXEC";
          break;
        case 163:
          val = "SKIPF";
          break;
        case 164:
          val = "TAB(";
          break;
        case 165:
          val = "TO";
          break;
        case 166:
          val = "SUB";
          break;
        case 167:
          val = "THEN";
          break;
        case 168:
          val = "NOT";
          break;
        case 169:
          val = "STEP";
          break;
        case 170:
          val = "OFF";
          break;
        case 171:
          val = "+";
          break;
        case 172:
          val = "-";
          break;
        case 173:
          val = "*";
          break;
        case 174:
          val = "/";
          break;
        case 175:
          val = "^";
          break;
        case 176:
          val = "AND";
          break;
        case 177:
          val = "OR";
          break;
        case 178:
          val = ">";
          break;
        case 179:
          val = "=";
          break;
        case 180:
          val = "<";
          break;
        case 181:
          val = "DEL";
          break;
        case 182:
          val = "EDIT";
          break;
        case 183:
          val = "TRON";
          break;
        case 184:
          val = "TROFF";
          break;
        case 185:
          val = "DEF";
          break;
        case 186:
          val = "LET";
          break;
        case 187:
          val = "LINE";
          break;
        case 188:
          val = "PCLS";
          break;
        case 189:
          val = "PSET";
          break;
        case 190:
          val = "PRESET";
          break;
        case 191:
          val = "SCREEN";
          break;
        case 192:
          val = "PCLEAR";
          break;
        case 193:
          val = "COLOR";
          break;
        case 194:
          val = "CIRCLE";
          break;
        case 195:
          val = "PAINT";
          break;
        case 196:
          val = "GET";
          break;
        case 197:
          val = "PUT";
          break;
        case 198:
          val = "DRAW";
          break;
        case 199:
          val = "PCOPY";
          break;
        case 200:
          val = "PMODE";
          break;
        case 201:
          val = "PLAY";
          break;
        case 202:
          val = "DLOAD";
          break;
        case 203:
          val = "RENUM";
          break;
        case 204:
          val = "FN";
          break;
        case 205:
          val = "USING";
          break;
        case 206:
          val = "DIR";
          break;
        case 207:
          val = "DRIVE";
          break;
        case 208:
          val = "FIELD";
          break;
        case 209:
          val = "FILES";
          break;
        case 210:
          val = "KILL";
          break;
        case 211:
          val = "LOAD";
          break;
        case 212:
          val = "LSET";
          break;
        case 213:
          val = "MERGE";
          break;
        case 214:
          val = "RENAME";
          break;
        case 215:
          val = "RSET";
          break;
        case 216:
          val = "SAVE";
          break;
        case 217:
          val = "WRITE";
          break;
        case 218:
          val = "VERIFY";
          break;
        case 219:
          val = "UNLOAD";
          break;
        case 220:
          val = "DSKINI";
          break;
        case 221:
          val = "BACKUP";
          break;
        case 222:
          val = "COPY";
          break;
        case 223:
          val = "DSKI$";
          break;
        case 224:
          val = "DSKO$";
          break;
        default:
          val = "<0x" + Integer.toHexString(token) + ">";
          break;
      }
    }
    return val.getBytes();
  }
}