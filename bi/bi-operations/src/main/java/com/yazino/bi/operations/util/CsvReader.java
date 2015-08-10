package com.yazino.bi.operations.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Reads a CSV file line by line and lets an external listener
 * do something with each line read
 */
public interface CsvReader {

    /**
     * Read the next record available in the reader stream
     *
     * @return Next record parsed or an empty record if the last record is reached
     * @throws IOException Thrown when there is a reading problem in the input stream
     */
    List<String> readNextRecord() throws IOException;

    /**
     * Sets the specific name for the input file
     *
     * @param filename Full path to the file
     */
    void setInputFileName(String filename);

    /**
     * Initializes the input stream to the given one
     *
     * @param inputStream input stream to read the CSV contents from
     */
    void setInputStream(InputStream inputStream);

    /**
     * Sets the separator expression
     *
     * @param separator      Simple version of the separator
     * @param regexSeparator The regex for the separator string, i.e. for instance you have to put \| for a pipe
     */
    void setSeparator(String regexSeparator, String separator);
}
