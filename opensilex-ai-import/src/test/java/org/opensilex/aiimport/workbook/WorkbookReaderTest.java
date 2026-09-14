//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.exception.WorkbookReadException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reads the sample VitisExplorer workbook, which is the file family this module was built for.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookReaderTest {

    @Test
    public void everySheetIsRead() throws Exception {
        WorkbookStructure workbook = WorkbookFixture.vitis();

        assertEquals(18, workbook.getSheets().size());
        assertTrue(workbook.hasSheet("ReadMe"));
        assertTrue(workbook.hasSheet("Chronologie"));
        assertTrue(workbook.hasSheet("Cartouche_Fixe"));
        assertTrue(workbook.hasSheet("10_Controle_Maturite"));
        assertTrue(workbook.hasSheet("15_Fin_Cycle"));
    }

    @Test
    public void theWorkbooksOwnDateSystemIsRead() throws Exception {
        assertTrue("the sample file is saved in the 1904 date system, and reading its serials as "
                + "1900 would shift every observation four years",
                WorkbookFixture.vitis().isDate1904());
    }

    @Test
    public void datesAreResolvedWithTheWorkbooksDateSystem() throws Exception {
        SheetStructure winter = WorkbookFixture.vitis().getSheet("1_Hiver")
                .orElseThrow(AssertionError::new);

        assertEquals("2020-04-06", winter.cell(winter.getRows().get(0), "Date"));
    }

    @Test
    public void theReadMeIsKeptAsText() throws Exception {
        SheetStructure readMe = WorkbookFixture.vitis().getSheet("ReadMe").orElseThrow(AssertionError::new);

        assertFalse("a prose sheet must not be read as a table", readMe.isTabular());
        assertTrue(readMe.getText().contains("NA"));
        assertTrue("the filling instructions are the format specification and must survive",
                readMe.getText().contains("Parcelle Unitaire"));
    }

    @Test
    public void theCartoucheHoldsOneRowPerUnitPlot() throws Exception {
        SheetStructure cartouche = WorkbookFixture.vitis().getSheet("Cartouche_Fixe")
                .orElseThrow(AssertionError::new);

        assertTrue(cartouche.isTabular());
        assertEquals(73, cartouche.getDataRowCount());
        assertTrue(cartouche.hasHeader("Dispositif"));
        assertTrue(cartouche.hasHeader("PU"));
        assertTrue(cartouche.hasHeader("Genotype"));
        assertTrue(cartouche.hasHeader("Statut"));
        assertTrue(cartouche.hasHeader("Millesime"));
        assertEquals("CEPInnov_Champagne", cartouche.cell(cartouche.getRows().get(0), "Dispositif"));
        assertEquals("Pinot noir N", cartouche.cell(cartouche.getRows().get(0), "Genotype"));
    }

    @Test
    public void theCatalogueListsTheVariables() throws Exception {
        SheetStructure catalogue = WorkbookFixture.vitis().getSheet("Chronologie")
                .orElseThrow(AssertionError::new);

        assertEquals(39, catalogue.getDataRowCount());
        assertTrue(catalogue.hasHeader("Libellé"));
        assertTrue(catalogue.hasHeader("Nom de la variable"));
    }

    @Test
    public void aStageSheetHoldsTheCartoucheThenItsVariables() throws Exception {
        SheetStructure stage = WorkbookFixture.vitis().getSheet("10_Controle_Maturite")
                .orElseThrow(AssertionError::new);

        assertTrue(stage.hasHeader("Date"));
        assertTrue(stage.hasHeader("Observateur"));
        assertTrue(stage.hasHeader("Bai_Suc_g"));
        assertTrue(stage.hasHeader("Bai_pH"));
        assertEquals(73, stage.getDataRowCount());
    }

    @Test
    public void aColumnCanBeSummarisedByItsDistinctValues() throws Exception {
        SheetStructure cartouche = WorkbookFixture.vitis().getSheet("Cartouche_Fixe")
                .orElseThrow(AssertionError::new);

        assertEquals(1, cartouche.distinctValues("Dispositif").size());
        assertTrue(cartouche.distinctValues("Genotype").contains("Chardonnay B"));
        assertTrue(cartouche.distinctValues("Statut").contains("TNT"));
    }

    @Test
    public void aSampleIsBounded() throws Exception {
        SheetStructure cartouche = WorkbookFixture.vitis().getSheet("Cartouche_Fixe")
                .orElseThrow(AssertionError::new);

        assertEquals(5, cartouche.sample(5).size());
        assertEquals("a sample larger than the sheet returns the sheet",
                cartouche.getDataRowCount(), cartouche.sample(10_000).size());
        assertTrue(cartouche.sample(0).isEmpty());
    }

    @Test(expected = WorkbookReadException.class)
    public void aFileThatIsNotASpreadsheetIsRejected() throws Exception {
        File notASpreadsheet = Files.createTempFile("ai-import", ".xlsx").toFile();
        try {
            Files.write(notASpreadsheet.toPath(), "this is not a workbook".getBytes());
            new WorkbookReader().read(notASpreadsheet, "broken.xlsx");
        } finally {
            deleteQuietly(notASpreadsheet);
        }
    }

    private void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
            // A leftover temporary file is not worth failing a test over.
        }
    }
}
