package com.hotel.reservation.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a list of text fields to and from one line of a data file.
 *
 * File format: fields are separated by '|'. If a value itself contains a special
 * character it is escaped with a backslash, so user input such as "Room A|B"
 * cannot break the file structure:
 * <pre>
 *   \  is written as  \\
 *   |  is written as  \|
 *   newline is written as  \n
 * </pre>
 * Lists (e.g. amenities) are stored inside a single field, separated by ';'
 * (with ';' inside an item escaped as "\;").
 */
public final class RecordCodec {

    public static final char FIELD_SEPARATOR = '|';
    public static final char LIST_SEPARATOR = ';';

    private RecordCodec() {
        // utility class - no objects needed
    }

    /** Joins fields into one escaped line. */
    public static String encode(List<String> fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                line.append(FIELD_SEPARATOR);
            }
            line.append(escape(fields.get(i), FIELD_SEPARATOR));
        }
        return line.toString();
    }

    /** Splits one escaped line back into its original fields. */
    public static String[] decode(String line) {
        return split(line, FIELD_SEPARATOR).toArray(new String[0]);
    }

    public static String encodeList(List<String> items) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                value.append(LIST_SEPARATOR);
            }
            value.append(escape(items.get(i), LIST_SEPARATOR));
        }
        return value.toString();
    }

    public static List<String> decodeList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        return split(value, LIST_SEPARATOR);
    }

    private static String escape(String value, char separator) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '\\') {
                escaped.append("\\\\");
            } else if (c == separator) {
                escaped.append('\\').append(separator);
            } else if (c == '\n') {
                escaped.append("\\n");
            } else if (c != '\r') {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private static List<String> split(String line, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (char c : line.toCharArray()) {
            if (escaping) {
                current.append(c == 'n' ? '\n' : c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == separator) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (escaping) {
            throw new IllegalArgumentException("Record ends with an incomplete escape sequence.");
        }
        parts.add(current.toString());
        return parts;
    }
}
