/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLModel;


public abstract class BaseVariableModel extends SPARQLModel {
    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    private String name;
    public static final String LABEL_FIELD_NAME = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment",
            required = true
    )
    private String comment;
    public static final String COMMENT_FIELD_NAME = "comment";

    public String getName() {
        return name;
    }

    public void setName(String label) {
        this.name = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
