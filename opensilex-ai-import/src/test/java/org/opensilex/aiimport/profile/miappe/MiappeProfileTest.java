//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.miappe;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checked against the MIAPPE v1.1 training spreadsheet, which is the checklist as it is handed to
 * a submitter: every section present and documented, no values filled in yet.
 * <p>
 * That is the state the profile must handle best. A submission arrives empty and gets filled, so a
 * reader that only works on a complete one is a reader that never helps anybody start.
 *
 * @author Arnaud Charleroy
 */
public class MiappeProfileTest {

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;

    private final MiappeProfile profile = new MiappeProfile();

    @BeforeClass
    public static void readTheFile() throws Exception {
        workbook = WorkbookFixture.miappe();
        plan = new MiappeProfile().extract(workbook);
    }

    //#region recognition

    @Test
    public void theChecklistIsRecognisedByItsSections() {
        assertEquals(100, profile.match(workbook));
    }

    /**
     * Eleven sections plus two appendices and a definitions page — the appendices are reference
     * lists and must not be mistaken for sections to import.
     */
    @Test
    public void everySectionOfTheChecklistIsFound() {
        MiappeSheets sheets = new MiappeSheets(workbook);
        assertEquals(MiappeSheets.SECTIONS.size(), sheets.sectionCount());
        assertFalse(sheets.section("Appendix I - Environment").isPresent());
    }

    /**
     * The registry has to pick MIAPPE over the two field templates, which is the whole point of
     * scoring rather than taking the first profile that says yes.
     */
    @Test
    public void theRegistryPicksItOverTheOtherTemplates() {
        ImportProfile selected = new ImportProfileRegistry().select(workbook);

        assertEquals(MiappeProfile.ID, selected.getId());
        assertEquals(0, new StarProfile().match(workbook));
        assertEquals(0, new VitisExplorerProfile().match(workbook));
    }

    //#endregion

    //#region the documentation rows

    /**
     * The three rows under each header hold the definition, an example and the format. Reading them
     * as values would import the standard's own examples as if they were the user's data — a plot
     * called "plot:894", a person called "Ines Chaves".
     */
    @Test
    public void theDocumentationRowsAreNotValues() {
        MiappeSection people = new MiappeSheets(workbook).section(MiappeSheets.PERSON)
                .orElseThrow(AssertionError::new);

        assertTrue("the example row must not be read as a value", people.values().isEmpty());
        assertTrue(plan.getObserverNames().isEmpty());
    }

    /**
     * Investigation is written the other way round, one field per row.
     */
    @Test
    public void theInvestigationSectionIsReadTransposed() {
        MiappeSection investigation = new MiappeSheets(workbook)
                .section(MiappeSheets.INVESTIGATION)
                .orElseThrow(AssertionError::new);

        assertTrue(investigation.isTransposed());
        assertTrue(investigation.fields().contains("Investigation title"));
        assertTrue(investigation.fields().contains("MIAPPE version"));
    }

    //#endregion

    //#region what the checklist says is mandatory

    /**
     * The asterisk is MIAPPE's own statement of what a submission must carry, so the profile can
     * report what is missing without a rule being written for each field.
     */
    @Test
    public void theAsteriskMarksTheMandatoryFields() {
        MiappeSection study = new MiappeSheets(workbook).section(MiappeSheets.STUDY)
                .orElseThrow(AssertionError::new);

        List<String> mandatory = study.mandatoryFields();
        assertTrue(mandatory.toString(), mandatory.contains("Study unique ID"));
        assertTrue(mandatory.toString(), mandatory.contains("Start date of study"));
        // Optional in the checklist, and it must not be reported as missing.
        assertFalse(mandatory.toString(), mandatory.contains("End date of study"));
    }

    /**
     * An empty template is not an error, but the user has to be told it is empty — otherwise the
     * page reports nothing found and leaves them wondering what went wrong.
     */
    @Test
    public void anEmptyTemplateSaysSoRatherThanSayingNothing() {
        String anomalies = String.join(" | ", plan.getAnomalies());

        assertFalse("an empty submission must be reported", plan.getAnomalies().isEmpty());
        assertTrue(anomalies, anomalies.contains("blank templates"));
    }

    @Test
    public void nothingIsExtractedFromAnEmptySubmission() {
        assertTrue(plan.getExperimentNames().isEmpty());
        assertTrue(plan.getProjectNames().isEmpty());
        assertTrue(plan.getScientificObjectNames().isEmpty());
        assertTrue(plan.getVariables().isEmpty());
        assertTrue(profile.extractEvents(workbook).isEmpty());
    }

    /**
     * The measurements are in the file the Data file section links to, never in this workbook.
     */
    @Test
    public void thereAreNoObservationsToInsert() {
        assertTrue(profile.extractDataPoints(workbook).isEmpty());
        assertTrue(profile.getPromptContext(workbook)
                .contains("The observations are NOT in this workbook"));
    }

    //#endregion
}
