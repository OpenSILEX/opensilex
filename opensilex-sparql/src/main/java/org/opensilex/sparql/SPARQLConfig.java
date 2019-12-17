/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.OpenSilex;
import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;

/**
 *
 * @author vince
 */
public interface SPARQLConfig extends ModuleConfig {

    @ConfigDescription(
            value = "Platform base URI",
            defaultString = OpenSilex.BASE_URI
    )
    public String baseURI();

    @ConfigDescription(
            value = "Platform base URI alias",
            defaultString = OpenSilex.BASE_URI_ALIAS
    )
    public String baseURIAlias();

    @ConfigDescription(
            value = "SPARQL data source"
    )
    public SPARQLService sparql();
}
