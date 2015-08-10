package com.yazino.bi.operations.util;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.yazino.bi.operations.util.FilenamePatternsHelper.treatFileName;

/**
 * Reads a CSV file as it should be prepared for the Maximiles export
 * The source file must be comma-delimited with mandatory quotes around the fields
 */
public class MaximilesSourceCsvReader implements CsvReader {

    private BufferedReader inputReader = null;
    private String inputFileName;
    private String separator = ",";
    private String regexSeparator = ",";

    /**
     * Opens an input stream from a known file name
     *
     * @param filename Resource name of the file to open
     */
    @Required
    @Override
    public void setInputFileName(final String filename) {
        this.inputFileName = filename;
        this.inputReader = null;
    }

    /**
     * Directly sets the input file reader
     *
     * @param inputReader Input file reader
     */
    public void setInputReader(final BufferedReader inputReader) {
        this.inputReader = inputReader;
    }

    @Override
    public List<String> readNextRecord() throws IOException {
        initializeInputReader();
        String incompleteResult = null;
        final List<String> results = new ArrayList<String>();
        while (true) {
            final String line = inputReader.readLine();
            if (line == null) {
                inputReader.close();
                inputReader = null;
                break;
            }
            final String[] parts = line.split(regexSeparator);

            String incompletePart = null;
            for (final String part : parts) {
                if ("\\N".equals(part)) {
                    results.add(null);
                    continue;
                }

                // Treating possible separators inside quote-delimited strings
                String actualPart;
                if ("".equals(part)) {
                    actualPart = "\"\"";
                } else {
                    actualPart = part;
                }
                if (incompletePart != null) {
                    actualPart = incompletePart.concat(actualPart);
                    incompletePart = null;
                }
                final char lastChar = actualPart.charAt(actualPart.length() - 1);
                final boolean startsWithQuote = actualPart.charAt(0) == '\"';
                if (lastChar != '\"' && (incompletePart != null || incompleteResult != null || startsWithQuote)) {
                    incompletePart = actualPart.concat(separator);
                    continue;
                }

                final String partToAdd;
                if (incompleteResult != null) {
                    partToAdd = incompleteResult.concat(actualPart.substring(0, actualPart.length() - 1));
                    incompleteResult = null;
                } else {
                    if (startsWithQuote) {
                        partToAdd = actualPart.substring(1, actualPart.length() - 1);
                    } else {
                        partToAdd = actualPart;
                    }
                }
                results.add(partToAdd);
            }

            // Treating possible end of line escape sequences
            if (incompletePart != null && incompletePart.charAt(incompletePart.length() - 2) == '\\') {
                if (incompleteResult == null) {
                    incompleteResult = "";
                }
                final String toAppend;
                if (incompletePart.charAt(0) == '\"') {
                    toAppend = incompletePart.substring(1, incompletePart.length() - 2);
                } else {
                    toAppend = incompletePart.substring(0, incompletePart.length() - 2);
                }
                incompleteResult = incompleteResult.concat(toAppend).concat("\n");
            } else {
                break;
            }
        }
        return results;
    }

    /**
     * Initialize the input reader if needed
     *
     * @throws IOException Thrown when there is a problem opening the input file
     */
    private void initializeInputReader() throws IOException {
        if (inputReader != null) {
            return;
        }
        final Resource resource;
        if (inputFileName.startsWith("classpath:")) {
            resource = new ClassPathResource(treatFileName(inputFileName.substring(10)));
        } else {
            resource = new FileSystemResource(treatFileName(inputFileName));
        }
        this.inputReader = new BufferedReader(new FileReader(resource.getFile()), 65536);
    }

    @Override
    public void setSeparator(final String newRegexSeparator, final String newSeparator) {
        this.separator = newSeparator;
        this.regexSeparator = newRegexSeparator;
    }

    /**
     * Skip one line in the input
     *
     * @throws IOException Throws if there is a problem
     */
    public void skipLine() throws IOException {
        initializeInputReader();
        inputReader.readLine();
    }

    @Override
    public void setInputStream(final InputStream inputStream) {
        this.inputReader = new BufferedReader(new InputStreamReader(inputStream));
    }
}
