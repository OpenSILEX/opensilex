package org.opensilex.sparql.ontology.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SPARQLTranslatedTreeModel<T extends SPARQLTreeModel<T>> extends SPARQLTreeModel<T> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
    )
    protected SPARQLLabel label;
    public static final String NAME_FIELD = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    protected SPARQLLabel comment;
    public static final String COMMENT_FIELD = "comment";

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

    protected Set<T> parents;

    public Set<T> getParents(){
        return parents;
    }

    public void setParents(Set<T> parents){
        this.parents = parents;
    }


    public SPARQLTranslatedTreeModel(){
        parents = new HashSet<>();
    }

    protected SPARQLTranslatedTreeModel(SPARQLTranslatedTreeModel<T> other){
        uri = other.getUri();
        rdfType = other.getType();

        if(other.getLabel() != null){
            label = new SPARQLLabel(other.getLabel());
        }
        if(other.getComment() != null){
            comment = new SPARQLLabel(other.getComment());
        }
        if(other.getTypeLabel() != null){
            rdfTypeName = new SPARQLLabel(other.getTypeLabel());
        }
    }

}
