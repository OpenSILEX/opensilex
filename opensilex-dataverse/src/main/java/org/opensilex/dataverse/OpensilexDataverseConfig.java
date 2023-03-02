package org.opensilex.dataverse;

import org.opensilex.config.ConfigDescription;

public interface OpensilexDataverseConfig {

    @ConfigDescription(
            value = "External dataverse API key",
            defaultString = "d8aea8b2-7c9e-474c-8f4d-22298b2d1107"
    )
    String externalAPIKey();

    @ConfigDescription(
            value = "Base path for the dataverse API",
            defaultString = "https://data-preproduction.inrae.fr"
    )
    String dataverseBasePath();

    @ConfigDescription(
            value = "Alias of the dataverse to point to",
            defaultString = "opensilex-tests"
    )
    String dataverseAlias();
}
