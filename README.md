# compactgnss
A compact format for GNSS data

Code and discussion relating to a very compact ("minimal") data format for GNSS data, specifically for the ESP logger and speedsurfing. The idea of creating a new format for GPS data came up in a discussion on Seabreeze (https://www.seabreeze.com.au/forums/Windsurfing/Gps/Another-DIY-GPS-logger-approach?page=19 and later pages). This includes the suggestion to use delta compression for things like date, position, and speed.

A further discussion of ideas for such a format took place at https://logiqx.github.io/open-gnss/. 

The immediate need for a new format relates to the open-source ESP 32 logger (see https://github.com/RP6conrad/ESP-GPS-Logger and https://www.seabreeze.com.au/forums/Windsurfing/Gps/Another-DIY-GPS-logger-approach). The developer of the Motion GPS and .oao file format asserted that the developers of the ESP logger firmware "had no permission whatsoever to OAO". While this assertion is, at best, highly questionable, the resulting discussion pointed out that a significantly more compact and less wasteful format which contained all the necessary data for speed analysis and track display would be both possible and desirable.

One feature requested by several participants in the discussion was the possibility to extend the format in a backward and forward compatible way. While there was general agreement that this would be desirable, further discussions (https://github.com/Logiqx/open-gnss/discussions) about how to implement such an extensible format became increasingly complex, and eventually ended in disagreement.

The initial format description (https://github.com/prichterich/compactgnss/issues/2) and example code posted here implements a "minimal compact" format that builds on several important aspects of the previous discussions:
1. Only a minima set of data items that is required for various aspects of speed calculation, and location information required for track display, is included. 
2. File size is further reduced through delta compression, with uncompressed "reference" records at regular intervals (every 32 seconds) and as needed. Compression is generally lossless (relative to the precison in u-blox based loggers like the ESP logger). The only exception is course (or "heading of motion", where accuracy is reduced from 5 to 2 decimal places (which is still overkill, since the point-to-point change in course is in the order of 1 degree even in straight line speedruns on relatively flat water).
3. Data structures in records are aligned to minimize potential problems when reading and writing files from C using structures.
4. Data integrety can be verified using 2 byte checksums. Records with checksum violations, whether from file transfers problems or manual editing, should be rejected and not used for speed analysis.  
5. To allow for future extensions without breaking older software, new record types can be ignored by the parser if they follow a couple of very simple rules how they should be composed (starting with a type byte that is not predefined, followed by the total record length in bytes 3 and 4, and a 2-byte Fletcher checksum with modulus 256 over the entire record at the end).
6. The intention is to define how "custom" records can be added to compact GNSS formats in the future by using "in-file" definitions of record structures and the meaning of data items. This will require pre-defined definition of data types, common data items, and possibly more.

The file extension for compact GNSS formats is ".gpy" ("the next thing after .gpx ... and one of the few 3-letter extensions that is not commonly used, according to file-extensions.org).

The size of a compressed "minimal" record in .gpy files is 20 bytes, compared to 52 bytes in .oao files and 100 bytes in typical u-blox files. Uncompressed reference records are 36 bytes, but are required rarely (every 33rd record for 1 hz data, every 328th record for 10 hz data), so they increase file size only minimally. Therefore, .gpy files are typically about 60% smaller than .oao files, and 80% smaller than .ubx files.
