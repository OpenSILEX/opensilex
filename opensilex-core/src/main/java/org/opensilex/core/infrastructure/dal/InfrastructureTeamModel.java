/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "InfrastructureTeam",
        graph = "groups",
        prefix = "grp"
)
public class InfrastructureTeamModel extends GroupModel {

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup",
            inverse = true
    )
    private InfrastructureModel infrastructure;

    public InfrastructureModel getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(InfrastructureModel infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public String[] getUriSegments(GroupModel instance) {
        return new String[]{
            ((InfrastructureTeamModel) instance).getInfrastructure().getName(),
            "teams",
            instance.getName()
        };
    }
}
