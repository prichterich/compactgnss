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

/**
 * GpyFileWriter.java
 *
 * Write compact GNSS (.gpy) files
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class GpyFileWriter {

	private DataOutputStream output;
	private MinimalDataRecord lastUncompressed = null;

	private int compressedCnt = 0;
	private int uncompressedCnt = 0;


	/**
	 * Create a new GpyFileWriter for writing to the specified file
	 *
	 * @throws FileNotFoundException if the file cannot be created for any reason
	 */
	public GpyFileWriter(File exportFile) throws FileNotFoundException {
		output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(exportFile)));
	}


	/**
	 * Write a file header describing the device used
	 *
	 * @param gnssDeviceType	Basic descriptor the GPS chip used, e.g. CompactGnssFileHeader.LOCOSYS, .UBLOX, .UNKNOWN
	 * @param deviceDescription Detailed text device description, e.g. "Locosys GT-31"
	 * @param deviceName		Device nickname, e.g. the MAC address for ESP loggers
	 * @param serialNumber		The MAC address for ESP loggers
	 * @param firmwareVersion	The firmware used on the logger
	 * @return the number of bytes written
	 *
	 * @throws IOException
	 */
	public int writeHeader(short gnssDeviceType, String deviceDescription, String deviceName, String serialNumber, String firmwareVersion) throws IOException {
		CompactGnssFileHeader fileHeader = new CompactGnssFileHeader(gnssDeviceType, deviceDescription, deviceName, serialNumber, firmwareVersion);
		return fileHeader.write(output);
	}


	/**
	 * Write one data record, using uncompressed records for the first record and when required, and compressed records
	 * whenever possible.
	 *
	 * See MinimalDataRecord for a description of the parameters and units used.
	 *
	 * @return the number of bytes written
	 */
	public int writeDataRecord(byte flags, short hdop, long dateTime, int speed, int speedErr, int latitude, int longitude, int course, byte sats, byte fix) throws IOException {
		MinimalDataRecord uncompressed = new MinimalDataRecord(
				flags, hdop, dateTime, speed, speedErr, latitude, longitude, course, sats,  fix
		);
		CompressedDataRecord compressed =  new CompressedDataRecord(uncompressed, lastUncompressed);

		CompactGnnsRecord recordToUse;
		if (compressed.isValid()) {		// valid unless lastUncompressed is null, or a delta overflow occurred
			recordToUse = compressed;
			compressedCnt++;
		} else {
			recordToUse = uncompressed;
			lastUncompressed = uncompressed;	// update the reference record
			uncompressedCnt++;
		}
		return recordToUse.write(output);
	}


	/**
	 * Close the output file. Must be called after writing all data records.
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (output != null) {
			output.close();
		}
	}

	public int getCompressedCnt() {
		return compressedCnt;
	}

	public int getUncompressedCnt() {
		return uncompressedCnt;
	}


	public DataOutputStream getOutput() {
		return output;
	}
}
