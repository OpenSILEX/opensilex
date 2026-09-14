//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Arnaud Charleroy
 */
public class VitisExplorerProfileTest {

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;

    private final VitisExplorerProfile profile = new VitisExplorerProfile();

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        workbook = WorkbookFixture.vitis();
        plan = new VitisExplorerProfile().extract(workbook);
    }

    @Test
    public void theTemplateIsRecognised() {
        assertEquals(100, profile.match(workbook));
        assertTrue("the template must beat the generic fallback",
                profile.match(workbook) > new GenericTabularProfile().match(workbook));
    }

    @Test
    public void theRegistryPicksIt() {
        assertEquals(VitisExplorerProfile.ID, new ImportProfileRegistry().select(workbook).getId());
    }

    @Test
    public void theTrialIsTheCandidateExperiment() {
        assertEquals(1, plan.getExperimentNames().size());
        assertEquals("CEPInnov_Champagne", plan.getExperimentNames().get(0));
    }

    @Test
    public void genotypesAreCandidateGermplasm() {
        assertTrue(plan.getGermplasmNames().contains("Pinot noir N"));
        assertTrue(plan.getGermplasmNames().contains("Chardonnay B"));
        assertTrue(plan.getGermplasmNames().contains("Floreal B"));
    }

    @Test
    public void unitPlotsAreCandidateScientificObjects() {
        assertFalse(plan.getScientificObjectNames().isEmpty());
        assertTrue(plan.getScientificObjectNames().contains("31"));
    }

    @Test
    public void observersAreCollectedForTheProvenance() {
        assertTrue(plan.getObserverNames().contains("JFR"));
    }

    @Test
    public void everyCatalogueVariableIsFound() {
        assertEquals("the catalogue sheet lists 39 variables",
                "39", plan.getNotes().get("catalogue entries"));
    }

    @Test
    public void aVariableColumnCarriesItsCropOntologyIdentifier() {
        VariableCandidate sugar = candidate("Bai_Suc_g");
        assertNotNull("the sugar content column must be offered as a variable", sugar);
        assertEquals("CO_356:1000217", sugar.getExternalId());
        assertEquals("Baie : concentration en sucre en g/l", sugar.getLabel());
        assertTrue(sugar.getSheets().contains("10_Controle_Maturite"));
        assertTrue("the same variable is observed at two stages",
                sugar.getSheets().contains("14_Maturite_Recolte"));
    }

    @Test
    public void aVariableWithoutAnOntologyIdentifierIsStillOffered() {
        VariableCandidate necrosis = candidate("F_pct_Nec");
        assertNotNull(necrosis);
        assertEquals("Fréquence de feuilles avec tâches nécrotiques en %", necrosis.getLabel());
    }

    @Test
    public void theCartoucheColumnsAreNotVariables() {
        assertNull(candidate("Dispositif"));
        assertNull(candidate("PU"));
        assertNull(candidate("Genotype"));
        assertNull(candidate("Statut"));
        assertNull(candidate("Millesime"));
        assertNull(candidate("Date"));
        assertNull(candidate("Observateur"));
        assertNull("the free comment column is not a measurement", candidate("Obs_libre"));
    }

    @Test
    public void theCartoucheSeasonDisagreeingWithTheSheetsIsRaised() {
        // The sample file declares 2018 in the cartouche and 2020 in every stage sheet, while the
        // template states the cartouche is used as-is by each sheet.
        assertEquals("2018", plan.getNotes().get("cartouche season"));
        assertEquals("2018, 2020", plan.getNotes().get("declared seasons"));
        assertEquals("2020", plan.getNotes().get("observation years"));

        assertTrue("the cartouche season and the stage sheets disagree and it must be raised",
                anomalyMatching("Cartouche_Fixe", "2018", "2020").isPresent());
    }

    @Test
    public void aWorkbookNotInThe1900DateSystemIsRaised() {
        assertEquals("1904, but the template requires 1900", plan.getNotes().get("date system"));
        assertTrue("the template requires the 1900 date system, so 1904 must be flagged",
                anomalyMatching("1904", "require").isPresent());
    }

    @Test
    public void observationDatesMatchingTheSeasonAreNotFlagged() {
        // The dates are 2020 and the stage sheets declare 2020, so only the cartouche mismatch
        // and the date system are worth raising; the dates themselves are consistent.
        assertFalse("the dates agree with the season declared beside them",
                anomalyMatching("observation dates fall in").isPresent());
    }

    @Test
    public void aColumnAbsentFromTheCatalogueIsRaised() {
        // The catalogue calls it Cep_HE while the sheet header says Souche_HE.
        assertNotNull(candidate("Souche_HE"));
        assertTrue(anomalyMatching("not listed in", "Souche_HE").isPresent());
    }

    @Test
    public void thePlotStatusesAreSurfaced() {
        String statuses = plan.getNotes().get("plot statuses");
        assertNotNull(statuses);
        assertTrue(statuses.contains("TT"));
        assertTrue(statuses.contains("DNT"));
    }

    @Test
    public void theTemplateConventionsReachThePrompt() {
        String context = profile.getPromptContext(workbook);
        assertTrue(context.contains("Cartouche_Fixe"));
        assertTrue(context.contains("CO_356"));
        assertTrue("the assistant must know NA means no value", context.contains("'NA' means no value"));
    }

    @Test
    public void observationsAreReadOutOfTheStageSheets() {
        List<DataPoint> points = profile.extractDataPoints(workbook);
        assertFalse(points.isEmpty());

        DataPoint sugar = points.stream()
                .filter(point -> "10_Controle_Maturite".equals(point.getSheet()))
                .filter(point -> "Bai_Suc_g".equals(point.getVariableKey()))
                .filter(point -> "2".equals(point.getObjectName()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals("171.666", sugar.getRawValue());
        assertEquals(LocalDate.of(2020, 9, 10), sugar.getDate());
        assertEquals("JFR", sugar.getObserver());
        assertEquals("the header is row 1, so the first data row is row 2", 2, sugar.getRowNumber());
    }

    @Test
    public void missingValuesProduceNoObservation() {
        List<DataPoint> points = profile.extractDataPoints(workbook);

        assertTrue("NA means no value and must not become an observation",
                points.stream().noneMatch(point -> ExcelValueParser.isMissing(point.getRawValue())));
    }

    @Test
    public void theCartoucheAndTheCatalogueCarryNoObservation() {
        List<DataPoint> points = profile.extractDataPoints(workbook);

        assertTrue(points.stream().noneMatch(point ->
                "Cartouche_Fixe".equals(point.getSheet())
                        || "Chronologie".equals(point.getSheet())
                        || "ReadMe".equals(point.getSheet())));
    }

    @Test
    public void aPhenologyDateIsBothTheDateAndTheObservation() {
        // 2_Pheno_Debourrement has no plain Date column: Deb_Date dates the row and is the value.
        List<DataPoint> points = profile.extractDataPoints(workbook);

        assertTrue("a stage sheet dated by its own column must still yield observations",
                points.stream().anyMatch(point -> "2_Pheno_Debourrement".equals(point.getSheet())));
    }

    @Test
    public void theGenericProfileRefusesToGuessObservations() {
        assertTrue("guessing the object and date columns would file data against the wrong plots",
                new GenericTabularProfile().extractDataPoints(workbook).isEmpty());
    }

    /**
     * @return the first anomaly containing every fragment
     */
    private Optional<String> anomalyMatching(String... fragments) {
        return plan.getAnomalies().stream()
                .filter(anomaly -> {
                    for (String fragment : fragments) {
                        if (!anomaly.contains(fragment)) {
                            return false;
                        }
                    }
                    return true;
                })
                .findFirst();
    }

    private VariableCandidate candidate(String columnKey) {
        return plan.getVariables().stream()
                .filter(variable -> variable.getColumnKey().equalsIgnoreCase(columnKey))
                .findFirst()
                .orElse(null);
    }

    private static void assertNull(Object value) {
        org.junit.Assert.assertNull(value);
    }

    private static void assertNull(String message, Object value) {
        org.junit.Assert.assertNull(message, value);
    }
}
