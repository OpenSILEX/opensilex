/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author vince
 */
public final class OntologyDAO {

    private final SPARQLService sparql;

    public OntologyDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, UserModel user, boolean excludeRoot) throws Exception {
        return sparql.searchResourceTree(
                ClassModel.class,
                user.getLanguage(),
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    if (parent != null) {
                        Var parentVar = makeVar(ClassModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(parent));
                        select.addWhere(makeVar(ClassModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                }
        );
    }

    public SPARQLTreeListModel<PropertyModel> searchProperties(URI parent, UserModel user, boolean excludeRoot) throws Exception {
        return sparql.searchResourceTree(
                PropertyModel.class,
                user.getLanguage(),
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    Node parentNode = SPARQLDeserializers.nodeURI(parent);
                    if (parentNode != null) {
                        Var parentVar = makeVar(PropertyModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, parentNode);
                        select.addWhere(makeVar(PropertyModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                }
        );
    }

    public List<PropertyMappingModel> getClassPropertiesMapping(URI classURI, UserModel currentUser) throws Exception {
        ClassModel classModel = sparql.getByURI(ClassModel.class, classURI, currentUser.getLanguage());

        if (classModel != null) {
            return classModel.getPropertyMapping();
        }

        return new ArrayList<>();
    }
}
