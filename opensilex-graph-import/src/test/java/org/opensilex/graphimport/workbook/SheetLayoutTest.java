//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.workbook;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A sheet laid out by hand: a title above the header, a row of units below it.
 *
 * @author Arnaud Charleroy
 */
public class SheetLayoutTest {

    private static final String WITH_UNITS = "Essai 2026;;\n;;\nnom;hauteur;date\n;cm;\nP1;12;2026-04-03\n\n"
            + "P2;14;2026-04-04\n";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void rawRowsKeepTheTitleTheEmptyRowsAndTheirNumbers() throws Exception {
        List<List<String>> rows = RawRows.read(csv(WITH_UNITS), "mesures.csv").get(0).rows();

        assertEquals(List.of("Essai 2026"), rows.get(0));
        assertTrue(rows.get(1).isEmpty());
        assertEquals(List.of("nom", "hauteur", "date"), rows.get(2));
        assertEquals(List.of("", "cm"), rows.get(3));
        assertEquals(List.of("P2", "14", "2026-04-04"), rows.get(6));
    }

    @Test
    public void theReaderLayoutIsFoundAmongTheRawRows() throws Exception {
        File file = csv(WITH_UNITS);
        SheetStructure detected = SheetSources.read(file, "mesures.csv").getSheets().get(0);
        List<List<String>> rows = RawRows.read(file, "mesures.csv").get(0).rows();

        assertEquals(Optional.of(new SheetLayout(2, 3)), SheetLayout.detectedIn(detected, rows));
        assertEquals("the units row was read as values", 3, detected.getDataRowCount());
    }

    @Test
    public void aChosenLayoutSkipsTheUnitsRow() throws Exception {
        File file = csv(WITH_UNITS);
        WorkbookStructure read = SheetSources.read(file, "mesures.csv");
        List<List<String>> rows = RawRows.read(file, "mesures.csv").get(0).rows();

        SheetStructure laidOut = new SheetLayout(2, 4).applyTo(read.getSheets().get(0), rows);

        assertEquals(List.of("nom", "hauteur", "date"), laidOut.getHeaders());
        assertEquals(2, laidOut.getDataRowCount());
        assertEquals(List.of("P1", "12", "2026-04-03"), laidOut.getRows().get(0));
        assertEquals("mesures", laidOut.getName());
        assertTrue(laidOut.isTabular());
    }

    @Test
    public void aLayoutMustFitTheSheet() {
        assertFalse(new SheetLayout(0, 1).whyNotFor(2).isPresent());
        assertTrue(new SheetLayout(2, 3).whyNotFor(2).isPresent());
        assertTrue(new SheetLayout(1, 1).whyNotFor(5).isPresent());
        assertTrue(new SheetLayout(1, 5).whyNotFor(5).isPresent());
        assertTrue(new SheetLayout(-1, 2).whyNotFor(5).isPresent());
    }

    @Test
    public void aProseSheetHasNoDetectedLayoutButCanBeLaidOut() throws Exception {
        File file = csv("Lisez-moi\nUne phrase\n");
        SheetStructure read = SheetSources.read(file, "notes.csv").getSheets().get(0);
        List<List<String>> rows = RawRows.read(file, "notes.csv").get(0).rows();

        assertEquals(Optional.empty(), SheetLayout.detectedIn(read, rows));
        SheetStructure table = new SheetLayout(0, 1).applyTo(read, rows);
        assertEquals(List.of("Lisez-moi"), table.getHeaders());
        assertEquals(1, table.getDataRowCount());
    }

    private File csv(String content) throws Exception {
        File file = new File(folder.newFolder(), "file.csv");
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
