package org.opensilex.core.ontology.dal.cache;

import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;

/**
 * Class which aggregate several objects to index by class URI
 */
final class ClassEntry {

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
}