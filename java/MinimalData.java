/*
The MIT License (MIT)

Copyright 2022 Peter Richterich

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the “Software”), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.compactgnss;

/**
 * MinimalData.java
 *
 * Minimal set if GNSS data items for GPS speed analysis and track display
 * see https://github.com/prichterich/compactgnss
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class MinimalData {
	// public byte type;		// used for IO; may be needed in C structs for alignment!
	public byte flags = 0;		// currently not used when logging, set to 0

	public short hdop;          // unit2 actual HDOP * 100

	public long dateTime;		// signed long, unix time, UTC time zone

	public int speed;           // uint4 	in mm/s
	public int speedErr;        // uint4	in mm/s
	public int latitude;        // int4		[-90 to 90] in degrees with resolution 0.0000001
	public int longitude;       // int4		[-180 to 180] in degrees with resolution 0.0000001
	public int course;          // uint4   	Course Over Ground [0 to 360] in degrees with resolution 0.00001

	public byte sats;            // uint1	Number of satellites used
	public byte fix;             // uint1	3 = 3D fix, 2 = 2D fix, 0 = no fix (following u-blox definitions)

	// For IO, two checksum bytes are added. Adding them to a C struct definition may be beneficial for alignment.
}
