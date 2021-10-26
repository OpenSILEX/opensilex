/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "InfrastructureFacility",
        graph = InfrastructureModel.GRAPH,
        prefix = "infra"
)
public class InfrastructureFacilityModel extends SPARQLTreeModel<InfrastructureFacilityModel> {

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
    protected List<InfrastructureFacilityModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            inverse = true
    )
    private List<InfrastructureModel> infrastructures;
    public static final String INFRASTRUCTURE_FIELD = "infrastructures";

    public List<InfrastructureModel> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<InfrastructureModel> infrastructures) {
        this.infrastructures = infrastructures;
    }

    @Override
    public String[] getUriSegments(SPARQLTreeModel<InfrastructureFacilityModel> instance) {
        return new String[]{
                "facilities",
                instance.getName()
        };
    }

}
