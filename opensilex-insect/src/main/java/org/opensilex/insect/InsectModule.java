//******************************************************************************
//                           InsectModule.java
// OpenSILEX
// Copyright © INRA 2023
// Creation date: 12 sep. 2023
// Contact: bernhard.gschloessl@inrae.fr, catherine.roussey@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// ******************************************************************************
package org.opensilex.insect;

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
import org.opensilex.insect.ontology.OesoInsect;
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
 * Insect opensilex module implementation
 */
public class InsectModule extends OpenSilexModule implements APIExtension, SPARQLExtension {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(InsectModule.class);

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                OesoInsect.NS,
                "ontologies/oeso-insect.owl",
                Lang.RDFXML,
                OesoInsect.PREFIX,
                null,
                true
        ));
        return list;
    }
    
    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Insert agrovoc species");
        insertInsectDefaultSpecies();
    }

    private void insertInsectDefaultSpecies()  {
        try {

            SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            SPARQLService sparql = factory.provide();

            SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
            String graph = sparql.getDefaultGraphURI(GermplasmModel.class).toString();
            OntologyFileDefinition ontologyDef = new OntologyFileDefinition(
                    graph,
                    "ontologies/insect_species.ttl",
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
