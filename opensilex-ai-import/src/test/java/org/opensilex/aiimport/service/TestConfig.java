//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.config.LlmConfig;

/**
 * Builds a module configuration in a test.
 * <p>
 * The real configuration is an interface proxied from YAML by the framework, so a test just
 * implements it.
 *
 * @author Arnaud Charleroy
 */
public class TestConfig implements AiImportConfig, LlmConfig {

    private boolean enabled = true;
    private String baseUrl = "";
    private String model = "stub-model";
    private String apiKey = "";
    private int maxTokens = 256;
    private double temperature = 0.2d;
    private int timeoutMs = 5000;
    private int maxToolIterations = 3;
    private int maxFileSizeMb = 20;
    private int sampleRowsPerSheet = 5;
    private int sessionTtlMinutes = 60;
    private boolean searchSharedResourceInstances = false;

    public static TestConfig pointingAt(String baseUrl) {
        TestConfig config = new TestConfig();
        config.baseUrl = baseUrl;
        return config;
    }

    public TestConfig withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public TestConfig withMaxToolIterations(int maxToolIterations) {
        this.maxToolIterations = maxToolIterations;
        return this;
    }

    public TestConfig withModel(String model) {
        this.model = model;
        return this;
    }

    public TestConfig withSessionTtlMinutes(int sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
        return this;
    }

    //#region AiImportConfig

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public LlmConfig llm() {
        return this;
    }

    @Override
    public int maxFileSizeMb() {
        return maxFileSizeMb;
    }

    @Override
    public int sampleRowsPerSheet() {
        return sampleRowsPerSheet;
    }

    @Override
    public int sessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    @Override
    public boolean searchSharedResourceInstances() {
        return searchSharedResourceInstances;
    }

    //#endregion

    //#region LlmConfig

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String apiKey() {
        return apiKey;
    }

    @Override
    public int maxTokens() {
        return maxTokens;
    }

    @Override
    public double temperature() {
        return temperature;
    }

    @Override
    public int timeoutMs() {
        return timeoutMs;
    }

    @Override
    public int maxToolIterations() {
        return maxToolIterations;
    }

    //#endregion
}
