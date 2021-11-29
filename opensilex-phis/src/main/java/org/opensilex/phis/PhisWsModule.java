//******************************************************************************
//                           PhisWsModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.phis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.riot.Lang;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.CoreModule;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phis opensilex module implementation
 */
public class PhisWsModule extends OpenSilexModule implements APIExtension, SPARQLExtension {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(PhisWsModule.class);

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso-ext#",
                "ontologies/oeso-ext.owl",
                Lang.RDFXML,
                "oeso"
        ));
        return list;
    }
    
    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Insert agrovoc species");
        insertDefaultSpecies();
    }

    private void insertDefaultSpecies() throws URISyntaxException, OpenSilexModuleNotFoundException, IOException, SPARQLException {
        try {

            SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            SPARQLService sparql = factory.provide();

            SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
            String graph = sparql.getDefaultGraphURI(GermplasmModel.class).toString();
            OntologyFileDefinition ontologyDef = new OntologyFileDefinition(
                    graph,
                    "ontologies/species.ttl",
                    Lang.TTL,
                    null
            );

            InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), ontologyDef.getFilePath()));
            sparql.loadOntology(ontologyDef.getUri(), ontologyStream, ontologyDef.getFileType());
            ontologyStream.close();
        
        } catch (Exception ex) {
            LOGGER.error("Error while insering default species", ex);
        } 
        
    }
}
