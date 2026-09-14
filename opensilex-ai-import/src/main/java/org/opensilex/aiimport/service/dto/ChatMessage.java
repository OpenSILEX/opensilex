//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One message of an OpenAI-compatible chat completion exchange.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TOOL = "tool";

    private String role;
    private String content;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    /**
     * Set on a {@link #ROLE_TOOL} message, echoing the identifier of the call it answers.
     */
    @JsonProperty("tool_call_id")
    private String toolCallId;

    /**
     * Set on a {@link #ROLE_TOOL} message: the name of the tool that produced the content. Some
     * gateways require it, others ignore it.
     */
    private String name;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ROLE_SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ROLE_USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ROLE_ASSISTANT, content);
    }

    public static ChatMessage toolResult(String toolCallId, String toolName, String content) {
        ChatMessage message = new ChatMessage(ROLE_TOOL, content);
        message.toolCallId = toolCallId;
        message.name = toolName;
        return message;
    }

    public String getRole() {
        return role;
    }

    public ChatMessage setRole(String role) {
        this.role = role;
        return this;
    }

    public String getContent() {
        return content;
    }

    public ChatMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public ChatMessage setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
        return this;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public ChatMessage setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
        return this;
    }

    public String getName() {
        return name;
    }

    public ChatMessage setName(String name) {
        this.name = name;
        return this;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
