/*
 * Transformenator - perform transformation operations on binary files
 * Copyright (C) 2013-2015 by David Schmidt
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

public class RegSpec
{
	// Just struct-ifying this data type
	public byte[] leftCompare;
	public byte[] leftMask;
	/*
	 * Commands: 
	 * 0 = Normal search
	 * 1 = EOF
	 * 2 = SOF
	 * 3 = SOF (greedy)
	 */
	public int command = 0;
	public boolean backtrack = true;
	public boolean toggle = false;
	/*
	 *  Start out in "false" state for toggling specifications -
	 *  the first action will be to emit the first element, or "on" state
	 */
	public boolean toggleState = false;
}
