package org.opensilex.dataverse;

import org.opensilex.config.ConfigDescription;

import java.util.List;

public interface OpensilexDataverseConfig {

    @ConfigDescription(
            value = "External dataverse API key",
            defaultString = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    )
    String externalAPIKey();

    @ConfigDescription(
            value = "Base path for the Recherche Data Gouv API",
            defaultString = "https://data-preproduction.inrae.fr"
    )
    String rechercheDataGouvBasePath();

    @ConfigDescription(
            value = "Alias of the dataverse to point to",
            defaultString = "opensilex-tests"
    )
    String dataverseAlias();

    @ConfigDescription(
            value = "Available languages for dataverse",
            defaultList = {"en", "fr"}
    )
    List<String> dataverseLanguages();

    @ConfigDescription(
            value = "Available languages for dataset metadata",
            defaultList = {"en", "fr"}
    )
    List<String> datasetMetadataLanguages();
}
