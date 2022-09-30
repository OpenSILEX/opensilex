/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.exception.DuplicateNameListException;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.SPARQLRelationFetcher;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeWithChildrenDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDAO {

    private final SPARQLService sparql;

    private final MongoDBService nosql;

    public static final String NON_UNIQUE_NAME_INTO_GRAPH_ERROR_MSG = "Object name <%s> must be unique onto the graph <%s>. %s has the same name";
    public static final String NON_UNIQUE_NAME_ERROR_MSG = "Object name <%s> must be unique. %s has the same name";

    private final URI defaultGraphURI;
    private final Node defaultGraphNode;


    public ScientificObjectDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;

        try{
            defaultGraphURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
            defaultGraphNode = sparql.getDefaultGraph(ScientificObjectModel.class);
        }catch (SPARQLException e){
            throw new RuntimeException(e);
        }
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

    public ListWithPagination<ScientificObjectNodeWithChildrenDTO> searchChildren(ScientificObjectSearchFilter searchFilter) throws Exception {

        if(!searchFilter.getRdfTypes().isEmpty() || !searchFilter.getFactorLevels().isEmpty() || !searchFilter.getPattern().isEmpty() && !searchFilter.getPattern().equals(".*") || searchFilter.getFacility() != null) {
            searchFilter.setOnlyFetchOsWithNoParent(false);
        } else {
            searchFilter.setOnlyFetchOsWithNoParent(true);
        }

        ListWithPagination<ScientificObjectNodeDTO> results = searchAsDto(searchFilter);
        if(results.getList().isEmpty()){
            return new ListWithPagination<>(Collections.emptyList());
        }

        List<URI> resultsUri = new ArrayList<>();
        results.getList().forEach(result ->
            resultsUri.add(result.getUri())
        );

        final Node contextNode = SPARQLDeserializers.nodeURI(searchFilter.getExperiment());
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
        addSearchfilter(count, true, searchFilter);

        List<SPARQLResult> countResult = sparql.executeSelectQuery(count);
        if (countResult.size() != 1) {
            throw new SPARQLException("Invalid count query");
        }

        return Integer.parseInt(countResult.get(0).getStringValue(countField));
    }

    private SelectBuilder getSelect(ScientificObjectSearchFilter searchFilter) throws Exception {

        SelectBuilder select = new SelectBuilder();

        addSearchfilter(select, false, searchFilter);

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

        // set default ORDER BY ?uri. Needed if we use multi-valued properties fetching
        if(CollectionUtils.isEmpty(searchFilter.getOrderByList())){
            searchFilter.setOrderByList(Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY));
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

    }

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

    }

    private static final String countField = "count";

    private void addSearchfilter(SelectBuilder builder, boolean isCount, ScientificObjectSearchFilter searchFilter) throws Exception {

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
        optionalTypeLabelHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
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
        // Only retrieve OS with no parent if getOnlyFetchOsWithNoParent() is true

        if (searchFilter.getOnlyFetchOsWithNoParent() != null && searchFilter.getOnlyFetchOsWithNoParent()) {
            if (searchFilter.getParentURI() != null) {
                builder.addGraph(contextNode, uriVar, Oeso.isPartOf, SPARQLDeserializers.nodeURI(searchFilter.getParentURI()));
            } else {
                // filter OS which have a parent
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
                graphQuery.addGraph(contextNode, uriVar, Oeso.isHosted, directFacility);
                graphQuery.addGraph(contextNode, uriVar, subPartOf, parentLinkURI);
                graphQuery.addGraph(contextNode, parentLinkURI, Oeso.isHosted, parentFacility);
                builder.addOptional(graphQuery);
            } else {
                WhereBuilder graphQuery = new WhereBuilder();
                graphQuery.addWhere(uriVar, Oeso.isHosted, directFacility);
                graphQuery.addWhere(uriVar, subPartOf, parentLinkURI);
                graphQuery.addWhere(parentLinkURI, Oeso.isHosted, parentFacility);
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

    /**
     * Check if a object with the same name already exists into objectGraph graph.
     * @param objectName name
     * @param objectGraph graph
     * @throws SPARQLException if some Exception is encountered during SPARQL query execution
     * @throws DuplicateNameException if an object with the same name already exists into objectGraph graph.
     */
    public void checkUniqueNameByGraph(URI objectGraph, String objectName, URI objectUri, boolean create) throws DuplicateNameException, SPARQLException {

        // unique name restriction only apply on some experiment graph
        if(SPARQLDeserializers.compareURIs(objectGraph, defaultGraphURI)){
            return;
        }

        ScientificObjectModel alreadyExistingOs;
        try {
            alreadyExistingOs = getByNameAndContext(objectName, objectGraph);
        }catch (Exception e){
            throw new SPARQLException(e);
        }

        // error on create if -> an already existing os has the same name
        // error on update if -> an already existing os different from <objectUri> has the same name

        if (alreadyExistingOs != null && (create || !SPARQLDeserializers.compareURIs(alreadyExistingOs.getUri(), objectUri))) {
            String errorMsg = String.format(NON_UNIQUE_NAME_INTO_GRAPH_ERROR_MSG, objectName, objectGraph.toString(), alreadyExistingOs.getUri().toString());
            throw new DuplicateNameException(errorMsg,objectName);
        }
    }

    private void checkLocalDuplicates(List<ScientificObjectModel> models) throws DuplicateNameListException{

        Set<String> uniquesNames = new HashSet<>();

        Map<String,URI> localDuplicatesByUri = new HashMap<>();
        models.forEach(model -> {
            // if name already exist, then register it as a duplicate name
            if(! uniquesNames.add(model.getName())){
                localDuplicatesByUri.put(model.getName(),model.getUri());
            }
        });

        if(!localDuplicatesByUri.isEmpty()){
            throw new DuplicateNameListException(localDuplicatesByUri);
        }
    }

    /**
     * Check into objectGraph if it exists any object with a name corresponding with a name from objects, throw {@link DuplicateNameListException} if so
     *
     * @param objects objects to check (need to have a non-null {@link SPARQLNamedResourceModel#getName()}
     * @param objectGraph graph into checking of duplicate name is done
     *
     * @throws DuplicateNameListException if at least one object from objects use a {@link SPARQLNamedResourceModel#getName()} already used by another object into objectGraph.
     * The exception has the {@link DuplicateNameListException#getExistingUriByName()} method which return association between existing name and uri.
     * @throws SPARQLException If some error is encountered during SPARQL query execution
     * @throws IllegalArgumentException if objects is null or empty or if objectGraph is null
     */
    public void checkUniqueNameByGraph(List<ScientificObjectModel> objects, URI objectGraph) throws DuplicateNameListException, SPARQLException {


        Objects.requireNonNull(objectGraph);

        // unique name restriction only apply on some experiment graph
        if(SPARQLDeserializers.compareURIs(objectGraph, defaultGraphURI)){
            return;
        }

        if(CollectionUtils.isEmpty(objects)){
            throw new IllegalArgumentException("objects is null or empty");
        }

        checkLocalDuplicates(objects);

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);

        Node graphNode = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(objectGraph.toString()));

        Stream<String> objectNames = objects.stream().map(SPARQLNamedResourceModel::getName);

        // create graph clause
        WhereBuilder graphWhere = new WhereBuilder()
                .addWhere(uriVar,RDF.type.asNode(),typeVar)
                .addWhere(uriVar,RDFS.label.asNode(),nameVar);

        SelectBuilder select = new SelectBuilder()
                .addVar(uriVar)
                .addVar(nameVar)
                .addWhere(uriVar, RDF.type.asNode(), typeVar)
                .addWhere(typeVar, RDFS.subClassOf.asNode(), Oeso.ScientificObject.asNode())
                .addGraph(graphNode, graphWhere);

        // append VALUES clause on name with objectNames
        SPARQLQueryHelper.appendValueStream(select,nameVar,objectNames);

        Map<String,URI> existingUriByName = sparql.executeSelectQueryAsStream(select).collect(
                Collectors.toMap(
                    result -> result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD), // key
                        result -> {
                            try {
                                return new URI(result.getStringValue(SPARQLResourceModel.URI_FIELD)); // value
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        (oldValue, newValue) -> oldValue, // binary operator to define merge strategy
                        PatriciaTrie::new // Efficient (in time and space) map implementation when indexing by String
                )
        );

        if(!existingUriByName.isEmpty()){
           throw new DuplicateNameListException(existingUriByName);
        }

    }

    public void create(List<ScientificObjectModel> models, URI contextURI) throws Exception {

        Objects.requireNonNull(contextURI);

        boolean useDefaultGraph = SPARQLDeserializers.compareURIs(defaultGraphNode.getURI(),contextURI);
        Node graphNode = useDefaultGraph ? defaultGraphNode : SPARQLDeserializers.nodeURI(contextURI);

        for(ScientificObjectModel model : models){

            // experimental context + no URI set
            if(! useDefaultGraph && model.getUri() == null) {

                // generate a globally unique URI
                // (by taking account of all OS into global graph, which also includes OS from any xp)
                sparql.generateUniqueURI(defaultGraphNode, model, model, true);
            }
            sparql.create(graphNode,model);
        }
    }

    /**
     *
     * @param contextURI object graph
     * @param soType object type
     * @param objectURI object uri
     * @param name object name
     * @param relations list of relations
     * @param currentUser  current user
     * @return the URI of the created object
     * @throws DuplicateNameException if some object with the same name exist into the given graph
     */
    public ScientificObjectModel create(URI contextURI, ExperimentModel experiment, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws DuplicateNameException, Exception {
        Objects.requireNonNull(contextURI);

        checkUniqueNameByGraph(contextURI,name,null,true);

        Node graphNode = SPARQLDeserializers.nodeURI(contextURI);

        ScientificObjectModel object = initObject(contextURI, experiment, soType, name, relations, currentUser);
        object.setUri(objectURI);

        try {
            sparql.startTransaction();
            nosql.startTransaction();

            // experimental context + no URI set
            if(! SPARQLDeserializers.compareURIs(defaultGraphNode.getURI(),contextURI) && objectURI == null) {

                // generate a globally unique URI
                // (by taking account of all OS into global graph, which also includes OS from any xp)
                sparql.generateUniqueURI(defaultGraphNode, object, object, true);
            }

            // if URI is already set, the service will check that URI is unique inside the provided graph
            // if the graph is global : check if OS is unique inside global graph
            // if the graph is an experiment : check if OS is unique inside experiment graph

            // if the graph is an experiment and the OS already exist into global graph -> OK, since here we consider
            // that we reuse this OS inside the experiment, so no need to performs additional checking
            sparql.create(graphNode, object);

            MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
            MoveModel facilityMoveEvent = new MoveModel();
            if (fillFacilityMoveEvent(facilityMoveEvent, object)) {
                moveDAO.create(facilityMoveEvent);
            }
            sparql.deletePrimitives(SPARQLDeserializers.nodeURI(contextURI), object.getUri(), Oeso.isHosted);
            nosql.commitTransaction();
            sparql.commitTransaction();
        } catch (Exception ex) {
            nosql.rollbackTransaction();
            sparql.rollbackTransaction(ex);
        }

        return object;
    }

    public static boolean fillFacilityMoveEvent(MoveModel facilityMoveEvent, SPARQLResourceModel object) throws Exception {
        List<URI> targets = new ArrayList<>();
        targets.add(object.getUri());
        facilityMoveEvent.setTargets(targets);

        facilityMoveEvent.setCreator(object.getCreator());

        facilityMoveEvent.setIsInstant(true);

        boolean hasFacility = false;
        for (SPARQLModelRelation relation : object.getRelations()) {
            if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.isHosted.getURI())) {
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

    /**
     *
     * @param contextURI object graph
     * @param soType object type
     * @param objectURI object uri
     * @param name object name
     * @param relations list of relations
     * @param currentUser  current user
     * @return the URI of the created object
     * @throws DuplicateNameException if some object with the same name exist into the given graph
     */
    public URI update(URI contextURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception, DuplicateNameException {

        checkUniqueNameByGraph(contextURI,name,objectURI,false);

        SPARQLResourceModel object = initObject(contextURI, null, soType, name, relations, currentUser);
        object.setUri(objectURI);

        Node graphNode = SPARQLDeserializers.nodeURI(contextURI);

        List<URI> childrenURIs = sparql.searchURIs(
                graphNode,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    select.addWhere(makeVar(SPARQLResourceModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(objectURI));
                });

        boolean hasFacilityURI = false;
        for (SPARQLModelRelation relation : object.getRelations()) {
            if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.isHosted.getURI())) {
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
            sparql.deletePrimitives(graphNode, objectURI, Oeso.isHosted);

            sparql.commitTransaction();
            nosql.commitTransaction();
        } catch (Exception ex) {
            nosql.rollbackTransaction();
            sparql.rollbackTransaction(ex);

        }

        return object.getUri();
    }

    private ScientificObjectModel initObject(URI contextURI, ExperimentModel xp, URI soType, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, new URI(Oeso.ScientificObject.getURI()), currentUser.getLanguage());

        ScientificObjectModel object = new ScientificObjectModel();
        object.setType(soType);
        object.setName(name);

        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                URI propertyShortURI = new URI(SPARQLDeserializers.getShortURI(relation.getProperty()));
                if (!ontologyDAO.validateObjectValue(contextURI, model, propertyShortURI, relation.getValue(), object)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

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

    /**
     *
     * @param objects URIs of the scientific object to check
     * @param nbObject number of scientific object (used to pass to {@link SPARQLQueryHelper#addWhereUriValues(WhereClause, String, Stream, int)}
     * @return true if any of the object from objects are involved into any experiment, false else
     * @throws SPARQLException if SPARQL query evaluation fail
     *
     * @apiNote Example of generated SPARQL query
     *
     * <pre>
     * ASK WHERE {
     *      ?type rdfs:subClassOf* vocabulary:ScientificObject
     *      GRAPH ?graph {
     *          ?uri a ?type.
     *      }
     *      FILTER (?graph != <http://www.phenome-fppn.fr/set/scientific-object>)
     *      VALUES ?uri {  test:so1 test:so2  }
     * }
     * </pre>
     *
     */
    public boolean isInvolvedIntoAnyExperiment(Stream<URI> objects, int nbObject) throws SPARQLException {

        Var uriVar = makeVar("uri");
        Var graphVar = makeVar("graph");
        Var typeVar = makeVar("type");

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        AskBuilder ask = new AskBuilder()
                .addWhere(typeVar, Ontology.subClassAny, Oeso.ScientificObject)
                .addGraph(graphVar, uriVar, RDF.type, typeVar)
                .addFilter(exprFactory.not(exprFactory.eq(graphVar, defaultGraphNode)));

        SPARQLQueryHelper.addWhereUriValues(ask, uriVar.getVarName(), objects, nbObject);
        return sparql.executeAskQuery(ask);
    }

    /**
     *
     * @param graph URI of the experiment in which we search if objects have children. Must not be null, since objects relations (except name and type) are only
     * handled into experimental context
     * @param objects URIs of the scientific object to check
     * @param nbObject number of scientific object (used to pass to {@link SPARQLQueryHelper#addWhereUriValues(WhereClause, String, Stream, int)}
     * @return true if any of the object from objects are involved has a children, false else
     * @throws SPARQLException if SPARQL query evaluation fail
     * @throws IllegalArgumentException if graph is null
     * @apiNote Example of generated SPARQL query
     *
     * <pre>
     * ASK WHERE {
     *      ?type rdfs:subClassOf* vocabulary:ScientificObject
     *      GRAPH test:experiment_graph {
     *          ?uri a ?type.
     *          ?children vocabulary:isPartOf ?uri
     *      }
     *      VALUES ?uri {  test:so1 test:so2  }
     * }
     * </pre>
     *
     */
    public boolean hasChildren(URI graph, Stream<URI> objects, int nbObject) throws SPARQLException {

        Objects.requireNonNull(graph);

        Var uriVar = makeVar("uri");
        Node graphNode = NodeFactory.createURI(URIDeserializer.getExpandedURI(graph.toString()));
        Var typeVar = makeVar("type");
        Var childVar = makeVar("children");

        AskBuilder ask = new AskBuilder()
                .addWhere(typeVar, Ontology.subClassAny, Oeso.ScientificObject)
                .addGraph(graphNode, new WhereBuilder()
                        .addWhere(uriVar, RDF.type, typeVar)
                        .addWhere(childVar, Oeso.isPartOf, uriVar));

        SPARQLQueryHelper.addWhereUriValues(ask, uriVar.getVarName(), objects, nbObject);
        return sparql.executeAskQuery(ask);
    }

    /**
     * Remove an OS from a graph
     * @param xpURI URI of the experiment in which the object is located. If null then the objectURI is deleted from the global os graph
     * @param objectURI URI of the object to delete
     * @throws Exception if some error is encountered during suppression of the object from the triplestore
     * @throws IllegalArgumentException if objectURI is null
     */
    public void delete(URI xpURI, URI objectURI) throws Exception {
        Objects.requireNonNull(objectURI);
        if(xpURI == null){
            sparql.deleteByURI(defaultGraphNode,objectURI);
        }else{
            sparql.deleteByURI(SPARQLDeserializers.nodeURI(xpURI), objectURI);
        }
    }

    /**
     *
     * @param objectName name
     * @param objectGraph graph
     * @return a ScientificObjectModel which have the given name and which is stored into objectGraph, return null if no object is found
     * @throws Exception if some Exception is encountered during SPARQL query execution
     * @throws DuplicateNameException if multiple object are found with objectName as name into objectGraph
     */
    public ScientificObjectModel getByNameAndContext(String objectName, URI objectGraph) throws Exception, DuplicateNameException {
        Node experimentGraph = SPARQLDeserializers.nodeURI(objectGraph);

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

    public ScientificObjectModel getObjectURINameByURI(URI objectURI, URI contextURI) throws Exception {
        SPARQLNamedResourceModel ObjectURIName = sparql.getByURI(SPARQLDeserializers.nodeURI(contextURI), SPARQLNamedResourceModel.class, objectURI, null);
        ScientificObjectModel experimentalObjectModel = new ScientificObjectModel();
        experimentalObjectModel.setName(ObjectURIName.getName());
        experimentalObjectModel.setUri(ObjectURIName.getUri());
        return experimentalObjectModel;
    }

    private void appendStrictNameFilter(SelectBuilder select, String name) throws Exception {
        select.addFilter(SPARQLQueryHelper.eq(ScientificObjectModel.NAME_FIELD, name));
    }

     
    public Collection<String> getScientificObjectsByDate(URI contextURI, String startDate, String endDate, Collection<URI> uris) throws Exception {
        Var uriVar = makeVar("uri");
        Var typeVar = makeVar("type");
        Var creationDateVar = makeVar("creationDate");
        Var destructionDateVar = makeVar("destructionDate");
        SelectBuilder select = new SelectBuilder();

        Node context = SPARQLDeserializers.nodeURI(contextURI);
        select.addGraph(context, uriVar, RDF.type, typeVar);
        select.addOptional(uriVar, Oeso.hasCreationDate, creationDateVar);
        select.addOptional(uriVar, Oeso.hasDestructionDate, destructionDateVar);
        LocalDate start = startDate == null ? null : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? null : LocalDate.parse(endDate);
        Expr expr = dateRange("creationDate", start, "destructionDate", end);
        select.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, uris));
        select.addFilter(expr);

        Collection<String> types = new HashSet<>();
        sparql.executeSelectQuery(select, (row) -> {
            try {
                URI uri = new URI(row.getStringValue("uri"));
                types.add(SPARQLDeserializers.getShortURI(uri));
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });
        return types;
    }
    
    public static Expr dateRange(String startDateVarName, Object startDate, String endDateVarName, Object endDate) throws Exception {
        
        Node startVar = NodeFactory.createVariable(startDateVarName);
        Node endVar = NodeFactory.createVariable(endDateVarName);
        DateDeserializer deserializer = new DateDeserializer();
        
        Expr range = null;
        Expr notStartBounded = SPARQLQueryHelper.getExprFactory().not(SPARQLQueryHelper.getExprFactory().bound(startVar));
        Expr notEndBounded = SPARQLQueryHelper.getExprFactory().not(SPARQLQueryHelper.getExprFactory().bound(endVar));
        
        if (endDate == null) {
            range = SPARQLQueryHelper.getExprFactory().ge(startVar, deserializer.getNode(startDate)); // constructionDate > startDate
        } else if (startDate == null) {
            range = SPARQLQueryHelper.getExprFactory().le(startVar, deserializer.getNode(endDate));  // constructionDate < endDate
        } else {
            LocalDate start = LocalDate.parse(startDate.toString());
            LocalDate end = LocalDate.parse(endDate.toString());
            Expr range1 = SPARQLQueryHelper.dateRange(endDateVarName, start, startDateVarName, end); // If destructionDate > startDate && creationDate < endDate

            Expr range2 = SPARQLQueryHelper.getExprFactory().ge(endVar, deserializer.getNode(startDate)); // If destructionDate > startDate
            Expr range3 = SPARQLQueryHelper.getExprFactory().and(range2, notStartBounded); // AND creationDate does not exist

            Expr range4 = SPARQLQueryHelper.getExprFactory().or(range1, notEndBounded); // range1 OR destructionDate does not exist
            range = SPARQLQueryHelper.getExprFactory().or(range4, range3); // range1 OR destructionDate does not exist OR creationDate does not exist AND destructionDate > startDate
        }
        Expr notBoundedDates = SPARQLQueryHelper.getExprFactory().and(notStartBounded, notEndBounded); // If SO has no creation and destruction dates
        Expr res = SPARQLQueryHelper.getExprFactory().or(notBoundedDates, range);
        return res;
    }
}
