/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2020 by David Schmidt
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

public class TrackReorger
{
	public static byte[] reorg(byte[] inData, int skewedSectorMap[], int bytesPerSector)
	{
		byte[] outData = new byte[inData.length];
		int bytesPerTrack = skewedSectorMap.length * bytesPerSector;
		if (inData.length % (bytesPerTrack) == 0)
		{
			// For each track
			for (int i = 0; i < inData.length / bytesPerTrack; i++)
			{
				System.err.println("DEBUG: working on track "+i);
				// For each sector in the track
				for (int j = 0; j < skewedSectorMap.length; j++)
				{
					System.err.print("sector "+j+": ");
					// System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
					System.arraycopy(inData, (bytesPerTrack * i) + (skewedSectorMap[j] * bytesPerSector), outData, (bytesPerTrack * i) + (j * bytesPerSector), bytesPerSector);
					System.err.print("moving "+skewedSectorMap[j]+" to "+j+"; ");
				}
				System.err.println();
			}
			return outData;
		}
		else
		{
			System.err.println("DEBUG: sector geometry doesn't fit incoming data.");
			return inData;
		}
	}
}