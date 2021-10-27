/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal;

import java.util.List;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLDagModel;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Organization",
        graph = "set/infrastructures",
        prefix = "infra"
)
public class InfrastructureModel extends SPARQLDagModel<InfrastructureModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<InfrastructureModel> parents;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            ignoreUpdateIfNull = true
    )
    protected List<InfrastructureModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            ignoreUpdateIfNull = true
    )
    private List<InfrastructureFacilityModel> facilities;
    public static final String FACILITIES_FIELD = "facilities";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup",
            cascadeDelete = true,
            ignoreUpdateIfNull = true
    )
    private List<GroupModel> groups;
    public static final String GROUP_FIELD = "groups";

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> group) {
        this.groups = group;
    }

    public List<InfrastructureFacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<InfrastructureFacilityModel> facilities) {
        this.facilities = facilities;
    }

}
