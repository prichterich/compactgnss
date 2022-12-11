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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * CompactGnnsRecord.java
 *
 * Abstract parent class of all records in CompactGNSS files
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public abstract class CompactGnnsRecord {
	// Record type definitions
	public final static byte FILE_HEADER 	  	= (byte) 0xF0;
	public final static byte MINIMAL_DATA     	= (byte) 0xE0;	// minimal data, uncompressed
	public final static byte COMPRESSED_MINIMAL = (byte) 0xD0;	// minimal data, compressed

	public byte type;
	public byte flags = 0;

//	int ckA, ckB;

	/**
	 * Return the size for saving, including type and checksum
	 */
	abstract int size();


	/**
	 * Parse data previously read from file, which include type and checksum
	 */
	abstract void parse(byte[] bytes) throws IOException, ChecksumException;


	/**
	 * Write the record to the given DataOutputStream
	 *
	 * @return the number of bytes written
	 */
	public int write(DataOutputStream output) throws IOException {
		int initialSize = Math.max(16, size());
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(initialSize);
		DataOutputStream byteOS = new DataOutputStream(byteArrayOutputStream);
		writeToByteArray(byteOS);
		byteOS.flush();
		byte[] result = byteArrayOutputStream.toByteArray();
		calcCheckSum(result);
		output.write(result);
		output.write(ckA);
		output.write(ckB);
		return result.length + 2;
	}


	int ckA, ckB;

	void calcCheckSum(byte[] data) {
		calcCheckSum(data, data.length);
		byte[] cs = new byte[2];
	}

	void calcCheckSum(byte[] data, int last) {
		ckA = 0;
		ckB = 0;
		for (int i = 0; i < last; i++) {
			ckA = (ckA + data[i])  & 0xFF;
			ckB = (ckB + ckA)  & 0xFF;
		}
	}


	/**
	 * Verify the checksum for the given record (which includes everything from the type byte to the two checksum bytes)
	 */
	public void verifyChecksum(byte[] record) throws ChecksumException {
		int len = record.length;
		int expectedA = byteToUnsignedInt(record[len-2]);
		int expectedB = byteToUnsignedInt(record[len-1]);

		calcCheckSum(record, record.length -2);	// omit the 2 checksum record
		if (ckA != expectedA || ckB != expectedB) {
			throw new ChecksumException(
					"Invalid checksum - expected " + expectedA + "," + expectedB + " but was " + ckA	 + "," + ckB
					);
		}
	}


	protected int byteToUnsignedInt(byte b) {
		return b >= 0 ? b : b + 256;
	}

	abstract void writeToByteArray(DataOutputStream byteOS) throws IOException;

}
