//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.deserializer.DateTimeDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.exceptions.SPARQLUnknownFieldException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.uri.generation.URIGenerator;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingBiConsumer;
import org.opensilex.utils.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiConsumer;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vincent
 */
public class SPARQLClassObjectMapper<T extends SPARQLResourceModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLClassObjectMapper.class);

    private final Class<T> objectClass;

    private final SPARQLClassObjectMapperIndex mapperIndex;

    private URI baseGraphURI;
    private URI generationPrefixURI;

    private Constructor<T> constructor;
    protected SPARQLClassQueryBuilder classQueryBuilder;
    protected SPARQLClassAnalyzer classAnalyzer;

    private final DateTimeDeserializer timeDeserializer;
    public static final OrderBy DEFAULT_ORDER_BY = new OrderBy(SPARQLResourceModel.URI_FIELD, Order.ASCENDING);

    /**
     * Default keyword used for each {@link org.opensilex.sparql.model.SPARQLResourceModel} associated graph
     */
    public static final String DEFAULT_GRAPH_KEYWORD = "set";

    protected SPARQLClassObjectMapper(Class<T> objectClass, URI baseGraphURI, URI generationPrefixURI, SPARQLClassObjectMapperIndex mapperIndex) {
        this.objectClass = objectClass;
        this.mapperIndex = mapperIndex;
        this.baseGraphURI = baseGraphURI;
        this.generationPrefixURI = generationPrefixURI;

        try {
            timeDeserializer = (DateTimeDeserializer) SPARQLDeserializers.getForClass(OffsetDateTime.class);
        } catch (SPARQLDeserializerNotFoundException e) {
           throw new RuntimeException(e);
        }
    }

    protected void init() throws SPARQLInvalidClassDefinitionException {
        try {
            constructor = objectClass.getConstructor();
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Impossible to find constructor with no parameters", ex);
        }

        try {
            classAnalyzer = new SPARQLClassAnalyzer(mapperIndex, objectClass);
            LOGGER.debug("Analyze class by reflection: {}", objectClass.getName());

            classQueryBuilder = new SPARQLClassQueryBuilder(mapperIndex, classAnalyzer);
            LOGGER.debug("Init SPARQL class query builder {}",objectClass.getName());

            if (classAnalyzer.getGraph() != null) {
                URI classGraph = new URI(classAnalyzer.getGraph());
                if (classGraph.isAbsolute()) {
                    generationPrefixURI = classGraph;
                    baseGraphURI = classGraph;
                }else{
                    generationPrefixURI = new URI(generationPrefixURI + URIGenerator.URI_SEPARATOR + classGraph);
                    baseGraphURI = new URI(baseGraphURI+DEFAULT_GRAPH_KEYWORD+ URIGenerator.URI_SEPARATOR + classGraph);
                }
            }else{
                baseGraphURI = null;
            }

        } catch (SPARQLInvalidClassDefinitionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Unexpected error while initializing SPARQL Mapping", ex);
        }

    }

    public boolean hasValidation() {
        return classAnalyzer.hasValidation();
    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public String getResourceGraphPrefix() {
        return classAnalyzer.getResourceGraphPrefix();
    }

    public String getResourceGraphNamespace() {
        Node defaultGraph = getDefaultGraph();
        if (defaultGraph != null) {
            return defaultGraph.toString();
        }

        return null;
    }

    public Class<?> getGenericListFieldType(Field f) {
        return ClassUtils.getGenericTypeFromClass(f.getClass());
    }

    public T createInstance(Node graph, URI uri, String lang, boolean useDefaultGraph, SPARQLService service) throws Exception {
        SPARQLProxyResource<T> proxy = new SPARQLProxyResource<>(mapperIndex, graph, uri, objectClass, lang, useDefaultGraph, service);
        T instance = proxy.loadIfNeeded();
        if (instance != null) {
            return proxy.getInstance();
        } else {
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public T createInstance(Node graph, SPARQLResult result, String lang, SPARQLService service) throws Exception {

        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);

        URI uri = uriDeserializer.fromString((result.getStringValue(classAnalyzer.getURIFieldName())));
        T instance = createInstance(uri);

        URI realType = new URI(result.getStringValue(getTypeFieldName()));
        instance.setType(SPARQLDeserializers.formatURI(realType));

        String typeLabelFieldName = getTypeLabelFieldName();
        String realTypeLabel = result.getStringValue(typeLabelFieldName);
        if (realTypeLabel == null) {
            realTypeLabel = "";
        }
        SPARQLProxyLabel proxyLabel = new SPARQLProxyLabel(mapperIndex, null, realTypeLabel, realType, RDFS.label, false, lang, service);
        instance.setTypeLabel(proxyLabel.getInstance());

        for (Field field : classAnalyzer.getDataPropertyFields()) {
            Method setter = classAnalyzer.getSetterFromField(field);

            String strValue = result.getStringValue(field.getName());

            if (strValue != null) {
                if (SPARQLDeserializers.existsForClass(field.getType())) {
                    Object objValue = SPARQLDeserializers.getForClass(field.getType()).fromString(strValue);
                    setter.invoke(instance, objValue);
                } else {
                    //TODO change exception type
                    throw new Exception("No deserializer for field: " + field.getName());
                }
            }

        }

        for (Field field : classAnalyzer.getObjectPropertyFields()) {
            Method setter = classAnalyzer.getSetterFromField(field);
            if (result.getStringValue(field.getName()) != null) {
                URI objURI = uriDeserializer.fromString(result.getStringValue(field.getName()));

                Class<? extends SPARQLResourceModel> fieldType = (Class<? extends SPARQLResourceModel>) field.getType();
                Node propertyGraph = graph;

                if (classAnalyzer.isReverseRelation(field)) {
                    propertyGraph = mapperIndex.getForClass(fieldType).getDefaultGraph();
                }

                SPARQLProxyResource<?> proxy = null;
                boolean useDefaultGraph = classAnalyzer.useDefaultGraph(field);

                // SPARQLNamedResourceModel optimization : get object name from SPARQL results without proxy-delegation
                if (SPARQLNamedResourceModel.class.isAssignableFrom(fieldType)) {
                    String fieldNameVar = SPARQLClassQueryBuilder.getObjectNameVarName(field.getName());
                    String name = result.getStringValue(fieldNameVar);

                    // try to build a proxy object including name with the SPARQL builder variable binding name
                    if (!StringUtils.isEmpty(name)) {
                        proxy = new SparqlProxyNamedResource(mapperIndex, propertyGraph, objURI, fieldType, name, lang, false, service);
                    } else {
                        // try to build a proxy object including name with an another SPARQL builder variable binding name
                        name = result.getStringValue(SPARQLClassQueryBuilder.getObjectDefaultNameVarName(field.getName()));

                        if (!StringUtils.isEmpty(name)) {
                            proxy = new SparqlProxyNamedResource(mapperIndex, propertyGraph, objURI, fieldType, name, lang, useDefaultGraph, service);
                        } else {
                            proxy = new SPARQLProxyResource<>(mapperIndex, propertyGraph, objURI, fieldType, lang, useDefaultGraph, service);
                        }
                    }
                // InstantModel optimization : build InstantModel with result without proxy (no additional SPARQL query for  InstantModel#getDateTimeStamp() retrieval)
                } else if(InstantModel.class.isAssignableFrom(fieldType)) {
                    buildInstantModelWithoutProxy(instance, field, result, objURI, setter);
                }else {
                    proxy = new SPARQLProxyResource<>(mapperIndex, propertyGraph, objURI, fieldType, lang, useDefaultGraph, service);
                }

                if(proxy != null){
                    setter.invoke(instance, proxy.getInstance());
                }
            }
        }

        for (Field field : classAnalyzer.getLabelPropertyFields()) {
            Method setter = classAnalyzer.getSetterFromField(field);

            String strValue = result.getStringValue(field.getName());

            if (strValue != null) {
                SPARQLProxyLabel proxy = new SPARQLProxyLabel(mapperIndex, graph, strValue, uri, classAnalyzer.getLabelPropertyByField(field), classAnalyzer.isReverseRelation(field), lang, service);
                setter.invoke(instance, proxy.getInstance());
            }

        }

        for (Field field : classAnalyzer.getDataListPropertyFields()) {
            Method setter = classAnalyzer.getSetterFromField(field);

            SPARQLProxyListData<?> proxy = new SPARQLProxyListData<>(mapperIndex, graph, uri, classAnalyzer.getDataListPropertyByField(field), ClassUtils.getGenericTypeFromField(field), classAnalyzer.isReverseRelation(field), lang, service);
            setter.invoke(instance, proxy.getInstance());
        }

        for (Field field : classAnalyzer.getObjectListPropertyFields()) {
            Method setter = classAnalyzer.getSetterFromField(field);

            Class<? extends SPARQLResourceModel> model = (Class<? extends SPARQLResourceModel>) ClassUtils.getGenericTypeFromField(field);
            Node propertyGraph = graph;
            boolean useDefaultGraph = classAnalyzer.useDefaultGraph(field);
            if (useDefaultGraph) {
                propertyGraph = mapperIndex.getForClass(model).getDefaultGraph();
            }
            SPARQLProxyListObject<? extends SPARQLResourceModel> proxy = new SPARQLProxyListObject<>(mapperIndex, propertyGraph, uri, graph, classAnalyzer.getObjectListPropertyByField(field), model, classAnalyzer.isReverseRelation(field), lang, service);
            setter.invoke(instance, proxy.getInstance());
        }

        Set<Property> properties = classAnalyzer.getManagedProperties();
        instance.setRelations(new SPARQLProxyRelationList(mapperIndex, graph, uri, properties, lang, service).getInstance());
        return instance;
    }

    /**
     * Set an {@link InstantModel} (by using result) as field of the given instance instead of Using {@link SPARQLProxy}
     * @param instance the {@link SPARQLResourceModel} to update
     * @param field the instance {@link Class} field, which is a sub-type of {@link InstantModel}
     * @param result the {@link SPARQLResult} which contains value associated to {@link InstantModel#getUri()} and {@link InstantModel#getDateTimeStamp()}
     * @param objURI URI of the {@link InstantModel}
     * @param setter setter from instance {@link Class}, used to set value for field
     * @throws Exception
     */
    private void buildInstantModelWithoutProxy(T instance, Field field, SPARQLResult result, URI objURI, Method setter) throws Exception {

        String timeStampVarName = getTimeStampVarName(field.getName());
        String timestamp = result.getStringValue(timeStampVarName);

        if (!StringUtils.isEmpty(timestamp)) {

            InstantModel instant = new InstantModel();
            instant.setUri(objURI);
            instant.setDateTimeStamp(timeDeserializer.fromString(timestamp));
            instant.setType(Time.InstantURI);

            // invoke setter with created InstantModel instead of using proxy
            setter.invoke(instance,instant);
        }
    }

    public T createInstance(URI uri) throws Exception {
        T instance = constructor.newInstance();

        if (uri != null) {
            Method uriSetter = classAnalyzer.getSetterFromField(classAnalyzer.getURIField());
            uriSetter.invoke(instance, uri);
        }

        return instance;
    }

    /**
     *
     * @throws SPARQLInvalidUriListException if any URI from uris could not be loaded
     */
    @SuppressWarnings("unchecked")
    public List<T> createInstanceList(Node graph, Collection<URI> uris, String lang, SPARQLService service) throws Exception {
        SPARQLProxyResourceList<T> proxy = new SPARQLProxyResourceList<>(mapperIndex, graph, uris, objectClass, lang, service);
        List<T> instances = proxy.loadIfNeeded();
        if (instances != null) {
            return proxy.getInstance();
        } else {
            return null;
        }

    }

    public URI getDefaultGraphURI() {
        return baseGraphURI;
    }

    public Node getDefaultGraph() {
        URI defaultGraphURI = getDefaultGraphURI();
        if (defaultGraphURI != null) {
            return NodeFactory.createURI(defaultGraphURI.toString());
        }

        return null;
    }

    public URI getGenerationPrefixURI() {
        return generationPrefixURI;
    }

    public AskBuilder getAskBuilder(Node graph, String lang) throws Exception {
        return classQueryBuilder.getAskBuilder(graph, lang);
    }

    public AskBuilder getAskBuilder(Node graph, String lang, ThrowingConsumer<AskBuilder,Exception> filterHandler, Map<String, WhereHandler> customHandlerByFields) throws Exception {
        return classQueryBuilder.getAskBuilder(graph, lang,filterHandler,customHandlerByFields);
    }


    public SelectBuilder getSelectBuilder(Node graph, String lang) throws Exception {
        return getSelectBuilder(graph,lang,null,null);
    }

    public SelectBuilder getSelectBuilder(Node graph, String lang, ThrowingConsumer<SelectBuilder,Exception> filterHandler, Map<String, WhereHandler> customHandlerByFields) throws Exception {
        return classQueryBuilder.getSelectBuilder(graph, lang,filterHandler,customHandlerByFields);
    }


    public SelectBuilder getCountBuilder(Node graph, String countFieldName, String lang) throws Exception {
        return getCountBuilder(graph, countFieldName, lang,null,null);
    }

    public SelectBuilder getCountBuilder(Node graph, String countFieldName, String lang,ThrowingConsumer<SelectBuilder,Exception> filterHandler,  Map<String, WhereHandler> customHandlerByFields) throws Exception {
        return classQueryBuilder.getCountBuilder(graph, countFieldName, lang,filterHandler,customHandlerByFields);

    }

    public UpdateBuilder getCreateBuilder(T instance) throws Exception {
        return getCreateBuilder(getDefaultGraph(), instance);
    }

    public UpdateBuilder getCreateBuilder(Node graph, T instance) throws Exception {
        return getCreateBuilder(graph, instance, false, null);
    }

    public UpdateBuilder getCreateBuilder(Node graph, T instance, boolean blankNode, BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {
        return classQueryBuilder.getCreateBuilder(graph, instance, blankNode, createExtension);
    }

    public void addCreateBuilder(T instance, UpdateBuilder create) throws Exception {
        addCreateBuilder(getDefaultGraph(), instance, create, false,null);
    }

    public void addCreateBuilder(Node graph, T instance, UpdateBuilder create, boolean blankNode,BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {
        classQueryBuilder.addCreateBuilder(graph, instance, create, blankNode,createExtension);
    }

    public UpdateBuilder getDeleteBuilder(T instance) throws Exception {
        return getDeleteBuilder(getDefaultGraph(), instance);
    }

    public UpdateBuilder getDeleteBuilder(Node graph, T instance) throws Exception {
        return classQueryBuilder.getDeleteBuilder(graph, instance);
    }

    public void addDeleteBuilder(T instance, UpdateBuilder delete) throws Exception {
        addDeleteBuilder(getDefaultGraph(), instance, delete);
    }

    public void addDeleteBuilder(Node graph, T instance, UpdateBuilder delete) throws Exception {
        classQueryBuilder.addDeleteBuilder(graph, instance, delete);
    }

    public URI getURI(Object instance) {
        return classAnalyzer.getURI(instance);
    }

    public void setUri(T instance, URI uri) throws Exception {
        classAnalyzer.setURI(instance, uri);
    }

    public String getURIFieldName() {
        return classAnalyzer.getURIFieldName();
    }

    /**
     * @param objectFieldName the field name
     * @return the name of the SPARQL variable which represent the object field name
     * @see SPARQLClassQueryBuilder#getObjectNameVarName(String) ()
     */
    public static String getObjectNameVarName(String objectFieldName) {
        return SPARQLClassQueryBuilder.getObjectNameVarName(objectFieldName);
    }

    /**
     * @param objectFieldName the field name
     * @return the name of the SPARQL variable which represent the object field timestamp
     * @see org.opensilex.sparql.model.time.InstantModel
     * @see org.opensilex.sparql.model.time.Time
     * @see SPARQLClassQueryBuilder#getTimeStampVarName(String) ()
     */
    public static String getTimeStampVarName(String objectFieldName) {
        return SPARQLClassQueryBuilder.getTimeStampVarName(objectFieldName);
    }

    /**
     * @param objectFieldName the field name
     * @return the name of the default SPARQL variable which represent the object field name
     * @see SPARQLClassQueryBuilder#getObjectDefaultNameVarName(String) ()
     */
    public static String getObjectDefaultNameVarName(String objectFieldName) {
        return SPARQLClassQueryBuilder.getObjectDefaultNameVarName(objectFieldName);
    }

    public ExprVar getURIFieldExprVar() {
        try {
            return getFieldExprVar(getURIFieldName());
        } catch (SPARQLUnknownFieldException ex) {
            LOGGER.error("Unknown URI field for a resource, should never happend", ex);
            return null;
        }
    }

    public Var getURIFieldVar() {
        return makeVar(getURIFieldName());
    }

    public String getTypeFieldName() {
        return classAnalyzer.getTypeFieldName();
    }

    public String getTypeLabelFieldName() {
        return classAnalyzer.getTypeLabelFieldName();
    }

    public Var getTypeFieldVar() {
        return makeVar(getTypeFieldName());
    }

    public ExprVar getFieldExprVar(String fieldName) throws SPARQLUnknownFieldException {
        Field f = classAnalyzer.getFieldFromName(fieldName);
        if (f != null) {
            return new ExprVar(f.getName());
        } else {
            throw new SPARQLUnknownFieldException(f);
        }
    }

    public Property getFieldProperty(Field field) {
        return classAnalyzer.getFieldProperty(field);
    }

    /**
     * @param fieldName the var name to put in the {@link Expr}
     * @return an @{@link Expr} with the {@link Field} corresponding with the given fieldName in the
     * {@link #classAnalyzer}, else return an {@link Expr} with the given fieldName
     * @see ExprVar
     */
    public Expr getFieldOrderExpr(String fieldName) {
        Field f = classAnalyzer.getFieldFromName(fieldName);
        if (f != null) {
            if (f.getType().equals(String.class) || f.getType().equals(SPARQLLabel.class)) {
                return new E_StrLowerCase(new ExprVar(f.getName()));
            } else {
                return new ExprVar(f.getName());
            }
        } else {
            return new ExprVar(fieldName);
        }
    }

    public Field getFieldFromUniqueProperty(Property property) {
        return classAnalyzer.getFieldFromUniqueProperty(property);
    }

    @SuppressWarnings("unchecked")
    public URIGenerator<T> getUriGenerator(T instance) {
        URIGenerator<? extends SPARQLResourceModel> generator = classAnalyzer.getUriGenerator();
        if (generator == null) {
            generator = (URIGenerator<? extends SPARQLResourceModel>) instance;
        }
        return (URIGenerator<T>) generator;
    }

    public Resource getRDFType() {
        return classAnalyzer.getRDFType();
    }

    public Method getURIMethod() {
        return classAnalyzer.getURIMethod();
    }

    public String generateSHACL() {
        if (hasValidation()) {
            return classQueryBuilder.generateSHACL();
        }

        return null;
    }

    public Map<Field, Class<? extends SPARQLResourceModel>> getCascadeDeleteClassesField() {
        return classAnalyzer.getCascadeDeleteClassesField();
    }

    public List<Field> getAutoUpdateFields() {
        return classAnalyzer.getAutoUpdateFields();
    }

    public List<Field> getAutoUpdateListFields() {
        return classAnalyzer.getAutoUpdateListFields();
    }

    /**
     *
     * @param subjectGraph graph used to store instance (could be null, the default graph or a given graph)
     * @param instance the instance for which we want to create all nested instance
     * @return association between sub-instance and theirs graph (key/graph is nullable)
     */
    public Map<URI,List<SPARQLResourceModel>> getNestedInstancesByGraph(URI subjectGraph, T instance) {

        Map<URI,List<SPARQLResourceModel>> nestedInstanceByGraph = new HashMap<>();

        classAnalyzer.forEachObjectProperty(ThrowingBiConsumer.wrap((field, property) -> {
            SPARQLResourceModel value = (SPARQLResourceModel) classAnalyzer.getFieldValue(field, instance);
            if (value != null) {
                addNestedResource(nestedInstanceByGraph, field, value, subjectGraph);
            }
        }, Field.class, Property.class, Exception.class));

        classAnalyzer.forEachObjectPropertyList(ThrowingBiConsumer.wrap((field, property) -> {
            List<? extends SPARQLResourceModel> values = (List<? extends SPARQLResourceModel>) classAnalyzer.getFieldValue(field, instance);
            if (values != null && !values.isEmpty()) {
                for (SPARQLResourceModel value : values) {
                    addNestedResource(nestedInstanceByGraph, field, value, subjectGraph);
                }
            }
        }, Field.class, Property.class, Exception.class));

        return nestedInstanceByGraph;
    }

    /**
     *
     * @param nestedResourcesByGraph association between sub-instance and theirs graph (key/graph is nullable)
     * @param field object field
     * @param value object value
     * @param subjectGraph graph used to store instance (could be null, the default graph or a given graph)
     */
    private void addNestedResource(Map<URI, List<SPARQLResourceModel>> nestedResourcesByGraph, Field field, SPARQLResourceModel value, URI subjectGraph) throws org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {

        SPARQLClassObjectMapper<SPARQLResourceModel> modelMapper = mapperIndex.getForClass(value.getClass());
        
        URI uri = modelMapper.getURI(value);
        if (uri == null) {
            URI graph;

            // build graph according SPARQLProperty.useDefaultGraph value
            if(modelMapper.getClassAnalyzer().useDefaultGraph(field)){
                graph = modelMapper.getDefaultGraphURI();  // use object default graph
            } else {
                graph = subjectGraph;  // use graph used to store subjects (can be the default graph or a custom graph)
            }
            // map values to graph
            List<SPARQLResourceModel> nestedResources =  nestedResourcesByGraph.computeIfAbsent(graph, graphKey -> new ArrayList<>());
            nestedResources.add(value);
        }
    }

    public boolean isReverseRelation(Field relationField) {
        return classAnalyzer.isReverseRelation(relationField);
    }

    public Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> getRelationsUrisByMapper(T instance) throws Exception {
        return getRelationsUrisByMapper(instance, new HashMap<>());
    }

    public Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> getRelationsUrisByMapper(T instance, Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> existingMap) throws Exception {
        return getRelationsUrisByMapper(instance, existingMap, false);
    }

    public Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> getReverseRelationsUrisByMapper(T instance) throws Exception {
        return getReverseRelationsUrisByMapper(instance, new HashMap<>());

    }

    public  Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> getReverseRelationsUrisByMapper(T instance, Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> existingMap) throws Exception {
        return getRelationsUrisByMapper(instance, existingMap, true);
    }

    private  Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> getRelationsUrisByMapper(T instance, Map<SPARQLClassObjectMapper<SPARQLResourceModel>, Set<URI>> existingMap, boolean reverse) {
        classAnalyzer.forEachObjectProperty(ThrowingBiConsumer.wrap((field, property) -> {
                    if ((reverse && classAnalyzer.isReverseRelation(field))
                            || (!reverse && !classAnalyzer.isReverseRelation(field))) {
                        Object fieldValue = classAnalyzer.getFieldValue(field, instance);

                        if (fieldValue != null) {
                            SPARQLClassObjectMapper<SPARQLResourceModel> mapper = mapperIndex.getForClass(fieldValue.getClass());
                            URI propertyFieldURI = mapper.getURI(fieldValue);

                            if (propertyFieldURI != null) {
                                if (!existingMap.containsKey(mapper)) {
                                    existingMap.put(mapper, new HashSet<>());
                                }
                                existingMap.get(mapper).add(propertyFieldURI);
                            }
                        }
                    }
                }, Field.class,
                Property.class,
                Exception.class
        ));

        classAnalyzer.forEachObjectPropertyList(ThrowingBiConsumer.wrap((field, property) -> {
                    if ((reverse && classAnalyzer.isReverseRelation(field))
                            || (!reverse && !classAnalyzer.isReverseRelation(field))) {
                        List<? extends SPARQLResourceModel> values = (List<? extends SPARQLResourceModel>) classAnalyzer.getFieldValue(field, instance);
                        if (values != null && !values.isEmpty()) {
                            for (SPARQLResourceModel value : values) {
                                SPARQLClassObjectMapper<SPARQLResourceModel> mapper = mapperIndex.getForClass(value.getClass());
                                URI propertyFieldURI = mapper.getURI(value);

                                if (propertyFieldURI != null) {
                                    if (!existingMap.containsKey(mapper)) {
                                        existingMap.put(mapper, new HashSet<>());
                                    }
                                    existingMap.get(mapper).add(propertyFieldURI);
                                }
                            }
                        }
                    }
                }, Field.class,
                Property.class,
                Exception.class
        ));

        return existingMap;
    }

    public UpdateBuilder getDeleteRelationsBuilder(Node graph, URI uri) throws Exception {
        UpdateBuilder delete = new UpdateBuilder();
        if (addDeleteRelationsBuilder(graph, uri, classAnalyzer.getRelatedResources(), delete)) {
            return delete;
        }

        return null;
    }

    public <T extends SPARQLResourceModel> boolean addDeleteRelationsBuilder(Node graph, URI uri, Set<Class<? extends SPARQLResourceModel>> relatedResources, UpdateBuilder delete) throws Exception {
        if (relatedResources == null) {
            return false;
        }

        int statementCount = 0;
        Var uriVar = makeVar("uri");
        Node uriNode = SPARQLDeserializers.nodeURI(uri);
        for (Class<? extends SPARQLResourceModel> relatedModelClass : relatedResources) {
            SPARQLClassObjectMapper<SPARQLResourceModel> relatedModelMapper = mapperIndex.getForClass(relatedModelClass);
            Set<Field> modelRelationFields = relatedModelMapper.classAnalyzer.getFieldsRelatedTo(objectClass);
            for (Field relationField : modelRelationFields) {
                Property property = relatedModelMapper.classAnalyzer.getFieldProperty(relationField);
                statementCount++;
                Var propertyVar = makeVar("_prop" + statementCount);
                Var objectVar = makeVar("_obj" + statementCount);
                if (graph != null) {
                    if (relatedModelMapper.classAnalyzer.isReverseRelation(relationField)) {
                        delete.addDelete(graph, objectVar, propertyVar, uriVar);
                    } else {
                        delete.addDelete(graph, uriVar, propertyVar, objectVar);
                    }
                } else {
                    if (relatedModelMapper.classAnalyzer.isReverseRelation(relationField)) {
                        delete.addDelete(objectVar, propertyVar, uriVar);
                    } else {
                        delete.addDelete(uriVar, propertyVar, objectVar);
                    }
                }

                if (relatedModelMapper.classAnalyzer.isReverseRelation(relationField)) {
                    delete.addWhere(objectVar, propertyVar, uriVar);
                    delete.addWhere(objectVar, property, uriNode);
                } else {
                    delete.addWhere(uriVar, propertyVar, objectVar);
                    delete.addWhere(uriNode, property, objectVar);
                }
            }
        }

        return statementCount > 0;
    }

    public void updateInstanceFromOldValues(T oldInstance, T newInstance) throws Exception {
        if (newInstance.getType() == null) {
            newInstance.setType(oldInstance.getType());
        }

        if (oldInstance.getPublisher() != null && newInstance.getPublisher() == null) {
            newInstance.setPublisher(oldInstance.getPublisher());
        }

        if (Objects.isNull(newInstance.getPublicationDate())) {
            newInstance.setPublicationDate(oldInstance.getPublicationDate());
        }

        newInstance.setLastUpdateDate(OffsetDateTime.now());

        for (Field field : classAnalyzer.getDataPropertyFields()) {
            Object oldFieldValue = classAnalyzer.getFieldValue(field, oldInstance);
            Object newFieldValue = classAnalyzer.getFieldValue(field, newInstance);

            if (newFieldValue == null && classAnalyzer.isNullIgnorableUpdateField(field)) {
                classAnalyzer.getSetterFromField(field).invoke(newInstance, oldFieldValue);
            }
        }

        for (Field field : classAnalyzer.getObjectPropertyFields()) {
            Object oldFieldValue = classAnalyzer.getFieldValue(field, oldInstance);
            Object newFieldValue = classAnalyzer.getFieldValue(field, newInstance);

            if (newFieldValue == null && classAnalyzer.isNullIgnorableUpdateField(field)) {
                classAnalyzer.getSetterFromField(field).invoke(newInstance, oldFieldValue);
            }
        }

        for (Field field : classAnalyzer.getLabelPropertyFields()) {
            Object oldFieldValue = classAnalyzer.getFieldValue(field, oldInstance);
            Object newFieldValue = classAnalyzer.getFieldValue(field, newInstance);

            if (newFieldValue == null && classAnalyzer.isNullIgnorableUpdateField(field)) {
                classAnalyzer.getSetterFromField(field).invoke(newInstance, oldFieldValue);
            }
        }

        for (Field field : classAnalyzer.getDataListPropertyFields()) {
            Object oldFieldValue = classAnalyzer.getFieldValue(field, oldInstance);
            Object newFieldValue = classAnalyzer.getFieldValue(field, newInstance);

            if (newFieldValue == null && classAnalyzer.isNullIgnorableUpdateField(field)) {
                classAnalyzer.getSetterFromField(field).invoke(newInstance, oldFieldValue);
            }
        }

        for (Field field : classAnalyzer.getObjectListPropertyFields()) {
            Object oldFieldValue = classAnalyzer.getFieldValue(field, oldInstance);
            Object newFieldValue = classAnalyzer.getFieldValue(field, newInstance);

            if (newFieldValue == null && classAnalyzer.isNullIgnorableUpdateField(field)) {
                classAnalyzer.getSetterFromField(field).invoke(newInstance, oldFieldValue);
            }
        }
    }

    public SPARQLClassAnalyzer getClassAnalyzer() {
        return classAnalyzer;
    }
}
