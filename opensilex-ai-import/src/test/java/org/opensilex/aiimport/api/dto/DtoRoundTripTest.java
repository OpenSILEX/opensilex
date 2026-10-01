//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.RequiredField;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.mapping.TypeIssue;
import org.opensilex.aiimport.mapping.ValueKind;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.resolve.LearnedCorrection;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedComponent;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.store.SavedSessionSummary;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * What the interface receives: every DTO written with the platform's own JSON mapper and read back.
 * <p>
 * The JSON names are the contract with the front end — snake_case, as everywhere in OpenSILEX — so
 * they are asserted by name, and reading the JSON back proves the DTOs can be sent as well as
 * received.
 *
 * @author Arnaud Charleroy
 */
public class DtoRoundTripTest {

    private static final ObjectMapper MAPPER = ObjectMapperContextResolver.getObjectMapper();
    private static final URI UNIT = URI.create("http://opensilex.test/id/unit/cm");

    private AiImportSession richSession() throws Exception {
        AiImportSession session = new AiImportSession("dto", URI.create("http://opensilex.test/id/account/a"));
        session.setFileName(WorkbookFixture.VITIS_FILE_NAME)
                .setWorkbook(WorkbookFixture.vitis())
                .setProfileId(VitisExplorerProfile.ID);

        ResolutionReport report = new ResolutionReport();
        ResolvedItem variable = new ResolvedItem("HAUT").setExternalId("CO_356:1000217")
                .setStatus(ResolutionStatus.MISSING)
                .setHint(ReportMessage.of("AiImport.report.hint.variableMissing", "missing"));
        ResolvedComponent unit = new ResolvedComponent("unit", "centimetre", "UO:0000015");
        unit.setUri(UNIT);
        ResolvedComponent method = new ResolvedComponent("method", "rulr", null);
        method.setSuggestion(new ResourceReference(URI.create("http://opensilex.test/id/method/ruler"), "ruler"));
        variable.getComponents().add(unit);
        variable.getComponents().add(method);
        variable.getSuggestions().add(new ResourceReference(URI.create("http://opensilex.test/id/v"), "HAUTEUR"));
        report.getVariables().add(variable);
        report.getExperiments().add(new ResolvedItem("CEPInnov").setStatus(ResolutionStatus.FOUND)
                .setConfirmedByUser(true)
                .setMatches(List.of(new ResourceReference(URI.create("http://opensilex.test/id/xp"), "CEPInnov"))));
        report.getFacilities().add(new ResolvedItem("Pcl Nord").setStatus(ResolutionStatus.FOUND)
                .setLearnedCorrection(new LearnedCorrection(URI.create("http://opensilex.test/set/c/1"),
                        ReportCategory.FACILITIES, "Pcl Nord", URI.create("http://opensilex.test/id/f"),
                        "Parcelle Nord", "Admin", OffsetDateTime.parse("2026-09-01T10:00:00Z")))
                .setMatches(List.of(new ResourceReference(URI.create("http://opensilex.test/id/f"), "Parcelle Nord"))));
        report.addWarning(ReportMessage.of("AiImport.report.warning.variableLookupFailed", "failed")
                .with("name", "HAUT"));
        session.setReport(report);

        session.setMappings(List.of(new ColumnMapping()
                .setColumn("HAUT")
                .addSheet("Stades")
                .setRole(ColumnRole.VARIABLE)
                .setResolvedUri(URI.create("http://opensilex.test/id/v"))
                .setResolvedName("HAUTEUR")
                .setResolutionStatus(ResolutionStatus.FOUND)
                .setExpectedDatatype("xsd:decimal")
                .setObservedKind(ValueKind.TEXT)
                .setValueCount(10)
                .setMissingCount(2)
                .setSampleValues(List.of("12", "tall"))
                .setSuggestion(ReportMessage.of("AiImport.mapping.suggestion", "fix the cells"))
                .setIssues(List.of(new TypeIssue()
                        .setSheet("Stades").setColumn("HAUT").setRowNumber(12).setValue("tall")
                        .setProblem(ReportMessage.of("AiImport.mapping.problem", "not a number"))
                        .setSuggestion(ReportMessage.of("AiImport.mapping.fix", "write a number"))))));

        session.getTranscript().add(AiImportMessage.user("Que manque-t-il ?"));
        session.getTranscript().add(AiImportMessage.assistant("Trois variables.").setLookups(List.of("get_report")));

        CreationProposal applied = new CreationProposal("p-1", CreationTarget.PROJECT)
                .put("name", "Vitis", CreationProposal.FieldSource.FILE)
                .setStatus(CreationProposal.Status.APPLIED);
        CreationProposal pending = new CreationProposal("p-2", CreationTarget.EXPERIMENT)
                .put("name", "Essai", CreationProposal.FieldSource.ASSISTANT)
                .setRationale("from the file");
        pending.getMissingRequired().add("objective");
        session.restoreProposal(applied);
        session.restoreProposal(pending);
        session.setPendingProposal(pending);
        return session;
    }

    private CreationRequirements requirements() {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.EXPERIMENT);
        RequiredField name = new RequiredField("name", "AiImport.proposal.field.name", "text", true);
        name.suggest("Essai", "AiImport.proposal.from.fileName");
        requirements.field(name);
        requirements.field(new RequiredField("project", "AiImport.proposal.field.project",
                RequiredField.KIND_URI, false).setResource(RequiredField.RESOURCE_PROJECT));
        requirements.block("AiImport.proposal.block.noDataPoints");
        return requirements;
    }

    @Test
    public void aSessionTravelsWithEverythingTheInterfaceDraws() throws Exception {
        AiImportSessionDTO dto = AiImportSessionDTO.fromModel(richSession(), 3, requirements());

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(dto));

        assertEquals("dto", json.get("session_id").asText());
        assertEquals(WorkbookFixture.VITIS_FILE_NAME, json.get("file_name").asText());
        JsonNode variable = json.get("report").get("variables").get(0);
        assertEquals("CO_356:1000217", variable.get("external_id").asText());
        assertEquals(2, variable.get("components").size());
        assertTrue(variable.get("components").toString().contains("suggested_uri"));
        assertFalse(variable.get("suggestions").isEmpty());
        assertTrue(json.get("report").get("experiments").get(0).get("confirmed_by_user").asBoolean());
        assertEquals("Admin", json.get("report").get("facilities").get(0).get("learned_correction")
                .get("author").asText());
        JsonNode issue = json.get("mapping").get(0).get("issues").get(0);
        assertEquals(12, issue.get("row_number").asInt());
        assertEquals(2, json.get("proposals").size());
        assertEquals("p-2", json.get("pending_proposal").get("id").asText());
        assertEquals(2, json.get("messages").size());

        AiImportSessionDTO back = MAPPER.readValue(MAPPER.writeValueAsString(dto), AiImportSessionDTO.class);
        assertEquals(dto.getSessionId(), back.getSessionId());
        assertEquals(dto.getReport().getVariables().get(0).getComponents().size(),
                back.getReport().getVariables().get(0).getComponents().size());
        assertEquals(dto.getProposals().size(), back.getProposals().size());
    }

    @Test
    public void theRequirementsTravelWithTheirSuggestionsAndBlockers() throws Exception {
        CreationRequirementsDTO dto = CreationRequirementsDTO.fromModel(requirements());

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(dto));
        assertEquals("EXPERIMENT", json.get("target").asText());
        assertFalse(json.get("is_available").asBoolean());
        assertEquals("Essai", json.get("fields").get(0).get("suggested_value").asText());
        assertEquals("project", json.get("fields").get(1).get("resource").asText());

        CreationRequirementsDTO back = MAPPER.readValue(MAPPER.writeValueAsString(dto), CreationRequirementsDTO.class);
        assertEquals(2, back.getFields().size());
    }

    @Test
    public void aStoredSessionTravelsWithItsExpiry() throws Exception {
        Instant updated = Instant.parse("2026-09-01T10:00:00Z");
        SavedSessionDTO dto = SavedSessionDTO.fromModel(new SavedSessionSummary("s-1", "file.xlsx",
                "vitis-explorer", updated, updated, 4), 30);

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(dto));
        assertEquals("s-1", json.get("session_id").asText());
        assertEquals(4, json.get("message_count").asInt());
        assertTrue(json.get("expires_at").asText().startsWith("2026-10-01"));

        SavedSessionDTO back = MAPPER.readValue(MAPPER.writeValueAsString(dto), SavedSessionDTO.class);
        assertEquals("file.xlsx", back.getFileName());
        assertEquals(dto.getExpiresAt(), back.getExpiresAt());
    }

    @Test
    public void aCreationResultTravelsWithItsBatches() throws Exception {
        CreationResultDTO dto = new CreationResultDTO().setTarget("DATA").setInsertedCount(12)
                .setBatches(List.of(URI.create("http://opensilex.test/id/batch/1")));

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(dto));
        assertEquals(12, json.get("inserted_count").asInt());
        assertEquals(1, json.get("batches").size());

        CreationResultDTO back = MAPPER.readValue(MAPPER.writeValueAsString(dto), CreationResultDTO.class);
        assertEquals(dto.getBatches(), back.getBatches());
    }
}
