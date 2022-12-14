package org.opensilex.dataverse;

import org.opensilex.config.ConfigDescription;

public interface OpensilexDataverseConfig {

    @ConfigDescription(
            value = "External dataverse API key",
            defaultString = "3fe10dae-faeb-48fc-8f86-3f4b8e5318c0"
    )
    String externalAPIKey();

    @ConfigDescription(
            value = "Base path for the dataverse API",
            defaultString = "https://data-preproduction.inrae.fr/api/"
    )
    String dataverseBasePath();

    @ConfigDescription(
            value = "Alias of the dataverse to point to",
            defaultString = "opensilex-tests"
    )
    String dataverseAlias();
}
