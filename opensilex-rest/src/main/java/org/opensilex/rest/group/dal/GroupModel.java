/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.dal;

import java.util.List;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 *
 * @author vidalmor
 */
@SPARQLResource(
        ontology = SecurityOntology.class,
        resource = "Group",
        graph = "groups",
        prefix = "grp"
)
public class GroupModel extends SPARQLNamedResourceModel<GroupModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description",
            required = true
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUserProfile",
            cascadeDelete = true
    )
    private List<GroupUserProfileModel> userProfiles;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GroupUserProfileModel> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileModel> userProfiles) {
        this.userProfiles = userProfiles;
    }

}
