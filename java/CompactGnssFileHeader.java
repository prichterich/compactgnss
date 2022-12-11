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


import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * CompactGnssFileHeader.java
 *
 * File header for CompactGNSS files, with information about the device used
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class CompactGnssFileHeader extends VariableLengthRecord {

	public static final short UNKNOWN 	=   0;
	public static final short LOCOSYS 	=   1;
	public static final short UBLOX 	=   2;
	public static final short GARMIN 	=   3;
	public static final short SUUNTO 	=   4;
	public static final short COROS 	=   5;
	public static final short OTHER_DEV = 255;


	public static final int STRING_IO_LENGTH = 16;

	// defined in the parent class
//	byte type;
//	byte flags = 0;	// set to 1 o indicate variable length????
//	short length; 	// the length of this record as saved, in bytes


	// Using unixtime, no session-specific epoch
	// Endian is always little endian
	// Compression is record-type specific, not defined in file header

	// Analysis needs to know if this is a Locosys, u-blox, or other type of GPS; "maker" is secondary (the Beitians are not made by u-blox)
	private short gnssDeviceType;

	// for simplicity, we write all strings as byte[16]; for shorter strings, a 0 is added after the content,
	// and for longer strings, only the first 16 characters (after UTF-8 conversion) are written
	private String deviceDescription;		// e.g. "Locosys GT-31"
	private String deviceName;		// nickname, settable by the user
	private String serialNumber;	// MAC for ESP loggers
	private String firmwareVersion;

	/**
	 * Constructor for reading from file
	 */
	public CompactGnssFileHeader() {
		this.type = FILE_HEADER;
		length = 6 + 4 * STRING_IO_LENGTH + 2;
	}


	/**
	 * Constructor for saving
	 * @param gnssDeviceType UNKNOWN = 0, LOCOSYS = 1, UBLOX = 2, GARMIN = 3, SUUNTO = 4, COROS = 5, other = 255
	 * @param deviceDescription for example "Locosys GT-31" (without quotes, max 16 characters)
	 * @param deviceName nickname, settable by the user
	 * @param serialNumber use MAC address for ESP loggers
	 * @param firmwareVersion as String,  max 16 characters
	 */
	public CompactGnssFileHeader(short gnssDeviceType, String deviceDescription, String deviceName, String serialNumber, String firmwareVersion) {
		this();
		this.flags = 0;	// could use the byte for length, with xFF for overflow

		// 2 bytes record length (including checksum) are next

		this.gnssDeviceType = gnssDeviceType;

		this.deviceDescription = deviceDescription;
		this.deviceName = deviceName;
		this.serialNumber = serialNumber;
		this.firmwareVersion = firmwareVersion;
	}
	

	/**
	 * Parse data previously read from file, which include type and checksum
	 */
	@Override
	public void parse(byte[] bytes) throws IOException, ChecksumException {
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
		type = is.readByte();
		flags = is.readByte();

		length 		= Short.reverseBytes(is.readShort());
		gnssDeviceType = Short.reverseBytes(is.readShort());

		deviceDescription = readCString(is, STRING_IO_LENGTH);
		deviceName 		= readCString(is, STRING_IO_LENGTH);
		serialNumber 	= readCString(is, STRING_IO_LENGTH);
		firmwareVersion = readCString(is, STRING_IO_LENGTH);

		System.out.println("Header:\n" + toString()
		);
		verifyChecksum(bytes);
	}


	/**
	 * Read fixedLength bytes from the input stream and
	 * parse it like a null-terminated C string.
	 * All fixedLength bytes can be used if the terminating null character omitted
	 * If the first byte is 0, an empty string is returned.
	 */
	private String readCString(DataInputStream is, int fixedLength) throws IOException {
		byte[] bytes = new byte[fixedLength];
		is.readFully(bytes);
		int l = 0;
		while (l < fixedLength && bytes[l] != 0) {
			l++;
		}
		String s = "";
		if (l > 0) {
			s = new String(bytes, 0, l, StandardCharsets.UTF_8);
		}
		return s;
	}


	/**
	 * Return a byte array representation of this header, including type & checksum
	 */
	public byte[] toByteArray()  {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(size());
		DataOutputStream byteOS = new DataOutputStream(byteArrayOutputStream);
		try {
			write(byteOS);
		} catch (IOException e) {
			// ignore
		}
		return byteArrayOutputStream.toByteArray();
	}


	@Override
	void writeToByteArray(DataOutputStream output) throws IOException {
		output.write(type);
		output.write(flags);
		output.writeShort(Short.reverseBytes((short) size()));
		output.writeShort(Short.reverseBytes(gnssDeviceType));
		writeUtfString(deviceDescription, output, STRING_IO_LENGTH);
		writeUtfString(deviceName, output, STRING_IO_LENGTH);
		writeUtfString(serialNumber, output, STRING_IO_LENGTH);
		writeUtfString(firmwareVersion, output, STRING_IO_LENGTH);
	}


	/**
	 * Write the given Java string encoded to UTF-8 as a byte array of the given length.
	 * Shorter strings will be padded with 0, longer strings written truncated
	 * @throws IOException
	 */
	private void writeUtfString(String s, DataOutputStream output, int length) throws IOException {
		byte[] toWrite = new byte[length];
		if (s != null && s.length() > 0) {
			byte[] stringBytes = s.getBytes(StandardCharsets.UTF_8);
			int copyLen = Math.min(length, stringBytes.length);
			System.arraycopy(stringBytes, 0, toWrite, 0, copyLen);
		}
		output.write(toWrite);
	}

	public String toString() {
		return 	"Header length: " + length
				+ " gnssDeviceType: '" + gnssDeviceType + "'"
				+ " deviceDescription: '" + deviceDescription + "'"
				+ " deviceName: '" + deviceName + "'"
				+ " serialNumber: '" + serialNumber + "'"
				+ " firmwareVersion: '" + firmwareVersion + "'"
				;
	}

	public short getGnssDeviceType() {
		return gnssDeviceType;
	}

	public String getDeviceDescription() {
		return deviceDescription;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}
}
