//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import org.junit.Test;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Cells read as a person fills a spreadsheet, and refused when they could mean two things.
 *
 * @author Arnaud Charleroy
 */
public class LiteralReaderTest {

    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";
    private static final URI DECIMAL = URI.create(XSD + "decimal");
    private static final URI DOUBLE = URI.create(XSD + "double");
    private static final URI INTEGER = URI.create(XSD + "integer");
    private static final URI POSITIVE = URI.create(XSD + "positiveInteger");
    private static final URI BOOLEAN = URI.create(XSD + "boolean");
    private static final URI DATE = URI.create(XSD + "date");
    private static final URI DATE_TIME = URI.create(XSD + "dateTime");
    private static final URI ANY_URI = URI.create(XSD + "anyURI");
    private static final URI STRING = URI.create(XSD + "string");
    private static final URI GYEAR = URI.create(XSD + "gYear");
    private static final URI UNKNOWN = URI.create("http://test.opensilex.org/graph-import#Unit");

    private final LiteralReader reader = new LiteralReader(ReadingContext.UNKNOWN);

    @Test
    public void decimalsAcceptACommaOrADot() {
        assertValue("3.5", reader.read("3,5", DECIMAL));
        assertValue("3.5", reader.read("3.5", DECIMAL));
        assertValue("-0.25", reader.read("-,25", DECIMAL));
        assertValue("1234567.5", reader.read("1 234 567,5", DECIMAL));
        assertValue("1500", reader.read("1.5e3", DOUBLE));
        assertValue("1.5", reader.read("1,50", DECIMAL));
        assertValue("1.5", reader.read("1.5E0", DOUBLE));
        assertValue("0", reader.read("0,00", DECIMAL));
    }

    @Test
    public void aNumberWithBothSeparatorsIsRefused() {
        assertProblem("literal.twoSeparators", reader.read("1.234,5", DECIMAL));
        assertProblem("literal.twoSeparators", reader.read("1,234.5", DECIMAL));
    }

    @Test
    public void whatJavaWouldReadAsANumberButAPersonWouldNotIsRefused() {
        for (String text : List.of("12f", "5d", "NaN", "Infinity", "0x1A", "1,2,3", "12 34", "douze")) {
            assertProblem(text, "literal.notANumber", reader.read(text, DECIMAL));
        }
    }

    @Test
    public void integersAcceptAnExcelTrailingZero() {
        assertValue("12", reader.read("12", INTEGER));
        assertValue("12", reader.read("12,0", INTEGER));
        assertValue("-7", reader.read("-7.00", INTEGER));
        assertProblem("literal.notAnInteger", reader.read("12,5", INTEGER));
    }

    @Test
    public void theDatatypeRangeIsChecked() {
        assertProblem("literal.invalid", reader.read("0", POSITIVE));
        assertValue("1", reader.read("1", POSITIVE));
    }

    @Test
    public void booleansAreReadInFrenchAndEnglish() {
        for (String yes : List.of("Oui", "vrai", "TRUE", "yes", "1")) {
            assertValue("true", reader.read(yes, BOOLEAN));
        }
        for (String no : List.of("non", "Faux", "false", "No", "0")) {
            assertValue("false", reader.read(no, BOOLEAN));
        }
        assertProblem("literal.notABoolean", reader.read("peut-être", BOOLEAN));
    }

    @Test
    public void datesAreReadInTheirUsualForms() {
        assertValue("2026-04-03", reader.read("2026-04-03", DATE));
        assertValue("2026-04-03", reader.read("2026-04-03T10:15:00", DATE));
        assertValue("2026-04-03", reader.read("2026/04/03", DATE));
        assertValue("2026-03-25", reader.read("25/03/2026", DATE));
        assertValue("2026-03-25", reader.read("03/25/2026", DATE));
        assertValue("2026-04-04", reader.read("04.04.2026", DATE));
        assertValue("2023-03-15", reader.read("45000", DATE));
    }

    @Test
    public void aDateBothOrdersReadIsRefusedUntilTheColumnDecides() {
        LiteralReading ambiguous = reader.read("03/04/2026", DATE);

        assertProblem("literal.ambiguousDate", ambiguous);
        assertEquals("2026-04-03", ambiguous.problem().getParams().get("dayFirst"));
        assertEquals("2026-03-04", ambiguous.problem().getParams().get("monthFirst"));

        LiteralReader dayFirst = new LiteralReader(new ReadingContext(false, DateOrder.DAY_FIRST, null));
        assertValue("2026-04-03", dayFirst.read("03/04/2026", DATE));
        assertProblem("literal.notADate", dayFirst.read("03/25/2026", DATE));

        LiteralReader monthFirst = new LiteralReader(new ReadingContext(false, DateOrder.MONTH_FIRST, null));
        assertValue("2026-03-04", monthFirst.read("03/04/2026", DATE));
    }

    @Test
    public void aDateThatDoesNotExistIsRefused() {
        assertProblem("literal.notADate", reader.read("31/02/2026", DATE));
        assertProblem("literal.notADate", reader.read("2026/02/31", DATE));
        assertProblem("literal.notADate", reader.read("demain", DATE));
        assertProblem("literal.notADate", reader.read("12", DATE));
    }

    @Test
    public void aSerialFollowsTheWorkbookDateSystem() {
        LiteralReader mac = new LiteralReader(new ReadingContext(true, DateOrder.UNDECIDED, null));

        assertValue("2027-03-16", mac.read("45000", DATE));
    }

    @Test
    public void theColumnDecidesTheOrderFromItsValues() {
        assertEquals(DateOrder.DAY_FIRST, DateOrder.detect(List.of("03/04/2026", "25/04/2026")));
        assertEquals(DateOrder.MONTH_FIRST, DateOrder.detect(List.of("03/04/2026", "04/25/2026 10:00")));
        assertEquals(DateOrder.UNDECIDED, DateOrder.detect(List.of("03/04/2026", "2026-04-25")));
        assertEquals(DateOrder.UNDECIDED, DateOrder.detect(Arrays.asList("25/04/2026", "04/25/2026", null)));
    }

    @Test
    public void aDateTimeNeedsAZone() {
        assertValue("2026-04-03T10:15:00+02:00", reader.read("2026-04-03T10:15+02:00", DATE_TIME));
        assertProblem("literal.missingZone", reader.read("2026-04-03 10:15", DATE_TIME));
        assertProblem("literal.missingTime", reader.read("2026-04-03", DATE_TIME));
        assertProblem("literal.notADate", reader.read("bientôt", DATE_TIME));
    }

    @Test
    public void aDateTimeWithoutZoneTakesTheOneTheColumnChose() {
        LiteralReader paris = new LiteralReader(new ReadingContext(false, DateOrder.DAY_FIRST, ZoneOffset.ofHours(1)));

        assertValue("2026-04-03T10:15:00+01:00", paris.read("2026-04-03 10:15", DATE_TIME));
        assertValue("2026-04-03T10:15:00+01:00", paris.read("03/04/2026 10:15", DATE_TIME));
        assertValue("2023-03-15T12:00:00+01:00", paris.read("45000,5", DATE_TIME));
        assertProblem("literal.missingTime", paris.read("45000", DATE_TIME));
        assertProblem("literal.notADate", paris.read("03/04/2026 midi", DATE_TIME));
        assertProblem("literal.notADate", paris.read("100,5", DATE_TIME));
    }

    @Test
    public void uris() {
        assertValue("http://www.opensilex.org/id/plot-1", reader.read("http://www.opensilex.org/id/plot-1", ANY_URI));
        assertProblem("literal.notAUri", reader.read("plot 1", ANY_URI));
        assertProblem("literal.notAUri", reader.read("plot-1", ANY_URI));
    }

    @Test
    public void textIsKeptAndMissingTokensAreEmpty() {
        assertValue("Parcelle A", reader.read("  Parcelle A ", STRING));
        assertTrue(reader.read("NA", DECIMAL).isEmpty());
        assertTrue(reader.read("", DATE).isEmpty());
        assertTrue(reader.read(null, STRING).isEmpty());
        assertFalse(reader.read(null, STRING).isRefused());
    }

    @Test
    public void aDatatypeNobodyReadsHereIsCheckedByJenaOrKept() {
        assertValue("2026", reader.read("2026", GYEAR));
        assertProblem("literal.invalid", reader.read("deux mille", GYEAR));
        assertValue("12 kg", reader.read("12 kg", UNKNOWN));
    }

    private static void assertValue(String expected, LiteralReading reading) {
        assertNull(String.valueOf(reading.problem()), reading.problem());
        assertEquals(expected, reading.lexical());
    }

    private static void assertProblem(String key, LiteralReading reading) {
        assertProblem("", key, reading);
    }

    private static void assertProblem(String message, String key, LiteralReading reading) {
        assertTrue(message + " should be refused, read " + reading.lexical(), reading.isRefused());
        assertEquals(message, key, reading.problem().getKey());
        assertNull(reading.lexical());
    }
}
