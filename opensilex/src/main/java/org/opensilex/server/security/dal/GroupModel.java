//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security.dal;

import org.opensilex.server.security.SecurityOntology;
import java.util.List;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.server.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = SecurityOntology.class,
        resource = "Group",
        graph = "groups"
)
public class GroupModel extends SPARQLResourceModel implements ClassURIGenerator<GroupModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public final static String DESCRIPTION_FIELD = "description";
    
    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    private String name;
    public final static String NAME_FIELD = "name";
    
    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasProfile",
            required = true
    )
    private SecurityProfileModel groupProfile;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUser"
    )
    private List<UserModel> users;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserModel> getUsers() {
        return users;
    }

    public void setUsers(List<UserModel> users) {
        this.users = users;
    }

    public SecurityProfileModel getGroupProfile() {
        return groupProfile;
    }

    public void setGroupProfile(SecurityProfileModel groupProfile) {
        this.groupProfile = groupProfile;
    }
    
    @Override
    public String[] getUriSegments(GroupModel instance) {
        return new String[]{
            "groups",
            instance.getName()
        };
    }

}
