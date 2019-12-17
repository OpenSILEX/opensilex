/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.security.dal;

import org.opensilex.rest.authentication.SecurityOntology;
import java.util.List;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = SecurityOntology.class,
        resource = "SecurityProfile",
        graph = "groups",
        prefix = "profiles"
)
public class SecurityProfileModel extends SPARQLResourceModel implements ClassURIGenerator<SecurityProfileModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    private String name;
    public final static String NAME_FIELD = "name";
    
    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasAccess"
    )
    private List<String> accessList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAccessList() {
        return accessList;
    }

    public void setAccessList(List<String> accessList) {
        this.accessList = accessList;
    }

    @Override
    public String[] getUriSegments(SecurityProfileModel instance) {
        return new String[]{
            "profiles",
            instance.getName()
        };
    }

}
