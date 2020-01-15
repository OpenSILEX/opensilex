/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.dal;

import java.util.List;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author vidalmor
 */
@SPARQLResource(
        ontology = SecurityOntology.class,
        resource = "Group",
        graph = "groups",
        prefix = "g"
)
public class GroupModel extends SPARQLResourceModel implements ClassURIGenerator<ProfileModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    private String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description",
            required = true
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUser"
    )
    private List<UserModel> users;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasProfile"
    )
    private List<ProfileModel> profiles;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<UserModel> getUsers() {
        return users;
    }

    public void setUsers(List<UserModel> users) {
        this.users = users;
    }

    public List<ProfileModel> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<ProfileModel> profiles) {
        this.profiles = profiles;
    }

    @Override
    public String[] getUriSegments(ProfileModel instance) {
        return new String[]{
            instance.getName()
        };
    }

}
