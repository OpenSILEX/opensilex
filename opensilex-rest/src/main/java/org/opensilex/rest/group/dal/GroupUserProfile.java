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
        resource = "GroupUserProfile",
        graph = "groups",
        prefix = "g"
)
public class GroupUserProfile extends SPARQLResourceModel implements ClassURIGenerator<GroupUserProfile> {

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUser"
    )
    private UserModel user;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasProfile"
    )
    private ProfileModel profile;

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }
    
    public String[] getUriSegments(GroupUserProfile instance) {
        return new String[]{
            instance.toString()
        };
    }

}
