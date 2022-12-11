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
 * VariableLengthRecord.java
 *
 * Parent class for variable length records in CompactGNSS files.
 * Provides functionality to required to read unknown (e.g. custom defined) records, as long
 * as such records have a unique type byte (byte 1), and contain the length of the record in bytes 3 and 4
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class VariableLengthRecord extends CompactGnnsRecord {


	short length = 0; 	// the length of this record as saved, in bytes

	byte[] content;	// all bytes for this record, including the checksum


	/**
	 * Create VariableLengthRecord of the given type and length for testing
	 */
	public static VariableLengthRecord makeTestRecord(byte type, short length) {
		VariableLengthRecord record = new VariableLengthRecord();
		record.length = length;
		record.type = type;
		record.flags = (byte) 0;
		record.content = new byte[length];
		return record;
	}


	/**
	 * Return the size of the record
	 */
	@Override
	public int size() {
		return length;
	}


	/**
	 * Parse data previously read from file, which include type and checksum
	 *
	 * This basic implementation only verifies the checksum at the end of the record.
	 * This allows reading through unknown (for example custom) data records
	 */
	@Override
	public void parse(byte[] bytes) throws IOException, ChecksumException {
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
		type = is.readByte();
		flags = is.readByte();

		length = Short.reverseBytes(is.readShort());
		verifyChecksum(bytes);
		content = bytes;
	}

	
	@Override
	void writeToByteArray(DataOutputStream byteOS) throws IOException {
		// write the content without the last 2 checksum bytes
		if (content != null && content.length > 6) {	// need at least 4 header + 2 checksum bytes
			int toWrite = content.length - 4 - 2;
			byteOS.write(type);
			byteOS.write(flags);
			byteOS.writeShort(Short.reverseBytes((short) size()));
			byteOS.write(content, 4, toWrite);
		} else {
			throw new IOException("Cannot writeToByteArray: content length = " + (content == null ? "0" : content.length));
		}
	}	
}
