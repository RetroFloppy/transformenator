/*
 * Transformenator - perform transformation operations on files
 * Copyright (C) 2023 by David Schmidt
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

import org.transformenator.internal.FileInterpreter;
import org.transformenator.internal.UnsignedByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;             
import java.util.Arrays;                                        

public class Datapoint extends ADetangler 
{
  @Override
  public void detangle(FileInterpreter interpreter, byte[] inData, String outDirectory, String inFile, String fileSuffix, boolean isDebugMode)
  {
    // For each directory-based sector (logical sector 12 in tracks 17-32)
    for (int dirTrack = 17; dirTrack <= 32; dirTrack ++)
    {
      int cursor = dosSector(dirTrack);
      int j;
      // For each "slot" (file entry) in this DOS sector
      for (int i = 0; i < 256; i += 16)
      {
        String fileName = "";
        int track = -1;
        int cluster = -1;
        if (inData[i+cursor] == -1)
          continue;
        for (j = 0; j < 8; j++)
        {
          fileName += (char)inData[i+cursor+4+j];
        }
        fileName = fileName.trim();
        fileName += ".";
        for (j = 8; j < 11; j++)
        {
          fileName += (char)inData[i+cursor+4+j];
        }
        track = UnsignedByte.intValue(inData[cursor+i]);
        cluster = UnsignedByte.intValue(inData[cursor+i+1])/64;
        emitFile(interpreter, track, cluster, inData, outDirectory, inFile, fileName, isDebugMode);
      }
    }
  }

  void emitFile(FileInterpreter interpreter, int track, int cluster, byte inData[], String outDirectory, String inFile, String fileName, boolean isDebugMode)
  {
    int fileAddr = clusterAddress(track,cluster);
    int pfn = UnsignedByte.intValue(inData[fileAddr]);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (isDebugMode)
      System.out.println("Filename: "+fileName + " Track: 0x"+Integer.toHexString(track)+" cluster: "+cluster + " fileAddr: 0x"+Integer.toHexString(clusterAddress(track,cluster)) + " PFN: 0x"+String.format("%1$02X",pfn));
    int cursor = 4;
    int sdTrack = UnsignedByte.intValue(inData[fileAddr+cursor]);
    int sdCluster = UnsignedByte.intValue(inData[fileAddr+cursor+1]) / 32 / 2;
    int sdNumClusters = UnsignedByte.intValue(inData[fileAddr+cursor+1]) & 31;
    int segNum = 0;
    boolean isText = fileName.endsWith(".TXT");
    // Process the segment descriptors
    while (sdTrack != 0xff && sdCluster != 0xff)
    {
      int ca = clusterAddress(sdTrack,sdCluster);
      if (isDebugMode)
      {
        System.out.println("  Segment "+segNum);
        System.out.println("    sdTrack: 0x"+String.format("%1$02X",sdTrack));
        System.out.println("    sdCluster: 0x"+String.format("%1$02X",sdCluster));
        System.out.println("    sdNumClusters: 0x"+String.format("%1$02X",sdNumClusters));
        System.out.println("    address: 0x"+String.format("%1$04X",ca));
        System.out.println("    PFN: 0x"+String.format("%1$02X",UnsignedByte.intValue(inData[ca])));
        System.out.println("    LRN: 0x"+String.format("%1$04X",UnsignedByte.intValue(inData[ca+1],inData[ca+2])));
      }
      int logicalSector = sdCluster * 3;
      boolean isRIB = true;
      for (int i = 0; i < sdNumClusters + 1; i++)
      {
        int physAddr;
        for (int j = 0; j < 3; j++)
        {
          physAddr = sectorAddress(sdTrack, logicalSector + j);
          if (isDebugMode)
          {
            System.out.print("    sector "+(j+1)+" addr: 0x"+String.format("%1$04X",physAddr));
            System.out.print(" PFN: 0x"+String.format("%1$02X",UnsignedByte.intValue(inData[physAddr])));
            System.out.print(" LRN: 0x"+String.format("%1$04X",UnsignedByte.intValue(inData[physAddr+1],inData[physAddr+2])));
          }
          if ((isRIB == false) || (j > 1))
          {
            if (isDebugMode)
              System.out.print(" (dumping)");
            dumpSector(out, inData, physAddr, isText, isDebugMode);
          }
          if (isDebugMode)
            System.out.println();
        }
        isRIB = false;
        logicalSector += 3;
        if (logicalSector > 9)
        {
          logicalSector = 0;
          sdTrack++;
        }
      }
      // Go get the next segment descriptor
      segNum ++;
      cursor += 2;
      sdTrack = UnsignedByte.intValue(inData[fileAddr+cursor]);
      sdCluster = UnsignedByte.intValue(inData[fileAddr+cursor+1]) / 32 / 2;
      sdNumClusters = UnsignedByte.intValue(inData[fileAddr+cursor+1]) & 31;
    }
    interpreter.emitFile(out.toByteArray(), outDirectory, inFile.substring(0,(inFile.lastIndexOf('.')>0?inFile.lastIndexOf('.'):inFile.length())), fileName);
  }

  void dumpSector(ByteArrayOutputStream out, byte inData[], int physicalAddress, boolean isText, boolean isDebugMode)
  {
    if (out != null)
    {
      int end = 255;
      int e5Accum = 0;
      if (isText)
      {
        for (end = 3; end < 255; end++)
        {
          if (UnsignedByte.intValue(inData[physicalAddress + end]) == 0xe5)
            e5Accum++;
          if (inData[physicalAddress + end] == 0x03)
          {
            if (isDebugMode)
              System.out.print(" stopping dump of sector at 0x"+String.format("%1$02X",end));
            break;
          }
        }
        if ((e5Accum + 3) == end)
        {
          if (isDebugMode)
            System.out.print(" sector appears to be all 0xe5 until "+end);
          end = 3;
        }
      }
      byte range[] = Arrays.copyOfRange(inData, physicalAddress+3, physicalAddress+end);
      try
      {
        out.write(range);
        out.flush();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }

  int dosSector (int track)
  {
    return (track * (128*26)) + (physicalSector(track, 12) * 256);
  }

  int physicalSector(int track, int logicalSector)
  {
    int remainder = track % 4;
    int pSector = (logicalSector + remainder) * 5;
    pSector = pSector % 13;
    return pSector;
  }

  int sectorAddress(int track, int logicalSector)
  {
    int cursor = track * (128 * 26);
    cursor += physicalSector(track,logicalSector) * 256;
    return cursor;
  }

  int clusterAddress(int track, int cluster)
  {
    int cursor = track * (128 * 26);
    cluster = cluster * 3;
    cursor += physicalSector(track,cluster) * 256;
    return cursor;
  }
}
