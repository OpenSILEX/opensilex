package org.opensilex.olga;

import org.opensilex.config.ConfigDescription;

public interface OlgaConfig {

    @ConfigDescription(
            value = "Olga host base URL"
    )
    public String host();

    @ConfigDescription(
            value = "Olga API token"
    )
    public String token();
}