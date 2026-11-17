//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import org.opensilex.aiimport.workbook.ExcelValueParser;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@code xsd:date} and {@code xsd:dateTime} cells: ISO forms, Excel serials, and dates written
 * with slashes, dots or dashes in the order the column decided.
 *
 * @author Arnaud Charleroy
 */
final class DateReader {

    private static final Pattern YEAR_FIRST = Pattern.compile("^(\\d{4})[/.](\\d{1,2})[/.](\\d{1,2})$");
    private static final Pattern SERIAL = Pattern.compile("^\\d+(?:[.,]\\d+)?$");
    private static final int MONTHS = 12;
    private static final long SECONDS_PER_DAY = 86_400L;

    private final ReadingContext context;

    DateReader(ReadingContext context) {
        this.context = context;
    }

    /** A date, or the reason the text is none. */
    ReadResult<LocalDate> date(String text) {
        ReadResult<LocalDateTime> dateTime = isoLocalDateTime(text);
        if (dateTime != null) {
            return ReadResult.of(dateTime.value().toLocalDate());
        }
        try {
            return ReadResult.of(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            // not ISO: try the other forms
        }
        if (SERIAL.matcher(text).matches()) {
            BigDecimal serial = new BigDecimal(text.replace(',', '.'));
            return ExcelValueParser.isPlausibleDateSerial(serial.doubleValue())
                    ? ReadResult.of(ExcelValueParser.serialToDate(serial.doubleValue(), context.date1904()))
                    : ReadResult.refused(Problems.notADate(text));
        }
        return writtenDate(text);
    }

    /** A date and time with its offset, or the reason the text is none. */
    ReadResult<OffsetDateTime> dateTime(String text) {
        try {
            return ReadResult.of(OffsetDateTime.parse(text.replace(' ', 'T')));
        } catch (DateTimeParseException e) {
            // no offset written: read it as local, then place it in the column's zone
        }
        ReadResult<LocalDateTime> local = localDateTime(text);
        if (local.isRefused()) {
            return ReadResult.refused(local.problem());
        }
        if (context.zone() == null) {
            return ReadResult.refused(Problems.missingZone(text));
        }
        return ReadResult.of(local.value().atOffset(context.zone()));
    }

    private ReadResult<LocalDateTime> localDateTime(String text) {
        ReadResult<LocalDateTime> iso = isoLocalDateTime(text);
        if (iso != null) {
            return iso;
        }
        if (SERIAL.matcher(text).matches() && (text.contains(".") || text.contains(","))) {
            BigDecimal serial = new BigDecimal(text.replace(',', '.'));
            if (!ExcelValueParser.isPlausibleDateSerial(serial.doubleValue())) {
                return ReadResult.refused(Problems.notADate(text));
            }
            LocalDate day = ExcelValueParser.serialToDate(serial.doubleValue(), context.date1904());
            long seconds = serial.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(SECONDS_PER_DAY)).longValue();
            return ReadResult.of(day.atStartOfDay().plusSeconds(seconds));
        }
        int space = text.indexOf(' ');
        if (space > 0) {
            ReadResult<LocalDate> day = writtenDate(text.substring(0, space));
            if (day.isRefused()) {
                return ReadResult.refused(day.problem());
            }
            try {
                return ReadResult.of(day.value().atTime(LocalTime.parse(text.substring(space + 1).trim())));
            } catch (DateTimeParseException e) {
                return ReadResult.refused(Problems.notADate(text));
            }
        }
        ReadResult<LocalDate> dateOnly = date(text);
        return ReadResult.refused(dateOnly.isRefused() ? dateOnly.problem() : Problems.missingTime(text));
    }

    private static ReadResult<LocalDateTime> isoLocalDateTime(String text) {
        if (text.length() <= "yyyy-MM-dd".length()) {
            return null;
        }
        try {
            return ReadResult.of(LocalDateTime.parse(text.replace(' ', 'T')));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** {@code 2026/04/03}, or {@code 03/04/2026} in the column's order. */
    private ReadResult<LocalDate> writtenDate(String text) {
        Matcher yearFirst = YEAR_FIRST.matcher(text);
        if (yearFirst.matches()) {
            return build(text, group(yearFirst, 1), group(yearFirst, 2), group(yearFirst, 3));
        }
        Matcher numeric = DateOrder.NUMERIC_DATE.matcher(text);
        if (!numeric.matches()) {
            return ReadResult.refused(Problems.notADate(text));
        }
        int first = group(numeric, 1);
        int second = group(numeric, 2);
        int year = group(numeric, 3);
        DateOrder order = context.dateOrder();
        if (order == DateOrder.UNDECIDED) {
            order = orderOf(first, second);
        }
        if (order == null) {
            return ambiguous(text, year, first, second);
        }
        return order == DateOrder.DAY_FIRST ? build(text, year, second, first) : build(text, year, first, second);
    }

    /** The only order a date allows by itself, or null when both orders read it. */
    private static DateOrder orderOf(int first, int second) {
        if (first == second || first > MONTHS) {
            return DateOrder.DAY_FIRST;
        }
        return second > MONTHS ? DateOrder.MONTH_FIRST : null;
    }

    private static ReadResult<LocalDate> ambiguous(String text, int year, int first, int second) {
        String dayFirst = LocalDate.of(year, second, first).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String monthFirst = LocalDate.of(year, first, second).format(DateTimeFormatter.ISO_LOCAL_DATE);
        return ReadResult.refused(Problems.ambiguousDate(text, dayFirst, monthFirst));
    }

    private static ReadResult<LocalDate> build(String text, int year, int month, int day) {
        try {
            return ReadResult.of(LocalDate.of(year, month, day));
        } catch (DateTimeException e) {
            return ReadResult.refused(Problems.notADate(text));
        }
    }

    private static int group(Matcher matcher, int index) {
        return Integer.parseInt(matcher.group(index));
    }
}
