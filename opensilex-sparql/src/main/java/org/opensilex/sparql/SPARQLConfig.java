/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
}
