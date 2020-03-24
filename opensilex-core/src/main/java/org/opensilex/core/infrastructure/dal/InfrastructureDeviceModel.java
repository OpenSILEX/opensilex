/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.dal;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "InfrastructureDevice",
        graph = "devices",
        prefix = "dvc"
)
public class InfrastructureDeviceModel extends SPARQLResourceModel implements ClassURIGenerator<InfrastructureDeviceModel> {

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "name",
            required = true
    )
    private String name;
    public static final String NAME_FIELD = "name";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String[] getUriSegments(InfrastructureDeviceModel instance) {
        return new String[]{
            "devices",
            instance.getName()
        };
    }

}
