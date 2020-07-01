/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "ObjectProperty",
        ignoreValidation = true
)
public class ObjectPropertyModel extends PropertyModel {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf",
            inverse = true
    )
    protected List<ObjectPropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected ObjectPropertyModel parent;

    protected URI objectClassRestriction;

    public URI getObjectClassRestriction() {
        return objectClassRestriction;
    }

    public void setObjectClassRestriction(URI objectClassRestriction) {
        this.objectClassRestriction = objectClassRestriction;
    }

}
