/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Facility",
        graph = InfrastructureModel.GRAPH,
        prefix = "infra"
)
public class InfrastructureFacilityModel extends SPARQLTreeModel<InfrastructureFacilityModel> {

    private static final String FACILITY = "facility";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected InfrastructureFacilityModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true,
            useDefaultGraph = false
    )
    protected List<InfrastructureFacilityModel> children = new LinkedList<>();

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            inverse = true
    )
    private List<InfrastructureModel> infrastructures = new LinkedList<>();
    public static final String INFRASTRUCTURE_FIELD = "infrastructures";

    public List<InfrastructureModel> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<InfrastructureModel> infrastructures) {
        this.infrastructures = infrastructures;
    }

    @Override
    public InfrastructureFacilityModel getParent() {
        return parent;
    }

    @Override
    public void setParent(InfrastructureFacilityModel parent) {
        this.parent = parent;
    }

    @Override
    public List<InfrastructureFacilityModel> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<InfrastructureFacilityModel> children) {
        this.children = children;
    }

    public List<URI> getInfrastructureUris() {
        return this.infrastructures
                .stream()
                .map(InfrastructureModel::getUri)
                .collect(Collectors.toList());
    }

    @Override
    public String[] getInstancePathSegments(SPARQLTreeModel<InfrastructureFacilityModel> instance) {
        return new String[]{
                FACILITY,
                instance.getName()
        };
    }

}
