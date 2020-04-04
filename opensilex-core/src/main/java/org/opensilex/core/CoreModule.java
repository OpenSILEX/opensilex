//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import java.net.URI;
import java.util.List;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OA;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.extensions.LoginExtension;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, LoginExtension, SPARQLExtension {

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
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        SPARQLConfig sparqlConfig = this.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                OA.NS,
                "install/oa.rdf",
                Lang.RDFXML,
                "oa"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso#",
                "install/oeso.owl",
                Lang.RDFXML,
                "oeso"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeev#",
                "install/oeev.owl",
                Lang.RDFXML,
                "oeev"
        ));
        list.add(new OntologyFileDefinition(
                sparqlConfig.baseURI() + "species",
                "install/species.ttl",
                Lang.TTL,
                sparqlConfig.baseURIAlias() + "-species"
        ));
        return list;
    }

    @Override
    public void startup() throws Exception {
        SPARQLConfig sparqlConfig = getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        SPARQLServiceFactory factory = sparqlConfig.sparql();
        if (factory instanceof RDF4JInMemoryServiceFactory) {
            install(false);
        }
        SPARQLService.addPrefix(SecurityOntology.PREFIX, SecurityOntology.NAMESPACE);

    }
}
