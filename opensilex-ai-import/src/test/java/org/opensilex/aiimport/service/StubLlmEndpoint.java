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
import java.util.Collections;
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
    private final List<String> replies = Collections.synchronizedList(new ArrayList<>());

    /**
     * Bodies received, so a test can assert what the connector actually sent.
     */
    private final List<String> requestBodies = Collections.synchronizedList(new ArrayList<>());

    private final List<String> authorizationHeaders = new ArrayList<>();

    private int status = 200;

    private int modelsStatus = 200;

    /**
     * The port the module's test configuration ({@code config/test/opensilex.yml}) points the
     * assistant at, for the tests that go through the REST API.
     */
    public static final int TEST_PORT = 28771;

    /**
     * On a free port, for the tests that build their own configuration.
     */
    public StubLlmEndpoint() throws IOException {
        this(0);
    }

    public StubLlmEndpoint(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBodies.add(read(exchange.getRequestBody()));
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));

            String body;
            synchronized (replies) {
                body = replies.isEmpty() ? assistantReply("no reply was queued") : replies.remove(0);
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        // The model list the status probe asks for. Not counted as a request: it sends no message.
        server.createContext("/v1/models", exchange -> {
            byte[] bytes = "{\"object\":\"list\",\"data\":[{\"id\":\"stub-model\",\"object\":\"model\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(modelsStatus, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    /**
     * Forgets the replies a previous test queued and did not use.
     */
    public StubLlmEndpoint reset() {
        replies.clear();
        requestBodies.clear();
        authorizationHeaders.clear();
        status = 200;
        modelsStatus = 200;
        return this;
    }

    public StubLlmEndpoint respondToModelsWithStatus(int status) {
        this.modelsStatus = status;
        return this;
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
