//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.mem.TupleSlot;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.service.BaseService;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.*;
import org.opensilex.sparql.mapping.SPARQLClassAnalyzer;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.uri.generation.URIGenerator;
import org.opensilex.utils.*;
import org.opensilex.utils.functionnal.ThrowingSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import static org.opensilex.utils.LogFilter.*;

/**
 * Implementation of SPARQLService
 */
@ServiceDefaultDefinition(config = SPARQLServiceConfig.class)
public class SPARQLService extends BaseService implements SPARQLConnection, Service, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLService.class);
    public static final Var URI_VAR = makeVar(SPARQLResourceModel.URI_FIELD);
    public static final Var TYPE_VAR = makeVar(SPARQLResourceModel.TYPE_FIELD);

    public static final String DEFAULT_SPARQL_SERVICE = "sparql";
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL UPDATE\n" + update.buildRequest().toString());
        }
        connection.executeUpdateQuery(update);
    }

    @Override
    public void executeUpdateQuery(String update) throws SPARQLException {
        connection.executeUpdateQuery(update);
    }

    @Override
    public void executeDeleteQuery(UpdateBuilder delete) throws SPARQLException {
        addPrefixes(delete);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL DELETE\n" + delete.buildRequest().toString());
        }
        connection.executeDeleteQuery(delete);
    }

    private int transactionLevel = 0;

    @Override
    public boolean hasActiveTransaction() {
        return connection.hasActiveTransaction();
    }

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
            if(ex != null){
                LOGGER.error("SPARQL TRANSACTION ROLLBACK: {}", ex.getMessage());
            }
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
        if (graph != null) {
            LOGGER.debug("SPARQL LOAD " + format.getName() + " FILE INTO GRAPH: " + graph);
        } else {
            LOGGER.debug("SPARQL LOAD " + format.getName() + " FILE INTO DEFAULT GRAPH");
        }
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


    /**
     *  @throws SPARQLInvalidUriListException if any URI from uris could not be loaded
     */
    public <T extends SPARQLResourceModel> List<T> getListByURIs(Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        return getListByURIs(getDefaultGraph(objectClass), objectClass, uris, lang, null, null);
    }


    /**
     *
     * @param graph object location
     * @param objectClass object class
     * @param uris object URIs
     * @param lang
     * @param resultHandler function used to convert SPARQL results in a custom way (can be null)
     * @param listFieldsToFetch Define which data/object list fields from a {@link SPARQLResourceModel} must be fetched.
     *                          By default these fields are lazily retrieved but you can retrieve these fields directly in a more optimized way (see {@link SPARQLListFetcher}).
     *                          The listFieldsToFetch associate to each field name, a boolean flag to tell if the corresponding triple
     *                          must be added into the query which fetch these fields data.
     * @return a non-null list containing all object which match uris
     * @param <T> object class/type
     * @throws SPARQLInvalidUriListException if any URI from uris could not be loaded
     */
    public <T extends SPARQLResourceModel> List<T> getListByURIs(Node graph, Class<T> objectClass, Collection<URI> uris, String lang,
                                                                 ThrowingFunction<SPARQLResult, T, Exception> resultHandler,
                                                                 Set<String> listFieldsToFetch
    ) throws Exception {
        if (CollectionUtils.isEmpty(uris)) {
            return Collections.emptyList();
        }

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }

        // default fetching behavior
        if(resultHandler == null){
            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
            return mapper.createInstanceList(graph, uris, lang, this);
        }

        // custom result handler -> just load by uris and convert with the given handler
        return this.loadListByURIs(
                graph,
                objectClass,
                uris,
                lang,
                resultHandler,
                listFieldsToFetch == null ? Collections.emptyList() : listFieldsToFetch
        );
    }


    public <T extends SPARQLResourceModel> T loadByURI(Class<T> objectClass, URI uri, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return loadByURI(getDefaultGraph(objectClass), objectClass, uri, lang, filterHandler, null);
    }

    public <T extends SPARQLResourceModel> T loadByURI(Node graph, Class<T> objectClass, URI uri, String lang) throws Exception {
        return loadByURI(graph, objectClass, uri, lang, null, null);
    }

    public <T extends SPARQLResourceModel> T loadByURI(Node graph, Class<T> objectClass, URI uri, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Map<String, WhereHandler> customHandlerByFields) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }

        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang, filterHandler, customHandlerByFields);

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
        return loadListByURIs(getDefaultGraph(objectClass), objectClass, uris, lang, null, null);
    }

    /**
     * @param graph             graph to query
     * @param objectClass       {@link SPARQLResourceModel} class
     * @param uris              the list of URI to load
     * @param lang              lang
     * @param resultHandler     function which define a custom transformation of {@link SPARQLResult} into a instance of T. Can be null
     * @param listFieldsToFetch Define which data/object list fields from a {@link SPARQLResourceModel} must be fetched.
     *                          By default these fields are lazily retrieved but you can retrieve these fields directly in a more optimized way (see {@link SPARQLListFetcher}).
     *                          The listFieldsToFetch associate to each field name, a boolean flag to tell if the corresponding triple
     *                          must be added into the query which fetch these fields data.
     * @param <T>               type of {@link SPARQLResourceModel}
     * @return the list of T corresponding to uris
     * @throws SPARQLInvalidUriListException if any URI from uris could not be loaded
     * @throws Exception if some error is encountered during query execution
     */
    public <T extends SPARQLResourceModel> List<T> loadListByURIs(Node graph, Class<T> objectClass,
                                                                  Collection<URI> uris,
                                                                  String lang,
                                                                  ThrowingFunction<SPARQLResult, T, Exception> resultHandler,
                                                                  Collection<String> listFieldsToFetch) throws Exception {

        if (CollectionUtils.isEmpty(uris)) {
            return Collections.emptyList();
        }

        // get pre-build SelectBuilder for the objectClass
        SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang);

        // append VALUES (?uri) { :uri_1 ... :uri_n } clause
        Set<URI> uniqueUris = new HashSet<>(uris);
        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(uniqueUris);
        select.addValueVar(mapper.getURIFieldExprVar(), uriNodes);

        // set default ORDER BY ?uri. Needed if we use multi-valued properties fetching
        List<T> results = executeSelectQueryAsStream(select).map(
                result -> {
                    try {
                        if (resultHandler == null) {
                            return mapper.createInstance(graph, result, lang != null ? lang : getDefaultLang(), this);
                        } else {
                            return resultHandler.apply(result);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(new SPARQLException(e));
                    }
                }
        ).collect(Collectors.toCollection(() -> new ArrayList<>(uniqueUris.size())));

        // check that all URIS have been loaded, if not then throw SPARQLInvalidUriListException
        if (results.size() < uniqueUris.size()) {

            Set<String> existingUris = results.stream()
                    .map(result -> SPARQLDeserializers.getShortURI(result.getUri()))
                    .collect(Collectors.toSet());

            // compute the list of URI from input uris which were not found from results
            List<URI> unknownUris = uniqueUris.stream()
                    .filter(uri -> !existingUris.contains(SPARQLDeserializers.getShortURI(uri)))
                    .collect(Collectors.toList());

            throw new SPARQLInvalidUriListException("[" + objectClass.getSimpleName() + "] URIs not found: ", unknownUris);
        }

        if(! CollectionUtils.isEmpty(listFieldsToFetch)){
            SPARQLListFetcher<T> listFetcher = new SPARQLListFetcher<>(
                    this,
                    objectClass,
                    graph,
                    listFieldsToFetch,
                    results
            );
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
        SelectBuilder select = mapper.getSelectBuilder(graph, lang, filterHandler, null);

        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        return executeSelectQueryAsStream(select)
                .map(result -> {
                    try {
                        return uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.URI_FIELD));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, null, false, filterHandler, null);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, URI root, boolean excludeRoot, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, root, excludeRoot, filterHandler, null);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, URI root, boolean excludeRoot) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, root, excludeRoot, null, null);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Node graph, Class<T> objectClass, String lang,
                                                                                    URI root, boolean excludeRoot,
                                                                                    ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                                    Map<String, WhereHandler> customHandlerByFields) throws Exception {
        if (lang == null) {
            lang = getDefaultLang();
        }
        List<T> list = search(graph, objectClass, lang, filterHandler, customHandlerByFields, null, null, null, null);

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
        SelectBuilder objectListQuery = mapper.getSelectBuilder(graph, language, filterHandler, null);

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
        return search(getDefaultGraph(objectClass), objectClass, lang, null, null, null, null, null, null);
    }


    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, filterHandler);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(graph, objectClass, lang, filterHandler, null, null, null, null, null);
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, List<OrderBy> orderByList) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, null, null, null, orderByList, null, null);
    }


    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Collection<OrderBy> orderByList) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, filterHandler, null, null, orderByList, null, null);
    }

    public <T extends SPARQLResourceModel> SelectBuilder getSelectBuilder(SPARQLClassObjectMapper<T> mapper, Node graph, String language,
                                                                          ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                          Map<String, WhereHandler> customHandlerByFields,
                                                                          final Collection<OrderBy> orderByList, Integer offset, Integer limit) throws Exception {

        SelectBuilder select = mapper.getSelectBuilder(graph, language, filterHandler, customHandlerByFields);

        if (orderByList != null) {

            Collection<OrderBy> finalOrderByList = orderByList;

            OrderBy defaultOrderBy = SPARQLClassObjectMapper.DEFAULT_ORDER_BY;
            boolean useDefaultOrder = orderByList.stream().anyMatch(orderBy -> orderBy.getFieldName().equals(defaultOrderBy.getFieldName()));

            // add the default order in order to maintain a strict order in case where two results are equals according
            // orderByList. Else results can be non-deterministic since there are no guarantee or result order from triplestore
            if (!useDefaultOrder) {

                // copy initial list in order to not update/write initial list
                finalOrderByList = new LinkedList<>(orderByList);
                finalOrderByList.add(defaultOrderBy);
            }

            for (OrderBy orderBy : finalOrderByList) {
                Expr fieldOrderExpr = mapper.getFieldOrderExpr(orderBy.getFieldName());
                if (fieldOrderExpr != null) {
                    select.addOrderBy(fieldOrderExpr, orderBy.getOrder());
                }
            }
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
     * @param graph                 graph to query
     * @param objectClass           {@link SPARQLResourceModel} class
     * @param lang                  lang
     * @param filterHandler         function defined to update the SPARQL query used for search.
     * @param customHandlerByFields map between {@link SPARQLResourceModel} field and a custom {@link WhereHandler}.
     *                              By default, all fields are automatically handled, but in some case you may want to specify
     *                              a custom way to handle this field into the query. Can be null
     * @param resultHandler         function which define a custom transformation of {@link SPARQLResult} into a instance of T. Can be null
     * @param orderByList
     * @param offset
     * @param limit
     * @param <T>                   type of {@link SPARQLResourceModel}
     * @return a list of T
     * @throws Exception
     */
    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang,
                                                          ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                          Map<String, WhereHandler> customHandlerByFields,
                                                          ThrowingFunction<SPARQLResult, T, Exception> resultHandler,
                                                          Collection<OrderBy> orderByList, Integer offset, Integer limit) throws Exception {

        Stream<T> stream = searchAsStream(graph, objectClass, lang, filterHandler, customHandlerByFields, resultHandler, orderByList, offset, limit);

        // convert stream to a list with limit as list capacity
        if (limit != null && limit > 0) {
            return stream.collect(Collectors.toCollection(() -> new ArrayList<>(limit)));
        } else {
            return stream.collect(Collectors.toList());
        }

    }

    public <T extends SPARQLResourceModel> Stream<T> searchAsStream(Node graph, Class<T> objectClass, String lang,
                                                                    ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                    Map<String, WhereHandler> customHandlerByFields,
                                                                    ThrowingFunction<SPARQLResult, T, Exception> resultHandler,
                                                                    Collection<OrderBy> orderByList, Integer offset, Integer limit) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        String language;
        if (lang == null) {
            language = getDefaultLang();
        } else {
            language = lang;
        }

        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = getSelectBuilder(mapper, graph, language, filterHandler, customHandlerByFields, orderByList, offset, limit);

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
        return count(getDefaultGraph(objectClass), objectClass, null, null, null);
    }

    public <T extends SPARQLResourceModel> int count(Node graph, Class<T> objectClass) throws Exception {
        return count(graph, objectClass, null, null, null);

    }

    public <T extends SPARQLResourceModel> int count(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Map<String, WhereHandler> customHandlerByFields) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder selectCount = mapper.getCountBuilder(graph, "count", lang, filterHandler, customHandlerByFields);

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
        return searchWithPagination(getDefaultGraph(objectClass), objectClass, lang, filterHandler, null, null, orderByList, page, pageSize);
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Node graph,
                                                                                      Class<T> objectClass,
                                                                                      String lang,
                                                                                      ThrowingConsumer<SelectBuilder, Exception> filterHandler,
                                                                                      Map<String, WhereHandler> customHandlerByFields,
                                                                                      ThrowingFunction<SPARQLResult, T, Exception> resultHandler,
                                                                                      Collection<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        if (lang == null) {
            lang = getDefaultLang();
        }
        int total = count(graph, objectClass, lang, filterHandler, customHandlerByFields);

        List<T> list;
        if (pageSize == null || pageSize == 0) {
            list = search(graph, objectClass, lang, filterHandler, customHandlerByFields, resultHandler, orderByList, null, null);
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
            list = search(graph, objectClass, lang, filterHandler, customHandlerByFields, resultHandler, orderByList, offset, limit);
        } else {
            list = new ArrayList<>();
        }

        return new ListWithPagination<>(list, page, pageSize, total);

    }

    public <T extends SPARQLResourceModel> void create(T instance) throws Exception {
        create(getDefaultGraph(instance.getClass()), instance);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance) throws Exception {
        create(graph, instance, true, true);
    }

    public <T extends SPARQLResourceModel> void create(T instance, boolean checkUriExist) throws Exception {
        create(getDefaultGraph(instance.getClass()), instance, checkUriExist, true);
    }

    public <T extends SPARQLResourceModel> void create(T instance, boolean checkUriExist, boolean setPublicationDate) throws Exception {
        create(getDefaultGraph(instance.getClass()), instance, checkUriExist, setPublicationDate);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance, boolean checkUriExist) throws Exception {
        create(graph, instance, null, null, checkUriExist, true, false, null);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance, boolean checkUriExist, boolean setPublicationDate) throws Exception {
        create(graph, instance, null, null, checkUriExist, setPublicationDate, false, null);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, T instance, boolean checkUriExist, boolean setPublicationDate, boolean blankNode, BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {
        create(graph, instance, null, null, checkUriExist, setPublicationDate, blankNode, createExtension);
    }


    /**
     * @param graph           the graph onto instance are created
     * @param instance        the instance to create
     * @param updateBuilder   an UpdateBuilder which can be updated by adding new statements if not null
     * @param checkUriExist   indicate if the service must check if instances already exist
     * @param setPublicationDate indicate if the service must set the publicationDate
     * @param blankNode       indicate if the instance URI must be a blank node
     * @param createExtension additional modification to the UpdateBuilder
     * @param <T>             the SPARQLResourceModel type
     */
    protected <T extends SPARQLResourceModel> void create(Node graph,
                                                          T instance,
                                                          SPARQLResourceModel parent,
                                                          UpdateBuilder updateBuilder,
                                                          boolean checkUriExist,
                                                          boolean setPublicationDate,
                                                          boolean blankNode,
                                                          BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {

        if (Objects.isNull(instance.getPublicationDate()) && setPublicationDate) {
            instance.setPublicationDate(OffsetDateTime.now());
        }

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(instance.getClass());

        boolean useNewBuilder = updateBuilder == null;
        UpdateBuilder create = useNewBuilder ? new UpdateBuilder() : updateBuilder;
        prepareInstanceCreation(graph, instance, parent, mapper, create, checkUriExist, blankNode);
        mapper.addCreateBuilder(graph, instance, create, blankNode, createExtension);

        if (useNewBuilder) {
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
    protected <T extends SPARQLResourceModel> void prepareInstanceCreation(
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

        validate(instance, parent);

        URI subjectGraph = graph != null ? URI.create(graph.toString()) : null;

        Map<URI, List<SPARQLResourceModel>> nestedResources = mapper.getNestedInstancesByGraph(subjectGraph, instance);
        for(Map.Entry<URI,List<SPARQLResourceModel>> entry : nestedResources.entrySet()){

            URI subGraph = entry.getKey();
            Node subGraphNode = subGraph != null ? SPARQLDeserializers.nodeURI(subGraph) : null;

            for (SPARQLResourceModel subInstance :  entry.getValue()) {
                create(subGraphNode, subInstance, instance, subInstanceUpdateBuilder, checkUriExist, true, blankNode, null);
            }
        }
    }

    public <T extends SPARQLResourceModel> void create(Collection<T> instances) throws Exception {
        Optional<T> anyElement = instances.stream().findAny();
        if (!anyElement.isPresent()) {
            return;
        }
        create(getDefaultGraph(anyElement.get().getClass()), instances);
    }

    public <T extends SPARQLResourceModel> void create(Class<T> clazz, Collection<T> instances) throws Exception {
        create(getDefaultGraph(clazz), instances);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, Collection<T> instances) throws Exception {
        create(graph, instances, null, true, true);
    }

    public static final int DEFAULT_MAX_INSTANCE_PER_QUERY = 1000;


    public <R> R withTransaction(ThrowingSupplier<R,Exception> operation) throws Exception {
        try{
            startTransaction();
            R result = operation.get();
            commitTransaction();
            return result;
        }catch (Exception e){
            rollbackTransaction(e);
            throw e;
        }
    }

    public <T extends SPARQLResourceModel> void createWithoutTransaction(Node graph, Collection<T> instances, Integer maxInstancePerQuery, boolean checkUriExist, boolean setPublicationDate) throws Exception {

        boolean reuseSameQuery = maxInstancePerQuery != null;
        if (reuseSameQuery && maxInstancePerQuery <= 0) {
            throw new IllegalArgumentException("maxInstancePerQuery must be strictly positive : " + maxInstancePerQuery);
        }

        if(instances.isEmpty()){
            return;
        }
        Instant start = Instant.now();

        validate(instances, null);
        UpdateBuilder updateBuilder = new UpdateBuilder();

        // use the same query for the instance and her sub-instance if a query batch size is specified
        UpdateBuilder subInstanceUpdateBuilder = reuseSameQuery ? updateBuilder : null;

        // set a maximum number of instance to insert into one query in order to ensure that the INSERT query will not be too big
        int insertedInstanceNb = 0;

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();

        for (T instance : instances) {
            if (Objects.isNull(instance.getPublicationDate()) && setPublicationDate) {
                instance.setPublicationDate(OffsetDateTime.now());
            }

            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(instance.getClass());
            prepareInstanceCreation(graph, instance, null, mapper, subInstanceUpdateBuilder, checkUriExist, false);
            mapper.addCreateBuilder(graph, instance, updateBuilder, false, null);

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

        long durationMs = Duration.between(start, Instant.now()).toMillis();
        LOGGER.debug("{} {}, connection: {}, insertCount: {}, duration: {} ms", kv(LOG_TYPE_KEY, "insertMany"), kv(LOG_STATUS_LOG_KEY, LOG_STATUS_OK), connection, instances.size(), kv(LOG_DURATION_MS_KEY, durationMs));
    }

    /**
     * @param graph               the graph onto instance are created
     * @param instances           the list of instance to create
     * @param maxInstancePerQuery number of instance to put in one query, if null then one query per instance is used
     * @param checkUriExist       indicate if the service must check if instances already exist
     * @param <T>                 the SPARQLResourceModel type
     */
    public <T extends SPARQLResourceModel> void create(Node graph, Collection<T> instances, Integer maxInstancePerQuery, boolean checkUriExist, boolean setPublicationDate) throws Exception {
        withTransaction(() -> {
            createWithoutTransaction(graph, instances, maxInstancePerQuery, checkUriExist, setPublicationDate);
            return null;
        });
    }

    /**
     * Generate a unique URI if instance URI is not set.
     * if checkUriExist is true, then the function re-generate a new URI while the URI is not unique inside graph
     * @param graph graph in which we check if the generated URI exists or not
     * @param instance model for which we generate a new URI
     * @param uriGenerator generation of new URI
     * @param checkUriExist indicate if we must check the existence of the generated URI
     * @param <T> type of SPARQLResourceModel
     */
    public <T extends SPARQLResourceModel> void generateUniqueURI(Node graph, T instance, URIGenerator<T> uriGenerator, boolean checkUriExist) throws SPARQLException, URISyntaxException {
        URI uri = instance.getUri();
        if (uri == null) {

            int retry = 0;
            String prefix = getDefaultGenerationURI(instance.getClass()).toString();
            uri = uriGenerator.generateURI(prefix, instance, retry);

            if (checkUriExist) {
                while (uriExists(graph, uri)) {
                    uri = uriGenerator.generateURI(prefix, instance, ++retry);
                }
            }
            instance.setUri(uri);
        }
    }

    public  <T extends SPARQLResourceModel> void generateUniqueUriIfNullOrValidateCurrent(Node graph, SPARQLClassObjectMapper<T> mapper, T instance, boolean checkUriExist) throws Exception {
        URIGenerator<T> uriGenerator = mapper.getUriGenerator(instance);
        URI uri = instance.getUri();
        if (uri == null) {
            generateUniqueURI(graph,instance,uriGenerator,checkUriExist);

            // only ensure that the URI hasn't some outgoing relation
            // the URI can have some in relations without problem (ex : skos or other in-coming relation)
        } else if (checkUriExist && uriExists(graph, uri,true,false)) {
            throw new SPARQLAlreadyExistingUriException(uri);
        }
    }

    public <T extends SPARQLResourceModel> void update(T instance) throws Exception {
        update(getDefaultGraph(instance.getClass()), instance);
    }


    private <T extends SPARQLResourceModel> void updateAutoUpdateFields(SPARQLClassObjectMapper<T> mapper, T oldInstance, T instance) throws Exception {

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
                if (!SPARQLDeserializers.compareURIs(oldFieldValue.getUri(), newFieldValue.getUri())) {
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
                        oldURIs.put(SPARQLDeserializers.formatURI(ofValue.getUri()), ofValue.getClass());
                    }
                }
                for (SPARQLResourceModel nfValue : newFieldValue) {
                    if (nfValue != null && nfValue.getUri() != null && oldURIs.containsKey(SPARQLDeserializers.formatURI(nfValue.getUri()))) {
                        autoUpdateFieldsToUpdate.add(nfValue);
                        oldURIs.remove(SPARQLDeserializers.formatURI(nfValue.getUri()));
                    }
                }
                autoUpdateFieldsToDelete.putAll(oldURIs);
            }
        }

        deleteURIClassMap(autoUpdateFieldsToDelete);
        update(autoUpdateFieldsToUpdate);
    }


    private static final  String NO_CLASS_MODEL_ERROR_MSG = "[%s] No ClassModel associated to type %s ." +
            " Add the corresponding ClassModel definition into your triplestore or remove the handleCustomProperties annotation on your model";

    /**
     *
     * @param graph
     * @param mapper
     * @param instance
     * @param <T>
     * @throws SPARQLException
     */
    private <T extends SPARQLResourceModel> void deleteCustomRelations(Node graph, SPARQLClassObjectMapper<T> mapper, T instance) throws SPARQLException {

        SPARQLClassAnalyzer analyzer = mapper.getClassAnalyzer();

        // dont handle model with no custom properties or model with default type
        if (!analyzer.isHandleCustomProperties() || instance.getType() == null) {
            return;
        }

        // try to retrieve associated ClassModel
        URI rootType = analyzer.getRdfTypeURI();
        ClassModel classModel;
        try {
            classModel = new OntologyDAO(this).getClassModel(instance.getType(), rootType, OpenSilex.DEFAULT_LANGUAGE);
        } catch (SPARQLInvalidURIException e) {
            throw new SPARQLInvalidModelException(String.format(NO_CLASS_MODEL_ERROR_MSG, instance.getClass().toString(), rootType.toString()));
        }

        if(MapUtils.isEmpty(classModel.getRestrictionsByProperties())){
            return;
        }

        Set<String> managedPropUris = mapper.getClassAnalyzer().getManagedPropertiesUris();

        // compute the set of custom properties : all properties from ClassModel restrictions which are not already managed
        Set<URI> customProperties = classModel.getRestrictionsByProperties()
                .values()
                .stream()
                .map(OwlRestrictionModel::getOnProperty)
                .filter(property -> ! managedPropUris.contains(property.toString()))
                .collect(Collectors.toSet());

        if(! customProperties.isEmpty()){
            deleteRelations(graph, instance.getUri(), customProperties);
        }
    }

    public <T extends SPARQLResourceModel> void update(Node graph, T instance) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();

        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) instance.getClass();
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);

        try {
            // @TODO : like for create/createWithException, allow to run this method without direct transaction handling and add another method
            startTransaction();

            // load old instance and replace new fields
            URI uri = mapper.getURI(instance);
            T oldInstance = loadByURI(graph, objectClass, uri, getDefaultLang());
            if (oldInstance == null) {
                throw new SPARQLInvalidURIException(instance.getUri());
            }
            mapper.updateInstanceFromOldValues(oldInstance, instance);

            // update dependants fields
            updateAutoUpdateFields(mapper, oldInstance, instance);

            deleteCustomRelations(graph,mapper,instance);

            // delete and create new instance
            UpdateBuilder delete = mapper.getDeleteBuilder(graph, oldInstance);
            executeDeleteQuery(delete);
            create(graph, instance, false, false);

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
            // @TODO : like for create/createWithException, allow to run this method without direct transaction handling and add another method
            startTransaction();

            if (instances.size() > 0) {

                validate(instances, null);

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

        // load object by uri in order to directly check if the object exist or not
        T instance = loadByURI(graph, objectClass, uri, getDefaultLang());
        if ( Objects.isNull( instance ) ){
            throw new NotFoundURIException(uri);
        }

        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        try {
            // @TODO : like for create/createWithException, allow to run this method without direct transaction handling and add another method
            startTransaction();
            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);

            // DELETE CASCADE object
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

            for (Map.Entry<Class<? extends SPARQLResourceModel>, List<URI>> relationToDelete : reverseRelationsToDelete.entrySet()) {
                delete(relationToDelete.getKey(), relationToDelete.getValue());
            }

            // DELETE reverse relations
            Iterator<Map.Entry<Class<? extends SPARQLResourceModel>, Field>> i = mapperIndex.getReverseReferenceIterator(objectClass);
            Node uriNode = SPARQLDeserializers.nodeURI(uri);

            while (i.hasNext()) {
                UpdateBuilder deleteAllReverseReferencesBuilder = new UpdateBuilder();
                Map.Entry<Class<? extends SPARQLResourceModel>, Field> entry = i.next();
                String uriVar = "?uri";
                SPARQLClassObjectMapper<SPARQLResourceModel> reverseMapper = mapperIndex.getForClass(entry.getKey());
                Property reverseProp = reverseMapper.getFieldProperty(entry.getValue());
                Node defaultGraph = reverseMapper.getDefaultGraph();

                if (defaultGraph != null) {
                    deleteAllReverseReferencesBuilder.addDelete(reverseMapper.getDefaultGraph(), uriVar, reverseProp, uriNode);
                } else {
                    deleteAllReverseReferencesBuilder.addDelete(uriVar, reverseProp, uriNode);
                }
                deleteAllReverseReferencesBuilder.addWhere(uriVar, reverseProp, uriNode);
                executeDeleteQuery(deleteAllReverseReferencesBuilder);
            }

            deleteCustomRelations(graph,mapper,instance);

            UpdateBuilder delete = mapper.getDeleteBuilder(graph, instance);
            executeDeleteQuery(delete);

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
                // @TODO : like for create/createWithException, allow to run this method without direct transaction handling and add another method
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
                // @TODO : like for create/createWithException, allow to run this method without direct transaction handling and add another method
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

    public boolean existInstanceOf(URI classURI) throws SPARQLException {

        Node classNode = NodeFactory.createURI(URIDeserializer.getExpandedURI(classURI));
        Var type = makeVar("type");
        Var instance = makeVar("instance");

        AskBuilder askQuery = new AskBuilder()
                .addWhere(classNode,RDF.type, OWL2.Class)
                .addWhere(type,Ontology.subClassAny,classNode)
                .addWhere(instance,RDF.type,type);

        return executeAskQuery(askQuery);
    }

    public boolean anyPropertyValue(URI property) throws SPARQLException {

        Node propertyNode = NodeFactory.createURI(URIDeserializer.getExpandedURI(property));
        Var instance = makeVar("instance");
        Var value = makeVar("value");

        return executeAskQuery(new AskBuilder()
                .addWhere(instance,propertyNode,value)
        );
    }

    /**
     *
     * @param graph SPARQL GRAPH in which search for URI existence (optional)
     * @param uri URI to check (required)
     * @param checkOutRelations true if outgoing relation existence must be checked, false else
     * @param checkInRelations true if incoming relation existence must be checked, false else
     * @return true if uri exist withing graph or in global repository (if graph is null).
     *
     * @throws SPARQLException if an error occurs during SPARQL ASK query execution
     * @throws IllegalArgumentException if uri is null or if both checkInRelations and checkOutRelations are false
     *
     * @apiNote
     * <pre>
     * Example of generated query with {@code uriExists(test:my_graph,<my_uri>,true,true)} call :
     *
     * {@code
     *
     * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     *
     * ASK WHERE {
     *      GRAPH test:my_graph {
     *          <my_uri> ?p_out ?o
     *      }
     *      UNION {
     *          GRAPH test:my_graph {
     *             ?s ?p_in <my_uri>
     *          }
     *      }
     *
     * }
     * }
     * </pre>
     */
    public boolean uriExists(Node graph, URI uri, boolean checkOutRelations, boolean checkInRelations) throws SPARQLException{

        if(! checkInRelations && ! checkOutRelations){
            throw new IllegalArgumentException("checkOutRelations and checkInRelations are both equals to false");
        }

        Objects.requireNonNull(uri);
        AskBuilder askQuery = new AskBuilder();

        Var subject = makeVar("s");
        Var outPredicate = makeVar("p_out");
        Var inPredicate = makeVar("p_in");
        Var object = makeVar("o");

        Node nodeUri = SPARQLDeserializers.nodeURI(uri);

        // check if incoming relation exists
        WhereBuilder outWhere = null;
        if(checkOutRelations){
            outWhere = new WhereBuilder();
            if (graph != null) {
                outWhere.addGraph(graph, nodeUri, outPredicate, object);
            } else {
                outWhere.addWhere(nodeUri, outPredicate, object);
            }
            askQuery.addWhere(outWhere);
        }

        // check if outgoing relation exists
        WhereBuilder inWhere;
        if(checkInRelations){
            inWhere = new WhereBuilder();
            if (graph != null) {
                inWhere.addGraph(graph, subject,inPredicate,nodeUri);
            } else {
                inWhere.addWhere(subject,inPredicate,nodeUri);
            }
            if(outWhere == null){
                askQuery.addWhere(inWhere);
            }else{
                askQuery.addUnion(inWhere);
            }
        }
        return executeAskQuery(askQuery);
    }

    /**
     *
     * @param graph SPARQL GRAPH in which search for URI existence (optional)
     * @param uri URI to check (not-null)
     * @return true if uri exist in the repository as subject or as object
     *
     * @throws SPARQLException if an error occurs during SPARQL ASK query execution
     * @throws IllegalArgumentException if uri is null
     *
     * @apiNote
     * The generated query is
     * <pre>
     *{@code
     *
     * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     *
     * ASK WHERE {
     *      {
     *          GRAPH test:my_graph {
     *              <my_uri> ?p ?o
     *          }
     *      }
     *      UNION {
    *           GRAPH test:my_graph {
     *              ?s ?p <my_uri>
     *          }
     *      }
     *
     * }
     * }
     * </pre>
     */
    public boolean uriExists(Node graph, URI uri) throws SPARQLException {
        Objects.requireNonNull(uri);

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

    public <T extends SPARQLResourceModel> boolean uriExists(Class<T> objectClass, URI uri) throws SPARQLException {
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
     * @param objectClass The model class to check
     * @param uris The collection of URIs to check
     * @param checkExist If true, checks the existence of the given URIs. If false, checks the absence of the given
     *                   URIs.
     * @param graph The graph to look for URIs (can be null to check in all graphs)
     * @param <T>         the SPARQLResourceModel type
     * @return the Set of unknown or existing URI from the given URI collection
     */
    public <T extends SPARQLResourceModel> Set<URI> getExistingUriStream(Class<T> objectClass, Stream<URI> uris,
                                                                         int size,
                                                                         boolean checkExist, Node graph) throws Exception {
        if (size == 0) {
            return Collections.emptySet();
        }

        SelectBuilder selectQuery = getUnknownUrisQuery(objectClass, uris, size, checkExist, graph);

        return executeSelectQueryAsStream(selectQuery).map(result -> {
            try {
                return new URI(result.getStringValue(SPARQLResourceModel.URI_FIELD));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());

    }

    /**
     * Checks the existence or absence of a collection of URIs in any graph. Short-hand for
     * {@link SPARQLService#getExistingUriStream(Class, Stream, int, boolean, Node)}, using the `stream` method of the
     * collection and passing `null` as the graph parameter.
     *
     * @param objectClass The model class to check
     * @param uris The collection of URIs to check
     * @param checkExist If true, checks the existence of the given URIs. If false, checks the absence of the given
     *                   URIs.
     * @param <T>         the SPARQLResourceModel type
     */
    public <T extends SPARQLResourceModel> Set<URI> getExistingUris(Class<T> objectClass, Collection<URI> uris,
                                                                    boolean checkExist) throws Exception {
        return getExistingUriStream(objectClass, uris.stream(), uris.size(), checkExist, null);
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

    /**
     * @param type the rdf:type (if null/empty then the query only search if there exist an occurrence of <b>( ?uri a ?rdfType )</b> triple pattern for each URI
     * @param uris the {@link Collection} of URI to check in {@link String} representation
     * @return a {@link SelectBuilder} which when executed, indicate for each element of uris, if the element exist (TRUE/FALSE)
     * as an instance of the type (or as any rdf:type if no type is provided)
     *
     * @apiNote The produced SPARQL query look likes
     * <pre>
     * {@code
     * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     * SELECT  (EXISTS {
     *         ?rdfType rdfs:subClassOf* :some_rdf_type.
     *         ?uri  a  ?rdfType
     *     } AS ?existing)
     * WHERE{
     *     VALUES ?uri {
     *         :uri_1 :uri_2 :uri_n
     *     }
     * }
     * }</pre>
     */
    public SelectBuilder getCheckUriListExistQuery(Stream<String> uris, int streamSize, String type, Node graph) {

        Objects.requireNonNull(uris);

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var existing = makeVar(SPARQLService.EXISTING_VAR);

        WhereBuilder where = new WhereBuilder();

        // check if URIs must have some type
        if (! StringUtils.isEmpty(type)) {
            where.addWhere(typeVar, Ontology.subClassAny, NodeFactory.createURI(URIDeserializer.getExpandedURI(type)));
        }

        // search inside graph, if provided
        if(graph != null){
            Node expandedGraph = NodeFactory.createURI(URIDeserializer.getExpandedURI(graph.toString()));
            where.addGraph(expandedGraph,uriVar, RDF.type, typeVar);
        }else{
            where.addWhere(uriVar, RDF.type, typeVar);
        }

        // add EXIST {} expression as var of SELECT
        SelectBuilder select = new SelectBuilder()
                .addVar(SPARQLQueryHelper.getExprFactory().exists(where), existing);

        // append VALUES ?uri  :uri_1 ... :uri_n
        SPARQLQueryHelper.addWhereUriStringValues(select, uriVar.getVarName(), uris, true, streamSize);

        return select;
    }

    public <T extends SPARQLResourceModel> SelectBuilder getUriListExistQuery(Class<T> objectClass, Stream<URI> uris, int streamSize) throws Exception {
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var existing = makeVar(EXISTING_VAR);

        SelectBuilder select = new SelectBuilder();
        SPARQLQueryHelper.addWhereUriValues(select, uriVar.getVarName(), uris,streamSize);

        WhereBuilder where = new WhereBuilder();

        if (objectClass != null) {
            SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);
            Var typeVar = mapper.getTypeFieldVar();
            Resource typeDef = mapper.getRDFType();
            where.addWhere(typeVar, Ontology.subClassAny, typeDef)
                    .addWhere(uriVar, RDF.type, typeVar);
        }else{
            where.addWhere(uriVar, makeVar("p"), makeVar("o"));
        }

        Expr existExpr = SPARQLQueryHelper.getExprFactory().exists(where);
        select.addVar(existExpr, existing);

        return select;
    }

    public <T extends SPARQLResourceModel> SelectBuilder getUriListExistQuery(Class<T> objectClass, Collection<URI> uris) throws Exception {
        return getUriListExistQuery(objectClass, uris.stream(), uris.size());
    }


    /**
     * @param objectClass the models class
     * @param uris        the URIs to check
     * @param checkExist  indicates if we check the existence or the non-existence of the given URI collection
     * @param <T>         the SPARQLResourceModel type
     * @return the query which return the set of existing/non-existing URIS
     */
    public <T extends SPARQLResourceModel> SelectBuilder getUnknownUrisQuery(Class<T> objectClass,
                                                                             Stream<URI> uris,
                                                                             int size, boolean checkExist,
                                                                             Node graph) throws Exception {

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);

        SelectBuilder select = new SelectBuilder();
        SPARQLQueryHelper.addWhereUriValues(select, uriVar.getVarName(), uris, size);

        WhereBuilder where = new WhereBuilder();

        if (graph != null) {
            where.addGraph(graph, uriVar, makeVar("p"), makeVar("o"));
        } else {
            where.addWhere(uriVar, makeVar("p"), makeVar("o"));
        }

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

            if(graph != null){
                insertQuery.addInsert(graph, SPARQLDeserializers.nodeURI(subject), property, nodeValue);
            }else{
                insertQuery.addInsert(SPARQLDeserializers.nodeURI(subject), property, nodeValue);
            }

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

    public URI getDefaultGraphURI(URI rdfType) throws NotFoundException, SPARQLInvalidClassDefinitionException {
        try {
            return getMapperIndex()
                    .getForResource(Ontology.resource(SPARQLDeserializers.getExpandedURI(rdfType)))
                    .getDefaultGraphURI();
        } catch (SPARQLMapperNotFoundException e) {
            throw new NotFoundException("No default graph found for RDF type <" + rdfType + ">");
        }
    }

    public Node getDefaultGraph(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getDefaultGraph();
    }

    public URI getDefaultGraphURI(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        SPARQLClassObjectMapperIndex i = getMapperIndex();
        SPARQLClassObjectMapper<SPARQLResourceModel> g = i.getForClass(modelClass);
        return g.getDefaultGraphURI();
    }

    public <T extends SPARQLResourceModel> Resource getRDFType(Class<T> objectClass) throws SPARQLException {
        return getMapperIndex().getForClass(objectClass).getRDFType();
    }

    public <T extends SPARQLResourceModel> String getRDFTypeURI(Class<T> objectClass) throws SPARQLException {
        return getRDFType(objectClass).getURI();
    }

    public URI getDefaultGenerationURI(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getGenerationPrefixURI();
    }

    public URI getBaseURI() {
        try{
            return getOpenSilex().getModuleByClass(SPARQLModule.class).getBaseURI();
        }catch (OpenSilexModuleNotFoundException e){
            throw new RuntimeException(e);
        }
    }

    public Var getURIFieldVar(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getURIFieldVar();
    }

    public <T extends SPARQLResourceModel> void validate(T instance, SPARQLResourceModel parent) throws Exception {
        if (!this.isShaclEnabled()) {
            validateRelations(instance, parent);
        }

        validateReverseRelations(instance, parent);
    }

    public <T extends SPARQLResourceModel> void validate(Collection<T> instances, SPARQLResourceModel parent) throws Exception {
        if (!this.isShaclEnabled()) {
            validateRelations(instances, parent);
        }

        validateReverseRelations(instances, parent);
    }

    private <T extends SPARQLResourceModel> void validateRelations(Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers, SPARQLResourceModel parent) throws Exception {
        for (Map.Entry<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMapper : urisByMappers.entrySet()) {
            SPARQLClassObjectMapper<SPARQLResourceModel> modelMapper = urisByMapper.getKey();
            Set<URI> urisToCheck = urisByMapper.getValue();

            // remove parent uri from uris to check, because the validation of the current relation come after the validation of the parent.
            // the parent is not yet present in the triplestore, since it must be created, so we must not include the parent URI in uris to check.
            if (parent != null) {
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

    private <T extends SPARQLResourceModel> void validateRelations(T instance, SPARQLResourceModel parent) throws Exception {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
        validateRelations(mapper.getRelationsUrisByMapper(instance), parent);

    }

    private <T extends SPARQLResourceModel> void validateRelations(Collection<T> instances, SPARQLResourceModel parent) throws Exception {
        Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers = new HashMap<>();
        for (T instance : instances) {
            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
            mapper.getRelationsUrisByMapper(instance, urisByMappers);
        }

        validateRelations(urisByMappers, parent);
    }

    private <T extends SPARQLResourceModel> void validateReverseRelations(T instance, SPARQLResourceModel parent) throws Exception {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
        validateRelations(mapper.getReverseRelationsUrisByMapper(instance), parent);
    }

    private <T extends SPARQLResourceModel> void validateReverseRelations(Collection<T> instances, SPARQLResourceModel parent) throws Exception {
        Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers = new HashMap<>();
        for (T instance : instances) {
            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
            mapper.getReverseRelationsUrisByMapper(instance, urisByMappers);
        }

        validateRelations(urisByMappers, parent);
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
        }catch (Exception e){
            rollbackTransaction();
            throw e;
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

    public void deleteRelations(Node graph, URI uri, Set<URI> properties) throws SPARQLException {

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

    public List<URI> getRdfTypes(URI uri, Node graph) throws SPARQLException {

        Var typeVar = makeVar("type");
        SelectBuilder select = new SelectBuilder().addVar(typeVar).setDistinct(true);
        Node subject = SPARQLDeserializers.nodeURI(uri);

        if(graph != null){
            select.addGraph(graph,subject, RDF.type, typeVar);
        }else{
            select.addWhere(subject, RDF.type, typeVar);
        }

        return connection.executeSelectQueryAsStream(select)
                .map(result -> URIDeserializer.formatURI(result.getStringValue(typeVar.getVarName())))
                .collect(Collectors.toList());
    }

    /**
     *
     * @param graph the SPARQL graph (optional)
     * @param uri a specified resource URI, for which primitives are searched (optional)
     * @param property the RDF property (required)
     * @param valuesType object/value type (required)
     * @return the List of primitives values
     *
     * @apiNote The query append a filter on datatype according the specified Class value type.
     * So values which don't match the given datatype will not be returned by the database
     */
    public <T> List<T> searchPrimitives(Node graph, URI uri, Property property, Class<T> valuesType) throws Exception {

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        SPARQLDeserializer<T> deserializer = SPARQLDeserializers.getForClass(valuesType);

        // use the specified uri, else use a variable
        Node subject = uri != null ? SPARQLDeserializers.nodeURI(uri) : makeVar("uri");
        Var valueVar = SPARQLQueryHelper.makeVar("value");
        Node dataType = NodeFactory.createURI(deserializer.getDataType().getURI());

        SelectBuilder select = new SelectBuilder().addVar(valueVar);
        if(graph != null){
            select.addGraph(graph,subject, property.asNode(), valueVar);
        }else{
            select.addWhere(subject, property.asNode(), valueVar);
        }
        select.addFilter(exprFactory.eq(exprFactory.datatype(valueVar), dataType));

        // execute query and parse with expected deserializer
        List<T> list = new ArrayList<>();

        connection.executeSelectQueryAsStream(select).forEach(results -> {
            try {
                list.add(deserializer.fromString(results.getStringValue(valueVar.getVarName())));
            } catch (Exception ex) {
                LOGGER.warn("Error while deserializing primitive result, your database may be inconsistent (value currently ignored)", ex);
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

    /**
     * Checks if the given URI exists in any of the triple of the triple store.
     *
     * @param uri The URI to check
     * @return whether the given URI matches any triple
     * @throws Exception
     */
    public boolean checkTripleURIExists(URI uri) throws Exception {
        try {
            AskBuilder ask = new AskBuilder();
            ask.addWhere(SPARQLQueryHelper.buildURIExistsClause(uri, TupleSlot.SUBJECT, true));
            ask.addUnion(SPARQLQueryHelper.buildURIExistsClause(uri, TupleSlot.SUBJECT, false));
            ask.addUnion(SPARQLQueryHelper.buildURIExistsClause(uri, TupleSlot.PREDICATE, true));
            ask.addUnion(SPARQLQueryHelper.buildURIExistsClause(uri, TupleSlot.PREDICATE, false));
            ask.addUnion(SPARQLQueryHelper.buildURIExistsClause(uri, TupleSlot.OBJECT, true));
            ask.addUnion(SPARQLQueryHelper.buildURIExistsClause(uri, TupleSlot.OBJECT, false));
            return executeAskQuery(ask);
        } catch (Exception e) {
            LOGGER.error("Failed to check URI " + uri, e);
            throw new SPARQLException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Updates all occurrences of a URI in the triple store.
     *
     * @param oldUri The URI to replace
     * @param newUri The new URI
     * @throws SPARQLException
     */
    public void renameTripleURI(URI oldUri, URI newUri) throws Exception {
        try {
            startTransaction();
            renameTripleURI(oldUri, newUri, TupleSlot.SUBJECT, true);
            renameTripleURI(oldUri, newUri, TupleSlot.SUBJECT, false);
            renameTripleURI(oldUri, newUri, TupleSlot.PREDICATE, true);
            renameTripleURI(oldUri, newUri, TupleSlot.PREDICATE, false);
            renameTripleURI(oldUri, newUri, TupleSlot.OBJECT, true);
            renameTripleURI(oldUri, newUri, TupleSlot.OBJECT, false);
            commitTransaction();
        } catch (Exception e) {
            LOGGER.error("Failed to update URI " + oldUri + " to " + newUri, e);
            rollbackTransaction(e);
            throw new SPARQLException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Updates an URI based on its position in triples and whether it is located in a named graph.
     *
     * @param oldUri The URI to replace
     * @param newUri The new URI
     * @param tupleSlot The position in triples
     * @param inNamedGraph Whether the URI should be replaced in named graphs or in default graphs
     * @throws Exception
     */
    public void renameTripleURI(URI oldUri, URI newUri, TupleSlot tupleSlot, boolean inNamedGraph) throws Exception {
        UpdateBuilder update = new UpdateBuilder();

        Node oldUriNode = Objects.requireNonNull(SPARQLDeserializers.nodeURI(oldUri));
        Node newUriNode = Objects.requireNonNull(SPARQLDeserializers.nodeURI(newUri));
        Var s = makeVar("s");
        Var p = makeVar("p");
        Var o = makeVar("o");
        Var g = makeVar("g");

        // Depending on the position in the triple, generates the triple to replace and the new triple.
        // Example : if tupleSlot == SUBJECT, the triple to replace will be `oldUri ?p ?o`, and the
        // new triple will be `newUri ?p ?o`.
        Triple oldUriTriple = SPARQLQueryHelper.buildUriTriple(s, p, o, oldUri, tupleSlot);
        Triple newUriTriple = SPARQLQueryHelper.buildUriTriple(s, p, o, newUri, tupleSlot);

        WhereBuilder delete = new WhereBuilder();
        WhereBuilder insert = new WhereBuilder();
        WhereBuilder where = new WhereBuilder();

        // Whether the triple are located in named graphs, the actual query will vary. An example with the SUBJECT position :
        // In named graphs :
        // delete {
        //     graph ?g {
        //         oldUri ?p ?o.
        //     }
        // } insert {
        //     graph ?g {
        //         newUri ?p ?o.
        //     }
        // } where {
        //     graph ?g {
        //  	 	oldUri ?p ?o.
        //  	 }
        // }
        //
        // In the default graph :
        // delete {
        //     oldUri ?p ?o.
        // } insert {
        //     newUri ?p ?o.
        // } where {
        // 	oldUri ?p ?o.
        //     filter not exists {
        //         graph ?g {
        //             oldUri ?p ?o.
        //         }
        //     }
        // }
        if (inNamedGraph) {
            delete.addGraph(g, oldUriTriple);
            insert.addGraph(g, newUriTriple);
        } else {
            delete.addWhere(oldUriTriple);
            insert.addWhere(newUriTriple);
        }
        SPARQLQueryHelper.addTripleWhereClause(where, oldUriTriple, inNamedGraph ? g : null);

        update.addDelete(delete);
        update.addInsert(insert);
        update.addWhere(where);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(update.build().toString());
        }

        executeUpdateQuery(update);
    }

    public URI getFavoriteRdfTypeFromURI(URI uri, List<URI> allowedTypes) throws Exception {
        SelectBuilder select = new SelectBuilder();
        WhereBuilder where = new WhereBuilder();

        Var rdfTypeVar = makeVar("rdfType");

        where.addWhere(SPARQLDeserializers.nodeURI(uri), Ontology.typeSubClassAny, rdfTypeVar);
        SPARQLQueryHelper.inURI(where, rdfTypeVar.getVarName(), allowedTypes);
        select.addVar(rdfTypeVar);
        select.addWhere(where);
        select.setDistinct(true);

        List<SPARQLResult> results = this.executeSelectQuery(select);

        if (CollectionUtils.isEmpty(results)) {
            return null;
        }

        if (results.size() > 1) {
            throw new Exception(results + "Multiple rdf types for uri " + uri);
        }

        return new URI(results.get(0).getStringValue(rdfTypeVar.getVarName()));
    }

    public String getFavoriteNameFromURI(URI uri) throws Exception {
        SelectBuilder select = new SelectBuilder();
        WhereBuilder where = new WhereBuilder();

        Var nameVar = makeVar("name");

        where.addWhere(SPARQLDeserializers.nodeURI(uri), org.apache.jena.vocabulary.RDFS.label.asNode(), nameVar);
        select.addVar(nameVar);
        select.addWhere(where);
        select.setDistinct(true);

        List<SPARQLResult> results = this.executeSelectQuery(select);

        if (CollectionUtils.isEmpty(results)) {
            return null;
        }

        if (results.size() > 1) {
            throw new Exception("Multiple rdfs label for uri " + uri);
        }

        return results.get(0).getStringValue(nameVar.getVarName());
    }

    /**
     * Returns a collection of {@link SPARQLNamedResourceModel} representing the given URI. Results are returned in a
     * map where the key is the URI of a graph containing the triples, and the value is a named resource model
     * containing the label and the type of the object in the corresponding graph. An example of result is shown below.
     * The expected types must be provided with the `allowedTypes` argument (it can be a supertype of the object).
     *
     * <pre><code>
     * {
     *     "xp1": {
     *         "label": "xp1-os1",
     *         "type": "plot"
     *     },
     *     "xp2": {
     *         "label": "xp2-os2",
     *         "type": "subplot"
     *     }
     * }
     * </code></pre>
     *
     * This method performs the following query :
     *
     * <pre><code>
     * select ?label ?type {
     *     allowedGraphs ?allowedGraphs {
     *        __uri__ rdf:type ?rdfType .
     *        __uri__ rdfs:label ?label .
     *     }
     *     ?rdfType rdfs:subClassOf* ?superType .
     *     filter (?superType in (__allowedTypes__))
     *     filter (?allowedGraphs = __graph__)
     * }
     * </code> </pre>
     *
     * @param uri
     * @param allowedTypes
     * @return
     */
    public Map<URI, SPARQLNamedResourceModel<?>> getNamedResourceModelContextMap(URI uri, Collection<URI> allowedTypes) throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var labelVar = makeVar("label");
        Var superTypeVar = makeVar("superType");
        Var rdfTypeVar = makeVar("rdfType");
        Var graphVar = makeVar("graph");

        select.addVar(labelVar);
        select.addVar(superTypeVar);
        select.addVar(graphVar);

        WhereBuilder graphWhere = new WhereBuilder();
        graphWhere.addWhere(SPARQLDeserializers.nodeURI(uri), RDF.type, rdfTypeVar);
        graphWhere.addWhere(SPARQLDeserializers.nodeURI(uri), org.apache.jena.vocabulary.RDFS.label, labelVar);

        select.addGraph(graphVar, graphWhere);

        select.addWhere(rdfTypeVar, Ontology.subClassAny, superTypeVar);
        select.addFilter(SPARQLQueryHelper.inURIFilter(superTypeVar, allowedTypes));

        Map<URI, SPARQLNamedResourceModel<?>> graphModelMap = new HashMap<>();

        executeSelectQueryAsStream(select).forEach(result -> {
            SPARQLNamedResourceModel<?> model = new SPARQLNamedResourceModel<>();
            model.setUri(uri);
            model.setName(result.getStringValue(labelVar.getVarName()));
            model.setType(URI.create(result.getStringValue(superTypeVar.getVarName())));
            graphModelMap.put(URI.create(result.getStringValue(graphVar.getVarName())), model);
        });

        return graphModelMap;
    }

    /**
     * @throws ConflictException if the instanceURI is linked with other ressources in the RDF database
     * Using the following sparkl ASK request :
     * <pre>
     * ASK
     * WHERE
     *   {
     *      ?s  ?p  <{instanceURI}>
     *   }
     * </pre>
     *
     * </br>
     * Exemple of request with the predicate "foaf:account" excluded
     * <pre>
     * ASK
     * WHERE
     *   { ?s  ?p  <{instanceURI}>
     *     FILTER ( ?p != <http://xmlns.com/foaf/0.1/account> )
     *   }
     * </pre>
     */
    public void requireUriIsNotLinkedWithOtherRessourcesInRDF(URI instanceURI, List<String> predicateUrisToExclude) throws ConflictException, SPARQLException {
        Node uriNode = SPARQLDeserializers.nodeURI(instanceURI);
        String pVar = "?p";

        AskBuilder isLinked = new AskBuilder()
                .addWhere(SPARQLQueryHelper.makeVar("?s"), SPARQLQueryHelper.makeVar(pVar), uriNode);

        if (Objects.nonNull(predicateUrisToExclude)) {
            predicateUrisToExclude.forEach(uri -> {
                Node predicateUri = SPARQLDeserializers.nodeURI(uri);
                isLinked.addFilter(new ExprFactory().ne(pVar, predicateUri));
            });
        }

        if ( executeAskQuery(isLinked) ){
            throw new ConflictException("URI <"+instanceURI+"> is linked with other ressources");
        }
    }
}
