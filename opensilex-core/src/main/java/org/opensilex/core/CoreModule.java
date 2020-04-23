//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
import org.opensilex.OpenSilexModule;

import java.util.List;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OA;
import org.opensilex.security.extensions.LoginExtension;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.rest.cache.JCSApiCacheExtension;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, LoginExtension, SPARQLExtension, JCSApiCacheExtension {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);
    @Override
    public Class<?> getConfigClass() {
        return CoreConfig.class;
    }

    @Override
    public String getConfigId() {
        return "core";
    }
    @Override
    public void login(UserModel user, JWTCreator.Builder tokenBuilder) throws Exception {

        // TODO add experiments, projects, infrastructures related to the user as token claims...
    }

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                OA.NS,
                "ontologies/oa.rdf",
                Lang.RDFXML,
                "oa"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso#",
                "ontologies/oeso-core.owl",
                Lang.RDFXML,
                "oeso"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeev#",
                "ontologies/oeev.owl",
                Lang.RDFXML,
                "oeev"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/opensilex-api#",
                "ontologies/oxapi.owl",
                Lang.RDFXML,
                "oxapi"
        ));
        return list;
    }
}
