/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.event.dal.move.ConcernedItemPositionModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.rest.serialization.CustomParamConverterProvider;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingConsumer;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDAO {

    private final SPARQLService sparql;

    private final MongoDBService nosql;

    public ScientificObjectDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
    }

    public List<ScientificObjectModel> searchByURIs(URI contextURI, List<URI> objectsURI, UserModel currentUser) throws Exception {
        Node experimentGraph = SPARQLDeserializers.nodeURI(contextURI);

        List<URI> uniqueObjectsUri = objectsURI.stream()
                .distinct()
                .collect(Collectors.toList());
        return sparql.getListByURIs(experimentGraph, ScientificObjectModel.class, uniqueObjectsUri, currentUser.getLanguage());
    }

    public ListWithPagination<ScientificObjectModel> searchChildren(URI contextURI, URI parentURI, URI facility, Integer page, Integer pageSize, UserModel currentUser) throws Exception {
        Node contextGraph = SPARQLDeserializers.nodeURI(contextURI);
        return sparql.searchWithPagination(
                contextGraph,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (parentURI != null) {
                        select.addGraph(contextGraph, makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    } else {
                        WhereBuilder whereFilter = new WhereBuilder().addGraph(
                                SPARQLDeserializers.nodeURI(contextURI),
                                makeVar(ScientificObjectModel.URI_FIELD),
                                Oeso.isPartOf.asNode(),
                                makeVar("parentURI")
                        );
                        select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(whereFilter));
                    }

                    if (facility != null) {
                        Node facilityNode = SPARQLDeserializers.nodeURI(facility);
                        Var directFacility = makeVar("__directFacility");
                        Var parentLinkURI = makeVar("__parentLinkURI");
                        Var parentFacility = makeVar("__parentFacility");
                        Path subPartOf = new P_ZeroOrMore1(new P_Link(Oeso.isPartOf.asNode()));

                        WhereBuilder graphQuery = new WhereBuilder();
                        graphQuery.addGraph(contextGraph, makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasFacility, directFacility);
                        graphQuery.addGraph(contextGraph, makeVar(ScientificObjectModel.URI_FIELD), subPartOf, parentLinkURI);
                        graphQuery.addGraph(contextGraph, parentLinkURI, Oeso.hasFacility, parentFacility);
                        select.addOptional(graphQuery);

                        select.addFilter(SPARQLQueryHelper.or(
                                SPARQLQueryHelper.eq("__directFacility", facilityNode),
                                SPARQLQueryHelper.eq("__parentFacility", facilityNode)
                        ));
                    }
                },
                null,
                page,
                pageSize);
    }

    public ListWithPagination<ScientificObjectModel> search(URI contextURI, String pattern, List<URI> rdfTypes, URI parentURI, URI germplasm, List<URI> factorLevels, URI facility, LocalDate existenceDate, LocalDate creationDate, Integer page, Integer pageSize, List<OrderBy> orderByList, UserModel currentUser) throws Exception {
        final Node contextNode;
        if (contextURI != null) {
            contextNode = SPARQLDeserializers.nodeURI(contextURI);
        } else {
            contextNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ScientificObjectModel.class));
        }

        return sparql.searchWithPagination(
                contextNode,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (pattern != null && !pattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(ScientificObjectModel.NAME_FIELD, pattern));
                    }
                    if (rdfTypes != null && rdfTypes.size() > 0) {
                        select.addFilter(SPARQLQueryHelper.inURIFilter(ScientificObjectModel.TYPE_FIELD, rdfTypes));
                    }

                    if (parentURI != null) {
                        Path deepPartOf = new P_OneOrMore1(new P_Link(Oeso.isPartOf.asNode()));
                        select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), deepPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    }

                    if (factorLevels != null && factorLevels.size() > 0) {
                        Var factorLevelVar = makeVar("__factorLevel");
                        if (contextURI != null) {
                            select.addGraph(contextNode, makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasFactorLevel, factorLevelVar);
                        } else {
                            select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasFactorLevel, factorLevelVar);
                        }

                        select.addFilter(SPARQLQueryHelper.inURIFilter(factorLevelVar, factorLevels));
                    }

                    if (germplasm != null) {
                        
                        select.addWhere(
                            new WhereBuilder()
                                .addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasGermplasm, SPARQLDeserializers.nodeURI(germplasm))
                                .addUnion(new WhereBuilder()
                                    .addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasGermplasm, makeVar("_g1"))
                                    .addWhere(makeVar("_g1"), Oeso.fromSpecies, SPARQLDeserializers.nodeURI(germplasm)))                                
                                .addUnion(new WhereBuilder()
                                    .addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasGermplasm, makeVar("_g2"))
                                    .addWhere(makeVar("_g2"), Oeso.fromVariety, SPARQLDeserializers.nodeURI(germplasm)))
                                .addUnion(new WhereBuilder()
                                    .addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasGermplasm, makeVar("_g2"))
                                    .addWhere(makeVar("_g2"), Oeso.fromVariety, SPARQLDeserializers.nodeURI(germplasm))));
                    }

                    if (facility != null) {
                        Node facilityNode = SPARQLDeserializers.nodeURI(facility);
                        Var directFacility = makeVar("__directFacility");
                        Var parentLinkURI = makeVar("__parentLinkURI");
                        Var parentFacility = makeVar("__parentFacility");
                        Path subPartOf = new P_ZeroOrMore1(new P_Link(Oeso.isPartOf.asNode()));
                        if (contextURI != null) {
                            WhereBuilder graphQuery = new WhereBuilder();
                            graphQuery.addGraph(contextNode, makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasFacility, directFacility);
                            graphQuery.addGraph(contextNode, makeVar(ScientificObjectModel.URI_FIELD), subPartOf, parentLinkURI);
                            graphQuery.addGraph(contextNode, parentLinkURI, Oeso.hasFacility, parentFacility);
                            select.addOptional(graphQuery);
                        } else {
                            WhereBuilder graphQuery = new WhereBuilder();
                            graphQuery.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.hasFacility, directFacility);
                            graphQuery.addWhere(makeVar(ScientificObjectModel.URI_FIELD), subPartOf, parentLinkURI);
                            graphQuery.addWhere(parentLinkURI, Oeso.hasFacility, parentFacility);
                            select.addOptional(graphQuery);
                        }

                        select.addFilter(SPARQLQueryHelper.or(
                                SPARQLQueryHelper.eq("__directFacility", facilityNode),
                                SPARQLQueryHelper.eq("__parentFacility", facilityNode)
                        ));
                    }

                    DateDeserializer dateDeserializer = new DateDeserializer();
                    ExprFactory exprFactory = new ExprFactory();
                    if (existenceDate != null) {
                        Node uriVar = NodeFactory.createVariable(ScientificObjectModel.URI_FIELD);
                        Node creationDateVar = NodeFactory.createVariable(ScientificObjectModel.CREATION_DATE_FIELD);
                        Node destructionDateVar = NodeFactory.createVariable(ScientificObjectModel.DESTRUCTION_DATE_FIELD);

                        WhereBuilder optionalDestructionDate = new WhereBuilder();
                        optionalDestructionDate.addWhere(uriVar, Oeso.hasDestructionDate, destructionDateVar);
                        select.addFilter(
                                exprFactory.and(
                                        exprFactory.le(creationDateVar, dateDeserializer.getNode(existenceDate)),
                                        exprFactory.or(
                                                exprFactory.not(exprFactory.exists(optionalDestructionDate)),
                                                exprFactory.ge(destructionDateVar, dateDeserializer.getNode(existenceDate))
                                        )
                                )
                        );
                    }

                    if (creationDate != null) {
                        Node creationDateVar = NodeFactory.createVariable(ScientificObjectModel.CREATION_DATE_FIELD);
                        select.addFilter(exprFactory.eq(creationDateVar, dateDeserializer.getNode(creationDate)));
                    }
                },
                orderByList,
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

        try {
            sparql.startTransaction();
            nosql.startTransaction();
            sparql.create(SPARQLDeserializers.nodeURI(contextURI), object);
            objectURI = object.getUri();

            MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
            MoveModel facilityMoveEvent = new MoveModel();
            if (fillFacilityMoveEvent(facilityMoveEvent, object)) {
                moveDAO.create(facilityMoveEvent);
            }
            sparql.deletePrimitives(SPARQLDeserializers.nodeURI(contextURI), object.getUri(), Oeso.hasFacility);
            nosql.commitTransaction();
            sparql.commitTransaction();
        } catch (Exception ex) {
            nosql.rollbackTransaction();
            sparql.rollbackTransaction(ex);
        }

        return objectURI;
    }

    public static boolean fillFacilityMoveEvent(MoveModel facilityMoveEvent, SPARQLResourceModel object) throws Exception {
        List<URI> concernedItems = new ArrayList<>();
        concernedItems.add(object.getUri());
        facilityMoveEvent.setConcernedItems(concernedItems);

        facilityMoveEvent.setCreator(object.getCreator());

        facilityMoveEvent.setIsInstant(true);

        boolean hasFacility = false;
        for (SPARQLModelRelation relation : object.getRelations()) {
            if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.hasFacility.getURI())) {
                InfrastructureFacilityModel infraModel = new InfrastructureFacilityModel();
                infraModel.setUri(new URI(relation.getValue()));
                facilityMoveEvent.setTo(infraModel);
                hasFacility = true;
            } else if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.hasCreationDate.getURI())) {
                InstantModel end = new InstantModel();
                SPARQLDeserializer<LocalDate> dateDeserializer = SPARQLDeserializers.getForClass(LocalDate.class);
                LocalDate date = dateDeserializer.fromString(relation.getValue());
                end.setDateTimeStamp(OffsetDateTime.of(date, LocalTime.MIN, ZoneOffset.UTC));
                facilityMoveEvent.setEnd(end);
            }
        }

        InstantModel end = facilityMoveEvent.getEnd();
        if (end != null) {
            if (end.getDateTimeStamp() == null) {
                end.setDateTimeStamp(OffsetDateTime.now());
            }
        } else if (hasFacility) {
            end = new InstantModel();
            end.setDateTimeStamp(OffsetDateTime.now());
            facilityMoveEvent.setEnd(end);
        }

        return hasFacility;
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

        boolean hasFacilityURI = false;
        for (SPARQLModelRelation relation : object.getRelations()) {
            if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.hasFacility.getURI())) {
                hasFacilityURI = true;
                break;
            }
        }

        try {
            sparql.startTransaction();
            nosql.startTransaction();
            sparql.deleteByURI(graphNode, objectURI);
            sparql.create(graphNode, object);
            if (childrenURIs.size() > 0) {
                sparql.insertPrimitive(graphNode, childrenURIs, Oeso.isPartOf, objectURI);
            }

            MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
            MoveModel event = moveDAO.getLastMoveEvent(objectURI);

            if (hasFacilityURI) {
                if (event != null) {
                    fillFacilityMoveEvent(event, object);
                    moveDAO.update(event);
                } else {
                    event = new MoveModel();
                    if (fillFacilityMoveEvent(event, object)) {
                        moveDAO.create(event);
                    }
                }
            } else {
                if (event != null) {
                    List<URI> newConcernedItems = new ArrayList<>();
                    for (URI item : event.getConcernedItems()) {
                        if (!SPARQLDeserializers.compareURIs(item, objectURI)) {
                            newConcernedItems.add(item);
                        }
                    }
                    if (newConcernedItems.size() == 0) {
                        moveDAO.delete(event.getUri());
                    } else {
                        event.setConcernedItems(newConcernedItems);

                        if (event.getNoSqlModel() != null) {
                            List<ConcernedItemPositionModel> newConcernedPositions = new ArrayList<>();
                            for (ConcernedItemPositionModel position : event.getNoSqlModel().getItemPositions()) {
                                if (!SPARQLDeserializers.compareURIs(position.getConcernedItem(), objectURI)) {
                                    newConcernedPositions.add(position);
                                }
                            }
                            event.getNoSqlModel().setItemPositions(newConcernedPositions);
                        }
                        moveDAO.update(event);
                    }
                }
            }
            sparql.deletePrimitives(graphNode, objectURI, Oeso.hasFacility);
            
            sparql.commitTransaction();
            nosql.commitTransaction();
        } catch (Exception ex) {
            nosql.rollbackTransaction();
            sparql.rollbackTransaction(ex);

        }

        return object.getUri();
    }

    private SPARQLResourceModel initObject(URI contextURI, URI soType, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, new URI(Oeso.ScientificObject.getURI()), currentUser.getLanguage());

        SPARQLResourceModel object = new SPARQLResourceModel();
        object.setType(soType);

        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                if (!ontologyDAO.validateObjectValue(contextURI, model, relation.getProperty(), relation.getValue(), object)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                } else if (SPARQLDeserializers.compareURIs(relation.getProperty(), Oeso.hasFacility.getURI())) {

                }
            }
        }

        object.addRelation(contextURI, new URI(RDFS.label.getURI()), String.class, name);

        return object;
    }

    public ScientificObjectModel getObjectByURI(URI objectURI, URI contextURI, UserModel currentUser) throws Exception {
        return sparql.getByURI(SPARQLDeserializers.nodeURI(contextURI), ScientificObjectModel.class, objectURI, currentUser.getLanguage());
    }

    public List<URI> getObjectContexts(URI objectURI) throws Exception {
        SelectBuilder select = new SelectBuilder();
        Node uri = SPARQLDeserializers.nodeURI(objectURI);
        Var graphVar = makeVar("graph");
        Var typeVar = makeVar("type");
        select.setDistinct(true);
        select.addVar(graphVar);
        select.addGraph(graphVar, uri, RDF.type, typeVar);
        select.addWhere(typeVar, Ontology.subClassAny, Oeso.ScientificObject);

        List<URI> resultList = new ArrayList<>();
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        sparql.executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            resultList.add(uriDeserializer.fromString((result.getStringValue("graph"))));
        }, Exception.class));

        return resultList;
    }

    public void delete(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        sparql.deleteByURI(SPARQLDeserializers.nodeURI(xpURI), objectURI);
    }

    public ScientificObjectModel getByNameAndContext(String objectName, URI contextUri) throws Exception {
        Node experimentGraph = SPARQLDeserializers.nodeURI(contextUri);

        ListWithPagination<ScientificObjectModel> searchWithPagination = sparql.searchWithPagination(
                experimentGraph,
                ScientificObjectModel.class,
                null,
                (SelectBuilder select) -> {
                    appendStrictNameFilter(select, objectName);
                }, 0, 1
        );
        if (searchWithPagination.getList().isEmpty()) {
            return null;
        }

        return searchWithPagination.getList().get(0);
    }

    public ScientificObjectModel getObjectURINameByNameAndContext(URI contextUri, String objectName) throws Exception {
        Node experimentGraph = SPARQLDeserializers.nodeURI(contextUri);

        ListWithPagination<SPARQLNamedResourceModel> searchWithPagination = sparql.searchWithPagination(
                experimentGraph,
                SPARQLNamedResourceModel.class,
                null,
                (SelectBuilder select) -> {
                    appendStrictNameFilter(select, objectName);
                }, 0, 1
        );
        if (searchWithPagination.getList().isEmpty()) {
            return null;
        }
        ScientificObjectModel experimentalObjectModel = new ScientificObjectModel();
        experimentalObjectModel.setName(searchWithPagination.getList().get(0).getName());;
        experimentalObjectModel.setUri(searchWithPagination.getList().get(0).getUri());

        return experimentalObjectModel;
    }

    public ScientificObjectModel getObjectURINameByURI(URI objectURI, URI contextURI) throws Exception {
        SPARQLNamedResourceModel ObjectURIName = sparql.getByURI(SPARQLDeserializers.nodeURI(contextURI), SPARQLNamedResourceModel.class, objectURI, null);
        ScientificObjectModel experimentalObjectModel = new ScientificObjectModel();
        experimentalObjectModel.setName(ObjectURIName.getName());;
        experimentalObjectModel.setUri(ObjectURIName.getUri());
        return experimentalObjectModel;
    }

    private void appendStrictNameFilter(SelectBuilder select, String name) throws Exception {
        select.addFilter(SPARQLQueryHelper.eq(ScientificObjectModel.NAME_FIELD, name));
    }

    public ScientificObjectModel getObjectByURI(URI objectURI, URI contextURI) throws Exception {
        List<URI> objectURIs = new ArrayList<>();

        objectURIs.add(objectURI);
        List<ScientificObjectModel> listByURIs = sparql.getListByURIs(SPARQLDeserializers.nodeURI(contextURI), ScientificObjectModel.class, objectURIs, null);
        if (listByURIs == null || listByURIs.isEmpty()) {
            return null;
        } else {
            return listByURIs.get(0);
        }
    }
}
