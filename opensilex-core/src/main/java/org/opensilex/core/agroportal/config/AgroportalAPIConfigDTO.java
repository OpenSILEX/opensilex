package org.opensilex.core.agroportal.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.AgroportalAPIConfig;

/**
 * Describes the AgroPortal configuration
 *
 * @author brice
 */
public class AgroportalAPIConfigDTO {

    @JsonProperty("path")
    protected String serverPath;

    @JsonProperty("apiUrl")
    protected String apiUrl;

    @JsonProperty("apiKey")
    protected String apiKey;

    public static AgroportalAPIConfigDTO fromConfig(AgroportalAPIConfig config) {
        return new AgroportalAPIConfigDTO()
                .setServerPath(config.basePath())
                .setApiUrl(config.baseAPIPath())
                .setApiKey(config.externalAPIKey());
    }

    public String getServerPath() {
        return serverPath;
    }

    public AgroportalAPIConfigDTO setServerPath(String serverPath) {
        this.serverPath = serverPath;
        return this;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public AgroportalAPIConfigDTO setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public AgroportalAPIConfigDTO setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

}
