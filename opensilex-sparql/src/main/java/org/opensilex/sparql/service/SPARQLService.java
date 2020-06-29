//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
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
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.opensilex.service.Service;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.*;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.opensilex.OpenSilex;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.utils.ClassUtils;

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
                put(XMLSchema.PREFIX, XMLSchema.NAMESPACE);
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
        return PrefixMapping.Factory.create().setNsPrefixes(prefixes);
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
        describe.addBind(new ExprFactory().iri(uri), uriVar);
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
    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLException {
        addPrefixes(update);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SPARQL UPDATE\n" + update.build().toString());
        }
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
        disableSHACL();
        LOGGER.debug("MOVE GRAPH " + oldGraphURI + " TO " + newGraphURI);
        connection.renameGraph(oldGraphURI, newGraphURI);
        enableSHACL();
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
        return getByURI(getDefaultGraph(objectClass), objectClass, uri, lang);
    }

    public <T extends SPARQLResourceModel> T getByURI(Node graph, Class<T> objectClass, URI uri, String lang) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        T instance = mapper.createInstance(graph, uri, lang, this);
        return instance;
    }

    public <T extends SPARQLResourceModel> List<T> getListByURIs(Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        return getListByURIs(getDefaultGraph(objectClass), objectClass, uris, lang);
    }

    public <T extends SPARQLResourceModel> List<T> getListByURIs(Node graph, Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        List<T> instances;
        if (uris.size() > 0) {
            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
            instances = mapper.createInstanceList(graph, uris, lang, this);
        } else {
            instances = new ArrayList<>();
        }
        return instances;
    }

    public <T extends SPARQLResourceModel> T loadByURI(Class<T> objectClass, URI uri, String lang) throws Exception {
        return loadByURI(getDefaultGraph(objectClass), objectClass, uri, lang);
    }

    public <T extends SPARQLResourceModel> T loadByURI(Node graph, Class<T> objectClass, URI uri, String lang) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, lang);

        select.addValueVar(mapper.getURIFieldExprVar(), SPARQLDeserializers.nodeURI(uri));

        List<SPARQLResult> results = executeSelectQuery(select);

        if (results.size() == 1) {
            return mapper.createInstance(graph, results.get(0), lang, this);
        } else if (results.size() > 1) {
            throw new SPARQLException("Multiple objects for the same URI: " + uri.toString());
        } else {
            return null;
        }
    }

    public <T extends SPARQLResourceModel> List<T> loadListByURIs(Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        return loadListByURIs(getDefaultGraph(objectClass), objectClass, uris, lang);
    }

    public <T extends SPARQLResourceModel> List<T> loadListByURIs(Node graph, Class<T> objectClass, Collection<URI> uris, String lang) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }

        List<T> resultObjects;

        if (uris.size() > 0) {
            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
            SelectBuilder select = mapper.getSelectBuilder(graph, lang);

            Collection<Node> uriNodes = SPARQLDeserializers.nodeListURI(uris);

            select.addValueVar(mapper.getURIFieldExprVar(), uriNodes.toArray());

            List<SPARQLResult> results = executeSelectQuery(select);

            resultObjects = new ArrayList<>(results.size());

            for (SPARQLResult result : results) {
                resultObjects.add(mapper.createInstance(graph, result, lang, this));
            }
        } else {
            resultObjects = new ArrayList<>();
        }
        return resultObjects;
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

    public <T extends SPARQLResourceModel> boolean existsByUniquePropertyValue(Class<T> objectClass, Property property, Object propertyValue, String lang) throws Exception {
        return existsByUniquePropertyValue(getDefaultGraph(objectClass), objectClass, property, propertyValue, lang);
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
        SelectBuilder select = mapper.getSelectBuilder(graph, lang);

        if (filterHandler != null) {
            filterHandler.accept(select);
        }

        List<URI> resultList = new ArrayList<>();
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            resultList.add(uriDeserializer.fromString((result.getStringValue(mapper.getURIFieldName()))));
        }, Exception.class));

        return resultList;
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, null, false, filterHandler);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Class<T> objectClass, String lang, URI root, boolean excludeRoot, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return searchResourceTree(getDefaultGraph(objectClass), objectClass, lang, root, excludeRoot, filterHandler);
    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchResourceTree(Node graph, Class<T> objectClass, String lang, URI root, boolean excludeRoot, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        if (lang == null) {
            lang = getDefaultLang();
        }
        List<T> list = search(graph, objectClass, lang, filterHandler);

        SPARQLTreeListModel<T> tree = new SPARQLTreeListModel<T>(list, SPARQLDeserializers.formatURI(root), excludeRoot);

        for (T item : list) {
            tree.addTree(item);
        }

        return tree;
    }

    public <T extends SPARQLTreeModel<T>> SPARQLPartialTreeListModel<T> searchPartialResourceTree(Node graph, Class<T> objectClass, String lang, String parentField, URI parentURI, int maxChild, int maxDepth, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        if (maxDepth < 0) {
            return null;
        }

        final String language;
        if (lang == null) {
            language = getDefaultLang();
        } else {
            language = lang;
        }

        Function<URI, List<T>> searchHandler = (parentSearchURI) -> {
            try {
                return search(graph, objectClass, language, (select) -> {
                    if (parentSearchURI == null) {
                        // TODO no parent URI explicit filter
                    } else {
                        // TODO add parent URI filter
                    }
                    filterHandler.accept(select);
                }, null, 0, maxChild);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        Function<URI, Integer> countHandler = (parentCountURI) -> {
            try {
                int totalSize = count(graph, objectClass, language, (select) -> {
                    if (parentCountURI == null) {
                        // TODO no parent URI explicit filter
                    } else {
                        // TODO add parent URI filter
                    }
                    filterHandler.accept(select);
                });

                return totalSize;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        List<T> rootList = searchHandler.apply(parentURI);

        SPARQLPartialTreeListModel<T> tree = new SPARQLPartialTreeListModel<T>(parentURI, searchHandler, countHandler);

        for (T item : rootList) {
            tree.loadChildren(item, maxDepth);
        }

        return tree;
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang) throws Exception {
        return search(graph, objectClass, lang, null, null, null, null);
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, filterHandler);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return search(graph, objectClass, lang, filterHandler, null, null, null);
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, List<OrderBy> orderByList) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, orderByList);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang, List<OrderBy> orderByList) throws Exception {
        return search(graph, objectClass, lang, null, orderByList, null, null);
    }

    public <T extends SPARQLResourceModel> List<T> search(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList) throws Exception {
        return search(getDefaultGraph(objectClass), objectClass, lang, filterHandler, orderByList);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList) throws Exception {
        return search(graph, objectClass, lang, filterHandler, orderByList, null, null);
    }

    public <T extends SPARQLResourceModel> List<T> search(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        String language;
        if (lang == null) {
            language = getDefaultLang();
        } else {
            language = lang;
        }

        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder select = mapper.getSelectBuilder(graph, language);

        if (filterHandler != null) {
            filterHandler.accept(select);
        }

        if (orderByList != null) {
            orderByList.forEach((OrderBy orderBy) -> {
                Expr fieldOrderExpr = mapper.getFieldOrderExpr(orderBy.getFieldName());
                if (fieldOrderExpr != null) {
                    select.addOrderBy(fieldOrderExpr, orderBy.getOrder());
                }
            });
        }

        if (page == null || page < 0) {
            page = 0;
        }

        if (pageSize != null && pageSize > 0) {
            select.setOffset(page * pageSize);
            select.setLimit(pageSize);
        }

        List<T> resultList = new ArrayList<>();
        executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            resultList.add(mapper.createInstance(graph, result, language, this));
        }, Exception.class));

        return resultList;
    }

    public <T extends SPARQLResourceModel> int count(Class<T> objectClass) throws Exception {
        return count(getDefaultGraph(objectClass), objectClass);
    }

    public <T extends SPARQLResourceModel> int count(Node graph, Class<T> objectClass) throws Exception {
        return count(graph, objectClass, null, null);
    }

    public <T extends SPARQLResourceModel> int count(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        return count(getDefaultGraph(objectClass), objectClass, lang, filterHandler);
    }

    public <T extends SPARQLResourceModel> int count(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        if (lang == null) {
            lang = getDefaultLang();
        }
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(objectClass);
        SelectBuilder selectCount = mapper.getCountBuilder(graph, "count", lang);

        if (filterHandler != null) {
            filterHandler.accept(selectCount);
        }

        List<SPARQLResult> resultSet = executeSelectQuery(selectCount);

        if (resultSet.size() == 1) {
            return Integer.valueOf(resultSet.get(0).getStringValue("count"));
        } else {
            throw new SPARQLException("Invalid count query");
        }
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Integer page, Integer pageSize) throws Exception {
        return searchWithPagination(getDefaultGraph(objectClass), objectClass, lang, filterHandler, page, pageSize);
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, Integer page, Integer pageSize) throws Exception {
        return searchWithPagination(graph, objectClass, lang, filterHandler, null, page, pageSize);
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return searchWithPagination(getDefaultGraph(objectClass), objectClass, lang, filterHandler, orderByList, page, pageSize);
    }

    public <T extends SPARQLResourceModel> ListWithPagination<T> searchWithPagination(Node graph, Class<T> objectClass, String lang, ThrowingConsumer<SelectBuilder, Exception> filterHandler, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        if (lang == null) {
            lang = getDefaultLang();
        }
        int total = count(graph, objectClass, lang, filterHandler);

        List<T> list;
        if (pageSize == null || pageSize == 0) {
            list = search(graph, objectClass, lang, filterHandler, orderByList);
        } else if (total > 0 && (page * pageSize) < total) {
            list = search(graph, objectClass, lang, filterHandler, orderByList, page, pageSize);
        } else {
            list = new ArrayList<>();
        }

        return new ListWithPagination<>(list, page, pageSize, total);
    }

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
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        try {
            startTransaction();
            SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(instance.getClass());
            prepareInstanceCreation(instance, mapper, checkUriExist);
            UpdateBuilder create = mapper.getCreateBuilder(graph, instance);
            executeUpdateQuery(create);
            commitTransaction();
        } catch (Exception ex) {
            rollbackTransaction(ex);
            throw ex;
        }
    }

    private <T extends SPARQLResourceModel> void prepareInstanceCreation(T instance, SPARQLClassObjectMapper<T> mapper, boolean checkUriExist) throws Exception {
        URI rdfType = instance.getType();
        if (rdfType == null) {
            instance.setType(new URI(mapper.getRDFType().getURI()));
        } else {
            instance.setType(rdfType);
        }
        generateUniqueUriIfNullOrValidateCurrent(mapper, instance, checkUriExist);

        validate(instance);

        for (SPARQLResourceModel subInstance : mapper.getAllDependentResourcesToCreate(instance)) {
            create(subInstance);
        }
    }

    public <T extends SPARQLResourceModel> void create(List<T> instances) throws Exception {
        Class<? extends SPARQLResourceModel> genericType = (Class<? extends SPARQLResourceModel>) ClassUtils.getGenericTypeFromClass(instances.getClass());
        create(getDefaultGraph(genericType), instances);
    }

    public <T extends SPARQLResourceModel> void create(Node graph, List<T> instances) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();

        if (instances.size() > 0) {
            UpdateBuilder create = new UpdateBuilder();
            validate(instances);
            for (T instance : instances) {
                SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(instance.getClass());
                prepareInstanceCreation(instance, mapper, true);
                mapper.addCreateBuilder(graph, instance, create);
            }

            executeUpdateQuery(create);
        }
    }

    private <T extends SPARQLResourceModel> void generateUniqueUriIfNullOrValidateCurrent(SPARQLClassObjectMapper<T> mapper, T instance, boolean checkUriExist) throws Exception {
        URIGenerator<T> uriGenerator = mapper.getUriGenerator(instance);
        URI uri = mapper.getURI(instance);
        if (uri == null) {

            int retry = 0;
            String graphPrefix = getDefaultGraph(instance.getClass()).toString();
            uri = uriGenerator.generateURI(graphPrefix, instance, retry);
            while (uriExists(uri)) {
                uri = uriGenerator.generateURI(graphPrefix, instance, ++retry);
            }

            mapper.setUri(instance, uri);
        } else if (checkUriExist && uriExists(uri)) {
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

                validate(instances);

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

    public <T extends SPARQLResourceModel> void delete(Class<T> objectClass, URI uri) throws Exception {
        delete(getDefaultGraph(objectClass), objectClass, uri);
    }

    public <T extends SPARQLResourceModel> void delete(Node graph, Class<T> objectClass, URI uri) throws Exception {
        SPARQLClassObjectMapperIndex mapperIndex = getMapperIndex();
        try {
            startTransaction();
            if (!uriExists(uri)) {
                throw new SPARQLInvalidURIException(uri);
            }
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

            UpdateBuilder deleteAllReverseReferencesBuilder = new UpdateBuilder();
            Iterator<Map.Entry<Class<? extends SPARQLResourceModel>, Field>> i = mapperIndex.getReverseReferenceIterator(objectClass);
            Node uriNode = SPARQLDeserializers.nodeURI(uri);

            int statementCount = 0;
            while (i.hasNext()) {
                Map.Entry<Class<? extends SPARQLResourceModel>, Field> entry = i.next();
                statementCount++;
                String var = "?x" + statementCount;
                SPARQLClassObjectMapper<SPARQLResourceModel> reverseMapper = mapperIndex.getForClass(entry.getKey());
                Property reverseProp = reverseMapper.getFieldProperty(entry.getValue());
                deleteAllReverseReferencesBuilder.addDelete(reverseMapper.getDefaultGraph(), var, reverseProp, uriNode);
                deleteAllReverseReferencesBuilder.addWhere(var, reverseProp, uriNode);
            }
            if (statementCount > 0) {
                executeDeleteQuery(deleteAllReverseReferencesBuilder);
            }

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

    public boolean uriExists(URI uri) throws SPARQLException {
        AskBuilder askQuery = new AskBuilder();
        Var s = makeVar("s");
        Var p = makeVar("p");
        Var o = makeVar("o");
        Node nodeUri = SPARQLDeserializers.nodeURI(uri);
        askQuery.addWhere(nodeUri, p, o);
        WhereBuilder reverseWhere = new WhereBuilder();
        reverseWhere.addWhere(s, p, nodeUri);
        askQuery.addUnion(reverseWhere);

        return executeAskQuery(askQuery);
    }

    public <T extends SPARQLResourceModel> boolean uriExists(Class<T> objectClass, URI uri) throws Exception {
        if (uri == null) {
            return false;
        }
        return executeAskQuery(getUriExistsQuery(objectClass, uri));
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
     * @param uri the {@link URI} to check
     * @return true if uri exists in the TripleStore and if it's an instance of rdfType
     */
    public boolean uriExists(URI rdfType, URI uri) throws SPARQLException {
        Var typeVar = makeVar("type");
        return executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(uri), RDF.type, typeVar)
                .addWhere(typeVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(rdfType))
        );
    }

    public <T extends SPARQLResourceModel> AskBuilder getUriExistsQuery(Class<T> objectClass, URI uri) throws SPARQLException {
        SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);

        AskBuilder askQuery = new AskBuilder();
        Node nodeUri = SPARQLDeserializers.nodeURI(uri);

        Var fieldType = mapper.getTypeFieldVar();
        askQuery.addWhere(nodeUri, RDF.type, fieldType);

        Resource typeDef = mapper.getRDFType();

        askQuery.addWhere(fieldType, Ontology.subClassAny, typeDef);
        return askQuery;
    }

    private static final String EXISTING_VAR = "existing";

    public <T extends SPARQLResourceModel> SelectBuilder getUriListExistQuery(Class<T> objectClass, Collection<URI> uris) throws Exception {

        SPARQLClassObjectMapper<T> mapper = getMapperIndex().getForClass(objectClass);

        Var uriVar = mapper.getURIFieldVar();
        Var existing = makeVar(EXISTING_VAR);
        Var typeVar = mapper.getTypeFieldVar();
        Resource typeDef = mapper.getRDFType();

        SelectBuilder select = new SelectBuilder();
        SPARQLQueryHelper.addWhereValues(select, uriVar.getVarName(), uris);

        WhereBuilder where = new WhereBuilder()
                .addWhere(uriVar, makeVar("p"), makeVar("o"));

        where.addWhere(typeVar, Ontology.subClassAny, typeDef)
                .addWhere(uriVar, RDF.type, typeVar);

        Expr existExpr = SPARQLQueryHelper.getExprFactory().exists(where);
        select.addVar(existExpr, existing);

        return select;
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

    /**
     * Insert a list of quad (graph,subject,property,object), (graph,subject,property,object_1) ... (graph,subject,property,object_k)
     *
     * and remove any old quad (graph,subject,property,?object) in the SPARQL graph
     *
     * @param graph the graph in which the triple(s) are present
     * @param subject the triple subject URI
     * @param property the triple property
     * @param objects the list of object values
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
     * Insert a list of quad (graph,subject,property,object), (graph,subject_1,property,object) ... (graph,subject_k,property,object)
     *
     * and remove any old quad (graph,?subject,property,object) in the SPARQL graph
     *
     * @param graph the graph in which the triple(s) are present
     * @param subjects the list of subject URIS
     * @param property the triple property
     * @param object the triple(s) object
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

    public Var getURIFieldVar(Class<? extends SPARQLResourceModel> modelClass) throws SPARQLException {
        return getMapperIndex().getForClass(modelClass).getURIFieldVar();
    }

    public <T extends SPARQLResourceModel> void validate(T instance) throws Exception {
        if (!this.isShaclEnabled()) {
//            validateRelations(instance);
        }

        validateReverseRelations(instance);
    }

    public <T extends SPARQLResourceModel> void validate(List<T> instances) throws Exception {
        if (!this.isShaclEnabled()) {
//            validateRelations(instances);
        }

        validateReverseRelations(instances);
    }

    private <T extends SPARQLResourceModel> void validateRelations(Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers) throws Exception {
        for (Map.Entry<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMapper : urisByMappers.entrySet()) {
            SPARQLClassObjectMapper<SPARQLResourceModel> modelMapper = urisByMapper.getKey();
            Set<URI> uris = urisByMapper.getValue();

            if (!uriListExists(modelMapper.getObjectClass(), uris)) {
                // TODO: better exception for validation
                throw new Exception("Invalid URI list !!");
            }
        }
    }

    private <T extends SPARQLResourceModel> void validateRelations(T instance) throws Exception {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
        validateRelations(mapper.getRelationsUrisByMapper(instance));

    }

    private <T extends SPARQLResourceModel> void validateRelations(List<T> instances) throws Exception {
        Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers = new HashMap<>();
        for (T instance : instances) {
            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
            mapper.getRelationsUrisByMapper(instance, urisByMappers);
        }

        validateRelations(urisByMappers);
    }

    private <T extends SPARQLResourceModel> void validateReverseRelations(T instance) throws Exception {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
        validateRelations(mapper.getReverseRelationsUrisByMapper(instance));
    }

    private <T extends SPARQLResourceModel> void validateReverseRelations(List<T> instances) throws Exception {
        Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> urisByMappers = new HashMap<>();
        for (T instance : instances) {
            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = getMapperIndex().getForClass(instance.getClass());
            mapper.getReverseRelationsUrisByMapper(instance, urisByMappers);
        }

        validateRelations(urisByMappers);
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

}
