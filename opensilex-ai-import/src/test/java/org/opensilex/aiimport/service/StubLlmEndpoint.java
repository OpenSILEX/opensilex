//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A chat completion endpoint that answers with canned replies, so the connector can be tested
 * without a language model.
 * <p>
 * Built on the JDK's own HTTP server rather than a test framework: this module already carries one
 * new dependency, and it does not need another.
 *
 * @author Arnaud Charleroy
 */
public class StubLlmEndpoint implements AutoCloseable {

    private final HttpServer server;

    /**
     * Replies handed out in order, one per request.
     */
    private final List<String> replies = new ArrayList<>();

    /**
     * Bodies received, so a test can assert what the connector actually sent.
     */
    private final List<String> requestBodies = new ArrayList<>();

    private final List<String> authorizationHeaders = new ArrayList<>();

    private int status = 200;

    public StubLlmEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBodies.add(read(exchange.getRequestBody()));
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));

            String body = replies.isEmpty()
                    ? assistantReply("no reply was queued")
                    : replies.remove(0);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    public StubLlmEndpoint queue(String responseBody) {
        replies.add(responseBody);
        return this;
    }

    public StubLlmEndpoint respondWithStatus(int status) {
        this.status = status;
        return this;
    }

    public List<String> getRequestBodies() {
        return requestBodies;
    }

    public List<String> getAuthorizationHeaders() {
        return authorizationHeaders;
    }

    public int getRequestCount() {
        return requestBodies.size();
    }

    //#region canned bodies

    public static String assistantReply(String content) {
        return "{\"model\":\"stub\",\"choices\":[{\"index\":0,\"finish_reason\":\"stop\","
                + "\"message\":{\"role\":\"assistant\",\"content\":\"" + escape(content) + "\"}}]}";
    }

    /**
     * A reply asking for one tool call, in the OpenAI-compatible shape where the arguments arrive
     * as a JSON string rather than an object.
     */
    public static String toolCallReply(String callId, String toolName, String argumentsJson) {
        return "{\"model\":\"stub\",\"choices\":[{\"index\":0,\"finish_reason\":\"tool_calls\","
                + "\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + "{\"id\":\"" + callId + "\",\"type\":\"function\",\"function\":{\"name\":\""
                + toolName + "\",\"arguments\":\"" + escape(argumentsJson) + "\"}}]}}]}";
    }

    public static String noChoicesReply() {
        return "{\"model\":\"stub\",\"choices\":[]}";
    }

    /**
     * A reply that omits the role, which some gateways do.
     */
    public static String replyWithoutRole(String content) {
        return "{\"model\":\"stub\",\"choices\":[{\"index\":0,"
                + "\"message\":{\"content\":\"" + escape(content) + "\"}}]}";
    }

    //#endregion

    @Override
    public void close() {
        server.stop(0);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = input.read(chunk)) > 0) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }
}
