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
import java.util.ArrayList;
import java.util.List;

/**
 * GpyFileReader.java
 *
 * Read GNSS files in the compact (.gpy) format.
 * Files must start with a file header, followed by data records.
 *
 * When checksum violations are found in known data records, the record is ignored, but reading continues.
 * If the damaged record is an uncompressed reference record, then the compressed records up to the next intact
 * reference record will also be ignored, which typically causes a loss of a 32-second section.
 *
 * This implementation is forward compatible to new, currently undefined ("unknown") record types, as long as these new
 * records are at least 8 bytes long, and contain the total length in bytes 3 and 4.
 *
 * Checksum violations when reading the file header or unknown records cause a complete read failure.
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */
public class GpyFileReader {

	public final  static int MINIMUM_RECORD_LENGTH = 8; // type, flags, length, >= 2 bytes payload, checksum

	private CompactGnssFileHeader fileHeader;

	private MinimalDataRecord lastFullRecord;


	private int totalRecords;				// data records that we tried to read, including records not used for any reason
	private int nrChecksumErrors;			// number of records with bad checksums
	private int nrCompressedButNoReference;	// number of compressed records ignored after a corrupted reference record
	private int unknownRecords;				// number of records with unknown data type

	private int maxErrorsToList = 50;
	private List<String> problems = new ArrayList<>();


	/**
	 * Read the given CompactGnss file
	 *
	 * @return a list of MinimalData records
	 *
	 * @throws IOException
	 * @throws ChecksumException if the file header or unknown records have an invalid checksum
	 */
	public List<MinimalData> readGPSFile(File gpsFile) throws IOException, ChecksumException {
		InputStream is = new BufferedInputStream(new FileInputStream(gpsFile));
		DataInputStream dataInputStream = new DataInputStream(is);

		List<MinimalData> dataList = null;

		// read the contents of the file
		try {

			fileHeader = readHeader(dataInputStream);

			dataList = new ArrayList<>(18000);	// start with one hour of 5 Hz data

			while (is.available() > 0) {
				totalRecords++;
				CompactGnnsRecord record = readDataRecord(dataInputStream);
				if (record != null && record instanceof MinimalDataRecord) {
					dataList.add(((MinimalDataRecord) record).getMinimalData());
				}
			}
		} finally {
			is.close();
		}
		return dataList;
	}


	/**
	 * Read the file header, which must be the first record in a file
	 */
	private CompactGnssFileHeader readHeader(DataInputStream is) throws IOException, ChecksumException {
		is.mark(16);
		// read the first byte

		// verify type
		int type = is.readByte();
		byte flags = is.readByte();

		// read length
		short headerLength = Short.reverseBytes(is.readShort());

		//System.out.println("Type: " + type + " flags: " + flags + " headerLength: " + headerLength);
		if (type != CompactGnnsRecord.FILE_HEADER) {
			throw new IOException("First byte " + type + " is not FILE_HEADER " + CompactGnnsRecord.FILE_HEADER);
		}
		// read content
		is.reset();
		byte[] headerBytes = new byte[headerLength];
		is.readFully(headerBytes);

		CompactGnssFileHeader fileHeader = new CompactGnssFileHeader();
		// parse content
		fileHeader.parse(headerBytes);

		return fileHeader;
	}


	/**
	 * Read one data record in minimal or compressed minimal format
	 */
	private CompactGnnsRecord readDataRecord(DataInputStream is) throws IOException, ChecksumException {
		is.mark(1);

		// read record type
		byte recordType = is.readByte();
		is.reset();

		if (recordType == CompactGnnsRecord.MINIMAL_DATA) {
			return readUncompressedRecord(is);

		} else if (recordType == CompactGnnsRecord.COMPRESSED_MINIMAL) {
			return readCompressedMinimalRecord(is);

		} else {
			return readUnknownRecord(is);
		}
	}


	/**
	 * Read and return an uncompressed MinimalDataRecord
	 *
	 * @throws IOException
	 */
	private MinimalDataRecord readUncompressedRecord(DataInputStream is) throws IOException {
		MinimalDataRecord record;
		try {
			record = new MinimalDataRecord();
			byte[] bytes = new byte[record.size()];
			is.readFully(bytes);
			record.parse(bytes);
			lastFullRecord = record;
			//addSpeedRecord(record, speedData);
			return record;
		} catch (ChecksumException e) {
			record = null;
			lastFullRecord = null;	// following compressed records will be overread until the next reference record
			nrChecksumErrors++;
			problems.add("Record " + totalRecords + " (uncompressed): " + e.getMessage());
		}
		return record;
	}


	/**
	 * Read a compressed record, and combine it with the last uncompressed reference record
	 * to return a MinimalDataRecord
	 *
	 * @throws IOException
	 */
	private MinimalDataRecord readCompressedMinimalRecord(DataInputStream is) throws IOException {
		CompressedDataRecord compressedRecord = new CompressedDataRecord();
		byte[] bytes = new byte[compressedRecord.size()];
		is.readFully(bytes);

		MinimalDataRecord fromCompressed = null;
		if (lastFullRecord == null) {
			// cannot use compressed records if we do not have a valid reference record,
			// which happens if an uncompressed reference record is corrupted
			nrCompressedButNoReference++;
			problems.add("Record " + totalRecords + " (compressed): No uncompressed reference");

		} else {
			try {
				compressedRecord.parse(bytes);
				fromCompressed = new MinimalDataRecord(lastFullRecord, compressedRecord);
			} catch (ChecksumException e) {
				nrChecksumErrors++;
				problems.add("Record " + totalRecords + " (compressed): " + e.getMessage());
			}
		}
		return fromCompressed;
	}


	/**
	 * Read an unknown record.
	 * Unknown (custom) records must contain the length ot the record (>=8) including type and checksum) after the
	 * first two bytes.
	 *
	 * @throws IOException if any IO problems (like invalid checksum or length) are encountered
	 */
	private CompactGnnsRecord readUnknownRecord(DataInputStream is) throws IOException, ChecksumException {
		is.mark(16);

		// read record type
		byte recordType = is.readByte();
		byte flags = is.readByte();
		int length = Short.reverseBytes(is.readShort());
		// length in unsigned short, which would result in a negative signed short for values > 32767
		if (length < 0) {
			length += 65536;
		}
		if (length < MINIMUM_RECORD_LENGTH || length > is.available()) {
			// a corrupt record that we cannot
			throw new IOException("Invalid length: " + length + " for unknown record of type " + recordType);
		}

		is.reset();
		byte[] bytes = new byte[length];
		is.readFully(bytes);
		unknownRecords++;
		VariableLengthRecord record = new VariableLengthRecord();
		record.parse(bytes);
		return record;
	}


	public List<String> getProblems() {
		return problems;
	}

	public CompactGnssFileHeader getFileHeader() {
		return fileHeader;
	}

	public int getNumProblems() {
		return nrChecksumErrors + nrCompressedButNoReference;
	}

	public int getUnknownRecords() {
		return unknownRecords;
	}
}