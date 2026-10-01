//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.rows;

import org.junit.Test;
import org.opensilex.aiimport.api.dto.BulkValidationDTO;
import org.opensilex.aiimport.create.UnresolvedRow;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Every error the platform reports on its intermediate CSV lands on the row and column of the
 * user's workbook — the only place they can act on it.
 * <p>
 * The validation models are filled with the very methods the platform's importers call, so these
 * tests check the numbering conventions as the importers produce them: physical CSV lines from 1
 * for the scientific-object engine, body indexes from 0 for the data import.
 *
 * @author Arnaud Charleroy
 */
public class PlatformValidationAdapterTest {

    /**
     * Two data rows generated from rows 12 and 15 of the plot sheet, whose "plot_id" column became
     * rdfs:label in the CSV.
     */
    private RowOrigins plotOrigins() {
        RowOrigins origins = new RowOrigins().addColumn("rdfs:label", "plot_id");
        origins.addRow("ed_placette", 12);
        origins.addRow("ed_placette", 15);
        return origins;
    }

    @Test
    public void aScientificObjectErrorLandsOnTheWorkbookRowAndColumn() {
        CSVValidationModel validation = new CSVValidationModel();
        // The second data row of the CSV is physical line 4: header, description, then rows.
        validation.addMissingRequiredValue(new CSVCell(4, 2, "", "rdfs:label"));

        List<RowError> errors = PlatformValidationAdapter.fromCsvImporter(validation, plotOrigins());

        assertEquals(1, errors.size());
        RowError error = errors.get(0);
        assertEquals("ed_placette", error.getSheet());
        assertEquals("the second generated row came from row 15", 15, error.getRow());
        assertEquals("the column the user knows, not the generated one", "plot_id", error.getColumn());
        assertEquals(RowError.Kind.MISSING_VALUE, error.getKind());
        assertEquals("AiImport.rows.kind.MISSING_VALUE", error.getMessage().getKey());
    }

    /**
     * The data import counts rows in the body from 0: a different convention, the same result.
     */
    @Test
    public void aDataErrorLandsOnTheWorkbookRow() {
        RowOrigins origins = new RowOrigins();
        origins.addRow("data_F1", 2);
        origins.addRow("data_F1", 3);
        DataCSVValidationModel validation = new DataCSVValidationModel();
        validation.addDuplicatedDataError(new CSVCell(1, 4, "12.5", "PM_LEAF_PC"));

        List<RowError> errors = PlatformValidationAdapter.fromDataImport(validation, origins);

        assertEquals(1, errors.size());
        assertEquals(3, errors.get(0).getRow());
        assertEquals("the validation only compares the file with itself",
                RowError.Kind.DUPLICATE_IN_FILE, errors.get(0).getKind());
    }

    /**
     * After the insertion, a duplicate is one MongoDB's unique index refused: already in the
     * instance.
     */
    @Test
    public void aDuplicateRefusedAtInsertionIsAlreadyInTheInstance() {
        RowOrigins origins = new RowOrigins();
        origins.addRow("data_F1", 2);
        DataCSVValidationModel validation = new DataCSVValidationModel();
        validation.addDuplicatedDataError(new CSVCell(0, 4, "12.5", "PM_LEAF_PC"));

        RowError error = PlatformValidationAdapter.fromDataImport(validation, origins, 0,
                PlatformValidationAdapter.Stage.INSERTION).get(0);

        assertEquals(RowError.Kind.DUPLICATE_IN_INSTANCE, error.getKind());
    }

    /**
     * A file split in batches is validated batch by batch, each counting from 0: the batch's first
     * row puts its errors back on the right workbook row.
     */
    @Test
    public void anErrorOfTheSecondBatchLandsOnItsOwnRow() {
        RowOrigins origins = new RowOrigins();
        for (int row = 2; row < 12; row++) {
            origins.addRow("data_F1", row);
        }
        DataCSVValidationModel validation = new DataCSVValidationModel();
        validation.addInvalidDataTypeError(new CSVCell(1, 2, "abc", "PM_LEAF_PC"));

        RowError error = PlatformValidationAdapter.fromDataImport(validation, origins, 5,
                PlatformValidationAdapter.Stage.VALIDATION).get(0);

        assertEquals("body row 5 + 1 is the seventh line of data, workbook row 8", 8, error.getRow());
    }

    @Test
    public void anUnknownObjectInTheDataIsAnUnknownReference() {
        RowOrigins origins = new RowOrigins();
        origins.addRow("data_F1", 2);
        DataCSVValidationModel validation = new DataCSVValidationModel();
        validation.addInvalidObjectError(new CSVCell(0, 1, "A99", "scientific_object"));

        RowError error = PlatformValidationAdapter.fromDataImport(validation, origins).get(0);

        assertEquals(RowError.Kind.UNKNOWN_REFERENCE, error.getKind());
        assertEquals("A99", error.getValue());
    }

    /**
     * Chunk-wide errors come back on row 0, which no data row has. They are about the file, and
     * pinning them on a row would send the user to the wrong place.
     */
    @Test
    public void anErrorWithNoRowIsAboutTheFile() {
        CSVValidationModel validation = new CSVValidationModel();
        validation.addInvalidValueError(new CSVCell(0, 0, "", "vocabulary:hasGermplasm"));

        RowError error = PlatformValidationAdapter.fromCsvImporter(validation, plotOrigins()).get(0);

        assertTrue(error.isAboutTheFile());
    }

    @Test
    public void aHeaderTheGeneratorGotWrongIsReportedAboutTheFile() {
        CSVValidationModel validation = new CSVValidationModel();
        validation.addInvalidHeaderURI(3, "vocabulary:notAProperty");

        RowError error = PlatformValidationAdapter.fromCsvImporter(validation, plotOrigins()).get(0);

        assertEquals(RowError.Kind.FILE, error.getKind());
        assertEquals("AiImport.rows.kind.invalidHeader", error.getMessage().getKey());
    }

    @Test
    public void aTooLargeBatchIsSaid() {
        DataCSVValidationModel validation = new DataCSVValidationModel();
        validation.setTooLargeDataset(true);

        List<RowError> errors = PlatformValidationAdapter.fromDataImport(validation, new RowOrigins());

        assertEquals("AiImport.rows.kind.tooLarge", errors.get(0).getMessage().getKey());
    }

    /**
     * The module's own refusals are told in the same terms, so the grid shows both the same way.
     */
    @Test
    public void theModulesOwnRefusalsBecomeRowErrorsToo() {
        RowError error = RowError.fromUnresolved(new UnresolvedRow("data_F1", 7, "PM_LEAF_PC",
                "AiImport.proposal.unresolved.object", "A99"));

        assertEquals("data_F1", error.getSheet());
        assertEquals(7, error.getRow());
        assertEquals(RowError.Kind.UNRESOLVED, error.getKind());
        assertEquals("AiImport.proposal.unresolved.object", error.getMessage().getKey());
    }

    /**
     * The grid draws the user's own rows, so the rows travel with their values, in the column order
     * of the sheet — and only the faulty ones.
     */
    @Test
    public void theFaultyRowsTravelWithTheirValues() {
        SheetStructure sheet = new SheetStructure().setName("data_F1").setTabular(true)
                .setHeaders(Arrays.asList("plot_id", "observation_date", "PM_LEAF_PC"))
                .setRows(new ArrayList<>(Arrays.asList(
                        Arrays.asList("A1", "2024-06-05", "12"),
                        Arrays.asList("A99", "2024-06-05", "15"))));
        WorkbookStructure workbook = new WorkbookStructure().setSheets(List.of(sheet));

        List<RowError> errors = List.of(new RowError("data_F1", 3, "plot_id", "A99",
                RowError.Kind.UNKNOWN_REFERENCE, org.opensilex.aiimport.report.ReportMessage.plain("x")));
        BulkValidationDTO dto = BulkValidationDTO.of("DATA", 2, errors, workbook);

        assertEquals(1, dto.getRowsInError());
        assertEquals(1, dto.getSheets().size());
        BulkValidationDTO.SheetRowsDTO rows = dto.getSheets().get(0);
        assertEquals(Arrays.asList("plot_id", "observation_date", "PM_LEAF_PC"), rows.getHeaders());
        assertEquals("row 3 is the second data row", "A99",
                rows.getRows().get(0).getValues().get("plot_id"));
    }
}
