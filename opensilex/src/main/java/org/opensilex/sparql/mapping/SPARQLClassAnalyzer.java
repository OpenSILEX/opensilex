/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.annotations.SPARQLResourceURI;
import org.opensilex.sparql.deserializer.Deserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author vincent
 */
public class SPARQLClassAnalyzer {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassAnalyzer.class);

    private final Class<?> objectClass;

    private final Resource resource;
    private final Node graph;

    private Field fieldURI;
    private final Map<Field, Property> dataProperties = new HashMap<>();

    private final Map<Field, Property> objectProperties = new HashMap<>();

    private final Map<Field, Property> dataPropertiesLists = new HashMap<>();

    private final Map<Field, Property> objectPropertiesLists = new HashMap<>();

    private final Map<String, Field> fieldsByName = new HashMap<>();

    private final Map<Property, Field> fieldsByUniqueProperty = new HashMap<>();

    private final BiMap<Method, Field> fieldsByGetter;

    private final BiMap<Method, Field> fieldsBySetter;

    private final List<Field> optionalFields = new ArrayList<>();

    private final List<Field> reverseRelationFields = new ArrayList<>();

    public SPARQLClassAnalyzer(Class<?> objectClass) throws SPARQLInvalidClassDefinitionException {
        LOGGER.debug("Start SPARQL annotation analyze for class: " + objectClass.getName());
        this.objectClass = objectClass;

        LOGGER.debug("Determine RDF Type for class: " + objectClass.getName());
        SPARQLResource resourceAnnotation = ClassInfo.findClassAnnotationRecursivly(objectClass, SPARQLResource.class);
        if (resourceAnnotation == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "SPARQLResource annotation not found");
        }
        try {
            Class<?> resourceOntology = resourceAnnotation.ontology();
            Field resourceField = resourceOntology.getField(resourceAnnotation.resource());
            resource = (Resource) resourceField.get(null);
            LOGGER.debug("RDF Type for class: " + objectClass.getName() + " is: " + resource.toString());
            if (!resourceAnnotation.graph().isEmpty()) {
                graph = Ontology.nodeURI(resourceAnnotation.graph());
            } else {
                graph = null;
            }
        } catch (NoSuchFieldException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Resource type " + resourceAnnotation.resource() + " does not exists in ontology: " + resourceAnnotation.ontology().getName(), ex);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Technical error while reading SPARQLResource annotation", ex);
        }

        LOGGER.debug("Process fields annotations");
        for (Field field : ClassInfo.getClassFieldsRecursivly(objectClass)) {
            field.setAccessible(true);
            SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);

            if (sProperty != null) {
                LOGGER.debug("Analyse SPARQLProperty annotation for field: " + field.getName());
                analyzeSPARQLPropertyField(sProperty, field);
            } else {
                SPARQLResourceURI sURI = field.getAnnotation(SPARQLResourceURI.class);
                if (sURI != null) {
                    LOGGER.debug("Analyse SPARQLResourceURI annotation for field: " + field.getName());
                    analyzeSPARQLResourceURIField(field);
                }
            }
        }

        LOGGER.debug("Check URI field is defined");
        if (fieldURI == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "SPARQLResourceURI annotation not found");
        }

        LOGGER.debug("Init fields accessor registry for: " + objectClass.getName());
        Method[] methods = objectClass.getDeclaredMethods();
        fieldsByGetter = HashBiMap.create(fieldsByName.size());
        fieldsBySetter = HashBiMap.create(fieldsByName.size());
        for (Method method : methods) {
            if (isGetter(method)) {
                Field getter = findFieldByGetter(method);
                if (getter != null) {
                    fieldsByGetter.put(method, getter);
                }
            } else if (isSetter(method)) {
                Field setter = findFieldBySetter(method);
                if (setter != null) {
                    fieldsBySetter.put(method, setter);
                }
            }
        }
    }

    private void analyzeSPARQLPropertyField(SPARQLProperty sProperty, Field field) throws SPARQLInvalidClassDefinitionException {
        Property property;
        try {
            Class<?> propertyOntology = sProperty.ontology();
            Field propertyField = propertyOntology.getField(sProperty.property());
            property = (Property) propertyField.get(null);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Property type " + sProperty.property() + " does not exists in ontology: " + sProperty.ontology().getName(), ex);
        }

        LOGGER.debug("Analyse field type for: " + field.getName());
        Type fieldType = field.getGenericType();
        if (ClassInfo.isGenericType(fieldType)) {
            ParameterizedType parameterizedType = (ParameterizedType) fieldType;
            if (ClassInfo.isGenericList(parameterizedType)) {
                Class<?> genericParameter = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                LOGGER.debug("Field " + field.getName() + " is a list of: " + genericParameter.getName());
                if (Deserializers.existsForClass(genericParameter)) {
                    LOGGER.debug("Field " + field.getName() + " is a data property list of: " + objectClass.getName());
                    dataPropertiesLists.put(field, property);
                } else {
                    LOGGER.debug("Field " + field.getName() + " is an object property list of: " + objectClass.getName());
                    objectPropertiesLists.put(field, property);
                }
            } else {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " as an unsupported type, only List are allowed as generics");
            }
        } else if (Deserializers.existsForClass((Class<?>) fieldType)) {
            LOGGER.debug("Field " + field.getName() + " is a data property of: " + objectClass.getName());
            dataProperties.put(field, property);
        } else {
            LOGGER.debug("Test if field type has a valid SPARQL class description for: " + field.getName());
            SPARQLClassObjectMapper.getForClass((Class<?>) fieldType);
            LOGGER.debug("Field " + field.getName() + " is an object property of: " + objectClass.getName());
            objectProperties.put(field, property);
        }

        LOGGER.debug("Determine if field " + field.getName() + " is a unique property (used only once for SPARQL property " + property.getLocalName() + ")");
        if (fieldsByUniqueProperty.containsKey(property)) {
            fieldsByUniqueProperty.remove(property);
        } else {
            fieldsByUniqueProperty.put(property, field);
        }

        LOGGER.debug("Determine if field " + field.getName() + " is required or optional");
        if (!sProperty.required()) {
            optionalFields.add(field);
        }

        LOGGER.debug("Determine if field " + field.getName() + " is a reversed property or not");
        if (sProperty.inverse()) {
            reverseRelationFields.add(field);
        }

        LOGGER.debug("Store field " + field.getName() + " in global index by name");
        fieldsByName.put(field.getName(), field);
    }

    private void analyzeSPARQLResourceURIField(Field field) throws SPARQLInvalidClassDefinitionException {
        if (fieldURI == null) {
            LOGGER.debug("Field " + field.getName() + " defined as URI field for: " + objectClass.getName());
            fieldURI = field;
            LOGGER.debug("Store field " + field.getName() + " in global index by name");
            fieldsByName.put(field.getName(), field);
        } else {
            throw new SPARQLInvalidClassDefinitionException(
                    objectClass,
                    "SPARQLResourceURI annotation must be unique "
                    + "and is defined multiple times for field " + fieldURI.getName()
                    + " and for field " + field.getName()
            );
        }
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
            return !method.getReturnType().equals(void.class);
        }
    }

    private Field findFieldByGetter(Method getter) {
        String getterName = getter.getName();

        Field candidateField = null;

        if (getterName.startsWith("get")) {
            String fieldName = getterName.replaceFirst("get", "");
            String firstLetter = fieldName.substring(0, 1);
            fieldName = fieldName.replaceFirst(firstLetter, firstLetter.toLowerCase());
            candidateField = fieldsByName.get(fieldName);
        } else if (getterName.startsWith("is")) {
            String fieldName = getterName.replaceFirst("is", "");
            String firstLetter = fieldName.substring(0, 1);
            fieldName = fieldName.replaceFirst(firstLetter, firstLetter.toLowerCase());
            Field booleanField = fieldsByName.get(fieldName);
            if (booleanField != null
                    && (booleanField.getType().equals(boolean.class)
                    || booleanField.getType().equals(Boolean.class))) {
                candidateField = booleanField;
            }
        }

        if (candidateField != null && candidateField.getType().equals(getter.getReturnType())) {
            return candidateField;
        } else {
            return null;
        }
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && method.getReturnType().equals(void.class);
    }

    private Field findFieldBySetter(Method setter) {
        String setterName = setter.getName();
        if (setterName.startsWith("set")) {
            String fieldName = setterName.replaceFirst("set", "");
            String firstLetter = fieldName.substring(0, 1);
            fieldName = fieldName.replaceFirst(firstLetter, firstLetter.toLowerCase());
            Field field = fieldsByName.get(fieldName);
            if (field != null
                    && setter.getParameterCount() == 1
                    && field.getType().equals(setter.getParameters()[0].getType())) {
                return field;
            }
        }

        return null;
    }

    public boolean isOptional(Field field) {
        return optionalFields.contains(field);
    }

    public boolean isReverseRelation(Field field) {
        return reverseRelationFields.contains(field);
    }

    public void forEachDataProperty(BiConsumer<Field, Property> lambda) {
        dataProperties.forEach(lambda);
    }

    public void forEachObjectProperty(BiConsumer<Field, Property> lambda) {
        objectProperties.forEach(lambda);
    }

    public void forEachDataPropertyList(BiConsumer<Field, Property> lambda) {
        dataPropertiesLists.forEach(lambda);
    }

    public void forEachObjectPropertyList(BiConsumer<Field, Property> lambda) {
        objectPropertiesLists.forEach(lambda);
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

    public Field getURIField() {
        return fieldURI;
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
        return resource;
    }

    public Node getGraph() {
        return graph;
    }

    public Field getFieldFromName(String fieldName) {
        return fieldsByName.get(fieldName);
    }

    public Field getFieldFromUniqueProperty(Property property) {
        return fieldsByUniqueProperty.get(property);
    }

    public Set<Field> getObjectPropertyFields() {
        return objectProperties.keySet();
    }

    public Property getObjectPropertyByField(Field field) {
        return objectProperties.get(field);
    }

    public Set<Field> getDataPropertyFields() {
        return dataProperties.keySet();
    }

    public Property getDataPropertyByField(Field field) {
        return dataProperties.get(field);
    }

    public Set<Field> getObjectListPropertyFields() {
        return objectPropertiesLists.keySet();
    }

    public Property getObjectListPropertyByField(Field field) {
        return objectPropertiesLists.get(field);
    }

    public Set<Field> getDataListPropertyFields() {
        return dataPropertiesLists.keySet();
    }

    public Property getDataListPropertyByField(Field field) {
        return dataPropertiesLists.get(field);
    }

    public URI getURI(Object instance) {
        try {
            return (URI) getURIField().get(instance);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            // TODO warn or error, should not happend
            return null;
        }
    }

}
