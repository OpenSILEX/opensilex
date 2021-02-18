//******************************************************************************
//                           PhisWsModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.phis;

import java.util.List;
import java.util.Set;
import org.apache.jena.riot.Lang;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;

/**
 * Phis opensilex module implementation
 */
public class PhisWsModule extends OpenSilexModule implements APIExtension, SPARQLExtension {

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso-phis#",
                "ontologies/oeso-phis.owl",
                Lang.RDFXML,
                "oeso"
        ));
//        list.add(new OntologyFileDefinition(
//                sparqlConfig.baseURI() + "germplasm",
//                "ontologies/species.ttl",
//                Lang.TTL,
//                sparqlConfig.baseURIAlias() + "-germplasm"
//        ));
        return list;
    }
}
