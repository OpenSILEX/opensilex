/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.ontology.dal;

import java.util.List;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OWL.class,
        resource = "DatatypeProperty"
)
public class PropertyModel extends SPARQLTreeModel<PropertyModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
    )
    protected SPARQLLabel label;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    protected SPARQLLabel comment;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf",
            inverse = true
    )
    protected List<PropertyModel> children;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subPropertyOf"
    )
    protected PropertyModel parent;

    @Override
    public String getName() {
        SPARQLLabel slabel = getLabel();
        if (slabel != null) {
            return getLabel().getDefaultValue();
        } else {
            return getUri().toString();
        }
    }

    public SPARQLLabel getLabel() {
        return label;
    }

    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }

    public SPARQLLabel getComment() {
        return comment;
    }

    public void setComment(SPARQLLabel comment) {
        this.comment = comment;
    }
}
