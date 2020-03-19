/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import java.util.List;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Infrastructure",
        graph = "set/infrastructures",
        prefix = "infra"
)
public class InfrastructureModel extends SPARQLResourceModel implements ClassURIGenerator<ProjectModel> {

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "name",
            required = true
    )
    private String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasChild"
    )
    private List<InfrastructureModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasChild",
            inverse = true
    )
    private InfrastructureModel parent;
    public static final String PARENT_FIELD = "parent";
    
    @SPARQLProperty(
        ontology = Oeso.class,
        property = "hasUser"
    )
    private List<UserModel> users;
    public static final String USERS_FIELD = "users";
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InfrastructureModel> getChildren() {
        return children;
    }

    public void setChildren(List<InfrastructureModel> children) {
        this.children = children;
    }

    public InfrastructureModel getParent() {
        return parent;
    }

    public void setParent(InfrastructureModel parent) {
        this.parent = parent;
    }

    public List<UserModel> getUsers() {
        return users;
    }

    public void setUsers(List<UserModel> users) {
        this.users = users;
    }
    
    @Override
    public String[] getUriSegments(ProjectModel instance) {
        return new String[]{
            "infrastructures",
            instance.getName()
        };
    }

}
