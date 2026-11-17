//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.junit.Test;
import org.opensilex.aiimport.exception.WorkbookReadException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A CSV file read as a sheet: the shapes a spreadsheet saves one in — a semicolon in a French
 * locale, quotes around a cell that holds the separator, a byte-order mark, Windows accents.
 *
 * @author Arnaud Charleroy
 */
public class CsvReaderTest {

    private static SheetStructure read(String name, String content, Charset charset) throws Exception {
        return read(name, content.getBytes(charset));
    }

    private static SheetStructure read(String name, byte[] bytes) throws Exception {
        File file = File.createTempFile("csv-", ".csv");
        try {
            Files.write(file.toPath(), bytes);
            WorkbookStructure workbook = new CsvReader().read(file, name);
            assertEquals("one sheet per file", 1, workbook.getSheets().size());
            assertEquals(name, workbook.getFileName());
            return workbook.getSheets().get(0);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void theSheetIsNamedAfterTheFile() throws Exception {
        assertEquals("plan essai", read("plan essai.csv", "a,b\n1,2\n", StandardCharsets.UTF_8).getName());
    }

    @Test
    public void eachSeparatorAPersonMightSaveWithIsDetected() throws Exception {
        for (String separator : List.of(",", ";", "\t", "|")) {
            SheetStructure sheet = read("t.csv", "plot" + separator + "variete\nP01" + separator + "Syrah\n",
                    StandardCharsets.UTF_8);
            assertEquals("separator '" + separator + "'", List.of("plot", "variete"), sheet.getHeaders());
            assertEquals(List.of("P01", "Syrah"), sheet.getRows().get(0));
        }
    }

    @Test
    public void aQuotedCellKeepsItsSeparatorAndItsNewLine() throws Exception {
        SheetStructure sheet = read("t.csv", "plot;remarque\nP01;\"bord; nord\"\nP02;\"deux\nlignes\"\n",
                StandardCharsets.UTF_8);

        assertEquals("bord; nord", sheet.getRows().get(0).get(1));
        assertEquals("deux\nlignes", sheet.getRows().get(1).get(1));
        assertEquals(2, sheet.getDataRowCount());
    }

    @Test
    public void aDecimalCommaStaysTextUntilTheValueIsRead() throws Exception {
        SheetStructure sheet = read("t.csv", "plot;hauteur\nP01;84,5\n", StandardCharsets.UTF_8);

        assertEquals("84,5", sheet.getRows().get(0).get(1));
    }

    @Test
    public void aTitleAboveTheHeaderIsSkippedAsInAWorkbook() throws Exception {
        SheetStructure sheet = read("t.csv", "Essai vigne 2026\n\nplot;variete\nP01;Syrah\n", StandardCharsets.UTF_8);

        assertEquals(List.of("plot", "variete"), sheet.getHeaders());
        assertEquals(1, sheet.getDataRowCount());
    }

    @Test
    public void aByteOrderMarkIsNotPartOfTheFirstHeader() throws Exception {
        byte[] content = "﻿plot;variete\nP01;Syrah\n".getBytes(StandardCharsets.UTF_8);

        SheetStructure sheet = read("t.csv", content);

        assertEquals("plot", sheet.getHeaders().get(0));
        assertNull("a UTF-8 file says nothing about its charset", sheet.getCharset());
    }

    @Test
    public void aFileThatIsNotUtf8IsReadAsWindowsAndSaysSo() throws Exception {
        SheetStructure sheet = read("t.csv", "plot;variété\nP01;Grenache noir é\n", CsvReader.FALLBACK_CHARSET);

        assertEquals("variété", sheet.getHeaders().get(1));
        assertEquals("Grenache noir é", sheet.getRows().get(0).get(1));
        assertEquals("windows-1252", sheet.getCharset());
    }

    @Test
    public void aFileWithoutATableIsKeptAsText() throws Exception {
        SheetStructure sheet = read("lisez-moi.csv", "Ceci est un texte\n", StandardCharsets.UTF_8);

        assertFalse(sheet.isTabular());
        assertTrue(sheet.getText().contains("Ceci est un texte"));
    }

    @Test
    public void emptyRowsAndTrailingEmptyCellsAreDropped() throws Exception {
        SheetStructure sheet = read("t.csv", "plot;variete;;\nP01;Syrah;;\n;;;\n\nP02;Grenache;;\n;;;\n",
                StandardCharsets.UTF_8);

        assertEquals(List.of("plot", "variete"), sheet.getHeaders());
        assertEquals(2, sheet.getDataRowCount());
    }

    @Test
    public void aSheetIsReadOnlyUpToTheRowLimit() throws Exception {
        StringBuilder content = new StringBuilder("plot;variete\n");
        for (int i = 0; i < 20; i++) {
            content.append("P").append(i).append(";Syrah\n");
        }
        File file = File.createTempFile("csv-", ".csv");
        try {
            Files.writeString(file.toPath(), content.toString());

            SheetStructure sheet = new CsvReader(5).read(file, "t.csv").getSheets().get(0);

            assertTrue("the header and four rows: " + sheet.getDataRowCount(), sheet.getDataRowCount() <= 5);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void theRegistryPicksTheReaderByExtension() throws Exception {
        assertTrue(SheetSources.extensions().containsAll(List.of("xlsx", "xlsm", "xls", "csv", "tsv")));
        assertTrue(SheetSources.forFile("PLAN.CSV").get() instanceof CsvReader);
        assertTrue(SheetSources.forFile("plan.xlsx").get() instanceof WorkbookReader);
        assertFalse(SheetSources.forFile("plan.pdf").isPresent());
    }

    @Test(expected = WorkbookReadException.class)
    public void aFileNoReaderKnowsIsRefused() throws Exception {
        File file = File.createTempFile("not-", ".pdf");
        try {
            SheetSources.read(file, "plan.pdf");
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test(expected = WorkbookReadException.class)
    public void aFileThatCannotBeOpenedIsRefused() throws IOException, WorkbookReadException {
        new CsvReader().read(new File("/nowhere/at/all.csv"), "all.csv");
    }
}
