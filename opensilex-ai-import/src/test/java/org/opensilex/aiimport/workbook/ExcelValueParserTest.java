//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Arnaud Charleroy
 */
public class ExcelValueParserTest {

    @Test
    public void naIsAMissingValue() {
        assertTrue(ExcelValueParser.parse("Bai_Suc_g", "NA").isMissing());
        assertTrue(ExcelValueParser.parse("Bai_Suc_g", "na").isMissing());
        assertTrue(ExcelValueParser.parse("Bai_Suc_g", " NA ").isMissing());
        assertTrue(ExcelValueParser.parse("Bai_Suc_g", "").isMissing());
        assertTrue(ExcelValueParser.parse("Bai_Suc_g", null).isMissing());
    }

    @Test
    public void zeroIsNotAMissingValue() {
        CellValue value = ExcelValueParser.parse("Souche_HE", "0");
        assertFalse(value.isMissing());
        assertEquals(Double.valueOf(0d), value.getNumber());
    }

    @Test
    public void aCommaIsADecimalSeparator() {
        assertEquals(Double.valueOf(171.666d), ExcelValueParser.parseNumber("171,666"));
        assertEquals(Double.valueOf(171.666d), ExcelValueParser.parseNumber("171.666"));
        assertEquals(Double.valueOf(1234.5d), ExcelValueParser.parseNumber("1.234,5"));
    }

    @Test
    public void invisibleCharactersAreStripped() {
        // A non-breaking space, the case the template's instructions warn about.
        assertEquals("7,6", ExcelValueParser.clean(" 7,6 "));
        assertEquals(Double.valueOf(7.6d),
                ExcelValueParser.parse("Bai_AT", " 7,6").getNumber());
    }

    @Test
    public void textIsLeftAsText() {
        CellValue value = ExcelValueParser.parse("Bai_Coul", "rouge");
        assertFalse(value.isMissing());
        assertNull(value.getNumber());
        assertNull(value.getDate());
        assertEquals("rouge", value.getRaw());
    }

    @Test
    public void aSerialInADateColumnIsADate() {
        CellValue value = ExcelValueParser.parse("Date", "42465");
        assertEquals(LocalDate.of(2016, 4, 5), value.getDate());

        assertEquals(LocalDate.of(2016, 9, 9), ExcelValueParser.serialToDate(42622));
        assertEquals(LocalDate.of(2016, 9, 13), ExcelValueParser.serialToDate(42626));
    }

    @Test
    public void theSameSerialReadsFourYearsApartInTheTwoDateSystems() {
        // The VitisExplorer sample file is saved in the 1904 system, where 42465 is 2020-04-06.
        // Read as 1900 the very same cell would be 2016-04-05, which is the silent error the
        // template's instructions warn about.
        assertEquals(LocalDate.of(2016, 4, 5), ExcelValueParser.serialToDate(42465, false));
        assertEquals(LocalDate.of(2020, 4, 6), ExcelValueParser.serialToDate(42465, true));

        assertEquals(LocalDate.of(2020, 4, 6),
                ExcelValueParser.parse("Date", "42465", true).getDate());
    }

    @Test
    public void theDefaultDateSystemIs1900() {
        assertEquals(ExcelValueParser.serialToDate(42465, false),
                ExcelValueParser.serialToDate(42465));
        assertEquals(ExcelValueParser.parse("Date", "42465", false).getDate(),
                ExcelValueParser.parse("Date", "42465").getDate());
    }

    @Test
    public void aSerialOutsideADateColumnStaysANumber() {
        CellValue value = ExcelValueParser.parse("Bai_Suc_g", "42465");
        assertNull(value.getDate());
        assertEquals(Double.valueOf(42465d), value.getNumber());
    }

    @Test
    public void aRatingIsNotMistakenForADate() {
        // A phenology column is named Deb_Date, so it is treated as a date column, but a small
        // number in it is a value, not a serial.
        CellValue value = ExcelValueParser.parse("Deb_Date", "3");
        assertNull(value.getDate());
        assertEquals(Double.valueOf(3d), value.getNumber());
    }

    @Test
    public void dateColumnsAreRecognisedByName() {
        assertTrue(ExcelValueParser.looksLikeDateColumn("Date"));
        assertTrue(ExcelValueParser.looksLikeDateColumn("Deb_Date"));
        assertTrue(ExcelValueParser.looksLikeDateColumn("Vdg_Date"));
        assertFalse(ExcelValueParser.looksLikeDateColumn("Observateur"));
        assertFalse(ExcelValueParser.looksLikeDateColumn("Bai_pH"));
    }

    @Test
    public void datetimeColumnsAreRecognisedToo() {
        // meteo_datetime neither starts with nor ends with "date", and was read as a plain number.
        assertTrue(ExcelValueParser.looksLikeDateColumn("meteo_datetime"));
        assertTrue("a column renamed simply 'datetime' must keep working",
                ExcelValueParser.looksLikeDateColumn("datetime"));
        assertTrue(ExcelValueParser.looksLikeDateColumn("observation_datetime"));
    }
}
