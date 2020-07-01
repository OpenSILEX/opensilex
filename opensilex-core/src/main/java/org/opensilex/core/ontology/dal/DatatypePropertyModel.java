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
        resource = "DatatypeProperty",
        ignoreValidation = true
)
public class DatatypePropertyModel extends PropertyModel {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf",
            inverse = true
    )
    protected List<DatatypePropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected DatatypePropertyModel parent;

    protected URI datatypeRestriction;

    public URI getDatatypeRestriction() {
        return datatypeRestriction;
    }

    public void setDatatypeRestriction(URI datatypeRestriction) {
        this.datatypeRestriction = datatypeRestriction;
    }

}
