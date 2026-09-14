//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.config;

import org.opensilex.config.ConfigDescription;

/**
 * Connection settings for an OpenAI-compatible chat completion endpoint. Works with a locally
 * hosted gateway (vLLM, Ollama, LiteLLM) as well as with a hosted provider.
 *
 * @author Arnaud Charleroy
 */
public interface LlmConfig {

    @ConfigDescription(
            value = "Base URL of the OpenAI-compatible API, without a trailing slash, e.g. http://localhost:11434/v1",
            defaultString = ""
    )
    String baseUrl();

    @ConfigDescription(
            value = "Model identifier passed in the completion request",
            defaultString = ""
    )
    String model();

    @ConfigDescription(
            value = "Bearer token sent in the Authorization header. Leave empty for an unauthenticated local endpoint",
            defaultString = ""
    )
    String apiKey();

    @ConfigDescription(
            value = "Maximum number of tokens the model may generate per reply",
            defaultInt = 2048
    )
    int maxTokens();

    @ConfigDescription(
            value = "Sampling temperature. Keep it low so the assistant stays factual",
            defaultDouble = 0.2d
    )
    double temperature();

    @ConfigDescription(
            value = "Read and connect timeout, in milliseconds",
            defaultInt = 120000
    )
    int timeoutMs();

    @ConfigDescription(
            value = "Maximum number of tool-call round trips allowed for a single user message",
            defaultInt = 6
    )
    int maxToolIterations();
}
