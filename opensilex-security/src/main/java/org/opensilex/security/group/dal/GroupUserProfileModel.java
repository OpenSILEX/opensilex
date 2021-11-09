/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.dal;

import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.dal.UserModel;
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
        graph = GroupModel.GRAPH,
        prefix = "grp"
)
public class GroupUserProfileModel extends SPARQLResourceModel implements ClassURIGenerator<GroupUserProfileModel> {

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUser",
            required = true
    )
    private UserModel user;

    public final static String USER_FIELD = "user";
    
    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasProfile",
            required = true
    )
    private ProfileModel profile;

    public final static String PROFILE_FIELD = "profile";
    
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
    
    public String[] getInstancePathSegments(GroupUserProfileModel instance) {
        return new String[]{
            "" + System.identityHashCode(instance)
        };
    }

}
