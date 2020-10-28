/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.nosql.service.NoSQLService;
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

    public List<ScientificObjectModel> searchByURIs(URI experimentURI, List<URI> objectsURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);

        List<URI> uniqueObjectsUri = objectsURI.stream()
                .distinct()
                .collect(Collectors.toList());
        return sparql.getListByURIs(experimentGraph, ScientificObjectModel.class, uniqueObjectsUri, currentUser.getLanguage());
    }

    public ListWithPagination<ScientificObjectModel> searchChildrenByExperiment(URI experimentURI, URI parentURI, Integer page, Integer pageSize, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
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

    public ListWithPagination<ScientificObjectModel> search(URI experimentURI, String pattern, URI rdfType, URI parentURI, Integer page, Integer pageSize, UserModel currentUser) throws Exception {
        Node experimentGraph = null;
        Expr graphFilter = null;
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        if (experimentURI != null) {
            xpDAO.validateExperimentAccess(experimentURI, currentUser);
            experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        } else if (!currentUser.isAdmin()) {
            Set<URI> allowedExperimentURIs = xpDAO.getUserExperiments(currentUser);
            List<URI> selectedExperimentURIs = allowedExperimentURIs.stream().filter((URI xpURI) -> {
                if (experimentURI == null) {
                    return true;
                }
                return SPARQLDeserializers.compareURIs(xpURI, experimentURI);
            }).collect(Collectors.toList());;
            if (selectedExperimentURIs.size() == 0) {
                return new ListWithPagination<ScientificObjectModel>(new ArrayList<>());
            } else if (selectedExperimentURIs.size() == 1) {
                experimentGraph = SPARQLDeserializers.nodeURI(selectedExperimentURIs.get(0));
            } else {
                experimentGraph = makeVar("?__graph");
                graphFilter = SPARQLQueryHelper.inURIFilter("__graph", selectedExperimentURIs);
            }
        }

        final Expr finalGraphFilter = graphFilter;
        return sparql.searchWithPagination(
                experimentGraph,
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

    public List<ScientificObjectModel> searchAll(URI experimentURI, URI rdfType, URI parentURI, UserModel currentUser) throws Exception {
        Node experimentGraph = null;
        Expr graphFilter = null;
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        if (experimentURI != null) {
            xpDAO.validateExperimentAccess(experimentURI, currentUser);
            experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        } else if (!currentUser.isAdmin()) {
            Set<URI> allowedExperimentURIs = xpDAO.getUserExperiments(currentUser);
            List<URI> selectedExperimentURIs = allowedExperimentURIs.stream().filter((URI xpURI) -> {
                if (experimentURI == null) {
                    return true;
                }
                return SPARQLDeserializers.compareURIs(xpURI, experimentURI);
            }).collect(Collectors.toList());;
            if (selectedExperimentURIs.size() == 0) {
                return new ArrayList<>();
            } else if (selectedExperimentURIs.size() == 1) {
                experimentGraph = SPARQLDeserializers.nodeURI(selectedExperimentURIs.get(0));
            } else {
                experimentGraph = makeVar("?__graph");
                graphFilter = SPARQLQueryHelper.inURIFilter("__graph", selectedExperimentURIs);
            }
        }

        final Expr finalGraphFilter = graphFilter;
        return sparql.search(
                experimentGraph,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (rdfType != null) {
                        select.addWhere(makeVar(ScientificObjectModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(rdfType));
                    }
                    if (finalGraphFilter != null) {
                        select.addFilter(finalGraphFilter);
                    }
                    if (parentURI != null) {
                        select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    }
                }
        );
    }

    public URI create(URI xpURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {

        SPARQLResourceModel object = initObject(xpURI, soType, name, relations, currentUser);

        if (objectURI == null) {
            ScientificObjectURIGenerator uriGenerator = new ScientificObjectURIGenerator(xpURI);
            int retry = 0;
            objectURI = uriGenerator.generateURI(xpURI.toString(), name, retry);
            while (sparql.uriExists(SPARQLDeserializers.nodeURI(xpURI), objectURI)) {
                retry++;
                objectURI = uriGenerator.generateURI(xpURI.toString(), name, retry);
            }
        }
        object.setUri(objectURI);

        sparql.create(SPARQLDeserializers.nodeURI(xpURI), object);

        return object.getUri();
    }

    public URI update(URI xpURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        SPARQLResourceModel object = initObject(xpURI, soType, name, relations, currentUser);
        object.setUri(objectURI);

        Node xpGraphNode = SPARQLDeserializers.nodeURI(xpURI);

        List<URI> childrenURIs = sparql.searchURIs(
                xpGraphNode,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(objectURI));
                });
        try {
            sparql.startTransaction();
            sparql.deleteByURI(xpGraphNode, objectURI);
            sparql.create(xpGraphNode, object);
            if (childrenURIs.size() > 0) {
                sparql.insertPrimitive(xpGraphNode, childrenURIs, Oeso.isPartOf, objectURI);
            }
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }

        return object.getUri();
    }

    private SPARQLResourceModel initObject(URI xpURI, URI soType, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, new URI(Oeso.ExperimentalObject.getURI()), currentUser.getLanguage());

        SPARQLResourceModel object = new SPARQLResourceModel();
        object.setType(soType);

        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                if (!ontologyDAO.validateObjectValue(xpURI, model, relation.getProperty(), relation.getValue(), object)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        object.addRelation(xpURI, new URI(RDFS.label.getURI()), String.class, name);

        return object;
    }

    public ExperimentalObjectModel getByURIAndExperiment(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        return sparql.getByURI(SPARQLDeserializers.nodeURI(xpURI), ExperimentalObjectModel.class, objectURI, currentUser.getLanguage());
    }

    public void delete(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        sparql.deleteByURI(SPARQLDeserializers.nodeURI(xpURI), objectURI);
    }
}
