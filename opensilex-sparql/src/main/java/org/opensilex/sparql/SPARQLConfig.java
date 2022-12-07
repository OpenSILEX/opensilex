package org.opensilex.sparql;

import java.util.Map;
import org.opensilex.config.ConfigDescription;
import org.opensilex.sparql.service.SPARQLServiceFactory;

/**
 *
 * @author vince
 */
public interface SPARQLConfig {

    @ConfigDescription(
            value = "Platform base URI",
            defaultString = "http://installation.domain.org/"
    )
    String baseURI();

    @ConfigDescription(
            value = "Platform base URI alias",
            defaultString = "local"
    )
    String baseURIAlias();

    @ConfigDescription(
            value = "Base domain for auto-generated URI (default to baseURI if not specified)",
            defaultString = ""
    )
    String generationBaseURI();
    
        @ConfigDescription(
            value = "Base domain for auto-generated URI (default to baseURI if not specified)",
            defaultString = "id"
    )
    String generationBaseURIAlias();
    
    @ConfigDescription(
            value = "SPARQL data source"
    )
    SPARQLServiceFactory sparql();

    @ConfigDescription(
            value = "Enable URI prefixes",
            defaultBoolean = true
    )
    boolean usePrefixes();

    @ConfigDescription(
            value = "Custom URI prefixes"
    )
    Map<String, String> customPrefixes();

    @ConfigDescription(
            value = "Enable SHACL usage (Experimental)",
            defaultBoolean = false
    )
    boolean enableSHACL();

    @ConfigDescription(
            value = "Enable optimized storage in RAM of ontologies classes, properties and restrictions",
            defaultBoolean = true
    )
    boolean enableOntologyStore();

    @ConfigDescription(
            value = "Number of line processed by batch during CSV import (must be strictly positive)." +
                    "Small value lead to less RAM usage at the cost of additional I/O during validation",
            defaultInt = 4096
    )
    int csvBatchSize();

    @ConfigDescription(
            value = "Maximum number of invalid line to encounter until CSV import interruption (must be strictly positive)",
            defaultInt = 100
    )
    int csvMaxErrorNb();
}
