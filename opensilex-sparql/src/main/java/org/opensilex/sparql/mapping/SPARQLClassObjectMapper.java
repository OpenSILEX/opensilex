//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLUnknownFieldException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.sparql.core.Var;
import org.opensilex.sparql.annotations.SPARQLManualLoading;

/**
 *
 * @author vincent
 */
public class SPARQLClassObjectMapper<T extends SPARQLResourceModel> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassObjectMapper.class);
    private static Set<Class<?>> SPARQL_CLASSES_LIST;
    private static Map<Class<?>, SPARQLClassObjectMapper<?>> SPARQL_CLASSES_MAPPER = new HashMap<>();
    private static Map<Resource, SPARQLClassObjectMapper<?>> SPARQL_RESOURCES_MAPPER = new HashMap<>();
    private static List<Class<? extends SPARQLResourceModel>> SPARQL_RESOURCES_MANUAL_INCLUSION_LIST = new ArrayList<>();

    private static <T> Class<? super T> getConcreteClass(Class<T> objectClass) {
        if (SPARQLProxyMarker.class.isAssignableFrom(objectClass)) {
            return getConcreteClass(objectClass.getSuperclass());
        } else {
            return objectClass;
        }
    }

    public static void forEach(BiConsumer<Resource, SPARQLClassObjectMapper<?>> lambda) throws SPARQLInvalidClassDefinitionException {
        initialize();
        SPARQL_RESOURCES_MAPPER.forEach(lambda);
    }

    @SuppressWarnings("unchecked")
    public static void initialize() throws SPARQLInvalidClassDefinitionException {
        if (SPARQL_CLASSES_LIST == null) {
            SPARQL_CLASSES_LIST = ClassUtils.getAnnotatedClasses(SPARQLResource.class);

            SPARQL_CLASSES_LIST.forEach((clazz) -> {
                LOGGER.debug("SPARQL Resource class found: " + clazz.getCanonicalName());
            });
            
            SPARQL_CLASSES_LIST.removeIf((Class<?> resource) -> {
                SPARQLManualLoading manualAnnotation = resource.getAnnotation(SPARQLManualLoading.class);
                return (manualAnnotation != null);
            });

            SPARQL_CLASSES_LIST.addAll(SPARQL_RESOURCES_MANUAL_INCLUSION_LIST);

            for (Class<?> sparqlModelClass : SPARQL_CLASSES_LIST) {
                SPARQLClassObjectMapper<?> sparqlObjectMapper = new SPARQLClassObjectMapper<>((Class<? extends SPARQLResourceModel>) sparqlModelClass);
                SPARQL_CLASSES_MAPPER.put(sparqlModelClass, sparqlObjectMapper);
                SPARQL_RESOURCES_MAPPER.put(sparqlObjectMapper.getRDFType(), sparqlObjectMapper);
            }
        }
    }

    /**
     * Add a class to the initialization class exclusion list
     *
     * @param clazz the class to remove from initialization
     * @see #initialize()
     */
    public static void includeResourceClass(Class<? extends SPARQLResourceModel> clazz) {
        SPARQL_RESOURCES_MANUAL_INCLUSION_LIST.add(clazz);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends SPARQLResourceModel> SPARQLClassObjectMapper<T> getForClass(Class<?> objectClass) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        initialize();

        Class<T> concreteObjectClass = (Class<T>) getConcreteClass(objectClass);

        if (SPARQL_CLASSES_MAPPER.containsKey(concreteObjectClass)) {
            return (SPARQLClassObjectMapper<T>) SPARQL_CLASSES_MAPPER.get(concreteObjectClass);
        } else {
            throw new SPARQLMapperNotFoundException(concreteObjectClass);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends SPARQLResourceModel> SPARQLClassObjectMapper<T> getForResource(Resource resource) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        initialize();
        if (SPARQL_RESOURCES_MAPPER.containsKey(resource)) {
            return (SPARQLClassObjectMapper<T>) SPARQL_RESOURCES_MAPPER.get(resource);
        } else {
            throw new SPARQLMapperNotFoundException(resource);
        }

    }

    public static Node getGraph(Class<? extends SPARQLResourceModel> c) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        return getForClass(c).getDefaultGraph();
    }

    public static boolean existsForClass(Class<?> c) throws SPARQLInvalidClassDefinitionException {
        initialize();
        return SPARQL_CLASSES_LIST.contains(c);
    }

    public static void reset() {
        SPARQL_CLASSES_LIST = null;
        SPARQL_CLASSES_MAPPER = new HashMap<>();
        SPARQL_RESOURCES_MAPPER = new HashMap<>();
        SPARQL_RESOURCES_MANUAL_INCLUSION_LIST = new ArrayList<>();
    }

    private final Class<T> objectClass;
    private final Constructor<T> constructor;
    private final SPARQLClassQueryBuilder classQueryBuilder;
    private final SPARQLClassAnalyzer classAnalizer;

    private SPARQLClassObjectMapper(Class<T> objectClass) throws SPARQLInvalidClassDefinitionException {
        LOGGER.debug("Initialize SPARQL ressource class object mapper for: " + objectClass.getName());
        this.objectClass = objectClass;

        LOGGER.debug("Look for object constructor with no arguments for class: " + objectClass.getName());
        try {
            constructor = objectClass.getConstructor();
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Impossible to find constructor with no parameters", ex);
        }

        LOGGER.debug("Analyze class by reflection: " + objectClass.getName());
        classAnalizer = new SPARQLClassAnalyzer(objectClass);

        LOGGER.debug("Init SPARQL class query builder: " + objectClass.getName());
        classQueryBuilder = new SPARQLClassQueryBuilder(classAnalizer);

    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public String getResourceGraphPrefix() {
        return classAnalizer.getResourceGraphPrefix();
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

    public T createInstance(URI uri, String lang, SPARQLService service) throws Exception {
        SPARQLProxyResource<T> proxy = new SPARQLProxyResource<>(getDefaultGraph(), uri, objectClass, lang, service);
        T instance = proxy.loadIfNeeded();
        if (instance != null) {
            return proxy.getInstance();
        } else {
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public T createInstance(SPARQLResult result, String lang, SPARQLService service) throws Exception {
        String realType = result.getStringValue(SPARQLQueryHelper.typeDefVar.getName());
        if (!realType.equals(getRDFType().toString())) {
            // TODO handle sub classes
        }

        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        URI uri = uriDeserializer.fromString((result.getStringValue(classAnalizer.getURIFieldName())));

        T instance = createInstance(uri);

        for (Field field : classAnalizer.getDataPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

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

        for (Field field : classAnalizer.getObjectPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);
            if (result.getStringValue(field.getName()) != null) {
                URI objURI = uriDeserializer.fromString(result.getStringValue(field.getName()));

                Class<? extends SPARQLResourceModel> fieldType = (Class<? extends SPARQLResourceModel>) field.getType();
                SPARQLProxyResource<?> proxy = new SPARQLProxyResource<>(getDefaultGraph(), objURI, fieldType, lang, service);
                setter.invoke(instance, proxy.getInstance());
            }
        }
        
        for (Field field : classAnalizer.getLabelPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

            String strValue = result.getStringValue(field.getName());

            if (strValue != null) {
                SPARQLProxyLabel proxy = new SPARQLProxyLabel(getDefaultGraph(), strValue, uri, classAnalizer.getLabelPropertyByField(field), classAnalizer.isReverseRelation(field), lang, service);
                setter.invoke(instance, proxy.getInstance());
            }

        }

        for (Field field : classAnalizer.getDataListPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

            SPARQLProxyListData<?> proxy = new SPARQLProxyListData<>(getDefaultGraph(), uri, classAnalizer.getDataListPropertyByField(field), ClassUtils.getGenericTypeFromField(field), classAnalizer.isReverseRelation(field), lang, service);
            setter.invoke(instance, proxy.getInstance());
        }

        for (Field field : classAnalizer.getObjectListPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

            Class<? extends SPARQLResourceModel> model = (Class<? extends SPARQLResourceModel>) ClassUtils.getGenericTypeFromField(field);
            SPARQLProxyListObject<? extends SPARQLResourceModel> proxy = new SPARQLProxyListObject<>(getDefaultGraph(), uri, classAnalizer.getObjectListPropertyByField(field), model, classAnalizer.isReverseRelation(field), lang, service);
            setter.invoke(instance, proxy.getInstance());
        }
        
        Set<String> properties = classAnalizer.getManagedProperties();
        instance.setRelations(new SPARQLProxyRelationList(null, uri, properties,lang,  service).getInstance());
        return instance;
    }

    public T createInstance(URI uri) throws Exception {
        T instance = constructor.newInstance();

        if (uri != null) {
            Method uriSetter = classAnalizer.getSetterFromField(classAnalizer.getURIField());
            uriSetter.invoke(instance, uri);
        }

        return instance;
    }

    
    @SuppressWarnings("unchecked")
    public List<T> createInstanceList(List<URI> uris, String lang, SPARQLService service) throws Exception {
        SPARQLProxyResourceList<T> proxy = new SPARQLProxyResourceList<>(getDefaultGraph(), uris, objectClass, lang, service);
        List<T> instances = proxy.loadIfNeeded();
        if (instances != null) {
            return proxy.getInstance();
        } else {
            return null;
        }

    }

    public Node getDefaultGraph() {
        if (classAnalizer.getGraphSuffix() != null) {
            try {
                String classGraphURI = SPARQLModule.getPlatformDomainGraphURI(classAnalizer.getGraphSuffix()).toString();
                return NodeFactory.createURI(classGraphURI);
            } catch (Exception ex) {
                LOGGER.error("Invalid class suffix for: " + objectClass.getCanonicalName() + " - " + classAnalizer.getGraphSuffix(), ex);
            }
        }

        return null;
    }

    public AskBuilder getAskBuilder() {
        return getAskBuilder(getDefaultGraph());
    }

    public AskBuilder getAskBuilder(Node graph) {
        return classQueryBuilder.getAskBuilder(graph);
    }

    public SelectBuilder getSelectBuilder(String lang) {
        return getSelectBuilder(getDefaultGraph(), lang);
    }

    public SelectBuilder getSelectBuilder(Node graph, String lang) {
        return classQueryBuilder.getSelectBuilder(graph, lang);
    }

    public SelectBuilder getCountBuilder(String countFieldName) {
        return getCountBuilder(getDefaultGraph(), countFieldName);
    }

    public SelectBuilder getCountBuilder(Node graph, String countFieldName) {
        return classQueryBuilder.getCountBuilder(graph, countFieldName);
    }

    public UpdateBuilder getCreateBuilder(T instance) throws Exception {
        return getCreateBuilder(getDefaultGraph(), instance);
    }

    public UpdateBuilder getCreateBuilder(Node graph, T instance) throws Exception {
        return classQueryBuilder.getCreateBuilder(graph, instance);
    }

    public void addUpdateBuilder(T oldInstance, T newInstance, UpdateBuilder update) throws Exception {
        addUpdateBuilder(getDefaultGraph(), oldInstance, newInstance, update);
    }

    public void addUpdateBuilder(Node graph, T oldInstance, T newInstance, UpdateBuilder update) throws Exception {
        classQueryBuilder.addUpdateBuilder(graph, oldInstance, newInstance, update);
    }

    public void addCreateBuilder(T instance, UpdateBuilder create) throws Exception {
        addCreateBuilder(getDefaultGraph(), instance, create);
    }

    public void addCreateBuilder(Node graph, T instance, UpdateBuilder create) throws Exception {
        classQueryBuilder.addCreateBuilder(graph, instance, create);
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
        return classAnalizer.getURI(instance);
    }

    public void setUri(T instance, URI uri) throws Exception {
        classAnalizer.setURI(instance, uri);
    }

    public String getURIFieldName() {
        return classAnalizer.getURIFieldName();
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

    public ExprVar getFieldExprVar(String fieldName) throws SPARQLUnknownFieldException {
        Field f = classAnalizer.getFieldFromName(fieldName);
        if (f != null) {
            return new ExprVar(f.getName());
        } else {
            throw new SPARQLUnknownFieldException(f);
        }
    }

    public Expr getFieldOrderExpr(String fieldName) throws SPARQLUnknownFieldException {
        Field f = classAnalizer.getFieldFromName(fieldName);
        if (f != null) {
            if (f.getType().equals(String.class)) {
                return new E_StrLowerCase(new ExprVar(f.getName()));
            } else {
                return new ExprVar(f.getName());
            }
        } else {
            throw new SPARQLUnknownFieldException(f);
        }
    }

    public Field getFieldFromUniqueProperty(Property property) {
        return classAnalizer.getFieldFromUniqueProperty(property);
    }

    @SuppressWarnings("unchecked")
    public URIGenerator<T> getUriGenerator(T instance) {
        URIGenerator<? extends SPARQLResourceModel> generator = classAnalizer.getUriGenerator();
        if (generator == null) {
            generator = (URIGenerator<? extends SPARQLResourceModel>) instance;
        }
        return (URIGenerator<T>) generator;
    }

    public Resource getRDFType() {
        return classAnalizer.getRDFType();
    }
}
