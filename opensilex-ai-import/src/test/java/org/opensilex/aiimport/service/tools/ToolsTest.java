//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.MappingService;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedComponent;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.ToolRegistry;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The tools the model may call, called directly on a session built from the VitisExplorer file: what
 * each answers, and how each refuses — a tool answers with an {@code error} field, never an
 * exception the model would not see.
 * <p>
 * Only the tools that read the session are here; the searches need an instance and are exercised
 * through the REST tests.
 *
 * @author Arnaud Charleroy
 */
public class ToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;
    private static List<ColumnMapping> mappings;

    private AiImportSession session;

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        VitisExplorerProfile profile = new VitisExplorerProfile();
        workbook = WorkbookFixture.vitis();
        plan = profile.extract(workbook);
        mappings = new MappingService().map(workbook, profile, new ResolutionReport());
    }

    private ToolContext context() {
        session = new AiImportSession("tools", URI.create("http://opensilex.test/id/account/alice"));
        session.setFileName(WorkbookFixture.VITIS_FILE_NAME).setWorkbook(workbook).setPlan(plan)
                .setProfileId(VitisExplorerProfile.ID);
        ResolutionReport report = new ResolutionReport();
        report.getExperiments().add(new ResolvedItem("CEPInnov_Champagne").setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(URI.create("http://opensilex.test/id/xp"), "CEPInnov"))));
        for (VariableCandidate candidate : plan.getVariables()) {
            ResolvedItem item = new ResolvedItem(candidate.getColumnKey()).setStatus(ResolutionStatus.MISSING);
            ResolvedComponent unit = new ResolvedComponent("unit", "centimetre", "UO:0000015");
            unit.setSuggestion(new ResourceReference(URI.create("http://opensilex.test/id/unit/cm"), "centimeter"));
            item.getComponents().add(unit);
            item.getSuggestions().add(new ResourceReference(URI.create("http://opensilex.test/id/v"), "close one"));
            report.getVariables().add(item);
        }
        session.setReport(report).setMappings(mappings);
        return new ToolContext(null, null, null, null, workbook, null, report, mappings, session,
                new AiImportCreationService(null, null, null, null));
    }

    private ToolContext emptyContext() {
        return new ToolContext(null, null, null, null, null, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> run(AiTool tool, String json, ToolContext context) throws Exception {
        JsonNode arguments = MAPPER.readTree(json);
        return (Map<String, Object>) tool.execute(arguments, context);
    }

    //#region the registry

    @Test
    public void everyToolDescribesItselfToTheModel() {
        ToolRegistry registry = new ToolRegistry(MAPPER);

        assertTrue(registry.getDefinitions().size() >= 10);
        for (String name : List.of("get_report", "get_mapping", "get_sheet_preview", "get_creation_fields",
                "propose_creation", "search_experiments", "search_projects", "search_germplasm",
                "search_variables", "search_variables_in_shared_resource")) {
            AiTool tool = registry.get(name).orElseThrow(() -> new AssertionError(name));
            assertEquals(name, tool.getName());
            assertFalse(tool.getDescription().isEmpty());
            ObjectNode schema = tool.getParametersSchema(MAPPER);
            assertEquals("object", schema.get("type").asText());
        }
        assertFalse(registry.get("no_such_tool").isPresent());
    }

    //#endregion

    //#region get_mapping

    @Test
    public void theMappingIsListedWholeOrForOneColumn() throws Exception {
        GetColumnMappingTool tool = new GetColumnMappingTool();
        ToolContext context = context();

        Map<String, Object> all = run(tool, "{}", context);
        assertFalse(all.toString(), all.containsKey("error"));

        String column = mappings.get(0).getColumn();
        Map<String, Object> one = run(tool, "{\"column\":\"" + column + "\"}", context);
        assertEquals(column, one.get("column"));
        assertFalse(((List<?>) one.get("mappings")).isEmpty());
    }

    @Test
    public void anUnknownColumnListsTheColumnsThatExist() throws Exception {
        Map<String, Object> error = run(new GetColumnMappingTool(), "{\"column\":\"nope\"}", context());

        assertTrue(error.containsKey("error"));
        assertFalse(((List<?>) error.get("available_columns")).isEmpty());
        assertTrue(run(new GetColumnMappingTool(), "{}", emptyContext()).containsKey("error"));
    }

    //#endregion

    //#region get_report

    @Test
    public void theReportIsReadByCategoryStatusAndPage() throws Exception {
        GetReportTool tool = new GetReportTool();
        ToolContext context = context();

        Map<String, Object> missing = run(tool,
                "{\"category\":\"variables\",\"status\":\"MISSING\",\"limit\":1,\"offset\":0}", context);
        assertEquals("MISSING", missing.get("status_filter"));
        assertEquals(1, missing.get("returned"));
        assertTrue("a long category says how to page on", missing.containsKey("note"));

        for (String category : List.of("experiments", "projects", "germplasm", "scientific_objects",
                "facilities", "persons")) {
            Map<String, Object> page = run(tool, "{\"category\":\"" + category + "\"}", context);
            assertFalse(category + ": " + page, page.containsKey("error"));
        }
    }

    @Test
    public void theReportToolRefusesWhatItCannotAnswer() throws Exception {
        GetReportTool tool = new GetReportTool();

        assertTrue(run(tool, "{}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"category\":\"variables\"}", emptyContext()).containsKey("error"));
        Map<String, Object> unknown = run(tool, "{\"category\":\"planets\"}", context());
        assertTrue(unknown.containsKey("available_categories"));
    }

    //#endregion

    //#region get_sheet_preview

    @Test
    public void aSheetIsPreviewedByName() throws Exception {
        GetSheetPreviewTool tool = new GetSheetPreviewTool();
        String sheet = workbook.getSheets().get(0).getName();

        Map<String, Object> preview = run(tool, "{\"sheet\":\"" + sheet + "\",\"rows\":3}", context());
        assertFalse(preview.toString(), preview.containsKey("error"));

        assertTrue(run(tool, "{}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"sheet\":\"No such sheet\"}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"sheet\":\"" + sheet + "\"}", emptyContext()).containsKey("error"));
    }

    //#endregion

    //#region creation

    @Test
    public void theFieldsOfATargetAreListed() throws Exception {
        GetCreationFieldsTool tool = new GetCreationFieldsTool();

        Map<String, Object> project = run(tool, "{\"target\":\"project\"}", context());
        assertFalse(project.toString(), project.containsKey("error"));

        assertTrue(run(tool, "{}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"target\":\"PLANET\"}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"target\":\"PROJECT\"}", emptyContext()).containsKey("error"));
    }

    @Test
    public void aProposalIsDraftedOnTheSession() throws Exception {
        ProposeCreationTool tool = new ProposeCreationTool();
        ToolContext context = context();

        Map<String, Object> drafted = run(tool, "{\"target\":\"PROJECT\",\"fields\":{\"name\":\"Vitis\","
                + "\"start_date\":\"2024-01-01\"},\"rationale\":\"from the cartouche\"}", context);

        assertFalse(drafted.toString(), drafted.containsKey("error"));
        assertNotNull("the draft awaits the user's confirmation", session.getPendingProposal());
        assertEquals("Vitis", session.getPendingProposal().getFields().get("name"));
    }

    @Test
    public void aProposalIsRefusedWithoutATargetOrAConversation() throws Exception {
        ProposeCreationTool tool = new ProposeCreationTool();

        assertTrue(run(tool, "{}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"target\":\"PLANET\"}", context()).containsKey("error"));
        assertTrue(run(tool, "{\"target\":\"PROJECT\"}", emptyContext()).containsKey("error"));
    }

    //#endregion

    //#region shared resources

    @Test
    public void withoutASharedInstanceTheSearchSaysSo() throws Exception {
        SearchSharedResourceVariablesTool tool = new SearchSharedResourceVariablesTool();

        Map<String, Object> answer = run(tool, "{\"name\":\"plant height\"}", context());
        assertEquals(0, answer.get("count"));
        assertTrue(answer.containsKey("note"));
        assertTrue(run(tool, "{}", context()).containsKey("error"));
    }

    //#endregion
}
