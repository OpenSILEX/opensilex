/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "InfrastructureDevice",
        graph = "infrastructures",
        prefix = "infra"
)
public class InfrastructureDeviceModel extends SPARQLNamedResourceModel<InfrastructureDeviceModel> {

    @Override
    public String[] getUriSegments(InfrastructureDeviceModel instance) {
        return new String[]{
            "devices",
            instance.getName()
        };
    }

}
