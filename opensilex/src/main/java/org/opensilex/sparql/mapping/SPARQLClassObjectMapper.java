/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.SPARQLResult;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.annotations.SPARQLCache;
import org.opensilex.sparql.annotations.SPARQLCacheOption;
import org.opensilex.sparql.cache.SPARQLCacheManager;
import org.opensilex.sparql.cache.SPARQLNoCacheManager;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.utils.ClassInfo;
import org.opensilex.sparql.deserializer.Deserializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class SPARQLClassObjectMapper<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassObjectMapper.class);
    private static final Map<Class<?>, SPARQLClassObjectMapper<?>> SPARQL_CLASSES_DESCRIPTIONS = new HashMap<>();
    private static final List<Class<?>> SPARQL_CLASSES_BUILDING = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static synchronized <T> SPARQLClassObjectMapper<T> getForClass(Class<T> objectClass) throws SPARQLInvalidClassDefinitionException {
        SPARQLClassObjectMapper<T> sparqlObjectMapper;
        if (SPARQL_CLASSES_DESCRIPTIONS.containsKey(objectClass)) {
            sparqlObjectMapper = (SPARQLClassObjectMapper<T>) SPARQL_CLASSES_DESCRIPTIONS.get(objectClass);
        } else if (!SPARQL_CLASSES_BUILDING.contains(objectClass)) {
            SPARQL_CLASSES_BUILDING.add(objectClass);
            sparqlObjectMapper = new SPARQLClassObjectMapper<>(objectClass);
            SPARQL_CLASSES_DESCRIPTIONS.put(objectClass, sparqlObjectMapper);
            SPARQL_CLASSES_BUILDING.remove(objectClass);
        } else {
            sparqlObjectMapper = null;
        }

        return sparqlObjectMapper;
    }

    private final Class<T> objectClass;
    private final Constructor<T> constructor;
    private final SPARQLCacheManager cacheManager;
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

        LOGGER.debug("Init SPARQL instance cache manager: " + objectClass.getName());
        cacheManager = createCacheManager();

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

    public void removeCacheInstance(URI uri) {
        cacheManager.removeCacheInstance(uri);
    }

    @SuppressWarnings("unchecked")
    public T getCacheInstance(URI uri) {
        return (T) cacheManager.getCacheInstance(uri);
    }

    public boolean hasCacheInstance(URI uri) {
        return cacheManager.hasCacheInstance(uri);
    }

    public void putCacheInstance(URI uri, T obj) {
        cacheManager.putCacheInstance(uri, obj);
    }

    private SPARQLCacheManager createCacheManager() {
        SPARQLCacheManager localCacheManager = null;
        SPARQLCache sCache = objectClass.getAnnotation(SPARQLCache.class);
        if (sCache != null) {
            try {
                SPARQLCacheOption[] cacheOptions = sCache.value();
                Map<String, String> options = new HashMap<>();
                for (SPARQLCacheOption option : cacheOptions) {
                    options.put(option.key(), option.value());
                }

                localCacheManager = sCache.implementation().getConstructor(Map.class).newInstance(options);
            } catch (Exception ex) {
                LOGGER.warn("Impossible to load cache manager for class: " + objectClass.getCanonicalName());
                LOGGER.warn(ex.getMessage());
            }
        }

        if (localCacheManager == null) {
            LOGGER.debug("Use no cache manager for class: " + objectClass.getCanonicalName());
            localCacheManager = new SPARQLNoCacheManager();
        }

        return localCacheManager;
    }

    public AskBuilder getAskBuilder() {
        return classQueryBuilder.getAskBuilder();
    }

    public SelectBuilder getSelectBuilder() throws SPARQLInvalidClassDefinitionException {
        return classQueryBuilder.getSelectBuilder();
    }

    public SelectBuilder getCountBuilder(String countFieldName) {
        return classQueryBuilder.getCountBuilder(countFieldName);
    }

    public UpdateBuilder getCreateBuilder(T instance) throws Exception {
        return classQueryBuilder.getCreateBuilder(instance);
    }

    public void addUpdateBuilder(T oldInstance, T newInstance, UpdateBuilder update) throws Exception {
        classQueryBuilder.addUpdateBuilder(oldInstance, newInstance, update);
    }

    public void addCreateBuilder(T instance, UpdateBuilder create) throws Exception {
        classQueryBuilder.addCreateBuilder(instance, create);
    }

    public UpdateBuilder getDeleteBuilder(T instance) throws Exception {
        return classQueryBuilder.getDeleteBuilder(instance);
    }

    public void addDeleteBuilder(T instance, UpdateBuilder delete) throws Exception {
        classQueryBuilder.addDeleteBuilder(instance, delete);
    }

    public URI getURI(Object instance) {
        return classAnalizer.getURI(instance);
    }

    public String getURIFieldName() {
        return classAnalizer.getURIFieldName();
    }

    public Field getFieldFromUniqueProperty(Property property) {
        return classAnalizer.getFieldFromUniqueProperty(property);
    }
}
