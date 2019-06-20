/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.opensilex.module.core.service.sparql.annotations.SPARQLProperty;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResource;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResourceURI;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLInvalidClassDescriptorException;
import org.opensilex.utils.deserializer.Deserializers;
import org.opensilex.utils.ontology.Ontology;
import org.opensilex.utils.ClassInfo;

/**
 *
 * @author vincent
 */
public class SPARQLClassDescriptor<T> {

    private final Class<T> objectClass;

    private final Resource RDFType;

    private Field fieldURI;

    private Constructor<T> constructor;

    private final Map<Field, Property> dataProperties = new HashMap<>();

    private final Map<Field, Property> objectProperties = new HashMap<>();

    private final Map<Field, Property> dataPropertiesLists = new HashMap<>();

    private final Map<Field, Property> objectPropertiesLists = new HashMap<>();

    private final Map<String, Field> fieldsByName = new HashMap<>();

    private final Map<Property, Field> fieldsByUniqueProperty = new HashMap<>();

    private BiMap<Method, Field> fieldsByGetter;

    private BiMap<Method, Field> fieldsBySetter;

    private List<Field> optionalFields;

    private List<Field> reverseRelationFields;

    @SuppressWarnings("rawtypes")
    private SPARQLClassDescriptor(Class<T> objectClass) throws SPARQLInvalidClassDescriptorException {
        this.objectClass = objectClass;

        try {
            constructor = objectClass.getConstructor();

            SPARQLResource sResource = objectClass.getAnnotation(SPARQLResource.class);

            if (sResource == null) {
                throw new SPARQLInvalidClassDescriptorException(objectClass, "SPARQLResource annotation not found");
            }

            Class resourceOntology = sResource.ontology();
            Field resourceField = resourceOntology.getField(sResource.resource());
            RDFType = (Resource) resourceField.get(null);

            for (Field field : objectClass.getFields()) {
                SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);

                if (sProperty != null) {
                    Class propertyOntology = sProperty.ontology();
                    Field propertyField = propertyOntology.getField(sProperty.property());
                    Property property = (Property) propertyField.get(null);
                    Type fieldType = field.getGenericType();

                    if (ClassInfo.isGenericType(fieldType)) {
                        ParameterizedType parameterizedType = (ParameterizedType) fieldType;
                        if (ClassInfo.isGenericList(parameterizedType)) {
                            Class<?> genericParameter = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                            if (Deserializers.existsForClass(genericParameter)) {
                                dataPropertiesLists.put(field, property);
                            } else {
                                objectPropertiesLists.put(field, property);
                            }
                        } else {
                            // TODO throw exception
                        }
                    } else if (Deserializers.existsForClass((Class<?>) fieldType)) {
                        dataProperties.put(field, property);
                    } else {
                        objectProperties.put(field, property);
                    }

                    if (fieldsByUniqueProperty.containsKey(property)) {
                        fieldsByUniqueProperty.remove(property);
                    } else {
                        fieldsByUniqueProperty.put(property, field);
                    }

                    if (!sProperty.required()) {
                        optionalFields.add(field);
                    }

                    if (sProperty.inverse()) {
                        reverseRelationFields.add(field);
                    }
                } else {
                    SPARQLResourceURI sURI = field.getAnnotation(SPARQLResourceURI.class);
                    if (sURI != null) {
                        if (fieldURI == null) {
                            fieldURI = field;
                        } else {
                            throw new SPARQLInvalidClassDescriptorException(
                                    objectClass,
                                    "SPARQLResourceURI annotation must be unique "
                                    + "and is defined multiple times for field " + fieldURI.getName()
                                    + " and for field " + field.getName()
                            );
                        }
                    }
                }
                fieldsByName.put(fieldURI.getName(), fieldURI);
            }

            if (fieldURI == null) {
                throw new SPARQLInvalidClassDescriptorException(objectClass, "SPARQLResourceURI annotation not found");
            }

            initFieldAccessorMaps();

        } catch (IllegalAccessException
                | IllegalArgumentException
                | NoSuchFieldException
                | NoSuchMethodException
                | SecurityException ex) {
            throw new SPARQLInvalidClassDescriptorException(objectClass, "Impossible to create SPARQL object class descriptor", ex);
        }
    }

    public boolean isOptional(Field field) {
        return optionalFields.contains(field);
    }

    public boolean isReverseRelation(Field field) {
        return reverseRelationFields.contains(field);
    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public void forEachDataProperty(BiConsumer<Field, Property> lambda) {
        dataProperties.forEach(lambda);
    }

    public void forEachObjectProperty(BiConsumer<Field, Property> lambda) {
        dataProperties.forEach(lambda);
    }

    public void forEachDataPropertyList(BiConsumer<Field, Property> lambda) {
        dataPropertiesLists.forEach(lambda);
    }

    public void forEachObjectPropertyList(BiConsumer<Field, Property> lambda) {
        objectPropertiesLists.forEach(lambda);
    }

    private void initFieldAccessorMaps() {
        Method[] methods = objectClass.getDeclaredMethods();
        fieldsByGetter = HashBiMap.create(fieldsByName.size());
        fieldsBySetter = HashBiMap.create(fieldsByName.size());
        for (Method method : methods) {
            String accessorName = method.getName();

            if (isGetter(method)) {
                Field getter = findFieldByGetterName(method.getName());
                if (getter != null) {
                    fieldsByGetter.put(method, getter);
                }
            } else if (isSetter(method)) {
                Field setter = findFieldBySetterName(method.getName());
                if (setter != null) {
                    fieldsBySetter.put(method, setter);
                }
            }
        }
    }

    public Field getFieldFromGetter(Method method) {
        return fieldsByGetter.get(method);
    }

    public Field getFieldFromSetter(Method method) {
        return fieldsBySetter.get(method);
    }

    public Method getGetterFromField(Field field) {
        return fieldsByGetter.inverse().get(field);
    }

    public Method getSetterFromField(Field field) {
        return fieldsBySetter.inverse().get(field);
    }

    public String getURIFieldName() {
        return fieldURI.getName();
    }

    public boolean isURIField(Field f) {
        return fieldURI.equals(f);
    }

    public boolean isDataPropertyField(Field f) {
        return dataProperties.containsKey(f);
    }

    public boolean isObjectPropertyField(Field f) {
        return objectProperties.containsKey(f);
    }

    boolean isDataListField(Field f) {
        return dataPropertiesLists.containsKey(f);
    }

    boolean isObjectListField(Field f) {
        return objectPropertiesLists.containsKey(f);
    }

    public Resource getRDFType() {
        return RDFType;
    }

    public Class<?> getGenericListFieldType(Field f) {
        return ClassInfo.getGenericTypeFromClass(f.getClass());
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class, SPARQLClassDescriptor> SPARQL_CLASSES_DESCRIPTIONS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> SPARQLClassDescriptor<T> getForClass(Class<T> objectClass) throws SPARQLInvalidClassDescriptorException {
        SPARQLClassDescriptor<T> descriptor;
        if (SPARQL_CLASSES_DESCRIPTIONS.containsKey(objectClass)) {
            descriptor = SPARQL_CLASSES_DESCRIPTIONS.get(objectClass);
        } else {
            descriptor = new SPARQLClassDescriptor<>(objectClass);
            SPARQL_CLASSES_DESCRIPTIONS.put(objectClass, descriptor);
        }

        return descriptor;
    }

    public Field getFieldFromName(String fieldName) {
        return fieldsByName.get(fieldName);
    }

    public Field getFieldFromUniqueProperty(Property property) {
        return fieldsByUniqueProperty.get(property);
    }

    private boolean isGetter(Method method) {
        if (!method.getName().startsWith("get")) {
            if (method.getName().startsWith("is")) {
                return method.getReturnType().equals(boolean.class)
                        || method.getReturnType().equals(Boolean.class);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private Field findFieldByGetterName(String getterName) {
        if (getterName.startsWith("get")) {
            String fieldName = getterName.replaceFirst("get", "").toLowerCase();
            return fieldsByName.get(fieldName);
        } else if (getterName.startsWith("is")) {
            String fieldName = getterName.replaceFirst("is", "").toLowerCase();
            Field booleanField = fieldsByName.get(fieldName);
            if (booleanField != null
                    && (booleanField.getType().equals(boolean.class)
                    || booleanField.getType().equals(Boolean.class))) {
                return booleanField;
            }

            return null;
        } else {
            return null;
        }
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set");
    }

    private Field findFieldBySetterName(String setterName) {
        if (setterName.startsWith("set")) {
            String fieldName = setterName.replaceFirst("set", "").toLowerCase();
            return fieldsByName.get(fieldName);
        } else {
            return null;
        }
    }

    public T createInstance(SPARQLResult result, SPARQLService service) throws Exception {
        URI uri = new URI(result.getStringValue(getURIFieldName()));

        T instance = createInstance(uri);

        dataProperties.forEach((Field field, Property property) -> {
            Method setter = getSetterFromField(field);
            String strValue = result.getStringValue(field.getName());
            if (Deserializers.existsForClass(field.getType())) {
                try {
                    Object objValue = Deserializers.getForClass(field.getType()).fromString(strValue);
                    setter.invoke(instance, objValue);
                } catch (Exception ex) {
                    // TODO warn
                }
            } else {
                //TODO warn
            }
        });

        objectProperties.forEach((Field field, Property property) -> {
            try {
                Method setter = getSetterFromField(field);
                URI objURI = new URI(result.getStringValue(field.getName()));

                SPARQLProxyResource proxy = new SPARQLProxyResource(objURI, field.getType(), service);
                setter.invoke(instance, proxy.getInstance());
            } catch (Exception ex) {
                // TODO warn
            }

        });

        dataPropertiesLists.forEach((Field field, Property property) -> {
            try {
                Method setter = getSetterFromField(field);

                SPARQLProxyListData proxy = new SPARQLProxyListData(uri, property, field.getType(), service);
                setter.invoke(instance, proxy.getInstance());
            } catch (Exception ex) {
                // TODO warn
            }

        });

        objectPropertiesLists.forEach((Field field, Property property) -> {
            try {
                Method setter = getSetterFromField(field);

                SPARQLProxyListObject proxy = new SPARQLProxyListObject(uri, property, field.getType(), service);
                setter.invoke(instance, proxy.getInstance());
            } catch (Exception ex) {
                // TODO warn
            }

        });

        return instance;
    }

    public T createInstance(URI uri) throws Exception {
        T instance = constructor.newInstance();

        if (uri != null) {
            Method uriSetter = getSetterFromField(fieldURI);
            uriSetter.invoke(instance, uri);
        }

        return instance;
    }

    private SelectBuilder selectBuilder;

    public SelectBuilder getSelectBuilder() throws SPARQLInvalidClassDescriptorException {
        if (selectBuilder == null) {
            selectBuilder = new SelectBuilder();

            String uriFieldName = getURIFieldName();
            selectBuilder.addVar(uriFieldName);
            selectBuilder.addWhere(uriFieldName, RDF.type, getRDFType());

            dataProperties.forEach((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field);
            });

            objectProperties.forEach((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field);
            });
        }

        return selectBuilder.clone();
    }

    private void addSelectProperty(SelectBuilder select, String uriFieldName, Property property, Field field) {
        String name = field.getName();
        if (isReverseRelation(field)) {
            if (isOptional(field)) {
                select.addOptional(name, property, uriFieldName);
            } else {
                select.addWhere(name, property, uriFieldName);
            }
        } else {
            if (isOptional(field)) {
                select.addOptional(uriFieldName, property, name);
            } else {
                select.addWhere(uriFieldName, property, name);
            }
        }
    }

    private SelectBuilder countBuilder;

    public SelectBuilder getCountBuilder(String countFieldName) {
        if (countBuilder == null) {
            countBuilder = new SelectBuilder();

            String uriFieldName = getURIFieldName();
            countBuilder.addVar("count(distinct ?" + uriFieldName + ") as " + countFieldName);
            countBuilder.addWhere(uriFieldName, RDF.type, getRDFType());

            dataProperties.forEach((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field);
            });

            objectProperties.forEach((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field);
            });

            countBuilder.addGroupBy(uriFieldName);
        }

        return countBuilder.clone();
    }

    public UpdateBuilder getCreateBuilder(T instance) {
        UpdateBuilder create = new UpdateBuilder();
        addCreateBuilder(instance, create);

        return create;
    }

    public void addCreateBuilder(T instance, UpdateBuilder create) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public UpdateBuilder getDeleteBuilder(URI uri) {
        UpdateBuilder delete = new UpdateBuilder();
        addDeleteBuilder(uri, delete);

        return delete;
    }

    public void addDeleteBuilder(URI uri, UpdateBuilder delete) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void addDeleteBuilder(T instance, UpdateBuilder delete) {
        addDeleteBuilder(getUri(instance), delete);
    }

    private URI getUri(T instance) {
        try {
            return (URI) fieldURI.get(instance);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            // TODO warn or error, should not happend
            return null;
        }
    }

}
