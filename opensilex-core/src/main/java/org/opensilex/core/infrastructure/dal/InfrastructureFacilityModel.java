/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "InfrastructureFacility",
        graph = "infrastructures",
        prefix = "infra"
)
public class InfrastructureFacilityModel extends ScientificObjectModel {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFacility",
            inverse = true
    )
    private InfrastructureModel infrastructure;
    public static final String INFRASTRUCTURE_FIELD = "infrastructure";

    public InfrastructureModel getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(InfrastructureModel infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public String[] getUriSegments(SPARQLTreeModel<ScientificObjectModel> instance) {
        InfrastructureFacilityModel facility = (InfrastructureFacilityModel) instance;
        return new String[]{
            facility.getInfrastructure().getName(),
            "facilities",
            facility.getName()
        };
    }

}
