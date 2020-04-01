/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import java.util.List;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

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
public class InfrastructureModel extends SPARQLTreeModel<InfrastructureModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    protected String name;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            inverse = true
    )
    protected InfrastructureModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart"
    )
    protected List<InfrastructureModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDevice",
            inverse = true
    )
    private List<InfrastructureDeviceModel> devices;
    public static final String DEVICE_FIELD = "devices";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup",
            cascadeDelete = true
    )
    private GroupModel group;
    public static final String GROUP_FIELD = "group";

    public GroupModel getGroup() {
        return group;
    }

    public void setGroup(GroupModel group) {
        this.group = group;
    }

    public List<InfrastructureDeviceModel> getDevices() {
        return devices;
    }

    public void setDevices(List<InfrastructureDeviceModel> devices) {
        this.devices = devices;
    }

}
