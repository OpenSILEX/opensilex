//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.profile.dal;

import org.opensilex.rest.authentication.SecurityOntology;
import java.util.List;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.rest.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = SecurityOntology.class,
        resource = "Profile",
        graph = "profiles",
        prefix = "prf"
)
public class ProfileModel extends SPARQLResourceModel implements ClassURIGenerator<ProfileModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    private String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasCredential"
    )
    private List<String> credentials;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasProfile",
            inverse = true,
            cascadeDelete = true
    )
    private List<GroupUserProfileModel> userProfiles;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    @Override
    public String[] getUriSegments(ProfileModel instance) {
        return new String[]{
            instance.getName()
        };
    }
}
