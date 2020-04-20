/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import java.util.List;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
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
            property = "hasDevice"
    )
    private List<InfrastructureDeviceModel> devices;
    public static final String DEVICE_FIELD = "devices";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup",
            cascadeDelete = true
    )
    private List<InfrastructureTeamModel> groups;
    public static final String GROUP_FIELD = "groups";

    public List<InfrastructureTeamModel> getGroups() {
        return groups;
    }

    public void setGroups(List<InfrastructureTeamModel> group) {
        this.groups = group;
    }

    public List<InfrastructureDeviceModel> getDevices() {
        return devices;
    }

    public void setDevices(List<InfrastructureDeviceModel> devices) {
        this.devices = devices;
    }

}
