//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Body of a chat completion request.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    private String model;
    private List<ChatMessage> messages;
    private List<ToolDefinition> tools;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Boolean stream = Boolean.FALSE;

    public String getModel() {
        return model;
    }

    public ChatRequest setModel(String model) {
        this.model = model;
        return this;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public ChatRequest setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        return this;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public ChatRequest setTools(List<ToolDefinition> tools) {
        this.tools = tools;
        return this;
    }

    public Double getTemperature() {
        return temperature;
    }

    public ChatRequest setTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public ChatRequest setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public Boolean getStream() {
        return stream;
    }

    public ChatRequest setStream(Boolean stream) {
        this.stream = stream;
        return this;
    }
}
