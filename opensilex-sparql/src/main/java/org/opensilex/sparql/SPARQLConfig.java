/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

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
    public String baseURI();

    @ConfigDescription(
            value = "Platform base URI alias",
            defaultString = "local"
    )
    public String baseURIAlias();

    @ConfigDescription(
            value = "SPARQL data source"
    )
    public SPARQLServiceFactory sparql();

    @ConfigDescription(
            value = "Enable URI prefixes",
            defaultBoolean = true
    )
    public boolean usePrefixes();

    @ConfigDescription(
            value = "Enable SHACL usage",
            defaultBoolean = false
    )
    public boolean enableSHACL();
}
