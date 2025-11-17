//******************************************************************************
//                           SiduriModule.java
//******************************************************************************
package org.opensilex.siduri;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.riot.Lang;

import org.opensilex.OpenSilex;
import org.opensilex.core.variable.dal.EntityModel;
// import org.opensilex.core.variable.dal.ResearchProductFamilyModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.germplasm.dal.GermplasmModel;
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


public class SiduriModule extends OpenSilexModule implements SPARQLExtension {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(SiduriModule.class);

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/ofs#",
                "ontologies/siduri.owl",
                Lang.RDFXML,
                "ofs"
        ));

        return list;
    }



    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Insert agrovoc species");
        insertDefaultSpecies();
        insertDefaultEntity();
        insertDefaultUnits();
        // insertDefaultResearchProductFamily();
    }

    // private void insertDefaultResearchProductFamily() throws URISyntaxException, OpenSilexModuleNotFoundException, IOException, SPARQLException {
    //     try {

    //         SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
    //         SPARQLService sparql = factory.provide();

    //         SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
    //         String graph = sparql.getDefaultGraphURI(ResearchProductFamilyModel.class).toString();
    //         OntologyFileDefinition ontologyDef = new OntologyFileDefinition(
    //                 graph,
    //                 "ontologies/researchProductFamily.ttl",
    //                 Lang.TTL,
    //                 null
    //         );

    //         InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), ontologyDef.getFilePath()));
    //         sparql.loadOntology(ontologyDef.getUri(), ontologyStream, ontologyDef.getFileType());
    //         ontologyStream.close();

    //     } catch (Exception ex) {
    //         LOGGER.error("Error while insering default species", ex);
    //     }

    // }

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

    private void insertDefaultEntity() throws URISyntaxException, OpenSilexModuleNotFoundException, IOException, SPARQLException {
        try {

            SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            SPARQLService sparql = factory.provide();

            SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
            String graph = sparql.getDefaultGraphURI(EntityModel.class).toString();
            OntologyFileDefinition ontologyDef = new OntologyFileDefinition(
                    graph,
                    "ontologies/entity.ttl",
                    Lang.TTL,
                    null
            );

            InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), ontologyDef.getFilePath()));
            sparql.loadOntology(ontologyDef.getUri(), ontologyStream, ontologyDef.getFileType());
            ontologyStream.close();

        } catch (Exception ex) {
            LOGGER.error("Error while insering default entity", ex);
        }

    }

    private void insertDefaultUnits() throws URISyntaxException, OpenSilexModuleNotFoundException, IOException, SPARQLException {
        try {

            SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            SPARQLService sparql = factory.provide();

            SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
            String graph = sparql.getDefaultGraphURI(UnitModel.class).toString();
            OntologyFileDefinition ontologyDef = new OntologyFileDefinition(
                    graph,
                    "ontologies/units.ttl",
                    Lang.TTL,
                    null
            );

            InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), ontologyDef.getFilePath()));
            sparql.loadOntology(ontologyDef.getUri(), ontologyStream, ontologyDef.getFileType());
            ontologyStream.close();

        } catch (Exception ex) {
            LOGGER.error("Error while insering default units", ex);
        }

    }
}
