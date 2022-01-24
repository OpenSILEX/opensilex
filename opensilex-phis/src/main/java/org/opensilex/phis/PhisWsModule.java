//******************************************************************************
//                           PhisWsModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.phis;

import org.apache.jena.riot.Lang;
import org.opensilex.OpenSilexModule;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.phis.ontology.OesoExt;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Phis opensilex module implementation
 */
public class PhisWsModule extends OpenSilexModule implements APIExtension, SPARQLExtension {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PhisWsModule.class);

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = new LinkedList<>();
        list.add(new OntologyFileDefinition(
                OesoExt.NS,
                SPARQLModule.ONTOLOGIES_DIRECTORY+"/oeso-ext.owl",
                Lang.RDFXML,
                OesoExt.PREFIX
        ));
        return list;
    }
    
    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Insert agrovoc species");
        insertDefaultSpecies();
    }

    private void insertDefaultSpecies() {
        try {

            SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            SPARQLService sparql = factory.provide();

            String graph = sparql.getDefaultGraphURI(GermplasmModel.class).toString();
            OntologyFileDefinition ontologyDef = new OntologyFileDefinition(
                    graph,
                    SPARQLModule.ONTOLOGIES_DIRECTORY+"/species.ttl",
                    Lang.TTL,
                    null
            );

            // try/with resource -> auto close stream (even in case of error)
            try(InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), ontologyDef.getFilePath()))){
                sparql.loadOntology(ontologyDef.getGraphURI(), ontologyStream, ontologyDef.getFileType());
            }

        } catch (Exception ex) {
            LOGGER.error("Error while inserting default species", ex);
        } 
        
    }
}
