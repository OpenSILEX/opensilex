//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.GenericTabularProfile;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The mapping is what the type checks and the creation forms are built on, so it is checked against
 * the real sample workbook rather than a fixture of convenience.
 *
 * @author Arnaud Charleroy
 */
public class MappingServiceTest {

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;

    private final VitisExplorerProfile profile = new VitisExplorerProfile();
    private final MappingService service = new MappingService();

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        workbook = WorkbookFixture.vitis();
        plan = new VitisExplorerProfile().extract(workbook);
    }

    //#region roles

    @Test
    public void theCartoucheColumnsMapOntoTheirConcepts() {
        List<ColumnMapping> mappings = service.map(workbook, profile, emptyReport());

        assertEquals(ColumnRole.TRIAL, roleOf(mappings, "Dispositif"));
        assertEquals(ColumnRole.OBJECT, roleOf(mappings, "PU"));
        assertEquals(ColumnRole.GERMPLASM, roleOf(mappings, "Genotype"));
        assertEquals(ColumnRole.FACTOR_LEVEL, roleOf(mappings, "Statut"));
        assertEquals(ColumnRole.SEASON, roleOf(mappings, "Millesime"));
        // Not a place: it says where the plot starts in the row layout, which is a property of
        // the plot, written once on the object rather than observed on a date.
        assertEquals(ColumnRole.OBJECT_PROPERTY, roleOf(mappings, "Premier_Rang"));
    }

    @Test
    public void aStageSheetSeparatesTheDateTheObserverAndTheMeasurements() {
        List<ColumnMapping> mappings = service.map(workbook, profile, emptyReport());

        assertEquals(ColumnRole.DATE, roleOf(mappings, "Date"));
        assertEquals(ColumnRole.OBSERVER, roleOf(mappings, "Observateur"));
        assertEquals(ColumnRole.COMMENT, roleOf(mappings, "Obs_libre"));
        assertEquals(ColumnRole.VARIABLE, roleOf(mappings, "Bai_Suc_g"));
    }

    @Test
    public void aRoleNamesTheConceptItFeeds() {
        assertEquals("ScientificObject", ColumnRole.OBJECT.getEntity());
        assertEquals("Variable", ColumnRole.VARIABLE.getEntity());
        assertEquals("Provenance", ColumnRole.OBSERVER.getEntity());
        assertNull("a free comment feeds nothing", ColumnRole.COMMENT.getEntity());
    }

    //#endregion

    //#region what the cells hold

    @Test
    public void theKindOfEachColumnIsReadFromItsCells() {
        List<ColumnMapping> mappings = service.map(workbook, profile, emptyReport());

        assertEquals(ValueKind.DECIMAL, kindOf(mappings, "Bai_Suc_g"));
        assertEquals(ValueKind.TEXT, kindOf(mappings, "Bai_Coul"));
        assertEquals(ValueKind.INTEGER, kindOf(mappings, "Millesime"));
        assertEquals(ValueKind.DATE, kindOf(mappings, "Deb_Date"));
    }

    @Test
    public void missingCellsAreCountedRatherThanTyped() {
        ColumnMapping sugar = find(service.map(workbook, profile, emptyReport()), "Bai_Suc_g");

        assertTrue("the sample file leaves plenty of cells at NA", sugar.getMissingCount() > 0);
        assertTrue(sugar.getValueCount() > 0);
        assertFalse(sugar.getSampleValues().isEmpty());
    }

    //#endregion

    //#region type mismatches

    @Test
    public void aColumnWithNoMatchingVariableGetsADatatypeProposed() {
        ColumnMapping sugar = find(service.map(workbook, profile, emptyReport()), "Bai_Suc_g");

        assertEquals(ResolutionStatus.MISSING, sugar.getResolutionStatus());
        assertNotNull(sugar.getSuggestion());
        assertTrue("the values are decimals, so decimal is what to create it with",
                sugar.getSuggestion().contains("decimal"));
    }

    @Test
    public void decimalsInAVariableDeclaredAsWholeNumbersAreReported() {
        ResolutionReport report = reportWhereSugarIs(XSDDatatype.XSDinteger.getURI());
        ColumnMapping sugar = find(service.map(workbook, profile, report), "Bai_Suc_g");

        assertEquals(ResolutionStatus.FOUND, sugar.getResolutionStatus());
        assertNotNull(sugar.getSuggestion());
        assertTrue(sugar.getSuggestion().contains("whole numbers"));
        assertTrue("the offending cells must be quoted, with their row", sugar.hasIssues());
        assertEquals("not a whole number", sugar.getIssues().get(0).getProblem());
        assertTrue(sugar.getIssues().get(0).getRowNumber() >= 2);
        assertNotNull(sugar.getIssues().get(0).getSuggestion());
    }

    @Test
    public void decimalsInAVariableDeclaredAsDecimalAreAccepted() {
        ResolutionReport report = reportWhereSugarIs(XSDDatatype.XSDdecimal.getURI());
        ColumnMapping sugar = find(service.map(workbook, profile, report), "Bai_Suc_g");

        assertFalse(sugar.hasIssues());
        assertNull(sugar.getSuggestion());
    }

    @Test
    public void aVariableWithNoDeclaredTypeIsSaidSoRatherThanCheckedSilently() {
        ResolutionReport report = reportWhereSugarIs(null);
        ColumnMapping sugar = find(service.map(workbook, profile, report), "Bai_Suc_g");

        assertFalse(sugar.hasIssues());
        assertNotNull(sugar.getSuggestion());
        assertTrue(sugar.getSuggestion().contains("no data type"));
    }

    @Test
    public void textInANumericVariableIsReported() {
        ResolutionReport report = new ResolutionReport();
        report.getVariables().add(new ResolvedItem("Bai_Coul")
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(
                        URI.create("http://opensilex.test/id/variable/berry_colour"), "Berry colour")
                        .setDatatype(XSDDatatype.XSDdecimal.getURI()))));

        ColumnMapping colour = find(service.map(workbook, profile, report), "Bai_Coul");

        assertTrue(colour.hasIssues());
        assertEquals("not a number", colour.getIssues().get(0).getProblem());
        assertTrue(colour.getSuggestion().contains("expects numbers"));
    }

    @Test
    public void onlyVariableColumnsAreTypeChecked() {
        // The observer column holds text, and nothing expects otherwise: checking it would produce
        // noise the user cannot act on.
        ColumnMapping observer = find(service.map(workbook, profile, emptyReport()), "Observateur");

        assertFalse(observer.hasIssues());
        assertNull(observer.getSuggestion());
    }

    //#endregion

    //#region the generic profile

    @Test
    public void aColumnAppearsOnceAndListsItsSheets() {
        List<ColumnMapping> mappings = service.map(workbook, profile, emptyReport());

        long sugarEntries = mappings.stream()
                .filter(mapping -> "Bai_Suc_g".equals(mapping.getColumn()))
                .count();
        assertEquals("a variable observed at two stages is still one variable", 1, sugarEntries);

        ColumnMapping sugar = find(mappings, "Bai_Suc_g");
        assertTrue(sugar.getSheets().contains("10_Controle_Maturite"));
        assertTrue(sugar.getSheets().contains("14_Maturite_Recolte"));

        ColumnMapping plot = find(mappings, "PU");
        assertTrue("the cartouche is repeated in every sheet and must collapse to one entry",
                plot.getSheets().size() > 10);

        assertTrue("one entry per column, not per sheet and column",
                mappings.size() < 100);
    }

    @Test
    public void valuesAreCountedAcrossEverySheetTheColumnAppearsIn() {
        ColumnMapping plot = find(service.map(workbook, profile, emptyReport()), "PU");

        // 73 plots in the cartouche plus 73 in each of the 15 stage sheets.
        assertTrue("counts must span the sheets, not just the first one",
                plot.getValueCount() > 500);
    }

    @Test
    public void anOffendingCellNamesItsSheet() {
        ColumnMapping sugar = find(
                service.map(workbook, profile, reportWhereSugarIs(
                        org.apache.jena.datatypes.xsd.XSDDatatype.XSDinteger.getURI())),
                "Bai_Suc_g");

        assertTrue(sugar.hasIssues());
        assertNotNull("a column spans sheets, so a cell has to say which one it is in",
                sugar.getIssues().get(0).getSheet());
    }

    @Test
    public void theGenericProfileStillProducesAMapping() {
        List<ColumnMapping> mappings = new MappingService()
                .map(workbook, new GenericTabularProfile(), emptyReport());

        assertFalse(mappings.isEmpty());

        // The header dictionary carries the vocabulary of agronomic spreadsheets, so an
        // unrecognised file still gets a business mapping — which is what the page has to show
        // when the language model is unreachable.
        assertEquals("a plot identifier is recognised from its header alone",
                ColumnRole.OBJECT, roleOf(mappings, "PU"));
        assertEquals("a date column is still recognisable by its name",
                ColumnRole.DATE, roleOf(mappings, "Date"));
        assertEquals("and a measurement is left as one",
                ColumnRole.VARIABLE, roleOf(mappings, "Bai_Suc_g"));
    }

    //#endregion

    //#region helpers

    /**
     * A report in which nothing was found, which is the state right after uploading to an empty
     * instance.
     */
    private ResolutionReport emptyReport() {
        ResolutionReport report = new ResolutionReport();
        for (VariableCandidate candidate : plan.getVariables()) {
            report.getVariables().add(new ResolvedItem(candidate.getColumnKey())
                    .setStatus(ResolutionStatus.MISSING));
        }
        return report;
    }

    private ResolutionReport reportWhereSugarIs(String datatype) {
        ResolutionReport report = emptyReport();
        report.getVariables().removeIf(item -> "Bai_Suc_g".equals(item.getSourceValue()));
        report.getVariables().add(new ResolvedItem("Bai_Suc_g")
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(
                        URI.create("http://opensilex.test/id/variable/berry_sugar"), "Berry sugar")
                        .setDatatype(datatype))));
        return report;
    }

    private ColumnMapping find(List<ColumnMapping> mappings, String column) {
        return mappings.stream()
                .filter(mapping -> mapping.getColumn().equals(column))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no mapping for " + column));
    }

    private ColumnRole roleOf(List<ColumnMapping> mappings, String column) {
        return find(mappings, column).getRole();
    }

    private ValueKind kindOf(List<ColumnMapping> mappings, String column) {
        return find(mappings, column).getObservedKind();
    }

    private static void assertNull(Object value) {
        org.junit.Assert.assertNull(value);
    }

    private static void assertNull(String message, Object value) {
        org.junit.Assert.assertNull(message, value);
    }

    //#endregion
}
