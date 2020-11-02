/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDAO {

    private final SPARQLService sparql;

    public ScientificObjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public List<ScientificObjectModel> searchByURIs(URI contextURI, List<URI> objectsURI, UserModel currentUser) throws Exception {
        Node experimentGraph = SPARQLDeserializers.nodeURI(contextURI);

        List<URI> uniqueObjectsUri = objectsURI.stream()
                .distinct()
                .collect(Collectors.toList());
        return sparql.getListByURIs(experimentGraph, ScientificObjectModel.class, uniqueObjectsUri, currentUser.getLanguage());
    }

    public ListWithPagination<ScientificObjectModel> searchChildrenByContext(URI contextURI, URI parentURI, Integer page, Integer pageSize, UserModel currentUser) throws Exception {
        Node experimentGraph = SPARQLDeserializers.nodeURI(contextURI);
        return sparql.searchWithPagination(
                experimentGraph,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (parentURI != null) {
                        select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    } else {
                        Triple parentTriple = new Triple(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf.asNode(), makeVar("parentURI"));
                        select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(parentTriple)));
                    }
                },
                null,
                page,
                pageSize);
    }

    public ListWithPagination<ScientificObjectModel> search(List<URI> contextURIs, String pattern, URI rdfType, URI parentURI, Integer page, Integer pageSize, UserModel currentUser) throws Exception {
        Node contextURI = null;
        Expr graphFilter = null;
        
        if (contextURIs.size() == 1) {
            contextURI = SPARQLDeserializers.nodeURI(contextURIs.get(0));
        } else if (contextURIs.size() > 1) {
            contextURI = makeVar("?__graph");
            graphFilter = SPARQLQueryHelper.inURIFilter("__graph", contextURIs);
        }

        final Expr finalGraphFilter = graphFilter;
        return sparql.searchWithPagination(
                contextURI,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (pattern != null && !pattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(ScientificObjectModel.NAME_FIELD, pattern));
                    }
                    if (rdfType != null) {
                        select.addWhere(makeVar(ScientificObjectModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(rdfType));
                    }
                    if (finalGraphFilter != null) {
                        select.addFilter(finalGraphFilter);
                    }
                    if (parentURI != null) {
                        select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    }
                },
                null,
                page,
                pageSize);
    }

    public List<ScientificObjectModel> searchAll(URI contextURI, URI rdfType, URI parentURI, UserModel currentUser) throws Exception {
        Node context = SPARQLDeserializers.nodeURI(contextURI);
        
        return sparql.search(
                context,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (rdfType != null) {
                        select.addWhere(makeVar(ScientificObjectModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(rdfType));
                    }
                    if (parentURI != null) {
                        select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    }
                }
        );
    }

    public URI create(URI contextURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {

        SPARQLResourceModel object = initObject(contextURI, soType, name, relations, currentUser);

        if (objectURI == null) {
            ScientificObjectURIGenerator uriGenerator = new ScientificObjectURIGenerator(contextURI);
            int retry = 0;
            objectURI = uriGenerator.generateURI(contextURI.toString(), name, retry);
            while (sparql.uriExists(SPARQLDeserializers.nodeURI(contextURI), objectURI)) {
                retry++;
                objectURI = uriGenerator.generateURI(contextURI.toString(), name, retry);
            }
        }
        object.setUri(objectURI);

        sparql.create(SPARQLDeserializers.nodeURI(contextURI), object);

        return object.getUri();
    }

    public URI update(URI contextURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        SPARQLResourceModel object = initObject(contextURI, soType, name, relations, currentUser);
        object.setUri(objectURI);

        Node graphNode = SPARQLDeserializers.nodeURI(contextURI);

        List<URI> childrenURIs = sparql.searchURIs(
                graphNode,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(objectURI));
                });
        try {
            sparql.startTransaction();
            sparql.deleteByURI(graphNode, objectURI);
            sparql.create(graphNode, object);
            if (childrenURIs.size() > 0) {
                sparql.insertPrimitive(graphNode, childrenURIs, Oeso.isPartOf, objectURI);
            }
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }

        return object.getUri();
    }

    private SPARQLResourceModel initObject(URI contextURI, URI soType, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, new URI(Oeso.ExperimentalObject.getURI()), currentUser.getLanguage());

        SPARQLResourceModel object = new SPARQLResourceModel();
        object.setType(soType);

        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                if (!ontologyDAO.validateObjectValue(contextURI, model, relation.getProperty(), relation.getValue(), object)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        object.addRelation(contextURI, new URI(RDFS.label.getURI()), String.class, name);

        return object;
    }

    public ExperimentalObjectModel geObjectByURI(URI objectURI, URI contextURI, UserModel currentUser) throws Exception {
        return sparql.getByURI(SPARQLDeserializers.nodeURI(contextURI), ExperimentalObjectModel.class, objectURI, currentUser.getLanguage());
    }

    public void delete(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        sparql.deleteByURI(SPARQLDeserializers.nodeURI(xpURI), objectURI);
    }
}
