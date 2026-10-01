//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opensilex.aiimport.create.bulk.BulkOutcome;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.TokenUsage;
import org.opensilex.aiimport.service.dto.ChatResponse;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The small models the creation and the conversation pass around: what each one says, and the one
 * rule each one carries.
 *
 * @author Arnaud Charleroy
 */
public class CreationModelsTest {

    private static final URI BATCH = URI.create("http://opensilex.test/id/batch/1");

    @Test
    public void anOutcomeIsCreatedInsertedRefusedOrInterrupted() {
        URI project = URI.create("http://opensilex.test/id/project/p");
        assertEquals(project, CreationOutcome.created(project).getUri());
        assertEquals(12, CreationOutcome.inserted(12).getInsertedCount());

        RowError error = RowError.ofFile(ReportMessage.plain("wrong"));
        CreationOutcome refused = CreationOutcome.refused(3, List.of(error));
        assertTrue(refused.isRefused());
        assertEquals(3, refused.getRowsChecked());
        assertEquals(List.of(error), refused.getErrors());

        CreationOutcome interrupted = CreationOutcome.of(
                BulkOutcome.interrupted(20, 10, List.of(error), List.of(BATCH)));
        assertTrue("some rows written, the rest refused", interrupted.isRefused());
        assertEquals(10, interrupted.getInsertedCount());
        assertEquals(List.of(BATCH), interrupted.getBatches());
        assertNull(interrupted.getUri());
    }

    /**
     * A required checkbox is a decision: "false", what an unticked box submits, does not satisfy it.
     */
    @Test
    public void aRequiredFieldIsSatisfiedAccordingToItsKind() {
        RequiredField text = new RequiredField()
                .setName("name").setLabelKey("AiImport.proposal.field.name")
                .setKind(RequiredField.KIND_TEXT).setRequired(true)
                .setSuggestedValue("Essai").setSuggestedFrom("AiImport.proposal.from.fileName");
        assertTrue(text.isSatisfiedBy("Essai"));
        assertFalse(text.isSatisfiedBy("  "));
        assertEquals("Essai", text.getSuggestedValue());
        assertEquals("AiImport.proposal.from.fileName", text.getSuggestedFrom());
        assertEquals("name", text.getName());
        assertEquals("AiImport.proposal.field.name", text.getLabelKey());

        RequiredField checkbox = new RequiredField("confirmed", "label", RequiredField.KIND_BOOLEAN, true);
        assertFalse(checkbox.isSatisfiedBy("false"));
        assertTrue(checkbox.isSatisfiedBy("true"));

        RequiredField optional = new RequiredField("description", "label", RequiredField.KIND_LONG_TEXT, false);
        assertTrue(optional.isSatisfiedBy(null));
        assertFalse(optional.isRequired());
        assertNull(optional.getResource());
    }

    @Test
    public void aReferenceCarriesWhereItCameFromAndHowCloseItIs() {
        ResourceReference reference = new ResourceReference()
                .setUri(URI.create("http://phenome.test/id/variable/v"))
                .setName("plant_height")
                .setSharedResourceInstance("http://phenome.test/rest")
                .setSharedResourceInstanceLabel("PHENOME")
                .setSimilarity(0.92)
                .setDatatype("xsd:decimal");

        assertEquals("plant_height", reference.getName());
        assertEquals("PHENOME", reference.getSharedResourceInstanceLabel());
        assertEquals(0.92, reference.getSimilarity(), 0.0001);
        assertEquals("xsd:decimal", reference.getDatatype());
    }

    /**
     * The endpoint's usage is added up across the conversation; a reply without it still counts as
     * a call.
     */
    @Test
    public void theTokenUsageAddsUp() throws Exception {
        ChatResponse response = new ObjectMapper().readValue("{\"choices\":[],\"usage\":{\"prompt_tokens\":120,"
                + "\"completion_tokens\":30,\"total_tokens\":150}}", ChatResponse.class);
        assertEquals(150, response.getUsage().getTotalTokens());
        response.getUsage().setPromptTokens(100);
        response.getUsage().setCompletionTokens(20);

        TokenUsage usage = new TokenUsage();
        usage.add(response);
        usage.add(null);

        assertEquals(100, usage.getPromptTokens());
        assertEquals(20, usage.getCompletionTokens());
        assertEquals(120, usage.getTotalTokens());
        assertEquals(2, usage.getCalls());
        assertFalse(usage.toString().isEmpty());
    }
}
