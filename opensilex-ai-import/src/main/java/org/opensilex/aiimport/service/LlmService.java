//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.config.LlmConfig;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.aiimport.service.dto.ChatRequest;
import org.opensilex.aiimport.service.dto.ChatResponse;
import org.opensilex.aiimport.service.dto.ToolDefinition;
import org.opensilex.server.exceptions.displayable.DisplayableServiceUnavailableException;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.service.reflection.SelfBound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Calls an OpenAI-compatible chat completion endpoint.
 * <p>
 * Deliberately a single connector rather than one per vendor: the {@code /chat/completions} shape
 * is what a locally hosted gateway speaks as well as a hosted provider, so an instance can keep the
 * uploaded file's metadata inside its own network by pointing {@code baseUrl} at an internal
 * endpoint.
 * <p>
 * Streaming is not used. The interface reports progress from the tool-call round trips instead,
 * which is what actually takes time here.
 *
 * @author Arnaud Charleroy
 */
@SelfBound
@Service
public class LlmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmService.class);

    private static final String UNAVAILABLE_TRANSLATION_KEY = "server.errors.ai-import-llm-unavailable";
    private static final String NOT_CONFIGURED_TRANSLATION_KEY = "server.errors.ai-import-llm-not-configured";

    private static final String COMPLETION_ENDPOINT = "chat/completions";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String AUTHORIZATION_HEADER_CONTENT_FORMAT = "Bearer %s";

    private final LlmConfig config;
    private final ObjectMapper mapper;

    @Inject
    public LlmService(AiImportConfig moduleConfig) {
        this.config = moduleConfig.llm();
        this.mapper = ObjectMapperContextResolver.getObjectMapper();
    }

    /**
     * @return true when an endpoint and a model are configured. When false, the module reports
     * itself as unavailable rather than failing at the first request.
     */
    public boolean isEnable() {
        return config != null && StringUtils.isNoneEmpty(config.baseUrl(), config.model());
    }

    public int getMaxToolIterations() {
        return config.maxToolIterations();
    }

    /**
     * Sends one completion request.
     *
     * @param messages the full conversation, system message included
     * @param tools    the tools the model may call, or an empty list
     * @return the assistant message, which may ask for tool calls instead of answering
     */
    public ChatMessage complete(List<ChatMessage> messages, List<ToolDefinition> tools)
            throws DisplayableServiceUnavailableException {
        return complete(messages, tools, null);
    }

    /**
     * @param usageListener notified with each response so a caller can accumulate what the call
     *                      actually cost. The endpoint's own figures, not an estimate.
     */
    public ChatMessage complete(List<ChatMessage> messages, List<ToolDefinition> tools,
                                Consumer<ChatResponse> usageListener)
            throws DisplayableServiceUnavailableException {
        throwIfNotEnable();

        ChatRequest request = new ChatRequest()
                .setModel(config.model())
                .setMessages(messages)
                .setTemperature(config.temperature())
                .setMaxTokens(config.maxTokens());
        if (tools != null && !tools.isEmpty()) {
            request.setTools(tools);
        }

        Client client = newClient();
        try {
            var target = client.target(config.baseUrl()).path(COMPLETION_ENDPOINT);
            var builder = target.request(MediaType.APPLICATION_JSON_TYPE);
            if (StringUtils.isNotEmpty(config.apiKey())) {
                builder = builder.header(AUTHORIZATION_HEADER_NAME,
                        String.format(AUTHORIZATION_HEADER_CONTENT_FORMAT, config.apiKey()));
            }

            try (Response response = builder.post(Entity.json(request))) {
                JsonNode body = response.readEntity(JsonNode.class);
                if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                    LOGGER.error("Chat completion at {} failed with status {}. Response:\n{}",
                            target.getUri(), response.getStatus(),
                            body == null ? "<empty>" : body.toPrettyString());
                    throw unavailable("Chat completion failed with status " + response.getStatus());
                }

                ChatResponse parsed = mapper.convertValue(body, ChatResponse.class);
                ChatMessage message = parsed.firstMessage();
                if (message == null) {
                    LOGGER.error("Chat completion at {} returned no choice. Response:\n{}",
                            target.getUri(), body == null ? "<empty>" : body.toPrettyString());
                    throw unavailable("Chat completion returned no choice");
                }
                logUsage(parsed);
                if (usageListener != null) {
                    usageListener.accept(parsed);
                }
                // Some gateways omit the role on the reply; downstream code relies on it.
                if (StringUtils.isEmpty(message.getRole())) {
                    message.setRole(ChatMessage.ROLE_ASSISTANT);
                }
                return message;
            }
        } catch (DisplayableServiceUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error("Could not reach the chat completion endpoint {}", config.baseUrl(), e);
            throw unavailable("Could not reach " + config.baseUrl());
        } finally {
            client.close();
        }
    }

    private Client newClient() {
        return ClientBuilder.newBuilder()
                .connectTimeout(config.timeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.timeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    private void logUsage(ChatResponse response) {
        if (!LOGGER.isDebugEnabled() || response.getUsage() == null) {
            return;
        }
        LOGGER.debug("Chat completion used {} prompt and {} completion tokens",
                response.getUsage().getPromptTokens(), response.getUsage().getCompletionTokens());
    }

    private void throwIfNotEnable() throws DisplayableServiceUnavailableException {
        if (!isEnable()) {
            throw new DisplayableServiceUnavailableException(
                    "No chat completion endpoint is configured for the ai-import module",
                    NOT_CONFIGURED_TRANSLATION_KEY,
                    Collections.emptyMap());
        }
    }

    private DisplayableServiceUnavailableException unavailable(String message) {
        return new DisplayableServiceUnavailableException(message, UNAVAILABLE_TRANSLATION_KEY,
                new HashMap<String, String>() {{
                    put("url", String.valueOf(config.baseUrl()));
                }});
    }
}
