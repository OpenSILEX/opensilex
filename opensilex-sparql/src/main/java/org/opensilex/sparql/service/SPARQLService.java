//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.opensilex.service.Service;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.*;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.ThrowingConsumer;
import org.opensilex.utils.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.opensilex.OpenSilex;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Implementation of SPARQLService
 */
@ServiceDefaultDefinition(config = SPARQLServiceConfig.class)
public class SPARQLService extends BaseService implements SPARQLConnection, Service, AutoCloseable {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLService.class);

    public final static String DEFAULT_SPARQL_SERVICE = "sparql";
    private final SPARQLConnection connection;

    public SPARQLService(SPARQLServiceConfig config) {
        super(config);
        this.connection = config.connection();
    }

    public SPARQLService(SPARQLConnection connection) {
        super(null);
        this.connection = connection;
    }

    private String defaultLang = OpenSilex.DEFAULT_LANGUAGE;

    public void setDefaultLang(String lang) {
        this.defaultLang = lang;
    }

    public String getDefaultLang() {
        return this.defaultLang;
    }

    @Override
    public void setup() throws Exception {
        connection.setOpenSilex(getOpenSilex());
        connection.setMapperIndex(getMapperIndex());
        connection.setup();
    }

    @Override
    public void clean() throws Exception {
        connection.clean();
    }

    @Override
    public void startup() throws Exception {
        connection.startup();
    }

    @Override
    public void shutdown() throws Exception {
        connection.shutdown();
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }

    private static HashMap<String, String> getDefaultPrefixes() {
        return new HashMap<String, String>() {
            {
                put(RDFS.PREFIX, RDFS.NAMESPACE);
                put(FOAF.PREFIX, FOAF.NAMESPACE);
                put("dc", DCTerms.NS);
                put(OWL.PREFIX, OWL.NAMESPACE);
                put(XSD.PREFIX, XSD.NAMESPACE);
            }
        };
    }

    private static HashMap<String, String> prefixes = getDefaultPrefixes();

    public static void addPrefix(String prefix, String namespace) {
        prefixes.put(prefix, namespace);
    }

    public static Map<String, String> getPrefixes() {
        return prefixes;
    }

    public static PrefixMapping getPrefixMapping() {
        return new SPARQLPrefixMapping().setNsPrefixes(prefixes);
    }

    private static void addPrefixes(UpdateBuilder builder) {
        builder.addPrefixes(getPrefixMapping());

    }

    private static void addPrefixes(AbstractQueryBuilder<?> builder) {
        builder.addPrefixes(getPrefixMapping());
    }

    public static void clearPrefixes() {
        prefixes = getDefaultPrefixes();
    }

    @Override
    public boolean executeAskQuery(AskBuilder ask) throws SPARQLException {
        addPrefixes(ask);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL ASK\n" + ask.buildString());
        }
        return connection.executeAskQuery(ask);
    }

    @Override
    public List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLException {
        addPrefixes(describe);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL DESCRIBE\n" + describe.buildString());
        }
        return connection.executeDescribeQuery(describe);
    }

    public List<SPARQLStatement> describe(Node graph, URI uri) throws SPARQLException {
        DescribeBuilder describe = new DescribeBuilder();
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        describe.addVar(uriVar);
        if (graph != null) {
            describe.from(graph.getURI());
        }
        describe.addBind(new ExprFactory().iri(SPARQLDeserializers.getExpandedURI(uri.toString())), uriVar);
        return executeDescribeQuery(describe);
    }

    @Override
    public List<SPARQLStatement> getGraphStatement(URI graph) throws SPARQLException {
        LOGGER.debug("SPARQL GET GRAPH STATEMENTS FOR: " + graph);
        return connection.getGraphStatement(graph);
    }

    @Override
    public List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLException {
        addPrefixes(construct);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL CONSTRUCT\n" + construct.buildString());
        }
        return connection.executeConstructQuery(construct);
    }

    @Override
    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLException {
        addPrefixes(select);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL SELECT\n" + select.buildString());
        }
        return connection.executeSelectQuery(select, resultHandler);
    }

    @Override
    public Stream<SPARQLResult> executeSelectQueryAsStream(SelectBuilder select) throws SPARQLException {
        addPrefixes(select);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL SELECT\n" + select.buildString());
        }
        return connection.executeSelectQueryAsStream(select);
    }

    @Override
    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLException {
        addPrefixes(update);
//        if (LOGGER.isDebugEnabled()) {
//            LOGGER.debug("SPARQL UPDATE\n" + update.build().toString());
//        }
        connection.executeUpdateQuery(update);
    }

    @Override
    public void executeDeleteQuery(UpdateBuilder delete) throws SPARQLException {
        addPrefixes(delete);
//        if (LOGGER.isDebugEnabled()) {
//            LOGGER.debug("SPARQL DELETE\n" + delete.buildRequest().toString());
//        }
        connection.executeDeleteQuery(delete);
    }

    private int transactionLevel = 0;

    @Override
    public void startTransaction() throws SPARQLException {
        if (transactionLevel == 0) {
            LOGGER.debug("SPARQL TRANSACTION START");
            connection.startTransaction();
        }
        transactionLevel++;
    }

    @Override
    public void commitTransaction() throws SPARQLException {
        transactionLevel--;
        if (transactionLevel == 0) {
            LOGGER.debug("SPARQL TRANSACTION COMMIT");
            connection.commitTransaction();
        }
    }

    @Override
    public void rollbackTransaction(Exception ex) throws Exception {
        if (transactionLevel != 0) {
            LOGGER.error("SPARQL TRANSACTION ROLLBACK: ", ex);
            transactionLevel = 0;
            connection.rollbackTransaction(ex);
        }
    }

    public void rollbackTransaction() throws Exception {
        rollbackTransaction(null);
    }

    @Override
    public void clearGraph(URI graph) throws SPARQLException {
        LOGGER.debug("SPARQL CLEAR GRAPH: " + graph);
        connection.clearGraph(graph);
    }

    public void clearGraph(String graph) throws SPARQLException, URISyntaxException {
        LOGGER.debug("SPARQL CLEAR GRAPH: " + graph);
        connection.clearGraph(new URI(graph));
    }

    @Override
    public void renameGraph(URI oldGraphURI, URI newGraphURI) throws SPARQLException {
        URI fullOldURI;
        try {
            fullOldURI = new URI(SPARQLDeserializers.getExpandedURI(oldGraphURI));
        } catch (URISyntaxException ex) {
            throw new SPARQLInvalidURIException("Invalid origin graph URI", oldGraphURI);
        }

        URI fullNewURI;
        try {
            fullNewURI = new URI(SPARQLDeserializers.getExpandedURI(newGraphURI));
        } catch (URISyntaxException ex) {
            throw new SPARQLInvalidURIException("Invalid destination graph URI", newGraphURI);
        }

        boolean isShaclEnabled = isShaclEnabled();
        if (isShaclEnabled) {
            disableSHACL();
        }
        LOGGER.debug("MOVE GRAPH " + fullOldURI + " TO " + fullNewURI);
        connection.renameGraph(fullOldURI, fullNewURI);
        if (isShaclEnabled) {
            enableSHACL();
        }
    }

    @Override
    public void clear() throws SPARQLException {
        LOGGER.debug("SPARQL CLEAR REPOSITORY");
        connection.clear();
    }

    public void loadOntology(URI graph, InputStream ontology, Lang format) throws SPARQLException {
        LOGGER.debug("SPARQL LOAD " + format.getName() + " FILE INTO GRAPH: " + graph.toString());
        connection.loadOntology(graph, ontology, format);
    }

    public <T extends SPARQLResourceModel> T getByURI(Class<T> objectClass, URI uri, String lang) throws Exception {
        return getByURI(getDefaultGraph(objectClass), objectClass, uri, lang, true);
    }

    public <T extends SPARQLResourceModel> T getByURI(Node graph, Class<T> objectClass, URI uri, String lang) throws Exception {
        return getByURI(graph, objectClass, uri, lang, false);
    }

    public <T extends SPARQLResourceModel> T getByURI(Node graph, Class<T> objectClass, URI uri, String lang, boolean useDefaultGraph) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        T instance = mapper.createInstance(graph, uri, lang, useDefaultGraph, this);
        return instance;
    }

    public <T extends SPARQLResourceModel> List<T> getListByURIs(Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        return getListByURIs(getDefaultGraph(objectClass), objectClass, uris, lang,null);
    }

    public <T extends SPARQLResourceModel> List<T> getListByURIs(Node graph, Class<T> objectClass, Collection<URI> uris, String lang,
                                                                 ThrowingFunction<SPARQLResult, T, Exception> resultHandler
    ) throws Exception {
        if (CollectionUtils.isEmpty(uris)) {
            return Collections.emptyList();
        }

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }

        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        return mapper.createInstanceList(graph, uris, lang, this);
    }


    public <T extends SPARQLResourceModel> T loadByURI(Class<T> objectClass, URI uri, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return loadByURI(getDefaultGraph(objectClass), objectClass, uri, lang, filterHandler,null);
    }

    public <T extends SPARQLResourceModel> T loadByURI(Node graph, Class<T> objectClass, URI uri, String lang) throws Exception {
        return loadByURI(graph, objectClass, uri, lang, null,null);
    }

    public <T extends SPARQLResourceModel> T loadByURI(Node graph, Class<T> objectClass, URI uri, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Map<String, WhereHandler> customHandlerByFields) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }

        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang,filterHandler,customHandlerByFields);

        select.addValueVar(mapper.getURIFieldExprVar(), SPARQLDeserializers.nodeURI(uri));

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 1) {
            return mapper.createInstance(graph, results.get(0), lang, this);
        } else if (results.size() > 1) {
            throw new SPARQLMultipleObjectException(uri, select.buildString());
        } else {
            return null;
        }
    }

    public <T extends SPARQLResourceModel> List<T> loadListByURIs(Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        return loadListByURIs(getDefaultGraph(objectClass), objectClass, uris, lang,null,null);
    }

    /**
     *
     * @param graph graph to query
     * @param objectClass {@link SPARQLResourceModel} class
     * @param uris the list of URI to load
     * @param lang lang
     * @param resultHandler function which define a custom transformation of {@link SPARQLResult} into a instance of T. Can be null
     * @param listFieldsToFetch . Define which data/object list fields from a {@link SPARQLResourceModel} must be fetched.
     * By default these fields are lazily retrieved but you can retrieve these fields directly in a more optimized way (see {@link SPARQLListFetcher}).
     * The listFieldsToFetch associate to each field name, a boolean flag to tell if the corresponding triple
     * must be added into the query which getch these fields data.
     *
     * @param <T> type of {@link SPARQLResourceModel}
     * @return the list of T corresponding to uris
     * @throws Exception
     */
    public <T extends SPARQLResourceModel> List<T> loadListByURIs(Node graph, Class<T> objectClass,
                                                                  Collection<URI> uris,
                                                                  String lang,
                                                                  ThrowingFunction<SPARQLResult, T, Exception> resultHandler,
                                                                  Map<String, Boolean> listFieldsToFetch) throws Exception {

        if (CollectionUtils.isEmpty(uris)) {
            return Collections.emptyList();
        }

        SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang);

        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(uris);
        select.addValueVar(mapper.getURIFieldExprVar(), uriNodes);

        String finalLang = lang != null ? lang : getDefaultLang();

        List<T> results =  executeSelectQueryAsStream(select).map(
                result -> {
                    try {
                        if (resultHandler == null) {
                            return mapper.createInstance(graph, result, finalLang, this);
                        } else {
                            return resultHandler.apply(result);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Unexpected exception", e);
                        throw new RuntimeException(new SPARQLException(e));
                    }
                }
        ).collect(Collectors.toCollection(() -> new ArrayList<>(uris.size())));

        if(listFieldsToFetch != null && ! listFieldsToFetch.isEmpty()){
            SPARQLListFetcher<T> listFetcher = new SPARQLListFetcher<>(this, objectClass,graph,listFieldsToFetch,select,results);
            listFetcher.updateModels();
        }

        return results;

    }

    public <T extends SPARQLResourceModel> T getByUniquePropertyValue(Class<T> objectClass, String lang, Property property, Object propertyValue) throws Exception {
        return getByUniquePropertyValue(getDefaultGraph(objectClass), objectClass, lang, property, propertyValue);
    }

    public <T extends SPARQLResourceModel> T getByUniquePropertyValue(Node graph, Class<T> objectClass, String lang, Property property, Object propertyValue) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang);
        Field field = mapper.getFieldFromUniqueProperty(property);

        SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForClass(propertyValue.getClass());
        select.addWhereValueVar(field.getName(), deserializer.getNode(propertyValue));

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.isEmpty()) {
            return null;
        } else if (results.size() == 1) {
            return mapper.createInstance(graph, results.get(0), lang, this);
        } else {
            throw new SPARQLException("Multiple objects for some unique property");
        }
    }

    public <T extends SPARQLResourceModel> boolean existsByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        return existsByUniquePropertyValue(getDefaultGraph(objectClass), objectClass, property, propertyValue);
    }

    public <T extends SPARQLResourceModel> boolean existsByUniquePropertyValue(Node graph, Class<T> objectClass, Property property, Object propertyValue) throws Exception {
        return existsByUniquePropertyValue(graph, objectClass, property, propertyValue, null);
    }

    public <T extends SPARQLResourceModel> boolean existsByUniquePropertyValue(Node graph, Class<T> objectClass, Property property, Object propertyValue, String lang) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (propertyValue == null) {
            return false;
        }
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        AskBuilder ask = mapper.getAskBuilder(graph, lang);
        Field field = mapper.getFieldFromUniqueProperty(property);
        SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForClass(propertyValue.getClass());
        ask.addGraph(graph, makeVar(SPARQLResourceModel.URI_FIELD), property.asNode(), makeVar(field.getName()));
        ask.setVar(field.getName(), deserializer.getNode(propertyValue));

        return executeAskQuery(ask);
    }

    public <T extends SPARQLResourceModel> List<URI> searchURIs(Class<T> objectClass, String lang) throws Exception {
        return searchURIs(getDefaultGraph(objectClass), objectClass, lang, null);
    }

    public <T extends SPARQLResourceModel> List<URI> searchURIs(Node graph, Class<T> objectClass, String lang) throws Exception {
        return searchURIs(graph, objectClass, lang, null);
    }

    public <T extends SPARQLResourceModel> List<URI> searchURIs(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchURIs(getDefaultGraph(objectClass), objectClass, lang, filterHandler);
    }

    public <T extends SPARQLResourceModel> List<URI> searchURIs(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang,filterHandler,null);

        List<URI> resultList = new ArrayList<>();
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            resultList.add(uriDeserializer.fromString((result.getStringValue(mapper.getURIFieldName()))));
        }, Exception.class));

        return resultList;
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, null, false, filterHandler,null);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, URI root, boolean excludeRoot, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, root, excludeRoot, filterHandler,null);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, URI root, boolean excludeRoot) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, root, excludeRoot, null,null);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Node graph, Class<T> objectClass, String lang,
                                                                                    URI root, boolean excludeRoot,
                                                                                    ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                                    Map<String,WhereHandler> customHandlerByFields) throws Exception {
        if (lang == null) {
            lang = getDefaultLang();
        }
        List<T> list = search(graph,objectClass,lang,filterHandler,customHandlerByFields,null,null,null,null);

        SPARQLTreeListModel<T> tree = new SPARQLTreeListModel<T>(list, SPARQLDeserializers.formatURI(root), excludeRoot);

        for (T item : list) {
            tree.addTree(item);
        }

        return tree;
    }

    public <T extends SPARQLTreeModel<T>> SPARQLPartialTreeListModel<T> searchPartialResourceTree(Node graph, Class<T> objectClass, String lang, String parentField, Property parentProperty, URI parentURI, int maxChild, int maxDepth, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        if (maxDepth < 0) {
            return null;
        }

        final String language;
        if (lang == null) {
            language = getDefaultLang();
        } else {
            language = lang;
        }

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        Var uriVar = makeVar("uri");

        List<T> rootList = search(graph, objectClass, language, (select) -> {
            if (parentURI == null) {
                Triple noParentTriple = new Triple(uriVar, parentProperty.asNode(), makeVar(parentField));
                select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(noParentTriple)));
            } else {
                select.addWhere(uriVar, parentProperty, SPARQLDeserializers.nodeURI(parentURI));
            }
            if (filterHandler != null) {
                filterHandler.accept(select);
            }
        });

        List<URI> rootURIs = new ArrayList<>(rootList.size());
        for (T i : rootList) {
            rootURIs.add(i.getUri());
        }

        Map<String, List<T>> objectMapByParent = new HashMap<>();
        SelectBuilder objectListQuery = mapper.getSelectBuilder(graph, language,filterHandler,null);

        if (maxDepth == 2 && parentURI == null) {
            objectListQuery.addFilter(SPARQLQueryHelper.inURIFilter(parentField, rootURIs));
        } else {
            Path propertyPath = PathFactory.pathOneOrMore1(
                    PathFactory.pathLink(parentProperty.asNode())
            );
            if (parentURI == null) {
                Var rootParentVar = makeVar("__rootParent");
                objectListQuery.addWhere(uriVar, propertyPath, rootParentVar);
                objectListQuery.addFilter(SPARQLQueryHelper.inURIFilter("__rootParent", rootURIs));
            } else {
                objectListQuery.addWhere(uriVar,
                        propertyPath,
                        SPARQLDeserializers.nodeURI(parentURI)
                );
            }
        }

        executeSelectQuery(objectListQuery, ThrowingConsumer.wrap((SPARQLResult result) -> {
            T instance = mapper.createInstance(graph, result, language, this);
            String instanceParentURI = SPARQLDeserializers.getExpandedURI(instance.getParent().getUri().toString());
            if (!objectMapByParent.containsKey(instanceParentURI)) {
                objectMapByParent.put(instanceParentURI, new ArrayList<>());
            }
            objectMapByParent.get(instanceParentURI).add(instance);
        }, Exception.class));

        Function<URI, List<T>> searchHandler = (parentSearchURI) -> {
            String expandURI = SPARQLDeserializers.getExpandedURI(parentSearchURI);
            if (objectMapByParent.containsKey(expandURI)) {
                return objectMapByParent.get(expandURI);
            } else {
                return new ArrayList<>();
            }
        };

        SelectBuilder objectCountQuery = mapper.getSelectBuilder(graph, language);
        Var objectCountUriVar = mapper.getURIFieldVar();
        objectCountQuery.getVars().clear();
        objectCountQuery.addVar(objectCountUriVar);
        Var childVar = makeVar("__child");
        objectCountQuery.addVar("COUNT(?__child)", "?__childCount");
        objectCountQuery.addWhere(childVar, parentProperty.asNode(), objectCountUriVar);
        if (filterHandler != null) {
            filterHandler.accept(objectCountQuery);
        }
        objectCountQuery.addGroupBy(objectCountUriVar);

        Map<String, Integer> countMap = new HashMap<>();
        executeSelectQuery(objectCountQuery, (result) -> {
            countMap.put(
                    result.getStringValue(mapper.getURIFieldName()),
                    Integer.valueOf(result.getStringValue("__childCount"))
            );
        });

        Function<URI, Integer> countHandler = (parentCountURI) -> {
            String expandURI = SPARQLDeserializers.getExpandedURI(parentCountURI);
            if (countMap.containsKey(expandURI)) {
                return countMap.get(expandURI);
            } else {
                return 0;
            }
        };

        SPARQLPartialTreeListModel<T> tree = new SPARQLPartialTreeListModel<T>(parentURI, searchHandler, countHandler);

        for (T item : rootList) {
            tree.loadChildren(item, null, maxDepth);
        }

        return tree;
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang,null,null, null,null, null, null);
    }


    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, filterHandler);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(graph, objectClass, lang, filterHandler, null,null,null, null, null);
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, List<OrderBy> orderByList) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, null,null,null,orderByList,null,null);
    }


    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Collection<OrderBy> orderByList) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, filterHandler,null, null,orderByList, null, null);
    }

    public <T extends SPARQLResourceModel> SelectBuilder getSelectBuilder(SPARQLClassObjectMapper<T> mapper, Node graph, String language,
                                                                          ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                          Map<String,WhereHandler> customHandlerByFields,
                                                                          Collection<OrderBy> orderByList, Integer offset, Integer limit) throws Exception {

        SelectBuilder select = mapper.getSelectBuilder(graph, language,filterHandler,customHandlerByFields);

        if (orderByList != null) {
            orderByList.forEach((OrderBy orderBy) -> {
                Expr fieldOrderExpr = mapper.getFieldOrderExpr(orderBy.getFieldName());
                if (fieldOrderExpr != null) {
                    select.addOrderBy(fieldOrderExpr, orderBy.getOrder());
                }
            });
        }

        if (offset != null && limit != null) {
            select.setOffset(offset);
            select.setLimit(limit);
        }

        return select;
    }

    /**
     *

     * @param uris the list of URI to load
     * @param lang lang
     * @param listFieldsToFetch . Define which data/object list fields from a {@link SPARQLResourceModel} must be fetched.
     * By default these fields are lazily retrieved but you can retrieve these fields directly in a more optimized way (see {@link SPARQLListFetcher}).
     * The listFieldsToFetch associate to each field name, a boolean flag to tell if the corresponding triple
     * must be added into the query which getch these fields data.
     *
     * @throws Exception
     */


    /**
     *
     * @param graph graph to query
     * @param objectClass {@link SPARQLResourceModel} class
     * @param lang lang
     * @param filterHandler function defined to update the SPARQL query used for search.
     * @param customHandlerByFields map between {@link SPARQLResourceModel} field and a custom {@link WhereHandler}.
     * By default, all fields are handled by {@link org.opensilex.sparql.mapping.SPARQLClassQueryBuilder},
     * but in some case you may wan't so specifiy a custom way to handle this field into the query. Can be null
     * @param resultHandler function which define a custom transformation of {@link SPARQLResult} into a instance of T. Can be null
     * @param orderByList
     * @param offset
     * @param limit
     * @param <T> type of {@link SPARQLResourceModel}
     * @return a list of T
     * @throws Exception
     */
    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang,
                                                          ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                          Map<String,WhereHandler> customHandlerByFields,
                                                          ThrowingFunction<SPARQLResult,T,Exception> resultHandler,
                                                          Collection<OrderBy> orderByList, Integer offset, Integer limit) throws Exception {

        Stream<T> stream = searchAsStream(graph, objectClass, lang, filterHandler, customHandlerByFields,resultHandler, orderByList, offset, limit);

        // convert stream to a list with limit as list capacity
        if (limit != null && limit > 0) {
            return stream.collect(Collectors.toCollection(() -> new ArrayList<>(limit)));
        } else {
            return stream.collect(Collectors.toList());
        }

    }

    public <T extends SPARQLResourceModel> Stream<T> searchAsStream(Node graph, Class<T> objectClass, String lang,
                                                                    ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                    Map<String,WhereHandler> customHandlerByFields,
                                                                    ThrowingFunction<SPARQLResult,T,Exception> resultHandler,
                                                                    Collection<OrderBy> orderByList, Integer offset, Integer limit) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        String language;
        if (lang == null) {
            language = getDefaultLang();
        } else {
            language = lang;
        }

        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = getSelectBuilder(mapper, graph, language, filterHandler, customHandlerByFields,orderByList, offset, limit);

        Stream<SPARQLResult> resultStream = executeSelectQueryAsStream(select);
        boolean hasResultHandler = resultHandler == null;

        return resultStream.map(result -> {
            try {
                if (hasResultHandler) {
                    return mapper.createInstance(graph, result, language, this);
                } else {
                    return resultHandler.apply(result);
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected exception", e);
                throw new RuntimeException(new SPARQLException(e));
            }
        });

    }

    public <T extends SPARQLResourceModel> int count(Class<T> objectClass) throws Exception {
        return count(getDefaultGraph(objectClass), objectClass,null, null,null);
    }

    public <T extends SPARQLResourceModel> int count(Node graph, Class<T> objectClass) throws Exception {
        return count(graph, objectClass,null, null,null);

    }

    public <T extends SPARQLResourceModel> int count(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Map<String,WhereHandler> customHandlerByFields) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder selectCount = mapper.getCountBuilder(graph, "count", lang,filterHandler,customHandlerByFields);

        // save the original COUNT var expression
        Map<Var, Expr> oldCountVars = new HashMap<>(selectCount.getSelectHandler().getProject().getExprs());

        if (filterHandler != null) {

            // ensure that there are no ORDER BY clause provided by the handler, into the COUNT query, else the query will be incorrect
            Query countQuery = selectCount.getHandlerBlock().getAggregationHandler().getQuery();
            if (countQuery.getOrderBy() != null) {
                countQuery.getOrderBy().clear();
            }

            // ensure that no additional variables have been added into the COUNT query, else the query could be incorrect (if no GROUP BY is associated to the additional variable)
            if (selectCount.getVars().size() > 1) {
                selectCount.getSelectHandler().getProject().clear();
                oldCountVars.forEach((var, expr) -> selectCount.addVar(expr, var));
            }

        }

        List<SPARQLResult> resultSet = executeSelectQuery(selectCount);

        if (resultSet.size() == 1) {
            return Integer.valueOf(resultSet.get(0).getStringValue("count"));
        } else {
            throw new SPARQLException("Invalid count query");
        }
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Collection<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return searchWithPagination(getDefaultGraph(objectClass), objectClass, lang, filterHandler, null,null,orderByList, page, pageSize);
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Node graph,
                                                                                      Class<T> objectClass,
                                                                                      String lang,
                                                                                      ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                                      Map<String,WhereHandler> customHandlerByFields,
                                                                                      ThrowingFunction<SPARQLResult,T,Exception> resultHandler,
                                                                                      Collection<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        if (lang == null) {
            lang = getDefaultLang();
        }
        int total = count(graph, objectClass, lang, filterHandler,customHandlerByFields);

        List<T> list;
        if (pageSize == null || pageSize == 0) {
            list = search(graph, objectClass, lang, filterHandler, customHandlerByFields,resultHandler, orderByList, null, null);
        } else if (total > 0 && (page * pageSize) < total) {
            Integer offset = null;
            Integer limit = null;
            if (page < 0) {
                page = 0;
            }
            if (pageSize > 0) {
                offset = page * pageSize;
                limit = pageSize;
            }
            list = search(graph, objectClass, lang, filterHandler,customHandlerByFields, resultHandler, orderByList, offset, limit);
        } else {
            list = new ArrayList<>();
        }

        return new ListWithPagination<>(list, page, pageSize, total);

    }

//    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Collection<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
//        return this.searchWithPagination(graph, objectClass, lang, filterHandler,null, null, orderByList, page, pageSize);
//    }

    public <T extends SPARQLResourceModel> void create(T instance) throws Exception {
        create(getDefaultGraph(instance.getClass()), instance);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance) throws Exception {
        create(graph, instance, true);
    }

    public <T extends SPARQLResourceModel> void create(T instance, boolean checkUriExist) throws Exception {
        create(getDefaultGraph(instance.getClass()), instance, checkUriExist);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance, boolean checkUriExist) throws Exception {
        create(graph, instance, null, null,checkUriExist, false, null);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance, boolean checkUriExist, boolean blankNode, BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {
        create(graph, instance, null, null, checkUriExist, blankNode, createExtension);
    }


    /**
     * @param graph           the graph onto instance are created
     * @param instance        the instance to create
     * @param updateBuilder   an UpdateBuilder which can be updated by adding new statements if not null
     * @param checkUriExist   indicate if the service must check if instances already exist
     * @param blankNode       indicate if the instance URI must be a blank node
     * @param createExtension additional modification to the UpdateBuilder
     * @param <T>             the SPARQLResourceModel type
     */
    protected  <T extends SPARQLResourceModel> void create(Node graph,
                                                           T instance,
                                                           SPARQLResourceModel parent,
                                                           UpdateBuilder updateBuilder,
                                                           boolean checkUriExist,
                                                           boolean blankNode,
                                                           BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(instance.getClass());

        boolean useNewBuilder = updateBuilder == null;
        UpdateBuilder create = useNewBuilder ? new UpdateBuilder() : updateBuilder;
        prepareInstanceCreation(graph, instance, parent, mapper, create, checkUriExist, blankNode);
        mapper.addCreateBuilder(graph, instance, create, blankNode,createExtension);

        if(useNewBuilder){
            executeUpdateQuery(create);
        }

    }

    /**
     * @param graph                    the graph onto instance are created
     * @param instance                 the instances to create
     * @param mapper                   the SPARQL mapper used in order to generate query according instance fields
     * @param subInstanceUpdateBuilder an UpdateBuilder which can be updated by adding new statements if not null
     * @param checkUriExist            indicate if the service must check if instances already exist
     * @param blankNode                indicate if the instance URI must be a blank node
     * @param <T>                      the SPARQLResourceModel type
     */
    protected  <T extends SPARQLResourceModel> void prepareInstanceCreation(
            Node graph,
            T instance,
            SPARQLResourceModel parent,
            SPARQLClassObjectMapper<T> mapper,
            UpdateBuilder subInstanceUpdateBuilder,
            boolean checkUriExist,
            boolean blankNode) throws Exception {

        URI rdfType = instance.getType();
        if (rdfType == null) {
            instance.setType(new URI(mapper.getRDFType().getURI()));
        } else {
            instance.setType(rdfType);
        }

        if (!blankNode) {
            generateUniqueUriIfNullOrValidateCurrent(graph, mapper, instance, checkUriExist);
        }

        validate(instance,parent);

        for (SPARQLResourceModel subInstance : mapper.getAllDependentResourcesToCreate(instance)) {
            create(getDefaultGraph(subInstance.getClass()), subInstance,instance, subInstanceUpdateBuilder, checkUriExist, blankNode, null);
        }
    }

    public <T extends SPARQLResourceModel> void create(Class<T> clazz, Collection<T> instances) throws Exception {
        create(getDefaultGraph(clazz), instances);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, Collection<T> instances) throws Exception {
        create(graph, instances, null, true);
    }

    public static final int DEFAULT_MAX_INSTANCE_PER_QUERY = 1000;

    /**
     * @param graph               the graph onto instance are created
     * @param instances           the list of instance to create
     * @param maxInstancePerQuery number of instance to put in one query, if null then one query per instance is used
     * @param checkUriExist       indicate if the service must check if instances already exist
     * @param <T>                 the SPARQLResourceModel type
     */
    public <T extends SPARQLResourceModel> void create(Node graph, Collection<T> instances, Integer maxInstancePerQuery, boolean checkUriExist) throws Exception {

        boolean reuseSameQuery = maxInstancePerQuery != null;

        if (reuseSameQuery && maxInstancePerQuery <= 0) {
            throw new IllegalArgumentException("maxInstancePerQuery must be strictly positive : " + maxInstancePerQuery);
        }

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();

        if (instances.size() > 0) {
            validate(instances,null);

            UpdateBuilder updateBuilder = new UpdateBuilder();

            // use the same query for the instance and her sub-instance if a query batch size is specified
            UpdateBuilder subInstanceUpdateBuilder = reuseSameQuery ? updateBuilder : null;

            try {
                // set a maximum number of instance to insert into one query in order to ensure that the INSERT query will not be too big
                int insertedInstanceNb = 0;

                startTransaction();

                for (T instance : instances) {
                    SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(instance.getClass());
                    prepareInstanceCreation(graph, instance, null,mapper, subInstanceUpdateBuilder, checkUriExist, false);
                    mapper.addCreateBuilder(graph, instance, updateBuilder, false,null);

                    // if query limit is reached, then insert query and reset builder
                    if (reuseSameQuery && insertedInstanceNb++ == maxInstancePerQuery) {
                        executeUpdateQuery(updateBuilder);
                        insertedInstanceNb = 0;
                        updateBuilder = new UpdateBuilder();
                        subInstanceUpdateBuilder = updateBuilder;
                    }
                }

                if (reuseSameQuery) {
                    if (insertedInstanceNb > 0) {
                        executeUpdateQuery(updateBuilder);
                    }
                } else {
                    executeUpdateQuery(updateBuilder);
                }

                commitTransaction();

            } catch (Exception e) {
                rollbackTransaction();
                throw e;
            }

        }
    }

    private <T extends SPARQLResourceModel> void generateUniqueUriIfNullOrValidateCurrent(Node graph, SPARQLClassObjectMapper<T> mapper, T instance, boolean checkUriExist) throws Exception {
        URIGenerator<T> uriGenerator = mapper.getUriGenerator(instance);
        URI uri = mapper.getURI(instance);
        if (uri == null) {

            int retry = 0;
            String prefix = getDefaultGenerationURI(instance.getClass()).toString();
            uri = uriGenerator.generateURI(prefix, instance, retry);

            if (checkUriExist) {
                while (uriExists(graph, uri)) {
                    uri = uriGenerator.generateURI(prefix, instance, ++retry);
                }
            }

            mapper.setUri(instance, uri);
        } else if (checkUriExist && uriExists(graph, uri)) {
            throw new SPARQLAlreadyExistingUriException(uri);
        }
    }

    public <T extends SPARQLResourceModel> void update(T instance) throws Exception {
        update(getDefaultGraph(instance.getClass()), instance);
    }

    public <T extends SPARQLResourceModel> void update(Node graph, T instance) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();

        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) instance.getClass();
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);

        try {
            startTransaction();

            URI uri = mapper.getURI(instance);
            T oldInstance = loadByURI(graph, objectClass, uri, getDefaultLang());
            if (oldInstance == null) {
                throw new SPARQLInvalidURIException(instance.getUri());
            }
            mapper.updateInstanceFromOldValues(oldInstance, instance);

            Map<URI, Class<? extends SPARQLResourceModel>> autoUpdateFieldsToDelete = new HashMap<>();
            List<SPARQLResourceModel> autoUpdateFieldsToUpdate = new ArrayList<>();

            List<Field> autoUpdateFields = mapper.getAutoUpdateFields();
            for (Field f : autoUpdateFields) {
                SPARQLResourceModel newFieldValue = (SPARQLResourceModel) f.get(instance);
                SPARQLResourceModel oldFieldValue = (SPARQLResourceModel) f.get(oldInstance);
                if (newFieldValue == null || newFieldValue.getUri() == null) {
                    if (oldFieldValue != null) {
                        autoUpdateFieldsToDelete.put(oldFieldValue.getUri(), oldFieldValue.getClass());
                    }
                } else if (oldFieldValue != null) {
                    if (!oldFieldValue.getUri().equals(newFieldValue.getUri())) {
                        autoUpdateFieldsToDelete.put(oldFieldValue.getUri(), oldFieldValue.getClass());
                    } else {
                        autoUpdateFieldsToUpdate.add(newFieldValue);
                    }
                }
            }

            List<Field> autoUpdateListFields = mapper.getAutoUpdateListFields();
            for (Field f : autoUpdateListFields) {
                List<? extends SPARQLResourceModel> newFieldValue = (List<? extends SPARQLResourceModel>) f.get(instance);
                List<? extends SPARQLResourceModel> oldFieldValue = (List<? extends SPARQLResourceModel>) f.get(oldInstance);

                if (newFieldValue == null) {
                    for (SPARQLResourceModel ofValue : oldFieldValue) {
                        autoUpdateFieldsToDelete.put(ofValue.getUri(), ofValue.getClass());
                    }
                } else {
                    Map<URI, Class<? extends SPARQLResourceModel>> oldURIs = new HashMap<>();
                    if (oldFieldValue != null) {
                        for (SPARQLResourceModel ofValue : oldFieldValue) {
                            oldURIs.put(ofValue.getUri(), ofValue.getClass());
                        }
                    }
                    for (SPARQLResourceModel nfValue : newFieldValue) {
                        if (nfValue != null && nfValue.getUri() != null) {
                            if (oldURIs.containsKey(nfValue.getUri())) {
                                autoUpdateFieldsToUpdate.add(nfValue);
                                oldURIs.remove(nfValue.getUri());
                            }
                        }
                    }
                    autoUpdateFieldsToDelete.putAll(oldURIs);
                }
            }

            deleteURIClassMap(autoUpdateFieldsToDelete);
            update(autoUpdateFieldsToUpdate);

            UpdateBuilder delete = mapper.getDeleteBuilder(graph, oldInstance);
            executeDeleteQuery(delete);
            create(graph, instance, false);

            commitTransaction();
        } catch (Exception ex) {
            rollbackTransaction(ex);
            throw ex;
        }
    }

    public <T extends SPARQLResourceModel> void update(List<T> instances) throws Exception {
        update(null, instances);
    }

    public <T extends SPARQLResourceModel> void update(Node graph, List<T> instances) throws Exception {
        try {
            startTransaction();

            if (instances.size() > 0) {

                validate(instances,null);

                for (T instance : instances) {
                    Node instanceGraph = graph;
                    if (graph == null) {
                        instanceGraph = getDefaultGraph(instance.getClass());
                    }

                    update(instanceGraph, instance);
                }
            }
            commitTransaction();
        } catch (Exception ex) {
            rollbackTransaction(ex);
            throw ex;
        }
    }

    public <T extends SPARQLResourceModel> void deleteIfExists(Class<T> objectClass, URI uri) throws Exception {
        try {
            delete(objectClass, uri);
        } catch (Exception ex) {

        }
    }

    public <T extends SPARQLResourceModel> void delete(Class<T> objectClass, URI uri) throws Exception {
        delete(getDefaultGraph(objectClass), objectClass, uri);
    }

    public <T extends SPARQLResourceModel> void delete(Node graph, Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        try {
            startTransaction();
//            if (!uriExists(graph, uri)) {
//                throw new SPARQLInvalidURIException(uri);
//            }
            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);

            Map<Class<? extends SPARQLResourceModel>, List<URI>> relationsToDelete = new HashMap<>();
            Map<Class<? extends SPARQLResourceModel>, List<URI>> reverseRelationsToDelete = new HashMap<>();
            Map<Field, Class<? extends SPARQLResourceModel>> cascadeDeleteClassesProperties = mapper.getCascadeDeleteClassesField();
            for (Map.Entry<Field, Class<? extends SPARQLResourceModel>> cascadeDeleteClassField : cascadeDeleteClassesProperties.entrySet()) {
                Field f = cascadeDeleteClassField.getKey();

                if (mapper.isReverseRelation(f)) {
                    List<URI> relations = getRelationsURI(objectClass, cascadeDeleteClassField.getValue(), f, uri);
                    reverseRelationsToDelete.put(cascadeDeleteClassField.getValue(), relations);
                } else {
                    List<URI> relations = getRelationsURI(objectClass, cascadeDeleteClassField.getValue(), f, uri);
                    relationsToDelete.put(cascadeDeleteClassField.getValue(), relations);
                }

            }
            T instance = loadByURI(graph, objectClass, uri, getDefaultLang());

            for (Map.Entry<Class<? extends SPARQLResourceModel>, List<URI>> relationToDelete : reverseRelationsToDelete.entrySet()) {
                delete(relationToDelete.getKey(), relationToDelete.getValue());
            }

            Iterator<Map.Entry<Class<? extends SPARQLResourceModel>, Field>> i = mapperIndex.getReverseReferenceIterator(objectClass);
            Node uriNode = SPARQLDeserializers.nodeURI(uri);

            while (i.hasNext()) {
                UpdateBuilder deleteAllReverseReferencesBuilder = new UpdateBuilder();
                Map.Entry<Class<? extends SPARQLResourceModel>, Field> entry = i.next();
                String var = "?x1";
                SPARQLClassObjectMapper<SPARQLResourceModel> reverseMapper = mapperIndex.getForClass(entry.getKey());
                Property reverseProp = reverseMapper.getFieldProperty(entry.getValue());
                Node defaultGraph = reverseMapper.getDefaultGraph();
                if (defaultGraph != null) {
                    deleteAllReverseReferencesBuilder.addDelete(reverseMapper.getDefaultGraph(), var, reverseProp, uriNode);
                } else {
                    deleteAllReverseReferencesBuilder.addDelete(var, reverseProp, uriNode);
                }
                deleteAllReverseReferencesBuilder.addWhere(var, reverseProp, uriNode);
                executeDeleteQuery(deleteAllReverseReferencesBuilder);
            }

            if (instance != null) {
                UpdateBuilder delete = mapper.getDeleteBuilder(graph, instance);
                executeDeleteQuery(delete);
            }

            UpdateBuilder deleteRelations = mapper.getDeleteRelationsBuilder(graph, uri);
            if (deleteRelations != null) {
                executeDeleteQuery(deleteRelations);
            }

            for (Map.Entry<Class<? extends SPARQLResourceModel>, List<URI>> relationToDelete : relationsToDelete.entrySet()) {
                delete(relationToDelete.getKey(), relationToDelete.getValue());
            }
            commitTransaction();
        } catch (Exception ex) {
            rollbackTransaction(ex);
            throw ex;
        }
    }

    public <T extends SPARQLResourceModel> void delete(Class<T> objectClass, List<URI> uris) throws Exception {
        delete(getDefaultGraph(objectClass), objectClass, uris);
    }

    public <T extends SPARQLResourceModel> void delete(Node graph, Class<T> objectClass, List<URI> uris) throws Exception {
        if (uris.size() > 0) {
            try {
                startTransaction();
                for (URI uri : uris) {
                    delete(graph, objectClass, uri);
                }
                commitTransaction();
            } catch (Exception ex) {
                rollbackTransaction(ex);
                throw ex;
            }
        }
    }

    private void deleteURIClassMap(Map<URI, Class<? extends SPARQLResourceModel>> deleteList) throws Exception {
        if (deleteList.size() > 0) {
            try {
                startTransaction();
                for (Map.Entry<URI, Class<? extends SPARQLResourceModel>> deleteItem : deleteList.entrySet()) {
                    delete(deleteItem.getValue(), deleteItem.getKey());
                }
                commitTransaction();
            } catch (Exception ex) {
                rollbackTransaction(ex);
                throw ex;
            }
        }
    }

    public <T extends SPARQLResourceModel, U extends SPARQLResourceModel> List<URI> getRelationsURI(Class<T> objectClass, Class<U> relationClass, Field objectField, URI objectURI) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        SPARQLClassObjectMapper<T> objectMapper = mapperIndex.getForClass(objectClass);
        SPARQLClassObjectMapper<T> relationMapper = mapperIndex.getForClass(relationClass);
        Node objectNode = SPARQLDeserializers.nodeURI(objectURI);
        Node relationGraph = getDefaultGraph(relationClass);
        Node graph = getDefaultGraph(objectClass);

        return this.searchURIs(relationGraph, relationClass, null, (select) -> {
            WhereHandler whereHandler = new WhereHandler();
            if (objectMapper.isReverseRelation(objectField)) {
                whereHandler.addWhere(select.makeTriplePath(objectMapper.getURIFieldVar(), objectMapper.getFieldProperty(objectField), objectNode));

                ElementNamedGraph elementNamedGraph = new ElementNamedGraph(relationGraph, whereHandler.getElement());
                select.getWhereHandler().getClause().addElement(elementNamedGraph);
            } else {
                whereHandler.addWhere(select.makeTriplePath(objectNode, objectMapper.getFieldProperty(objectField), relationMapper.getURIFieldVar()));

                ElementNamedGraph elementNamedGraph = new ElementNamedGraph(graph, whereHandler.getElement());
                select.getWhereHandler().getClause().addElement(elementNamedGraph);
            }

        });
    }

    public boolean uriExists(Node graph, URI uri) throws SPARQLException {
        AskBuilder askQuery = new AskBuilder();
        Var s = makeVar("s");
        Var p = makeVar("p");
        Var o = makeVar("o");
        Node nodeUri = SPARQLDeserializers.nodeURI(uri);
        if (graph == null) {
            askQuery.addWhere(nodeUri, p, o);
            WhereBuilder reverseWhere = new WhereBuilder();
            reverseWhere.addWhere(s, p, nodeUri);
            askQuery.addUnion(reverseWhere);
        } else {
            askQuery.addGraph(graph, new Triple(nodeUri, p, o));
            WhereBuilder reverseWhere = new WhereBuilder();
            reverseWhere.addGraph(graph, new Triple(s, p, nodeUri));
            askQuery.addUnion(reverseWhere);
        }

        return executeAskQuery(askQuery);
    }

    public <T extends SPARQLResourceModel> boolean uriExists(Class<T> objectClass, URI uri) throws Exception {
        if (uri == null) {
            return false;
        }
        if (objectClass != null) {
            return executeAskQuery(getUriExistsQuery(objectClass, uri));
        }
        return uriExists((Node) null, uri);
    }

    public <T extends SPARQLResourceModel> boolean anyUriExists(Class<T> objectClass, Collection<URI> uris) throws Exception {
        if (CollectionUtils.isEmpty(uris)) {
            return false;
        }
        if (uris.size() == 1) {
            return uriExists(objectClass, uris.iterator().next());
        }

        SelectBuilder selectQuery = getUriListExistQuery(objectClass, uris);
        for (SPARQLResult result : executeSelectQuery(selectQuery)) {
            boolean value = Boolean.parseBoolean(result.getStringValue(EXISTING_VAR));
            if (value) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param objectClass the models class
     * @param uris        the URIs to check
     * @param checkExist  indicates if we check the existence or the non-existence of the given URI collection
     * @param <T>         the SPARQLResourceModel type
     * @return the Set of unknown or existing URI from the given URI collection
     */
    public <T extends SPARQLResourceModel> Set<URI> getExistingUris(Class<T> objectClass, Collection<URI> uris, boolean checkExist) throws Exception {

        if (CollectionUtils.isEmpty(uris)) {
            return Collections.emptySet();
        }

        SelectBuilder selectQuery = getUnknownUrisQuery(objectClass, uris, checkExist);

        return executeSelectQueryAsStream(selectQuery).map(result -> {
            try {
                return new URI(result.getStringValue(SPARQLResourceModel.URI_FIELD));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());

    }

    public <T extends SPARQLResourceModel> boolean uriListExists(Class<T> objectClass, Collection<URI> uris) throws Exception {
        if (uris == null || uris.isEmpty()) {
            return false;
        }
        if (uris.size() == 1) {
            return uriExists(objectClass, uris.iterator().next());
        }

        SelectBuilder selectQuery = getUriListExistQuery(objectClass, uris);
        for (SPARQLResult result : executeSelectQuery(selectQuery)) {
            boolean value = Boolean.parseBoolean(result.getStringValue(EXISTING_VAR));
            if (!value) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param rdfType the {@link RDF#type} to check
     * @param uri     the {@link URI} to check
     * @return true if uri exists in the TripleStore and if it's an instance of
     * rdfType
     */
    public boolean uriExists(URI rdfType, URI uri) throws SPARQLException {
        Var typeVar = makeVar("type");
        return executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(uri), RDF.type, typeVar)
                .addWhere(typeVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(rdfType))
        );
    }

    public <T extends SPARQLResourceModel> AskBuilder getUriExistsQuery(Class<T> objectClass, URI uri) throws SPARQLException {

        AskBuilder askQuery = new AskBuilder();
        Node nodeUri = SPARQLDeserializers.nodeURI(uri);

        SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);
        Var fieldType = mapper.getTypeFieldVar();
        askQuery.addWhere(nodeUri, RDF.type, fieldType);

        Resource typeDef = mapper.getRDFType();

        askQuery.addWhere(fieldType, Ontology.subClassAny, typeDef);
        return askQuery;
    }

    public static final String EXISTING_VAR = "existing";

    public <T extends SPARQLResourceModel> SelectBuilder getUriListExistQuery(Class<T> objectClass, Collection<URI> uris) throws Exception {

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var existing = makeVar(EXISTING_VAR);

        SelectBuilder select = new SelectBuilder();
        SPARQLQueryHelper.addWhereValues(select, uriVar.getVarName(), uris);

        WhereBuilder where = new WhereBuilder()
                .addWhere(uriVar, makeVar("p"), makeVar("o"));

        if (objectClass != null) {
            SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);
            Var typeVar = mapper.getTypeFieldVar();
            Resource typeDef = mapper.getRDFType();
            where.addWhere(typeVar, Ontology.subClassAny, typeDef)
                    .addWhere(uriVar, RDF.type, typeVar);
        }

        Expr existExpr = SPARQLQueryHelper.getExprFactory().exists(where);
        select.addVar(existExpr, existing);

        return select;
    }

    /**
     * @param objectClass the models class
     * @param uris        the URIs to check
     * @param checkExist  indicates if we check the existence or the non-existence of the given URI collection
     * @param <T>         the SPARQLResourceModel type
     * @return the query which return the set of existing/non-existing URIS
     */
    public <T extends SPARQLResourceModel> SelectBuilder getUnknownUrisQuery(Class<T> objectClass, Collection<URI> uris, boolean checkExist) throws Exception {

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);

        SelectBuilder select = new SelectBuilder();
        SPARQLQueryHelper.addWhereValues(select, uriVar.getVarName(), uris);

        WhereBuilder where = new WhereBuilder()
                .addWhere(uriVar, makeVar("p"), makeVar("o"));

        if (objectClass != null) {
            SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);
            Var typeVar = mapper.getTypeFieldVar();
            Resource typeDef = mapper.getRDFType();
            where.addWhere(typeVar, Ontology.subClassAny, typeDef)
                    .addWhere(uriVar, RDF.type, typeVar);
        }

        Expr notExistsExpr = checkExist
                ? SPARQLQueryHelper.getExprFactory().exists(where)
                : SPARQLQueryHelper.getExprFactory().notexists(where);

        return select.addFilter(notExistsExpr);
    }

    public void insertPrimitive(Node graph, URI subject, Property property, Object value) throws Exception {
        UpdateBuilder insertQuery = new UpdateBuilder();
        Node nodeValue;

        if (SPARQLDeserializers.existsForClass(value.getClass())) {
            nodeValue = SPARQLDeserializers.getForClass(value.getClass()).getNode(value);

            insertQuery.addInsert(graph, SPARQLDeserializers.nodeURI(subject), property, nodeValue);

            executeUpdateQuery(insertQuery);
        }

    }

    public void insertPrimitive(Node graph, List<URI> uris, Property property, Object value) throws Exception {
        UpdateBuilder insertQuery = new UpdateBuilder();
        Node nodeValue;

        if (SPARQLDeserializers.existsForClass(value.getClass())) {
            nodeValue = SPARQLDeserializers.getForClass(value.getClass()).getNode(value);

            for (URI subject : uris) {
                insertQuery.addInsert(graph, SPARQLDeserializers.nodeURI(subject), property, nodeValue);
            }

            executeUpdateQuery(insertQuery);
        }
    }

    /**
     * Insert a list of quad (graph,subject,property,object),
     * (graph,subject,property,object_1) ... (graph,subject,property,object_k)
     * <p>
     * and remove any old quad (graph,subject,property,?object) in the SPARQL
     * graph
     *
     * @param graph    the graph in which the triple(s) are present
     * @param subject  the triple subject URI
     * @param property the triple property
     * @param objects  the list of object values
     */
    public void updateObjectRelations(Node graph, URI subject, Property property, List<?> objects) throws Exception {

        UpdateBuilder updateBuilder = new UpdateBuilder();

        Node subjectNode = SPARQLDeserializers.nodeURI(subject);
        Node objectVariable = makeVar("o");
        Node propertyNode = property.asNode();

        // build a triple for each new object to insert
        Iterator<Triple> insertTriplesIt = objects.stream().map(object -> {
            try {
                return new Triple(subjectNode, propertyNode, SPARQLDeserializers.getForClass(object.getClass()).getNode(object));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();

        updateBuilder.addDelete(graph, subjectNode, property, objectVariable);
        updateBuilder.addInsert(graph, insertTriplesIt);
        updateBuilder.addOptional(subjectNode, property, objectVariable);

        executeUpdateQuery(updateBuilder);
    }

    /**
     * Insert a list of quad (graph,subject,property,object),
     * (graph,subject_1,property,object) ... (graph,subject_k,property,object)
     * <p>
     * and remove any old quad (graph,?subject,property,object) in the SPARQL
     * graph
     *
     * @param graph    the graph in which the triple(s) are present
     * @param subjects the list of subject URIS
     * @param property the triple property
     * @param object   the triple(s) object
     */
    public void updateSubjectRelations(Node graph, List<URI> subjects, Property property, Object object) throws Exception {

        UpdateBuilder updateBuilder = new UpdateBuilder();

        Node objectNode = SPARQLDeserializers.getForClass(object.getClass()).getNode(object);
        Node subjectVariable = makeVar("s");
        Node propertyNode = property.asNode();

        // build a triple for each new subject to insert
        Iterator<Triple> insertTriplesIt = subjects.stream().map(subject -> {
            try {
                return new Triple(SPARQLDeserializers.nodeURI(subject), propertyNode, objectNode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).iterator();

        updateBuilder.addDelete(graph, subjectVariable, property, objectNode);
        updateBuilder.addInsert(graph, insertTriplesIt);
        updateBuilder.addOptional(subjectVariable, property, objectNode);

        executeUpdateQuery(updateBuilder);
    }

    public Map<String, String> getTranslations(Node graph, URI resourceURI, Property labelProperty, boolean reverseRelation) throws Exception {
        Map<String, String> translations = new HashMap<>();

        SelectBuilder select = new SelectBuilder();

        Var valueVar = makeVar("value");
        Var langVar = makeVar("lang");

        select.addVar(valueVar);
        select.addVar(SPARQLQueryHelper.getExprFactory().lang(valueVar), langVar);

        if (reverseRelation) {
            select.addWhere(valueVar, labelProperty, SPARQLDeserializers.nodeURI(resourceURI));
        } else {
            select.addWhere(SPARQLDeserializers.nodeURI(resourceURI), labelProperty, valueVar);
        }

        executeSelectQuery(select, (SPARQLResult result) -> {
            String value = result.getStringValue("value");
            String resultLang = result.getStringValue("lang");
            translations.put(resultLang, value);
        });

        return translations;
    }

    @Override
    public void disableSHACL() throws SPARQLException {
        LOGGER.debug("DISABLE SHACL Validation");
        connection.disableSHACL();
    }

    @Override
    public void enableSHACL() throws SPARQLException {
        LOGGER.debug("ENABLE SHACL Validation");
        connection.enableSHACL();
    }

    @Override
    public boolean isShaclEnabled() {
        return connection.isShaclEnabled();
    }

    @Deprecated
    public RepositoryConnection getRepositoryConnection() {
        RDF4JConnection cnt = (RDF4JConnection) this.connection;
        return cnt.getRepositoryConnectionImpl();
    }

    public Node getDefaultGraph(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getDefaultGraph();
    }

    public URI getDefaultGraphURI(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        SPARQLClassObjectMapperIndex i = getMapperIndex();
        SPARQLClassObjectMapper<SPARQLResourceModel> g = i.getForClass(modelClass);
        return g.getDefaultGraphURI();
    }

    public URI getDefaultGenerationURI(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getGenerationPrefixURI();
    }

    public Var getURIFieldVar(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getURIFieldVar();
    }

    public <T extends SPARQLResourceModel> void validate(T instance, SPARQLResourceModel parent) throws Exception {
        if (!this.isShaclEnabled()) {
            validateRelations(instance,parent);
        }

        validateReverseRelations(instance,parent);
    }

    public <T extends SPARQLResourceModel> void validate(Collection<T> instances,SPARQLResourceModel parent) throws Exception {
        if (!this.isShaclEnabled()) {
            validateRelations(instances,parent);
        }

        validateReverseRelations(instances,parent);
    }

    private <T extends SPARQLResourceModel> void validateRelations(Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers, SPARQLResourceModel parent) throws Exception {
        for (Map.Entry<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMapper : urisByMappers.entrySet()) {
            SPARQLClassObjectMapper<SPARQLResourceModel> modelMapper = urisByMapper.getKey();
            Set<URI> urisToCheck = urisByMapper.getValue();

            // remove parent uri from uris to check, because the validation of the current relation come after the validation of the parent.
            // the parent is not yet present in the triplestore, since it must be created, so we must not include the parent URI in uris to check.
            if(parent != null){
                urisToCheck.remove(parent.getUri());
            }

            if (!urisToCheck.isEmpty()) {
                Set<URI> unknownUris = getExistingUris(modelMapper.getObjectClass(), urisToCheck, false);
                if (!unknownUris.isEmpty()) {
                    // #TODO append property for which URI are unknown
                    throw new SPARQLInvalidUriListException("[" + modelMapper.getObjectClass().getSimpleName() + "] Unknown URIS : ", unknownUris);
                }
            }


        }
    }

    private <T extends SPARQLResourceModel> void validateRelations(T instance,SPARQLResourceModel parent) throws Exception {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
        validateRelations(mapper.getRelationsUrisByMapper(instance),parent);

    }

    private <T extends SPARQLResourceModel> void validateRelations(Collection<T> instances,SPARQLResourceModel parent) throws Exception {
        Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers = new HashMap<>();
        for (T instance : instances) {
            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
            mapper.getRelationsUrisByMapper(instance, urisByMappers);
        }

        validateRelations(urisByMappers,parent);
    }

    private <T extends SPARQLResourceModel> void validateReverseRelations(T instance,SPARQLResourceModel parent) throws Exception {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
        validateRelations(mapper.getReverseRelationsUrisByMapper(instance),parent);
    }

    private <T extends SPARQLResourceModel> void validateReverseRelations(Collection<T> instances,SPARQLResourceModel parent) throws Exception {
        Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers = new HashMap<>();
        for (T instance : instances) {
            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
            mapper.getReverseRelationsUrisByMapper(instance, urisByMappers);
        }

        validateRelations(urisByMappers,parent);
    }

    public <T extends SPARQLResourceModel> SPARQLClassObjectMapper<T> getForClass(Class<T> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass);
    }

    @Override
    public SPARQLClassObjectMapperIndex getMapperIndex() {
        return this.connection.getMapperIndex();
    }

    @Override
    public void setMapperIndex(SPARQLClassObjectMapperIndex mapperIndex) {
        this.connection.setMapperIndex(mapperIndex);
    }

    public void clearGraphs(String... graphs) throws Exception {
        try {
            startTransaction();
            for (String graph : graphs) {
                clearGraph(graph);
            }
            commitTransaction();
        } finally {
            rollbackTransaction();
        }
    }

    public void clearGraph(Class<? extends SPARQLResourceModel> resourceClass) throws Exception {
        clearGraph(getMapperIndex().getForClass(resourceClass).getDefaultGraphURI());
    }

    public <T> void insertPrimitives(Node graph, URI uri, Property property, List<T> values, Class<T> valuesType) throws Exception {
        if (values.size() > 0) {

            UpdateBuilder update = new UpdateBuilder();
            Node nodeUri = SPARQLDeserializers.nodeURI(uri);
            SPARQLDeserializer<T> deserializer = SPARQLDeserializers.getForClass(valuesType);

            for (T value : values) {

                update.addInsert(new Quad(graph, nodeUri, property.asNode(), deserializer.getNode(value)));
            }

            executeUpdateQuery(update);
        }
    }

    public void deletePrimitives(Node graph, URI uri, Property property) throws Exception {
        UpdateBuilder delete = new UpdateBuilder();
        Node nodeUri = SPARQLDeserializers.nodeURI(uri);
        delete.addDelete(graph, nodeUri, property, "?value");
        delete.addWhere(nodeUri, property, "?value");
        executeDeleteQuery(delete);
    }

    public void deletePrimitive(Node graph, URI uri, Property property, Object value) throws Exception {
        UpdateBuilder delete = new UpdateBuilder();
        Node nodeUri = SPARQLDeserializers.nodeURI(uri);
        delete.addDelete(graph, nodeUri, property, "?value");
        delete.addWhere(nodeUri, property, "?value");
        delete.addFilter(SPARQLQueryHelper.eq("?value", value));
        executeDeleteQuery(delete);
    }

    public void deleteRelations(Node graph, URI uri, Set<URI> properties) throws Exception {

        Node subjectUri = SPARQLDeserializers.nodeURI(uri);
        Var propertyVar = makeVar("p");
        Node objectVar = makeVar("o");

        UpdateBuilder delete = new UpdateBuilder();
        delete.addDelete(graph, subjectUri, propertyVar, objectVar);

        WhereBuilder where = new WhereBuilder().addWhere(new Triple(subjectUri, propertyVar, objectVar));
        delete.addGraph(graph, where);

        delete.addFilter(SPARQLQueryHelper.inURIFilter(propertyVar, properties));

        executeDeleteQuery(delete);
    }

    public <T> List<T> searchPrimitives(Node graph, URI uri, Property property, Class<T> valuesType) throws Exception {
        List<T> list = new ArrayList<>();
        SelectBuilder select = new SelectBuilder();

        Var valueVar = SPARQLQueryHelper.makeVar("value");
        select.addVar(valueVar);
        select.addWhere(SPARQLDeserializers.nodeURI(uri), property.asNode(), valueVar);
        SPARQLDeserializer<T> deserializer = SPARQLDeserializers.getForClass(valuesType);
        connection.executeSelectQuery(select, results -> {
            try {
                list.add(deserializer.fromString(results.getStringValue(valueVar.getVarName())));
            } catch (Exception ex) {
                LOGGER.warn("Error while deserializing primitive result, your database may be inconsitent (value currently ignored)", ex);
            }
        });

        return list;
    }

    public void copy(Node originGraph, URI objectURI, Node destinationGraph) throws SPARQLException {
        List<URI> uriList = new ArrayList<>();
        uriList.add(objectURI);
        copyAll(originGraph, uriList, destinationGraph);
    }

    public void copyAll(Node originGraph, List<URI> objectList, Node destinationGraph) throws SPARQLException {
        if (objectList.size() > 0) {
            UpdateBuilder copy = new UpdateBuilder();
            int varCount = 0;
            for (URI objectURI : objectList) {
                Node objectURINode = SPARQLDeserializers.nodeURI(objectURI);
                copy.addInsert(destinationGraph, objectURINode, "?p" + varCount, "?o" + varCount);
                copy.addWhere(new WhereBuilder().addGraph(originGraph, objectURINode, "?p" + varCount, "?o" + varCount));
                varCount++;
            }
            executeUpdateQuery(copy);
        }
    }

    public void deleteAll(Node graph, List<URI> objectList) throws SPARQLException {
        if (objectList.size() > 0) {
            UpdateBuilder delete = new UpdateBuilder();
            int varCount = 0;
            for (URI objectURI : objectList) {
                Node objectURINode = SPARQLDeserializers.nodeURI(objectURI);
                delete.addDelete(graph, objectURINode, "?p" + varCount, "?o" + varCount);
                delete.addWhere(new WhereBuilder().addGraph(graph, objectURINode, "?p" + varCount, "?o" + varCount));
                varCount++;
            }
            executeUpdateQuery(delete);
        }
    }

    public void deleteByURI(Node graph, URI uri) throws SPARQLException {
        UpdateBuilder delete = new UpdateBuilder();

        Var s = makeVar("s");
        Var p = makeVar("p");
        Var o = makeVar("o");

        Triple triple = new Triple(s, p, o);
        if (graph != null) {
            Quad quad = new Quad(graph, triple);
            delete.addDelete(quad);
        } else {
            delete.addDelete(triple);
        }

        delete.addWhere(triple);
        Node nodeURI = SPARQLDeserializers.nodeURI(uri);
        delete.addFilter(SPARQLQueryHelper.or(
                SPARQLQueryHelper.eq(s.getVarName(), nodeURI),
                SPARQLQueryHelper.eq(o.getVarName(), nodeURI)
        ));

        executeUpdateQuery(delete);
    }

}
