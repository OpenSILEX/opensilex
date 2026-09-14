//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.MappingService;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keeps the system prompt within a budget.
 * <p>
 * The prompt is rebuilt and resent on every message, so its size is a per-message cost, not a
 * one-off. Without a test, adding one more helpful paragraph to it is invisible until the bill
 * arrives; with one, the cost of a change shows up at review time.
 * <p>
 * The estimate is deliberately crude — the ratio of characters to tokens depends on the tokenizer,
 * which depends on the configured model. It is used to catch a change of order of magnitude, not to
 * predict a price.
 *
 * @author Arnaud Charleroy
 */
public class PromptBudgetTest {

    /**
     * Rough characters-per-token for the mixed English prose, French text and JSON this prompt is
     * made of. Byte-pair tokenizers land between 3.5 and 4.5 on this kind of content.
     */
    private static final double CHARS_PER_TOKEN = 4.0;

    /**
     * What a single analysis of the reference workbook may cost, in estimated tokens.
     * <p>
     * Set just above what the prompt measures today (~5 300), so a change that inflates it fails at
     * review time. It was 17 900 before the report, the mapping and the file structure were reduced
     * to summaries with the detail served by get_report, get_mapping and get_sheet_preview.
     */
    private static final int PROMPT_BUDGET_TOKENS = 6_500;

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;
    private static List<ColumnMapping> mappings;

    private final VitisExplorerProfile profile = new VitisExplorerProfile();

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        VitisExplorerProfile profile = new VitisExplorerProfile();
        workbook = WorkbookFixture.vitis();
        plan = profile.extract(workbook);
        mappings = new MappingService().map(workbook, profile, reportWhereNothingIsFound());
    }

    @Test
    public void theSystemPromptStaysWithinBudget() {
        String prompt = buildPrompt(5);
        int tokens = (int) Math.round(prompt.length() / CHARS_PER_TOKEN);

        System.out.printf("[prompt budget] %,d chars, ~%,d tokens (budget %,d)%n",
                prompt.length(), tokens, PROMPT_BUDGET_TOKENS);

        assertTrue("the system prompt is resent on every message, so its size is a recurring cost;"
                        + " measured ~" + tokens + " tokens against a budget of "
                        + PROMPT_BUDGET_TOKENS,
                tokens <= PROMPT_BUDGET_TOKENS);
    }

    @Test
    public void theRowSampleNoLongerDrivesTheSize() {
        int oneRow = buildPrompt(1).length();
        int fiveRows = buildPrompt(5).length();
        int twentyRows = buildPrompt(20).length();

        System.out.printf("[prompt budget] sampleRowsPerSheet 1 -> %,d chars, 5 -> %,d, 20 -> %,d%n",
                oneRow, fiveRows, twentyRows);

        assertTrue("more rows must still cost more", oneRow < fiveRows && fiveRows < twentyRows);

        // The sample is now bounded to the first few sheets rather than applied to all seventeen,
        // so a row costs a few hundred characters instead of ~2 200. This pins that: if the bound
        // is ever removed, the setting becomes a cost driver again and this fails.
        int perRow = (twentyRows - oneRow) / 19;
        assertTrue("a row should cost under 1 000 characters now that only a few sheets are "
                        + "sampled, measured " + perRow,
                perRow > 0 && perRow < 1_000);
    }

    @Test
    public void thePromptExplainsHowToDraftACreation() {
        String prompt = buildPrompt(5);

        assertTrue("the tool that drafts a creation must be named",
                prompt.contains("propose_creation"));
        assertTrue("the order matters: describe, then propose",
                prompt.contains("Describe first, propose second"));
        assertTrue("inventing a required value has to be forbidden explicitly",
                prompt.contains("Never invent a value for a required field"));
        assertTrue("the assistant must not claim it created anything",
                prompt.contains("never say you have created it"));

        // The assistant used to send users to the general screens, throwing away the draft this
        // page can build from their file.
        assertTrue(prompt.contains("Never tell the user to go to the general OpenSILEX screens"));

        // Derived from the code, so a field renamed in Java shows up here rather than drifting.
        assertTrue(prompt.contains("EXPERIMENT"));
        assertTrue(prompt.contains("required_fields"));
        assertTrue("the experiment objective is required and the prompt must say so",
                prompt.contains("objective"));
        assertTrue("the file suggests the trial name, and the prompt should carry it",
                prompt.contains("CEPInnov_Champagne"));
    }

    @Test
    public void thePromptNoLongerMentionsTheRemovedPanel() {
        String prompt = buildPrompt(5);

        assertFalse("the forms panel is gone; a prompt still describing it would send users looking"
                        + " for something that is not on screen",
                prompt.contains("creation panel on this page"));
    }

    @Test
    public void theDetailIsReachableRatherThanSent() {
        String prompt = buildPrompt(5);

        // The summaries are only defensible if the model is told where the rest is.
        assertTrue(prompt.contains("get_report"));
        assertTrue(prompt.contains("get_mapping"));
        assertTrue(prompt.contains("get_sheet_preview"));

        // 73 plots, all unresolved: the prompt must carry the count and a few examples, not 73 lines.
        assertTrue("the report has to be summarised", prompt.contains("by_status"));
        assertTrue("the shared cartouche columns must be declared once",
                prompt.contains("columns_present_in_every_table_sheet"));
    }

    private String buildPrompt(int sampleRows) {
        return new PromptBuilder(new ObjectMapper(), sampleRows)
                .build(workbook, profile, reportWhereNothingIsFound(), mappings,
                        creationRequirements(), "fr");
    }

    /**
     * What the creation panel would ask for. Computed with the real service, since the point of
     * putting it in the prompt is that it cannot drift from the form the user sees.
     */
    private List<CreationRequirements> creationRequirements() {
        AiImportSession session = new AiImportSession("budget",
                URI.create("http://opensilex.test/id/account/alice"));
        session.setFileName(WorkbookFixture.VITIS_FILE_NAME)
                .setWorkbook(workbook)
                .setPlan(plan)
                .setReport(reportWhereNothingIsFound())
                .setDataPoints(new VitisExplorerProfile().extractDataPoints(workbook));

        AiImportCreationService service = new AiImportCreationService(null, null, null, null);
        List<CreationRequirements> requirements = new ArrayList<>();
        for (CreationTarget target : CreationTarget.values()) {
            requirements.add(service.requirementsFor(target, session));
        }
        return requirements;
    }

    /**
     * The worst realistic case: an empty instance, where every name is missing and therefore every
     * entry carries a hint.
     */
    private static ResolutionReport reportWhereNothingIsFound() {
        ResolutionReport report = new ResolutionReport().setProfileId(VitisExplorerProfile.ID);
        report.getAnomalies().addAll(plan.getAnomalies());
        report.getNotes().putAll(plan.getNotes());

        plan.getExperimentNames().forEach(name -> report.getExperiments().add(
                new ResolvedItem(name).setStatus(ResolutionStatus.MISSING)
                        .setHint("No experiment carries this name.")));
        for (VariableCandidate candidate : plan.getVariables()) {
            report.getVariables().add(new ResolvedItem(candidate.getColumnKey())
                    .setExternalId(candidate.getExternalId())
                    .setStatus(ResolutionStatus.MISSING)
                    .setHint("No variable matches this column."));
        }
        plan.getGermplasmNames().forEach(name -> report.getGermplasm().add(
                new ResolvedItem(name).setStatus(ResolutionStatus.MISSING)
                        .setHint("No germplasm carries this name.")));
        plan.getScientificObjectNames().forEach(name -> report.getScientificObjects().add(
                new ResolvedItem(name).setStatus(ResolutionStatus.NOT_CHECKED)
                        .setHint("Settle the experiment first.")));
        return report;
    }
}
