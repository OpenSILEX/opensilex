//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Normalises the conventions field spreadsheets are filled with: {@code NA} for a missing value,
 * a comma as decimal separator, stray non-breaking spaces, and dates stored as raw 1900-system
 * serial numbers when the column carries no date format.
 *
 * @author Arnaud Charleroy
 */
public class ExcelValueParser {

    /**
     * Base of the 1900 date system as Excel actually implements it. Excel counts the non-existent
     * 1900-02-29, so from serial 61 onwards — every date a field observation can carry — the base
     * that reproduces Excel's own arithmetic is 1899-12-30.
     */
    public static final LocalDate EXCEL_1900_EPOCH = LocalDate.of(1899, 12, 30);

    /**
     * Base of the 1904 date system, the one Excel for Mac used and that a workbook can still carry.
     * Serial 1 is 1904-01-02.
     * <p>
     * This is not a detail: the same serial reads four years apart in the two systems, so a file
     * saved in one and read in the other silently files the data under the wrong season.
     */
    public static final LocalDate EXCEL_1904_EPOCH = LocalDate.of(1904, 1, 1);

    /**
     * Smallest serial we accept as a date rather than a plain number. 20000 is 1954-10-03, well
     * below any observation date and well above any rating scale or measurement.
     */
    public static final double MIN_DATE_SERIAL = 20000d;

    /**
     * Largest serial we accept as a date. 60000 is 2064-03-05.
     */
    public static final double MAX_DATE_SERIAL = 60000d;

    private static final Set<String> MISSING_TOKENS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("na", "n/a", "nd", "n/d", "null", "-", "")));

    /**
     * Words that make a column a date column. {@code datetime} is here because a header such as
     * {@code meteo_datetime} neither starts with nor ends with {@code date}, and was being read as
     * a plain number.
     */
    private static final List<String> DATE_COLUMN_HINTS = Collections.unmodifiableList(
            Arrays.asList("date", "_date", "datetime", "_datetime"));

    private ExcelValueParser() {
    }

    /**
     * Reads a cell assuming the 1900 date system. Prefer
     * {@link #parse(String, String, boolean)} whenever the workbook's date system is known.
     */
    public static CellValue parse(String header, String raw) {
        return parse(header, raw, false);
    }

    /**
     * @param header    the column header, used only to decide whether a bare number is a date serial
     * @param raw       the cell content as text
     * @param date1904  true when the workbook uses the 1904 date system
     */
    public static CellValue parse(String header, String raw, boolean date1904) {
        String cleaned = clean(raw);
        if (isMissing(cleaned)) {
            return CellValue.missing(cleaned);
        }

        Double number = parseNumber(cleaned);
        if (number == null) {
            return CellValue.text(cleaned);
        }

        if (looksLikeDateColumn(header) && isPlausibleDateSerial(number)) {
            return CellValue.date(cleaned, serialToDate(number, date1904));
        }
        return CellValue.number(cleaned, number);
    }

    /**
     * Strips the invisible characters the ReadMe of field templates warns about: non-breaking
     * spaces, narrow no-break spaces and zero-width spaces, plus ordinary surrounding whitespace.
     */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(' ', ' ')
                .replace(' ', ' ')
                .replace("​", "")
                .replace("﻿", "")
                .trim();
    }

    public static boolean isMissing(String cleaned) {
        return cleaned == null || MISSING_TOKENS.contains(cleaned.toLowerCase());
    }

    /**
     * Accepts both decimal separators. A comma is the separator prescribed by the field templates,
     * but files come back with dots too.
     */
    public static Double parseNumber(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }
        String candidate = cleaned.replace(" ", "");
        if (candidate.indexOf(',') >= 0 && candidate.indexOf('.') >= 0) {
            // Ambiguous, e.g. "1.234,5": treat the comma as the decimal separator and drop the
            // thousands separator.
            candidate = candidate.replace(".", "");
        }
        candidate = candidate.replace(',', '.');
        try {
            return Double.valueOf(candidate);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean looksLikeDateColumn(String header) {
        if (header == null) {
            return false;
        }
        String lower = header.toLowerCase();
        for (String hint : DATE_COLUMN_HINTS) {
            if (lower.equals(hint) || lower.endsWith(hint) || lower.startsWith(hint)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlausibleDateSerial(double serial) {
        return serial >= MIN_DATE_SERIAL && serial <= MAX_DATE_SERIAL;
    }

    /**
     * Converts a serial assuming the 1900 date system.
     */
    public static LocalDate serialToDate(double serial) {
        return serialToDate(serial, false);
    }

    public static LocalDate serialToDate(double serial, boolean date1904) {
        LocalDate epoch = date1904 ? EXCEL_1904_EPOCH : EXCEL_1900_EPOCH;
        return epoch.plus((long) Math.floor(serial), ChronoUnit.DAYS);
    }
}
