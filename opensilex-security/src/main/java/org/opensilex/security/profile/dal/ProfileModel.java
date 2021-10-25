//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.profile.dal;

import java.util.List;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 *
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = SecurityOntology.class,
        resource = "Profile",
        graph = ProfileModel.GRAPH,
        prefix = "prf"
)
public class ProfileModel extends SPARQLNamedResourceModel<ProfileModel> {

    public static final String GRAPH = "profile";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasCredential"
    )
    private List<String> credentials;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasProfile",
            inverse = true,
            cascadeDelete = true,
            ignoreUpdateIfNull = true
    )
    private List<GroupUserProfileModel> userProfiles;

    public List<String> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<String> credentials) {
        this.credentials = credentials;
    }

    public List<GroupUserProfileModel> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileModel> userProfiles) {
        this.userProfiles = userProfiles;
    }

}
