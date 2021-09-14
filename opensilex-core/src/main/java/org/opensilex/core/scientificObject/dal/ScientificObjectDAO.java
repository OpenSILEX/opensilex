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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.SPARQLRelationFetcher;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeWithChildrenDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;

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
        return searchByURIs(contextURI,objectsURI,currentUser,false);
    }

    public List<ScientificObjectModel> searchByURIs(URI contextURI, List<URI> objectsURI, UserModel currentUser, boolean loadChildren) throws Exception {

        Map<String,Boolean> fieldsToFetch = new HashMap<>();
        fieldsToFetch.put(ScientificObjectModel.FACTOR_LEVEL_FIELD,true);

        if(loadChildren){
            fieldsToFetch.put(SPARQLTreeModel.CHILDREN_FIELD,true);
        }

        return sparql.loadListByURIs(
                SPARQLDeserializers.nodeURI(contextURI),
                ScientificObjectModel.class,
                objectsURI,
                currentUser.getLanguage(),
                fromResult(currentUser.getLanguage()),

                // if object children must be retrieved later, then don't use listFetcher since it doesn't handle children properties (eg : name)
                loadChildren ? null : fieldsToFetch
        );
    }

    public ListWithPagination<ScientificObjectNodeWithChildrenDTO> searchChildren(URI contextURI, URI parentURI, URI facility, List<OrderBy> orderByList, Integer page, Integer pageSize, UserModel currentUser) throws Exception {

        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(contextURI)
                .setParentURI(parentURI)
                .setFacility(facility);

        searchFilter.setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList)
                .setLang(currentUser.getLanguage());

        ListWithPagination<ScientificObjectNodeDTO> results = searchAsDto(searchFilter);

        List<URI> resultsUri = new ArrayList<>();
        results.getList().forEach(result -> {
            resultsUri.add(result.getUri());
        });

        final Node contextNode;
        if (contextURI != null) {
            contextNode = SPARQLDeserializers.nodeURI(contextURI);
        } else {
            contextNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ScientificObjectModel.class));
        }

        SelectBuilder childCountByUri = new SelectBuilder();

        Var uriVar = makeVar("uri");
        Var childUriVar = makeVar("child_uri");
        Var childCountVar = makeVar("child_count");

        childCountByUri.addVar(uriVar);
        childCountByUri.addVar("(COUNT(DISTINCT ?child_uri))", childCountVar);
        childCountByUri.addGraph(contextNode, childUriVar, Oeso.isPartOf, uriVar);
        childCountByUri.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, resultsUri));
        childCountByUri.addGroupBy(uriVar);
        ExprFactory positiveCount = new ExprFactory();
        childCountByUri.addHaving(positiveCount.gt(childCountVar, 0));

        Map<String, Integer> childCountByParent = new HashMap<>();
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        sparql.executeSelectQuery(childCountByUri, (row) -> {
            try {
                String formatedUriString = uriDeserializer.fromString(row.getStringValue("uri")).toString();
                childCountByParent.put(formatedUriString, Integer.parseInt(row.getStringValue("child_count")));
            } catch (Exception ex) {
                throw new RuntimeException("Invalid URI returned by query, should never happend", ex);
            }

        });

        return results.convert(ScientificObjectNodeWithChildrenDTO.class, (nodeDTO) -> {
            ScientificObjectNodeWithChildrenDTO dto = new ScientificObjectNodeWithChildrenDTO();
            dto.setUri(nodeDTO.getUri());
            dto.setName(nodeDTO.getName());
            dto.setType(nodeDTO.getType());
            dto.setTypeLabel(nodeDTO.getTypeLabel());
            dto.setCreationDate(nodeDTO.getCreationDate());
            dto.setDestructionDate(nodeDTO.getDestructionDate());

            Integer childCount = childCountByParent.get(nodeDTO.getUri().toString());

            if (childCount == null) {
                childCount = 0;
            }
            dto.setChildCount(childCount);

            return dto;
        });
    }

    public Map<String, List<FactorLevelModel>> getScientificObjectsFactors(URI experimentURI, Collection<URI> objectsURI, String lang) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = sparql.getMapperIndex();
        String language;
        if (lang == null) {
            language = sparql.getDefaultLang();
        } else {
            language = lang;
        }

        SPARQLClassObjectMapper<FactorLevelModel> mapper = mapperIndex.getForClass(FactorLevelModel.class);
        Node graph = mapper.getDefaultGraph();
        Var soVar = makeVar("_so_uri");
        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        SelectBuilder select = sparql.getSelectBuilder(mapper, graph, language, (builder) -> {
            builder.addVar(soVar.getVarName());
            builder.addGraph(experimentGraph, soVar, Oeso.hasFactorLevel, makeVar(FactorLevelModel.URI_FIELD));
            builder.addFilter(SPARQLQueryHelper.inURIFilter(soVar, objectsURI));
        }, null,null, null, null);

        Map<String, List<FactorLevelModel>> resultMap = new HashMap<>();
        Map<String, FactorLevelModel> loadedFactors = new HashMap<>();

        sparql.executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            String expandedFactorURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(FactorLevelModel.URI_FIELD));
            if (!loadedFactors.containsKey(expandedFactorURI)) {
                loadedFactors.put(expandedFactorURI, mapper.createInstance(graph, result, language, sparql));
            }
            String expandedSoURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(soVar.getVarName()));
            if (!resultMap.containsKey(expandedSoURI)) {
                resultMap.put(expandedSoURI, new ArrayList<>());
            }
            resultMap.get(expandedSoURI).add(loadedFactors.get(expandedFactorURI));
        }, Exception.class));

        return resultMap;
    }


    private int getCount(ScientificObjectSearchFilter searchFilter) throws Exception {

        SelectBuilder count = new SelectBuilder();
        addSearchfilter(count, true, searchFilter,false);

        List<SPARQLResult> countResult = sparql.executeSelectQuery(count);
        if (countResult.size() != 1) {
            throw new SPARQLException("Invalid count query");
        }

        return Integer.parseInt(countResult.get(0).getStringValue(countField));
    }

    private SelectBuilder getSelect(ScientificObjectSearchFilter searchFilter) throws Exception {

        SelectBuilder select = new SelectBuilder();

        addSearchfilter(select, false, searchFilter,false);

        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = sparql.getMapperIndex().getForClass(ScientificObjectModel.class);
        if (searchFilter.getOrderByList() != null) {
            searchFilter.getOrderByList() .forEach((OrderBy orderBy) -> {
                Expr fieldOrderExpr = mapper.getFieldOrderExpr(orderBy.getFieldName());
                if (fieldOrderExpr != null) {
                    select.addOrderBy(fieldOrderExpr, orderBy.getOrder());
                }
            });
        }

        if (searchFilter.getPage() == null || searchFilter.getPage()  < 0) {
            searchFilter.setPage(0);
        }
        if (searchFilter.getPageSize() != null && searchFilter.getPageSize() > 0) {
            select.setOffset(searchFilter.getPage() * searchFilter.getPageSize());
            select.setLimit(searchFilter.getPageSize());
        }

       return select;
    }

    private static <RT> List<RT> streamToList(Stream<SPARQLResult> resultStream, ThrowingFunction<SPARQLResult, RT, Exception> resultHandler) {

        return resultStream.map(
                result -> {
                    try {
                        return resultHandler.apply(result);
                    } catch (Exception e) {
                        throw new RuntimeException(new SPARQLException(e));
                    }
                }
        ).collect(Collectors.toCollection(ArrayList::new));
    }



    public ListWithPagination<ScientificObjectModel> search(ScientificObjectSearchFilter searchFilter, Collection<String> fieldsToFetch) throws Exception {

        int total = getCount(searchFilter);
        if (total == 0) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        SelectBuilder select = getSelect(searchFilter);
        Stream<SPARQLResult> resultStream = sparql.executeSelectQueryAsStream(select);

        if(resultStream == null){
            return new ListWithPagination<>(Collections.emptyList());
        }

        List<ScientificObjectModel> results = streamToList(resultStream,fromResult(searchFilter.getLang()));

        if(! CollectionUtils.isEmpty(fieldsToFetch)){

            Map<String,Boolean> fieldToFetchMap = fieldsToFetch.stream().collect(Collectors.toMap(Function.identity(),uri -> true));

            // if object children must be retrieved later, then don't use listFetcher since it doesn't handle children properties (eg : name)
            SPARQLListFetcher<ScientificObjectModel> listFetcher = new SPARQLListFetcher<>(
                    sparql,
                    ScientificObjectModel.class,
                    SPARQLDeserializers.nodeURI(searchFilter.getExperiment()),
                    fieldToFetchMap,
                    select,
                    results
            );
            listFetcher.updateModels();
        }

        SPARQLRelationFetcher<ScientificObjectModel> relationFetcher = new SPARQLRelationFetcher<>(
                sparql,
                ScientificObjectModel.class,
                SPARQLDeserializers.nodeURI(searchFilter.getExperiment()),
                select,
                results
        );

        relationFetcher.updateModels();

        // handle object property fetching + uri list selection
        return new ListWithPagination<>(results,searchFilter.getPage(),searchFilter.getPageSize(),total);
    }


    public ListWithPagination<ScientificObjectNodeDTO> searchAsDto(
            ScientificObjectSearchFilter searchFilter) throws Exception {

        int total = getCount(searchFilter);
        if (total == 0) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        SelectBuilder select = getSelect(searchFilter);
        Stream<SPARQLResult> resultStream = sparql.executeSelectQueryAsStream(select);
        if(resultStream == null){
            return new ListWithPagination<>(Collections.emptyList());
        }

        List<ScientificObjectNodeDTO> results = streamToList(resultStream,dtoFromResult());

        return new ListWithPagination<>(results,searchFilter.getPage(),searchFilter.getPageSize(),total);
    }

    private ThrowingFunction<SPARQLResult,ScientificObjectModel,Exception> fromResult(String lang) throws Exception{

        SPARQLDeserializer<LocalDate> dateDeserializer = SPARQLDeserializers.getForClass(LocalDate.class);
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);

        return (result) -> {
            ScientificObjectModel model = new ScientificObjectModel();

            model.setUri(uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            model.setType(uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));

            SPARQLLabel typeLabel = new SPARQLLabel();
            typeLabel.setDefaultLang(StringUtils.isEmpty(lang) ? OpenSilex.DEFAULT_LANGUAGE : lang);
            typeLabel.setDefaultValue(result.getStringValue(SPARQLResourceModel.TYPE_NAME_FIELD));
            model.setTypeLabel(typeLabel);

            model.setName(result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));
            model.setCreationDate(dateDeserializer.fromString(result.getStringValue(ScientificObjectModel.CREATION_DATE_FIELD)));
            model.setDestructionDate(dateDeserializer.fromString(result.getStringValue(ScientificObjectModel.DESTRUCTION_DATE_FIELD)));

            String parentUri = result.getStringValue(SPARQLTreeModel.PARENT_FIELD);
            if(parentUri != null){
                model.setParent(new ScientificObjectModel());
                model.getParent().setUri(new URI(parentUri));
            }
            return model;
        };

    };

    private ThrowingFunction<SPARQLResult,ScientificObjectNodeDTO,Exception> dtoFromResult() throws Exception{

        SPARQLDeserializer<LocalDate> dateDeserializer = SPARQLDeserializers.getForClass(LocalDate.class);
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);

        return (result) -> {
            ScientificObjectNodeDTO dto = new ScientificObjectNodeDTO();

            dto.setUri(uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            dto.setType(uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));

            dto.setTypeLabel(result.getStringValue(SPARQLResourceModel.TYPE_NAME_FIELD));

            dto.setName(result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));
            dto.setCreationDate(dateDeserializer.fromString(result.getStringValue(ScientificObjectModel.CREATION_DATE_FIELD)));
            dto.setDestructionDate(dateDeserializer.fromString(result.getStringValue(ScientificObjectModel.DESTRUCTION_DATE_FIELD)));

            return dto;
        };

    };

    private static String countField = "count";

    private void addSearchfilter(SelectBuilder builder, boolean isCount, ScientificObjectSearchFilter searchFilter, boolean onlyDirectChildren) throws Exception {

        final Node contextNode;
        if (searchFilter.getExperiment() != null) {
            contextNode = SPARQLDeserializers.nodeURI(searchFilter.getExperiment());
        } else {
            contextNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ScientificObjectModel.class));
        }

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var typeNameVar = makeVar(SPARQLResourceModel.TYPE_NAME_FIELD);
        Var parentVar = makeVar(SPARQLTreeModel.PARENT_FIELD);
        Var creationDateVar = makeVar(ScientificObjectModel.CREATION_DATE_FIELD);
        Var destructionDateVar = makeVar(ScientificObjectModel.DESTRUCTION_DATE_FIELD);

        // Define request var
        if (!isCount) {
            builder.addVar(uriVar);
            builder.addVar(nameVar);
            builder.addVar(typeVar);
            builder.addVar(typeNameVar);
            builder.addVar(parentVar);
            builder.addVar(creationDateVar);
            builder.addVar(destructionDateVar);
        } else {
            builder.addVar("(COUNT(DISTINCT ?" + SPARQLResourceModel.URI_FIELD + "))", makeVar(countField));
        }

        // Add label and type in where clause
        WhereBuilder graphHandler = new WhereBuilder();
        builder.addWhere(typeVar, Ontology.subClassAny, Oeso.ScientificObject);
        graphHandler.addWhere(uriVar, RDFS.label, nameVar);
        graphHandler.addWhere(uriVar, RDF.type, typeVar);

        // Add rdf type label in where clause
        WhereHandler optionalTypeLabelHandler = new WhereHandler();
        optionalTypeLabelHandler.addWhere(builder.makeTriplePath(typeVar, RDFS.label, typeNameVar));
        // Add rdf type label lang filter
        Locale locale = Locale.forLanguageTag(searchFilter.getLang());
        optionalTypeLabelHandler.addFilter(SPARQLQueryHelper.langFilter(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
        builder.getWhereHandler().addOptional(optionalTypeLabelHandler);

        // Add creation and destruction date as optional fields
        graphHandler.addOptional(uriVar, Oeso.isPartOf, parentVar);
        graphHandler.addOptional(uriVar, Oeso.hasCreationDate, creationDateVar);
        graphHandler.addOptional(uriVar, Oeso.hasDestructionDate, destructionDateVar);

        // add VALUES clause with included uris
        if(!CollectionUtils.isEmpty(searchFilter.getUris())){
            Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(searchFilter.getUris());
            builder.addValueVar(SPARQLResourceModel.URI_FIELD, uriNodes);
        }
        // add NOT IN filter with excluded uris
        if(!CollectionUtils.isEmpty(searchFilter.getExcludedUris())){
            Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(searchFilter.getExcludedUris());
            Expr notInFilter = SPARQLQueryHelper.getExprFactory().notin(SPARQLResourceModel.URI_FIELD,uriNodes);
            graphHandler.addFilter(notInFilter);
        }

        builder.addGraph(contextNode, graphHandler);

        // Add pattern filter
        if (searchFilter.getPattern() != null && !searchFilter.getPattern().trim().isEmpty()) {
            builder.addFilter(SPARQLQueryHelper.regexFilter(ScientificObjectModel.NAME_FIELD, searchFilter.getPattern()));
        }

        // Add rdf type filter
        if (searchFilter.getRdfTypes() != null && searchFilter.getRdfTypes().size() > 0) {
            builder.addFilter(SPARQLQueryHelper.inURIFilter(ScientificObjectModel.TYPE_FIELD, searchFilter.getRdfTypes()));
        }

        // Add parent filter
        if (onlyDirectChildren) {
            if (searchFilter.getParentURI() != null) {
                builder.addGraph(contextNode, uriVar, Oeso.isPartOf, SPARQLDeserializers.nodeURI(searchFilter.getParentURI()));
            } else {
                WhereBuilder whereFilter = new WhereBuilder().addGraph(
                        contextNode,
                        uriVar,
                        Oeso.isPartOf.asNode(),
                        makeVar("parentURI")
                );
                builder.addFilter(SPARQLQueryHelper.getExprFactory().notexists(whereFilter));
            }
        } else if (searchFilter.getParentURI() != null) {
            Path deepPartOf = new P_OneOrMore1(new P_Link(Oeso.isPartOf.asNode()));
            builder.addGraph(contextNode, uriVar, deepPartOf, SPARQLDeserializers.nodeURI(searchFilter.getParentURI()));
        }

        // Add factor level filter
        if (! CollectionUtils.isEmpty(searchFilter.getFactorLevels())) {
            Var factorLevelVar = makeVar("__factorLevel");
            if (searchFilter.getExperiment() != null) {
                builder.addGraph(contextNode, uriVar, Oeso.hasFactorLevel, factorLevelVar);
            } else {
                builder.addWhere(uriVar, Oeso.hasFactorLevel, factorLevelVar);
            }

            builder.addFilter(SPARQLQueryHelper.inURIFilter(factorLevelVar, searchFilter.getFactorLevels()));
        }

        // Add germplasm filter
        if (searchFilter.getGermplasm() != null) {
            Node germplasmNode = SPARQLDeserializers.nodeURI(searchFilter.getGermplasm());
            builder.addWhere(
                    new WhereBuilder()
                            .addWhere(uriVar, Oeso.hasGermplasm, germplasmNode)
                            .addUnion(new WhereBuilder()
                                    .addWhere(uriVar, Oeso.hasGermplasm, makeVar("_g1"))
                                    .addWhere(makeVar("_g1"), Oeso.fromSpecies, germplasmNode))
                            .addUnion(new WhereBuilder()
                                    .addWhere(uriVar, Oeso.hasGermplasm, makeVar("_g2"))
                                    .addWhere(makeVar("_g2"), Oeso.fromVariety, germplasmNode))
                            .addUnion(new WhereBuilder()
                                    .addWhere(uriVar, Oeso.hasGermplasm, makeVar("_g3"))
                                    .addWhere(makeVar("_g3"), Oeso.fromAccession, germplasmNode)));

        }

        // Add facility filter
        if (searchFilter.getFacility() != null) {
            Node facilityNode = SPARQLDeserializers.nodeURI(searchFilter.getFacility());
            Var directFacility = makeVar("__directFacility");
            Var parentLinkURI = makeVar("__parentLinkURI");
            Var parentFacility = makeVar("__parentFacility");
            Path subPartOf = new P_ZeroOrMore1(new P_Link(Oeso.isPartOf.asNode()));
            if (searchFilter.getExperiment() != null) {
                WhereBuilder graphQuery = new WhereBuilder();
                graphQuery.addGraph(contextNode, uriVar, Oeso.hasFacility, directFacility);
                graphQuery.addGraph(contextNode, uriVar, subPartOf, parentLinkURI);
                graphQuery.addGraph(contextNode, parentLinkURI, Oeso.hasFacility, parentFacility);
                builder.addOptional(graphQuery);
            } else {
                WhereBuilder graphQuery = new WhereBuilder();
                graphQuery.addWhere(uriVar, Oeso.hasFacility, directFacility);
                graphQuery.addWhere(uriVar, subPartOf, parentLinkURI);
                graphQuery.addWhere(parentLinkURI, Oeso.hasFacility, parentFacility);
                builder.addOptional(graphQuery);
            }

            builder.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq("__directFacility", facilityNode),
                    SPARQLQueryHelper.eq("__parentFacility", facilityNode)
            ));
        }

        // Add filter to check if object exists at given date
        DateDeserializer dateDeserializer = new DateDeserializer();
        ExprFactory exprFactory = new ExprFactory();
        if (searchFilter.getExistenceDate() != null) {

            WhereBuilder optionalDestructionDate = new WhereBuilder();
            optionalDestructionDate.addWhere(uriVar, Oeso.hasDestructionDate, destructionDateVar);
            builder.addFilter(
                    exprFactory.and(
                            exprFactory.le(creationDateVar, dateDeserializer.getNode(searchFilter.getExistenceDate())),
                            exprFactory.or(
                                    exprFactory.not(exprFactory.exists(optionalDestructionDate)),
                                    exprFactory.ge(destructionDateVar, dateDeserializer.getNode(searchFilter.getExistenceDate()))
                            )
                    )
            );
        }

        // Add filter for creation date
        if (searchFilter.getCreationDate() != null) {
            builder.addFilter(exprFactory.eq(creationDateVar, dateDeserializer.getNode(searchFilter.getCreationDate())));
        }
    }

    public URI create(URI contextURI, URI uriGenerationPrefix, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {

        SPARQLResourceModel object = initObject(contextURI, soType, name, relations, currentUser);

        if (objectURI == null) {
            ScientificObjectURIGenerator uriGenerator = new ScientificObjectURIGenerator(uriGenerationPrefix);
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
        List<URI> targets = new ArrayList<>();
        targets.add(object.getUri());
        facilityMoveEvent.setTargets(targets);

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
                    List<URI> newTargets = new ArrayList<>();
                    for (URI item : event.getTargets()) {
                        if (!SPARQLDeserializers.compareURIs(item, objectURI)) {
                            newTargets.add(item);
                        }
                    }
                    if (newTargets.size() == 0) {
                        moveDAO.delete(event.getUri());
                    } else {
                        event.setTargets(newTargets);

                        if (event.getNoSqlModel() != null) {
                            List<TargetPositionModel> newTargetsPositions = new ArrayList<>();
                            for (TargetPositionModel position : event.getNoSqlModel().getTargetPositions()) {
                                if (!SPARQLDeserializers.compareURIs(position.getTarget(), objectURI)) {
                                    newTargetsPositions.add(position);
                                }
                            }
                            event.getNoSqlModel().setTargetPositions(newTargetsPositions);
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

    public ScientificObjectModel getObjectByURI(URI objectURI, URI contextURI, String lang) throws Exception {
        return sparql.getByURI(SPARQLDeserializers.nodeURI(contextURI), ScientificObjectModel.class, objectURI, lang);
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
                },
                null,
                null,
                null,
                0, 1
        );
        if (searchWithPagination.getList().isEmpty()) {
            return null;
        }
        
        if (searchWithPagination.getList().size() > 1) {
            throw new DuplicateNameException(objectName);
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
                },
                null,
                null,
                null,
                0, 1
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

//    public ScientificObjectModel getObjectByURI(URI objectURI, URI contextURI) throws Exception {
//        List<URI> objectURIs = new ArrayList<>();
//
//        objectURIs.add(objectURI);
//        List<ScientificObjectModel> listByURIs = sparql.getListByURIs(SPARQLDeserializers.nodeURI(contextURI), ScientificObjectModel.class, objectURIs, null);
//        if (listByURIs == null || listByURIs.isEmpty()) {
//            return null;
//        } else {
//            return listByURIs.get(0);
//        }
//    }
}
