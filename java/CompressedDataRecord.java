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
 * CompressedDataRecord.java
 *
 * A compressed version of minimal GNSS data records
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class CompressedDataRecord extends MinimalDataRecord {
	public static final int COMPRESSED_MINIMAL_RECORD_SIZE = 20;

	// order defined so that it can be copied directly to a C struct

	// Common to all records so defined in the parent class
//	byte type;
//	byte flags = 0;

//	short hdop;                // unit2

	// all delta values are signed (2 byte integers)
	short timeDelta;
	short speedDelta;
	short speedErrDelta;
	short latitudeDelta;
	short longitudeDelta;
	short courseDelta;        // delta (course / 1000) !

//	byte sats;                 // uint1
//	byte fix;                  // uint1


	private boolean valid;


	/**
	 * Create a new compressed record for reading from file
	 */
	public CompressedDataRecord() {
		type = COMPRESSED_MINIMAL;
	}

	/**
	 * Create a compressed record by calculating the differences to the last compressed record
	 * (and, for course, also scaling), checking for data overflow.
	 * If overflow is detected, the dataOverflow flag is set.
	 */
	public CompressedDataRecord(MinimalDataRecord current, MinimalDataRecord lastUncompressed) {

		type = COMPRESSED_MINIMAL;

		if (lastUncompressed == null) {
			valid = false;
		} else {
			sats = current.sats;
			hdop = current.hdop;
			fix = current.fix;

			try {
				timeDelta 		= deltaLong(current.dateTime, lastUncompressed.dateTime);
				speedDelta = deltaInt(current.speed, lastUncompressed.speed);
				speedErrDelta = deltaInt(current.speedErr, lastUncompressed.speedErr);
				latitudeDelta = deltaInt(current.latitude, lastUncompressed.latitude);
				longitudeDelta = deltaInt(current.longitude, lastUncompressed.longitude);
				courseDelta 	= deltaInt(current.course / 1000, lastUncompressed.course / 1000);

				valid = true;
			} catch (ArithmeticException e) {
				valid = false;
			}
		}
	}


	/**
	 * Calculate the different between the given current and last long values,
	 * and return it as a short
	 *
	 * @throws ArithmeticException when overflow occurs
	 */
	short deltaLong(long currentV, long lastV) throws ArithmeticException {
		long deltaLong = currentV - lastV;
		short deltaShort = (short) deltaLong;
		if (deltaLong != deltaShort) {
			//System.out.println("Overflow for " + currentV + " - " + lastV + ": " + deltaLong + " != " + deltaShort);

			throw new ArithmeticException();
		}
		return deltaShort;
	}


	/**
	 * Calculate the different between the given current and last int values,
	 * and return it as a short
	 *
	 * @throws ArithmeticException when overflow occurs
	 */
	short deltaInt(int currentV, int lastV) throws ArithmeticException {
		int deltaInt = currentV - lastV;
		short deltaShort = (short) deltaInt;
		if (deltaInt != deltaShort) {
			throw new ArithmeticException();
		}
		return deltaShort;
	}


	public int size() {
		return COMPRESSED_MINIMAL_RECORD_SIZE;
	}


	/**
	 * Parse data previously read from file, which include type and checksum
	 */
	@Override
	public void parse(byte[] bytes) throws IOException, ChecksumException {
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
		type = is.readByte();
		if (type != COMPRESSED_MINIMAL) {
			throw new IOException("Wrong first byte - expected " + MINIMAL_DATA + " but was " + type + " " + bytes[0]);
		}
		flags = is.readByte();
		hdop = Short.reverseBytes(is.readShort());

		timeDelta = Short.reverseBytes(is.readShort());
		speedDelta = Short.reverseBytes(is.readShort());

		speedErrDelta = Short.reverseBytes(is.readShort());
		latitudeDelta = Short.reverseBytes(is.readShort());
		longitudeDelta = Short.reverseBytes(is.readShort());
		courseDelta = Short.reverseBytes(is.readShort());

		sats = is.readByte();
		fix = is.readByte();
		verifyChecksum(bytes);
	}


	@Override
	void writeToByteArray(DataOutputStream output) throws IOException {
		output.write(type);
		output.write(flags);
		output.writeShort(Short.reverseBytes(hdop));

		output.writeShort(Short.reverseBytes(timeDelta));
		output.writeShort(Short.reverseBytes(speedDelta));
		output.writeShort(Short.reverseBytes(speedErrDelta));
		output.writeShort(Short.reverseBytes(latitudeDelta));
		output.writeShort(Short.reverseBytes(longitudeDelta));
		output.writeShort(Short.reverseBytes(courseDelta));

		output.write(sats);
		output.write(fix);
	}

	/**
	 * Is this record valid?
	 *
	 * @return false if any data overflow occurred
	 */
	public boolean isValid() {
		return valid;
	}
}
