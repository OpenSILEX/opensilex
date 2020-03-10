//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
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

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, LoginExtension {

    public static final String TOKEN_USER_GROUP_URIS = "user_group_uris";

    @Override
    public void login(UserModel user, JWTCreator.Builder tokenBuilder) {

        // TODO add experiments, projects, infrastructures related to the user as token claims...
        SPARQLServiceFactory sparqlServiceFactory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = sparqlServiceFactory.provide();
        try {
            GroupDAO groupDAO = new GroupDAO(sparql);

            List<URI> groupUris = groupDAO.getGroupUriList(user);
            if (groupUris.isEmpty()) {
                tokenBuilder.withArrayClaim(TOKEN_USER_GROUP_URIS, new String[0]);
            } else {
                String[] groupArray = (String[]) groupUris.stream().map(URI::toString).toArray();
                tokenBuilder.withArrayClaim(TOKEN_USER_GROUP_URIS, groupArray);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            sparqlServiceFactory.dispose(sparql);
        }
    }
}
