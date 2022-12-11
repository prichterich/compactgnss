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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * MinimalDataRecord.java
 *
 * The uncompressed version of minimal GNSS data records
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class MinimalDataRecord extends CompactGnnsRecord {


	public final static int MINIMAL_RECORD_SIZE = 36;

	// order defined so that it can be copied directly to a C struct

	// Common to all records so defined in the parent class
//	byte type;				 // MINIMAL_DATA =  0xE0
//	byte flags = 0;			 // currently not used when logging, set to 0

	public short hdop;              // unit2 actual HDOP * 100

	public long dateTime;			 // signed long, unix time, UTC time zone

	public int speed;               // uint4 	in mm/s
	public int speedErr;            // uint4	in mm/s
	public int latitude;            // int4	[-90 to 90] in degrees with resolution 0.0000001
	public int longitude;           // int4	[-180 to 180] in degrees with resolution 0.0000001
	public int course;              // uint4   Course Over Ground [0 to 360] in degrees with resolution 0.00001

	public byte sats;               // uint1		number of satellites used
	public byte fix;                // uint1		3 = 3D fix, 2 = 2D fix, 0 = no fix

//	byte ckA;				 // full records also contains two checksum bytes (Fletcher's checksum using modulo 256)
//  byte ckB;


	/**
	 * Constructor for reading from file
	 */
	public MinimalDataRecord() {
		this.type = MINIMAL_DATA;
	}


	/**
	 * Constructor for saving
	 */
	public MinimalDataRecord(byte flags, short hdop, long dateTime, int speed, int speedErr, int latitude, int longitude, int course, byte sats, byte fix) {
		this.type = MINIMAL_DATA;
		this.flags = flags;
		this.hdop = hdop;

		this.dateTime = dateTime;
		this.speed = speed;
		this.speedErr = speedErr;
		this.latitude = latitude;
		this.longitude = longitude;
		this.course = course;

		this.sats = sats;
		this.fix = fix;
	}


	public MinimalData getMinimalData() {
		MinimalData md = new MinimalData();
		md.flags = flags;
		md.hdop = hdop;

		md.dateTime = dateTime;
		md.speed = speed;
		md.speedErr = speedErr;
		md.latitude = latitude;
		md.longitude = longitude;
		md.course = course;

		md.sats = sats;
		md.fix = fix;
		return md;
	}

//	static int c = 0;

	/**
	 * Create a new MinimalDataRecord by adding combining the last full reference record and the data in
	 * the delta-compressed record
	 */
	public MinimalDataRecord(MinimalDataRecord lastFullRecord, CompressedDataRecord delta) {
		this.type = MINIMAL_DATA;
		this.flags = delta.flags;
		this.hdop =  delta.hdop;

		this.dateTime 	=  lastFullRecord.dateTime 	+ delta.timeDelta;
		this.speed 		=  lastFullRecord.speed 	+ delta.speedDelta;
		this.speedErr	=  lastFullRecord.speedErr 	+ delta.speedErrDelta;
		this.latitude 	=  lastFullRecord.latitude 	+ delta.latitudeDelta;
		this.longitude 	=  lastFullRecord.longitude + delta.longitudeDelta;
		this.course 	=  (lastFullRecord.course / 1000 	+ delta.courseDelta) * 1000;

		this.sats =  delta.sats;
		this.fix =  delta.fix;
	}


	/**
	 * Return the total record size, including type and checksum
	 */
	public int size() {
		return MINIMAL_RECORD_SIZE;
	}

	/**
	 * Parse data previously read from file, which include type and checksum
	 */
	@Override
	public void parse(byte[] bytes) throws IOException, org.compactgnss.ChecksumException {
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
		type = is.readByte();
		if (type != MINIMAL_DATA) {
			throw new IOException("Wrong first byte - expected " + MINIMAL_DATA + " but was " + type + " " + bytes[0]);
		}
		flags = is.readByte();

		hdop 		= Short.reverseBytes(is.readShort());
		dateTime 	= Long.reverseBytes(is.readLong());
		speed 		= Integer.reverseBytes(is.readInt());
		speedErr 	= Integer.reverseBytes(is.readInt());
		latitude 	= Integer.reverseBytes(is.readInt());
		longitude 	= Integer.reverseBytes(is.readInt());
		course 		= Integer.reverseBytes(is.readInt());

		sats = is.readByte();
		fix = is.readByte();

		verifyChecksum(bytes);
	}




	@Override
	void writeToByteArray(DataOutputStream output) throws IOException {
		output.write(type);
		output.write(flags);
		output.writeShort(Short.reverseBytes(hdop));
		output.writeLong(Long.reverseBytes(dateTime));
		output.writeInt(Integer.reverseBytes(speed));
		output.writeInt(Integer.reverseBytes(speedErr));
		output.writeInt(Integer.reverseBytes(latitude));
		output.writeInt(Integer.reverseBytes(longitude));
		output.writeInt(Integer.reverseBytes(course));
		output.write(sats);
		output.write(fix);
	}
}
