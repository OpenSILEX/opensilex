//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;


/**
 * RDF4J configuration interface
 */
public interface RDF4JConfig extends ServiceConfig {

    /**
     * RDF4J Server URI
     *
     * @return URI
     */
    @ConfigDescription(
            value = "RDF4J Server URI",
            defaultString = "http://localhost:8080/rdf4j-server/"
    )
    String serverURI();

    /**
     * RDF4J Server URI
     *
     * @return URI
     */
    @ConfigDescription(
            value = "RDF4J repository name",
            defaultString = "opensilex"
    )
    String repository();

}
