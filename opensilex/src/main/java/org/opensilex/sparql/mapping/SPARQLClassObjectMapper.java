//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.expr.*;
import org.opensilex.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.annotations.*;
import org.opensilex.sparql.deserializer.*;
import org.opensilex.sparql.exceptions.*;
import org.opensilex.sparql.utils.*;
import org.opensilex.utils.*;
import org.slf4j.*;

/**
 *
 * @author vincent
 */
public class SPARQLClassObjectMapper<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassObjectMapper.class);
    private static Set<Class<?>> SPARQL_CLASSES_LIST;
    private static final Map<Class<?>, SPARQLClassObjectMapper<?>> SPARQL_CLASSES_MAPPER = new HashMap<>();
    private static final Map<Resource, SPARQLClassObjectMapper<?>> SPARQL_RESOURCES_MAPPER = new HashMap<>();

    private static <T> Class<? super T> getConcreteClass(Class<T> objectClass) {
        if (SPARQLProxyMarker.class.isAssignableFrom(objectClass)) {
            return getConcreteClass(objectClass.getSuperclass());
        } else {
            return objectClass;
        }
    }

    public static void initialize() throws SPARQLInvalidClassDefinitionException {
        SPARQL_CLASSES_LIST = ClassInfo.getAnnotatedClasses(SPARQLResource.class);
        
        for (Class<?> sparqlModelClass : SPARQL_CLASSES_LIST) {
            SPARQLClassObjectMapper<?> sparqlObjectMapper = new SPARQLClassObjectMapper<>(sparqlModelClass);
            SPARQL_CLASSES_MAPPER.put(sparqlModelClass, sparqlObjectMapper);
            SPARQL_RESOURCES_MAPPER.put(sparqlObjectMapper.getRDFType(), sparqlObjectMapper);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> SPARQLClassObjectMapper<T> getForClass(Class<T> objectClass) throws SPARQLMapperNotFoundException {
        Class<T> concreteObjectClass = (Class<T>) getConcreteClass(objectClass);

        if (SPARQL_CLASSES_MAPPER.containsKey(concreteObjectClass)) {
            return (SPARQLClassObjectMapper<T>) SPARQL_CLASSES_MAPPER.get(concreteObjectClass);
        } else {
            throw new SPARQLMapperNotFoundException(concreteObjectClass);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> SPARQLClassObjectMapper<T> getForResource(Resource resource) throws SPARQLMapperNotFoundException {

        if (SPARQL_RESOURCES_MAPPER.containsKey(resource)) {
            return (SPARQLClassObjectMapper<T>) SPARQL_RESOURCES_MAPPER.get(resource);
        } else {
            throw new SPARQLMapperNotFoundException(resource);
        }

    }

    public static Node getGraph(Class<?> c) throws SPARQLMapperNotFoundException {
        return getForClass(c).getDefaultGraph();
    }

    public static boolean existsForClass(Class<?> c) {
        return SPARQL_CLASSES_LIST.contains(c);
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

    public Class<?> getGenericListFieldType(Field f) {
        return ClassInfo.getGenericTypeFromClass(f.getClass());
    }

    public T createInstance(URI uri, SPARQLService service) throws Exception {
        SPARQLProxyResource<T> proxy = new SPARQLProxyResource<>(uri, objectClass, service);
        T instance = proxy.loadIfNeeded();
        if (instance != null) {
            return proxy.getInstance();
        } else {
            return null;
        }

    }

    public T createInstance(SPARQLResult result, SPARQLService service) throws Exception {
        String realType = result.getStringValue(SPARQLClassQueryBuilder.typeDef.getName());
        if (!realType.equals(getRDFType().toString())) {

        }

        URI uri = new URI(result.getStringValue(classAnalizer.getURIFieldName()));

        T instance = createInstance(uri);

        for (Field field : classAnalizer.getDataPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

            String strValue = result.getStringValue(field.getName());

            if (strValue != null) {
                if (Deserializers.existsForClass(field.getType())) {
                    Object objValue = Deserializers.getForClass(field.getType()).fromString(strValue);
                    setter.invoke(instance, objValue);
                } else {
                    //TODO change exception
                    throw new Exception("No deserializer for field: " + field.getName());
                }
            }
        }

        for (Field field : classAnalizer.getObjectPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);
            if (result.getStringValue(field.getName()) != null) {
                URI objURI = new URI(result.getStringValue(field.getName()));

                SPARQLProxyResource<?> proxy = new SPARQLProxyResource<>(objURI, field.getType(), service);
                setter.invoke(instance, proxy.getInstance());
            }
        }

        for (Field field : classAnalizer.getDataListPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

            SPARQLProxyListData<?> proxy = new SPARQLProxyListData<>(uri, classAnalizer.getDataListPropertyByField(field), ClassInfo.getGenericTypeFromField(field), classAnalizer.isReverseRelation(field), service);
            setter.invoke(instance, proxy.getInstance());
        }

        for (Field field : classAnalizer.getObjectListPropertyFields()) {
            Method setter = classAnalizer.getSetterFromField(field);

            SPARQLProxyListObject<?> proxy = new SPARQLProxyListObject<>(uri, classAnalizer.getObjectListPropertyByField(field), ClassInfo.getGenericTypeFromField(field), classAnalizer.isReverseRelation(field), service);
            setter.invoke(instance, proxy.getInstance());
        }

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

    public Node getDefaultGraph() {
        if (classAnalizer.getGraphSuffix() != null) {
            try {
                String classGraphURI = OpenSilex.getPlatformURI(classAnalizer.getGraphSuffix()).toString();
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

    public SelectBuilder getSelectBuilder() {
        return getSelectBuilder(getDefaultGraph());
    }

    public SelectBuilder getSelectBuilder(Node graph) {
        return classQueryBuilder.getSelectBuilder(graph);
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
        URIGenerator<Object> generator = classAnalizer.getUriGenerator();
        if (generator == null) {
            generator = (URIGenerator<Object>) instance;
        }
        return (URIGenerator<T>) generator;
    }

    public Resource getRDFType() {
        return classAnalizer.getRDFType();
    }
}
