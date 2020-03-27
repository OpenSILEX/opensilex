/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import java.util.List;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Infrastructure",
        graph = "infrastructures",
        prefix = "infra"
)
public class InfrastructureModel extends SPARQLTreeModel<InfrastructureModel> implements ClassURIGenerator<InfrastructureModel> {

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "name",
            required = true
    )
    protected String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasChild"
    )
    protected List<InfrastructureModel> children;
    public static final String CHILDREN_FIELD = "children";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasChild",
            inverse = true
    )
    protected InfrastructureModel parent;
    public static final String PARENT_FIELD = "parent";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDevice",
            inverse = true
    )
    private List<InfrastructureDeviceModel> devices;
    public static final String DEVICE_FIELD = "devices";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUser"
    )
    private List<UserModel> users;
    public static final String USERS_FIELD = "users";

    public List<UserModel> getUsers() {
        return users;
    }

    public void setUsers(List<UserModel> users) {
        this.users = users;
    }

    public List<InfrastructureDeviceModel> getDevices() {
        return devices;
    }

    public void setDevices(List<InfrastructureDeviceModel> devices) {
        this.devices = devices;
    }

    @Override
    public String[] getUriSegments(InfrastructureModel instance) {
        return new String[]{
            "infrastructures",
            instance.getName()
        };
    }

}
