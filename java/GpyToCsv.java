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


import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

/**
 * GpyToCsv.java
 *
 * Example code to convert between GPY and CSV files
 *
 * Copyright 2022 Peter Richterich
 *
 * Change history:
 * 12/11/22 PR  Original implementation
 */

public class GpyToCsv extends JFrame {

	private String delim = ",";

	private MessagePanel messagePanel;


	GpyToCsv() {
		super("GPY <-> CSV Converter");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		messagePanel = new MessagePanel();
		add(messagePanel);
		// pack(); (do we need this?)
		setSize(new Dimension(600, 400));
		setVisible(true);
	}


	/**
	 * Show a dialog to select one or more files to convert, and convert them
	 * from .gpy to .csv, or vice versa, without any checks
	 */
	private File[] convertFiles() {
		FileDialog fd = new FileDialog(this, "Choose a .gpy or .csv file", FileDialog.LOAD);
		fd.setVisible(true);
		File[] files = fd.getFiles();
		if (files != null && files.length > 0) {
			try {
				for (File file : files) {
					if (file.getName().toLowerCase().endsWith(".gpy")) {
						gpy2Csv(file);
					}
					else if (file.getName().toLowerCase().endsWith(".csv")) {
						csv2Gpy(file);
					}
					else {
						message("Wrong file type for '" + file.getName() + "'");
					}
				}
			} catch (IOException | ChecksumException e) {
				message(e.getMessage());
			}
		}
		return files;
	}


	/**
	 * Read the given CSV file and create a GPY file from the data
	 */
	private void csv2Gpy(File csvFile) throws IOException {
		List<String> lines = Files.readAllLines(csvFile.toPath());
		String gpyFn = csvFile.getName().replace(".csv", ".gpy");
		File gpyFile = new File(csvFile.getParent(), gpyFn);
		if (gpyFile.exists()) {
			message("File " + gpyFn + " exists, not overwriting .gpy files.");
			return;
		}
		message("Converting " + csvFile.getName() + " to " + gpyFn);
		GpyFileWriter writer = new GpyFileWriter(gpyFile);
		writer.writeHeader(CompactGnssFileHeader.UNKNOWN, "File", csvFile.getName(), "unknown","GpyToCsv");
		for (int i = 1; i < lines.size(); i++) {
			String[] items = lines.get(i).split(delim);
			writer.writeDataRecord(
				  Byte.valueOf(items[0])
				, Short.valueOf(items[1]) 		// hdop
				, Long.valueOf(items[2]) 		// dateTime
				, Integer.valueOf(items[3]) 	// speed
				, Integer.valueOf(items[4]) 	// speedErr
				, Integer.valueOf(items[5]) 	// latitude
				, Integer.valueOf(items[6]) 	// longitude
				, Integer.valueOf(items[7]) 	// course
				, Byte.valueOf(items[8])	 	// sats
				, Byte.valueOf(items[9])		// fix
			);
		}
		writer.close();
		message("Wrote .gpy file " + gpyFile.getAbsolutePath());
	}


	/**
	 * Read the given GPY file and create a CSV file from the data
	 */
	private void gpy2Csv(File gpyFile) throws IOException, ChecksumException {
		GpyFileReader reader = new GpyFileReader();
		java.util.List<MinimalData> dataRecords = reader.readGPSFile(gpyFile);

		String csvFn = gpyFile.getName().replace(".gpy", ".csv");
		File csvFile = new File(gpyFile.getParent(), csvFn);
		message("Converting " + csvFn + " to " + gpyFile.getName());
		writeCsv(dataRecords, csvFile);
		message("Wrote .csv file " + csvFile.getAbsolutePath());
	}


	/**
	 * Write the data in given MinimalData records to the given CSV file
	 */
	private void writeCsv(List<MinimalData> dataRecords, File csvFile) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(csvFile);
		writer.println(
				 		"flags"
				+ delim + "hdop"
				+ delim + "dateTime"
				+ delim + "speed"
				+ delim + "speedErr"
				+ delim + "latitude"
				+ delim + "longitude"
				+ delim + "course"
				+ delim + "sats"
				+ delim + "fix"
		);
		for (MinimalData md : dataRecords) {
			writer.println(md.flags
					+ delim + md.hdop
					+ delim + md.dateTime
					+ delim + md.speed
					+ delim + md.speedErr
					+ delim + md.latitude
					+ delim + md.longitude
					+ delim + md.course
					+ delim + md.sats
					+ delim + md.fix

			);
		}
		System.out.println("Wrote .gpy file " + csvFile.getAbsolutePath());
		writer.close();
	}


	private class MessagePanel extends JPanel {
		protected JTextArea textArea;

		private MessagePanel() {
			super(new GridBagLayout());

			textArea = new JTextArea(10, 40);
			textArea.setEditable(false);
			JScrollPane scrollPane = new JScrollPane(textArea);

			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = GridBagConstraints.REMAINDER;

			c.fill = GridBagConstraints.HORIZONTAL;

			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 1.0;
			add(scrollPane, c);
		}
	}


	public void message(String msg) {
		messagePanel.textArea.append(msg + "\n");
		messagePanel.textArea.setCaretPosition(messagePanel.textArea.getDocument().getLength());
	}


	static public void main(String[] args) {
		final GpyToCsv converter = new GpyToCsv();
		converter.message("Select a .gpy or .csv file to convert."
				+ "\nPress cancel to exit.\n"
		);

		File[] selected = null;
		do {
			selected = converter.convertFiles();
		} while (selected != null && selected.length > 0);
		System.exit(1);

	}
}