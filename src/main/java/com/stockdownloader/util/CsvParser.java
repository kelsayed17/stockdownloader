package com.stockdownloader.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight CSV parser that handles standard CSV formatting.
 * Replaces external OpenCSV dependency for simple delimited data.
 */
public final class CsvParser implements AutoCloseable {

    private final BufferedReader reader;
    private final char separator;

    public CsvParser(Reader reader) {
        this(reader, ',');
    }

    public CsvParser(Reader reader, char separator) {
        this.reader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        this.separator = separator;
    }

    public CsvParser(InputStream input) {
        this(new InputStreamReader(input), ',');
    }

    public CsvParser(InputStream input, char separator) {
        this(new InputStreamReader(input), separator);
    }

    public String[] readNext() throws IOException {
        String line = reader.readLine();
        if (line == null) return null;
        return parseLine(line);
    }

    public List<String[]> readAll() throws IOException {
        List<String[]> result = new ArrayList<>();
        String[] line;
        while ((line = readNext()) != null) {
            result.add(line);
        }
        return result;
    }

    public void skipLines(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            reader.readLine();
        }
    }

    private String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == separator && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
