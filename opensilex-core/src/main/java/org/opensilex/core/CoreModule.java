//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
import java.io.FileInputStream;
import java.io.InputStream;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.rest.extensions.APIExtension;
import org.opensilex.rest.extensions.LoginExtension;
import org.opensilex.rest.group.dal.GroupDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import java.net.URI;
import java.util.List;
import org.apache.jena.riot.Lang;
import org.opensilex.rest.RestModule;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, LoginExtension {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);

    public static final String TOKEN_USER_GROUP_URIS = "user_group_uris";

    @Override
    public void login(UserModel user, JWTCreator.Builder tokenBuilder) throws Exception {

        // TODO add experiments, projects, infrastructures related to the user as token claims...
        SPARQLServiceFactory sparqlServiceFactory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = sparqlServiceFactory.provide();
        try {
            GroupDAO groupDAO = new GroupDAO(sparql);

            List<URI> groupUris = groupDAO.getGroupUriList(user);
            if (groupUris.isEmpty()) {
                tokenBuilder.withArrayClaim(TOKEN_USER_GROUP_URIS, new String[0]);
            } else {
                String[] groupArray = new String[groupUris.size()];
                int index = 0;
                for (URI groupUri : groupUris) {
                    groupArray[index] = groupUri.toString();
                    index++;
                }
                tokenBuilder.withArrayClaim(TOKEN_USER_GROUP_URIS, groupArray);
            }

        } finally {
            sparqlServiceFactory.dispose(sparql);
        }
    }

    @Override
    public void check() throws Exception {
        LOGGER.info("Check ontologies initialization");
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        URI uri = new URI("http://www.w3.org/ns/oa");
        List<SPARQLStatement> results = sparql.getGraphStatement(uri);

        if (results.size() == 0) {
            String errorMsg = uri.toString() + " is missing data into your triple store, did you execute `opensilex system setup` command ?";
            LOGGER.warn("/!\\ " + errorMsg);
            throw new Exception(errorMsg);
        }
        
//        uri = new URI("http://www.opensilex.org/vocabulary/oeso");
//        results = sparql.getGraphStatement(uri);
//
//        if (results.size() == 0) {
//            String errorMsg = uri.toString() + " is missing data into your triple store, did you execute `opensilex system setup` command ?";
//            LOGGER.warn("/!\\ " + errorMsg);
//            throw new Exception(errorMsg);
//        }
//        
//        uri = new URI("http://www.opensilex.org/vocabulary/oeev");
//        results = sparql.getGraphStatement(uri);
//        factory.dispose(sparql);
//
//        if (results.size() == 0) {
//            String errorMsg = uri.toString() + " is missing data into your triple store, did you execute `opensilex system setup` command ?";
//            LOGGER.warn("/!\\ " + errorMsg);
//            throw new Exception(errorMsg);
//        }
    }

    @Override
    public void install(boolean reset) throws Exception {
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        // Import default ontologies
        LOGGER.info("Install oa ontology: http://www.w3.org/ns/oa");
        InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(CoreModule.class, "install/oa.rdf"));
        sparql.loadOntology(new URI("http://www.w3.org/ns/oa"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        LOGGER.info("Install oeso ontology: http://www.opensilex.org/vocabulary/oeso");
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(CoreModule.class, "install/oeso.owl"));
        sparql.loadOntology(new URI("http://www.opensilex.org/vocabulary/oeso"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        LOGGER.info("Install oeev ontology: http://www.opensilex.org/vocabulary/oeev");
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(CoreModule.class, "install/oeev.owl"));
        sparql.loadOntology(new URI("http://www.opensilex.org/vocabulary/oeev"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        SPARQLConfig sparqlConfig = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        URI graph = new URI(sparqlConfig.baseURI() + "species");

        LOGGER.info("Install Agrovoc species: " + graph.toString());
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(CoreModule.class, "install/species.ttl"));
        sparql.loadOntology(graph, ontologyStream, Lang.TTL);
        ontologyStream.close();

        factory.dispose(sparql);
    }

    @Override
    public void startup() throws Exception {
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        if (factory instanceof RDF4JInMemoryServiceFactory) {
            install(false);
        }
    }
}
