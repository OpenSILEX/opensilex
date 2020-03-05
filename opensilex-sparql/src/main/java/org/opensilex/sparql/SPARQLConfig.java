/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;
import org.opensilex.sparql.service.SPARQLServiceFactory;

/**
 *
 * @author vince
 */
public interface SPARQLConfig extends ModuleConfig {

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
            value = "Diasable URI prefixes",
            defaultBoolean = true
    )
    public boolean usePrefixes();
}
