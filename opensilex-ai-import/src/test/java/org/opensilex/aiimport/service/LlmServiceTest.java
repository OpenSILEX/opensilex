//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.aiimport.service.dto.ToolDefinition;
import org.opensilex.server.exceptions.displayable.DisplayableServiceUnavailableException;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Arnaud Charleroy
 */
public class LlmServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StubLlmEndpoint endpoint;

    @Before
    public void startTheEndpoint() throws Exception {
        endpoint = new StubLlmEndpoint();
    }

    @After
    public void stopTheEndpoint() {
        if (endpoint != null) {
            endpoint.close();
        }
    }

    @Test
    public void anUnconfiguredEndpointDisablesTheService() {
        LlmService service = new LlmService(TestConfig.pointingAt(""));
        assertFalse(service.isEnable());

        try {
            service.complete(Collections.singletonList(ChatMessage.user("hello")), null);
            fail("an unconfigured service must refuse rather than call nowhere");
        } catch (DisplayableServiceUnavailableException expected) {
            assertTrue(expected.getMessage().contains("No chat completion endpoint is configured"));
        }
        assertEquals("nothing must be sent", 0, endpoint.getRequestCount());
    }

    @Test
    public void aMissingModelAlsoDisablesTheService() {
        assertFalse(new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()).withModel(""))
                .isEnable());
    }

    @Test
    public void theRequestCarriesTheModelTheMessagesAndTheTools() throws Exception {
        endpoint.queue(StubLlmEndpoint.assistantReply("understood"));

        LlmService service = new LlmService(
                TestConfig.pointingAt(endpoint.getBaseUrl()).withModel("my-model"));
        ChatMessage reply = service.complete(
                List.of(ChatMessage.system("be brief"), ChatMessage.user("what is in the file?")),
                List.of(aTool()));

        assertEquals("understood", reply.getContent());
        assertEquals(ChatMessage.ROLE_ASSISTANT, reply.getRole());

        JsonNode sent = MAPPER.readTree(endpoint.getRequestBodies().get(0));
        assertEquals("my-model", sent.get("model").asText());
        assertEquals(2, sent.get("messages").size());
        assertEquals("system", sent.get("messages").get(0).get("role").asText());
        assertEquals("what is in the file?", sent.get("messages").get(1).get("content").asText());
        assertEquals(1, sent.get("tools").size());
        assertEquals("search_variables",
                sent.get("tools").get(0).get("function").get("name").asText());
        assertFalse("streaming is not used", sent.get("stream").asBoolean());
        assertEquals(256, sent.get("max_tokens").asInt());
    }

    @Test
    public void anEmptyApiKeyIsNotSentAsAHeader() throws Exception {
        endpoint.queue(StubLlmEndpoint.assistantReply("ok"));

        new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()))
                .complete(List.of(ChatMessage.user("hi")), null);

        assertNull("a local endpoint must not receive an empty bearer token",
                endpoint.getAuthorizationHeaders().get(0));
    }

    @Test
    public void anApiKeyIsSentAsABearerToken() throws Exception {
        endpoint.queue(StubLlmEndpoint.assistantReply("ok"));

        new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()).withApiKey("sk-secret"))
                .complete(List.of(ChatMessage.user("hi")), null);

        assertEquals("Bearer sk-secret", endpoint.getAuthorizationHeaders().get(0));
    }

    @Test
    public void aToolCallReplyIsParsed() throws Exception {
        endpoint.queue(StubLlmEndpoint.toolCallReply("call_1", "search_variables",
                "{\"name\":\"Bai_Suc_g\"}"));

        ChatMessage reply = new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()))
                .complete(List.of(ChatMessage.user("does it exist?")), List.of(aTool()));

        assertTrue(reply.hasToolCalls());
        assertEquals(1, reply.getToolCalls().size());
        assertEquals("call_1", reply.getToolCalls().get(0).getId());
        assertEquals("search_variables", reply.getToolCalls().get(0).getFunctionName());
        assertEquals("{\"name\":\"Bai_Suc_g\"}",
                reply.getToolCalls().get(0).getFunctionArguments());
    }

    @Test
    public void aReplyWithoutARoleIsStillAnAssistantMessage() throws Exception {
        endpoint.queue(StubLlmEndpoint.replyWithoutRole("some gateways omit the role"));

        ChatMessage reply = new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()))
                .complete(List.of(ChatMessage.user("hi")), null);

        assertEquals(ChatMessage.ROLE_ASSISTANT, reply.getRole());
        assertEquals("some gateways omit the role", reply.getContent());
    }

    @Test
    public void anErrorStatusIsReportedAsUnavailable() {
        endpoint.respondWithStatus(500).queue("{\"error\":\"boom\"}");

        try {
            new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()))
                    .complete(List.of(ChatMessage.user("hi")), null);
            fail("a failing endpoint must not look like a successful answer");
        } catch (DisplayableServiceUnavailableException expected) {
            assertTrue(expected.getMessage().contains("500"));
        }
    }

    @Test
    public void aReplyWithoutAnyChoiceIsReportedAsUnavailable() {
        endpoint.queue(StubLlmEndpoint.noChoicesReply());

        try {
            new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl()))
                    .complete(List.of(ChatMessage.user("hi")), null);
            fail("an empty choice list is not an answer");
        } catch (DisplayableServiceUnavailableException expected) {
            assertTrue(expected.getMessage().contains("no choice"));
        }
    }

    @Test
    public void anUnreachableEndpointIsReportedAsUnavailable() {
        // Port 1 is reserved and nothing listens there.
        try {
            new LlmService(TestConfig.pointingAt("http://127.0.0.1:1/v1"))
                    .complete(List.of(ChatMessage.user("hi")), null);
            fail("an unreachable endpoint must be reported, not swallowed");
        } catch (DisplayableServiceUnavailableException expected) {
            assertTrue(expected.getMessage().contains("Could not reach"));
        }
    }

    //#region the status probe

    @Test
    public void anAnsweringEndpointIsReachableAndCostsNoCompletion() {
        assertTrue(new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl())).isReachable());
        assertEquals("the probe must not send a message", 0, endpoint.getRequestCount());
    }

    @Test
    public void aRefusedModelListIsNotReachable() {
        endpoint.respondToModelsWithStatus(401);

        assertFalse("a refused key would refuse the first question too",
                new LlmService(TestConfig.pointingAt(endpoint.getBaseUrl())).isReachable());
    }

    @Test
    public void anEndpointNobodyListensOnIsNotReachable() {
        assertFalse(new LlmService(TestConfig.pointingAt("http://127.0.0.1:1/v1")).isReachable());
    }

    @Test
    public void anUnconfiguredEndpointIsNotProbed() {
        assertFalse(new LlmService(TestConfig.pointingAt("")).isReachable());
    }

    //#endregion

    @Test
    public void theToolIterationLimitComesFromTheConfiguration() {
        assertEquals(4, new LlmService(
                TestConfig.pointingAt(endpoint.getBaseUrl()).withMaxToolIterations(4))
                .getMaxToolIterations());
    }

    private ToolDefinition aTool() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("name").put("type", "string");
        return ToolDefinition.of("search_variables", "Search variables", schema);
    }
}
