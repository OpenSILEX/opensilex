//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The draft is where the assistant's proposal meets the server's rules.
 * <p>
 * The databases are passed as null throughout, which is the point: building a draft must not touch
 * them. A test that needed a running instance would be proving the opposite of what is wanted.
 *
 * @author Arnaud Charleroy
 */
public class ProposalBuilderTest {

    private static final URI ACCOUNT = URI.create("http://opensilex.test/id/account/alice");

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;

    private final ProposalBuilder builder =
            new ProposalBuilder(new AiImportCreationService(null, null, null, null));

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        VitisExplorerProfile profile = new VitisExplorerProfile();
        workbook = WorkbookFixture.vitis();
        plan = profile.extract(workbook);
    }

    //#region what the assistant gets wrong

    @Test
    public void afieldTheAssistantInventedIsRefused() {
        try {
            builder.build(session(), CreationTarget.EXPERIMENT,
                    Map.of("budget", "50000"), "because");
            fail("a field that does not exist must not reach the draft");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("budget"));
            assertTrue("the message must list what does exist, so the assistant can correct itself",
                    expected.getMessage().contains("objective"));
        }
    }

    @Test
    public void aMalformedDateIsRefused() {
        try {
            builder.build(session(), CreationTarget.EXPERIMENT,
                    Map.of("start_date", "15/03/2024"), "because");
            fail("a date the server cannot parse must not reach the draft");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("start_date"));
            assertTrue(expected.getMessage().contains("yyyy-MM-dd"));
        }
    }

    //#endregion

    //#region what the draft holds

    @Test
    public void aMissingRequiredFieldIsNamedRatherThanInvented() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", "Essai 2024");
        fields.put("start_date", "2024-03-01");

        CreationProposal proposal =
                builder.build(session(), CreationTarget.EXPERIMENT, fields, "because");

        assertTrue("no data file states an objective, so it must come back as missing",
                proposal.getMissingRequired().contains("objective"));
        assertFalse("a draft missing a required field is not ready", proposal.isReady());
        assertNull(proposal.getFields().get("objective"));
    }

    @Test
    public void aCompleteDraftIsReady() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", "Essai 2024");
        fields.put("start_date", "2024-03-01");
        fields.put("objective", "Évaluer les biocontrôles");

        CreationProposal proposal =
                builder.build(session(), CreationTarget.EXPERIMENT, fields, "because");

        assertTrue(proposal.getMissingRequired().toString(), proposal.isReady());
        assertEquals(CreationProposal.Status.PENDING, proposal.getStatus());
        assertEquals("because", proposal.getRationale());
    }

    @Test
    public void afieldTheAssistantOmittedFallsBackToTheFile() {
        // The trial name and the dates are in the file; the assistant need not repeat them.
        CreationProposal proposal = builder.build(session(), CreationTarget.EXPERIMENT,
                Map.of("objective", "Évaluer les biocontrôles"), "because");

        assertEquals("CEPInnov_Champagne", proposal.getFields().get("name"));
        assertEquals(CreationProposal.FieldSource.FILE, proposal.getFieldSources().get("name"));
        assertEquals("2020-04-06", proposal.getFields().get("start_date"));
        assertTrue(proposal.isReady());
    }

    @Test
    public void theOriginOfEachValueIsRecorded() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", "Un autre nom");
        fields.put("objective", "Évaluer les biocontrôles");

        CreationProposal proposal =
                builder.build(session(), CreationTarget.EXPERIMENT, fields, "because");

        assertEquals("a name the assistant chose is not a name read from the file",
                CreationProposal.FieldSource.ASSISTANT, proposal.getFieldSources().get("name"));
        assertEquals(CreationProposal.FieldSource.ASSISTANT,
                proposal.getFieldSources().get("objective"));
        assertEquals("an untouched suggestion still comes from the file",
                CreationProposal.FieldSource.FILE, proposal.getFieldSources().get("start_date"));
    }

    @Test
    public void aValueEqualToTheFilesSuggestionCountsAsComingFromTheFile() {
        CreationProposal proposal = builder.build(session(), CreationTarget.EXPERIMENT,
                Map.of("name", "CEPInnov_Champagne", "objective", "x"), "because");

        assertEquals(CreationProposal.FieldSource.FILE, proposal.getFieldSources().get("name"));
    }

    @Test
    public void aBlockedTargetProducesABlockedDraft() {
        // Nothing resolves, so the data cannot be inserted whatever the assistant proposes.
        CreationProposal proposal =
                builder.build(session(), CreationTarget.DATA, Map.of(), "because");

        assertFalse(proposal.isReady());
        assertFalse(proposal.getBlockers().isEmpty());
    }

    @Test
    public void theFieldsKeepTheOrderTheServerDeclares() {
        CreationProposal proposal = builder.build(session(), CreationTarget.PROJECT,
                Map.of("start_date", "2024-03-01", "name", "Un projet"), "because");

        assertEquals("the card must read the same way whatever order the assistant sent",
                "name", proposal.getFields().keySet().iterator().next());
    }

    //#endregion

    private AiImportSession session() {
        AiImportSession session = new AiImportSession("test", ACCOUNT);
        session.setFileName(WorkbookFixture.VITIS_FILE_NAME)
                .setWorkbook(workbook)
                .setPlan(plan)
                .setDataPoints(new VitisExplorerProfile().extractDataPoints(workbook));

        ResolutionReport report = new ResolutionReport();
        plan.getExperimentNames().forEach(name -> report.getExperiments()
                .add(new ResolvedItem(name).setStatus(ResolutionStatus.MISSING)));
        for (VariableCandidate candidate : plan.getVariables()) {
            report.getVariables().add(new ResolvedItem(candidate.getColumnKey())
                    .setStatus(ResolutionStatus.MISSING));
        }
        session.setReport(report);
        return session;
    }
}
