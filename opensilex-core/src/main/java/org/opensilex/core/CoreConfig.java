/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core;

import org.opensilex.config.ConfigDescription;

/**
 *
 * @author charlero
 */
public interface CoreConfig {

    @ConfigDescription(
            value = "Activate access logs by user",
            defaultBoolean = false
    )
    boolean enableLogs();


    /**
     * @see org.opensilex.core.ontology.dal.cache.OntologyCache
     * @see org.opensilex.core.ontology.api.OntologyAPI
     */
    @ConfigDescription(
            value = "Indicate if the module must put in cache classes and properties"
    )
    boolean enableOntologyCaching();

}
