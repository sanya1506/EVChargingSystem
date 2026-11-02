package com.abcgreen.evcharging.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV import / export (Module III - file handling).
 * Handles quoted values containing commas, quotes and new lines.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    public static void write(File file, String[] header, List<Object[]> rows) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            write(out, header, rows);
        }
    }

    public static void write(Writer out, String[] header, List<Object[]> rows) throws IOException {
        out.write(toLine(header));
        out.write("\r\n");
        for (Object[] row : rows) {
            out.write(toLine(row));
            out.write("\r\n");
        }
        out.flush();
    }

    private static String toLine(Object[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(values[i]));
        }
        return sb.toString();
    }

    static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Reads a CSV file; the first row (header) is skipped. */
    public static List<String[]> read(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = in.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (!line.isBlank()) {
                    rows.add(parseLine(line));
                }
            }
        }
        return rows;
    }

    static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }
}
