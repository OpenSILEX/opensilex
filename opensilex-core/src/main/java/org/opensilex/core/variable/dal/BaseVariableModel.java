/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLResourceModel;


public abstract class BaseVariableModel extends SPARQLResourceModel {
    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    private String label;
    public static final String LABEL_FIELD = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    private String comment;
    public static final String COMMENT_FIELD = "comment";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
