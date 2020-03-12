//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.annotations.SPARQLResourceURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class SPARQLClassAnalyzer {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassAnalyzer.class);

    private final Class<?> objectClass;

    private final Resource resource;
    private final String graphSuffix;
    private final String graphPrefix;

    private Field fieldURI;
    private final Map<Field, Property> dataProperties = new HashMap<>();

    private final Map<Field, Property> objectProperties = new HashMap<>();

    private final Map<Field, Property> dataPropertiesLists = new HashMap<>();

    private final Map<Field, Property> objectPropertiesLists = new HashMap<>();

    private final Map<Field, Property> labelProperties = new HashMap<>();

    private final Map<Field, Property> labelPropertiesLists = new HashMap<>();

    private final Map<String, Field> fieldsByName = new HashMap<>();

    private final Map<Property, Field> fieldsByUniqueProperty = new HashMap<>();

    private final Set<String> managedProperties = new HashSet<>();

    private final BiMap<Method, Field> fieldsByGetter;

    private final BiMap<Method, Field> fieldsBySetter;

    private final Map<Field, SPARQLProperty> annotationsByField = new HashMap<>();
    private final List<Field> optionalFields = new ArrayList<>();

    private final List<Field> reverseRelationFields = new ArrayList<>();

    private final URIGenerator<? extends SPARQLResourceModel> uriGenerator;

    @SuppressWarnings("unchecked")
    public SPARQLClassAnalyzer(Class<?> objectClass) throws SPARQLInvalidClassDefinitionException {
        LOGGER.debug("Start SPARQL model class analyze for: " + objectClass.getName());
        this.objectClass = objectClass;

        LOGGER.debug("Determine RDF Type for class: " + objectClass.getName());
        SPARQLResource resourceAnnotation = ClassUtils.findClassAnnotationRecursivly(objectClass, SPARQLResource.class);
        if (resourceAnnotation == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "annotation not found: " + SPARQLResource.class.getCanonicalName());
        }

        try {
            if (URIGenerator.class.isAssignableFrom(objectClass)) {
                uriGenerator = null;
            } else {
                uriGenerator = resourceAnnotation.uriGenerator().getConstructor().newInstance();
            }
        } catch (NoSuchMethodException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Resource type " + resourceAnnotation.resource() + " uri generator must have an empty constructor", ex);
        } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Technical error while creating uri generator", ex);
        }

        try {
            Class<?> resourceOntology = resourceAnnotation.ontology();
            Field resourceField = resourceOntology.getField(resourceAnnotation.resource());
            resource = (Resource) resourceField.get(null);
            LOGGER.debug("RDF Type for class: " + objectClass.getName() + " is: " + resource.toString());
            if (!resourceAnnotation.graph().isEmpty()) {
                graphSuffix = resourceAnnotation.graph();
            } else {
                graphSuffix = null;
            }

            if (!resourceAnnotation.prefix().isEmpty()) {
                graphPrefix = resourceAnnotation.prefix();
            } else {
                graphPrefix = null;
            }

        } catch (NoSuchFieldException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Resource type " + resourceAnnotation.resource() + " does not exists in ontology: " + resourceAnnotation.ontology().getName(), ex);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Technical error while reading annotation: " + SPARQLResource.class.getCanonicalName(), ex);
        }

        LOGGER.debug("Process fields annotations");
        for (Field field : ClassUtils.getClassFieldsRecursivly(objectClass)) {
            field.setAccessible(true);
            SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);

            if (sProperty != null) {
                LOGGER.debug("Analyse " + SPARQLProperty.class.getCanonicalName() + " annotation for field: " + field.getName());
                analyzeSPARQLPropertyField(sProperty, field);
            } else {
                SPARQLResourceURI sURI = field.getAnnotation(SPARQLResourceURI.class);
                if (sURI != null) {
                    LOGGER.debug("Analyse " + SPARQLResourceURI.class.getCanonicalName() + " annotation for field: " + field.getName());
                    analyzeSPARQLResourceURIField(field);
                }
            }
        }

        LOGGER.debug("Check URI field is defined");
        if (fieldURI == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, SPARQLResourceURI.class.getCanonicalName() + " annotation not found");
        }

        LOGGER.debug("Init fields accessor registry for: " + objectClass.getName());
        Method[] methods = objectClass.getMethods();
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

        for (Field field : ClassUtils.getClassFieldsRecursivly(objectClass)) {

            SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);
            if (sProperty == null) {
                continue;
            }
            Method getter = getGetterFromField(field);
            if (getter == null) {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "no getter found for the field :" + field.getName());
            }
            Method setter = getSetterFromField(field);
            if (setter == null) {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "no setter found for the field :" + field.getName());
            }
        }
    }

    private void analyzeSPARQLPropertyField(SPARQLProperty sProperty, Field field) throws SPARQLInvalidClassDefinitionException {
        Property property;
        try {
            Class<?> propertyOntology = sProperty.ontology();
            Field propertyField = propertyOntology.getField(sProperty.property());
            property = (Property) propertyField.get(null);
        } catch (Exception ex) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Property type " + sProperty.property() + " does not exists in ontology: " + sProperty.ontology().getName(), ex);
        }

        LOGGER.debug("Analyse field type for: " + field.getName());
        Type fieldType = field.getGenericType();
        if (ClassUtils.isGenericType(fieldType)) {
            ParameterizedType parameterizedType = (ParameterizedType) fieldType;
            if (ClassUtils.isGenericList(parameterizedType)) {
                Class<?> genericParameter = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                LOGGER.debug("Field " + field.getName() + " is a list of: " + genericParameter.getName());
                if (genericParameter == SPARQLLabel.class) {
                    LOGGER.debug("Field " + field.getName() + " is a label list of: " + objectClass.getName());
                    labelPropertiesLists.put(field, property);
                } else if (SPARQLDeserializers.existsForClass(genericParameter)) {
                    LOGGER.debug("Field " + field.getName() + " is a data property list of: " + objectClass.getName());
                    dataPropertiesLists.put(field, property);
                } else {
                    LOGGER.debug("Field " + field.getName() + " is an object property list of: " + objectClass.getName());
                    objectPropertiesLists.put(field, property);
                }
            } else {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " as an unsupported type, only List are allowed as generics");
            }
        } else if ((Class<?>) fieldType == SPARQLLabel.class) {
            LOGGER.debug("Field " + field.getName() + " is a label of: " + objectClass.getName());
            labelProperties.put(field, property);
        } else if (SPARQLDeserializers.existsForClass((Class<?>) fieldType)) {
            LOGGER.debug("Field " + field.getName() + " is a data property of: " + objectClass.getName());
            dataProperties.put(field, property);
        } else if (SPARQLClassObjectMapper.existsForClass((Class<?>) fieldType)) {
            LOGGER.debug("Field " + field.getName() + " is an object property of: " + objectClass.getName());
            objectProperties.put(field, property);
        } else {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " refer to an invalid SPARQL class model: " + fieldType.getTypeName());
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

        annotationsByField.put(field, sProperty);

        LOGGER.debug("Store field " + field.getName() + " in global index by name");
        fieldsByName.put(field.getName(), field);

        managedProperties.add(property.getURI());
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
                    SPARQLResourceURI.class.getCanonicalName() + " annotation must be unique "
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

    public void forEachLabelProperty(BiConsumer<Field, Property> lambda) {
        labelProperties.forEach(lambda);
    }

    public void forEachLabelPropertyList(BiConsumer<Field, Property> lambda) {
        labelPropertiesLists.forEach(lambda);
    }

    public Field getFieldFromGetter(Method method) {
        return fieldsByGetter.get(method);
    }

    public Field getFieldFromSetter(Method method) {
        return fieldsBySetter.get(method);
    }

    private Method getGetterFromField(Field field) {
        return fieldsByGetter.inverse().get(field);
    }

    public Object getFieldValue(Field field, Object instance) throws Exception {
        return instance.getClass().getMethod(fieldsByGetter.inverse().get(field).getName()).invoke(instance);
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

    public boolean isDataListField(Field f) {
        return dataPropertiesLists.containsKey(f);
    }

    public boolean isObjectListField(Field f) {
        return objectPropertiesLists.containsKey(f);
    }

    public boolean isLabelField(Field f) {
        return labelProperties.containsKey(f);
    }

    public boolean isLabelListField(Field f) {
        return labelPropertiesLists.containsKey(f);
    }

    public Resource getRDFType() {
        return resource;
    }

    public String getGraphSuffix() {
        return graphSuffix;
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

    public Set<Field> getLabelPropertyFields() {
        return labelProperties.keySet();
    }
    
    public Property getLabelPropertyByField(Field field) {
        return labelProperties.get(field);
    }

    public Set<Field> getLabelListPropertyFields() {
        return labelPropertiesLists.keySet();
    }
    
    public Property getLabelListPropertyByField(Field field) {
        return labelPropertiesLists.get(field);
    }

    public URI getURI(Object instance) {
        try {
            return (URI) getFieldValue(getURIField(), instance);
        } catch (Exception ex) {
            LOGGER.error("Exception while getting SPARQL object URI (should never happend)", ex);
            return null;
        }
    }

    public void setURI(Object instance, URI uri) throws Exception {
        try {
            getSetterFromField(getURIField()).invoke(instance, uri);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.error("Error while setting object uri", ex);
            throw ex;
        }
    }

    public URIGenerator<? extends SPARQLResourceModel> getUriGenerator() {
        return uriGenerator;
    }

    public Set<String> getManagedProperties() {
        return managedProperties;
    }

    public String getResourceGraphPrefix() {
        return this.graphPrefix;
    }

    public SPARQLProperty getFieldAnnotation(Field field) {
        return annotationsByField.get(field);
    }
}
