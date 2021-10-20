package org.opensilex.core.ontology.dal.cache;

import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;

/**
 * Class which aggregate several objects to index by class URI
 */
final class ClassEntry {

    public ClassEntry(){
        dataPropertiesWithDomain = new SPARQLTreeListModel<>();
        objectPropertiesWithDomain = new SPARQLTreeListModel<>();
    }

    /**
     * ClassModel with filled data/object properties, restrictions and label/comments
     */
    ClassModel classModel;

    /**
     * Tree of data-properties with a domain which is a subClassOf* of classModel
     */
    SPARQLTreeListModel<DatatypePropertyModel> dataPropertiesWithDomain;

    /**
     * Tree of object-properties with a domain which is a subClassOf* of classModel
     */
    SPARQLTreeListModel<ObjectPropertyModel> objectPropertiesWithDomain;

    @Override
    public String toString() {
        return classModel != null ? classModel.getUri().toString() : "";
    }
}