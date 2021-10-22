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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.sparql.annotations.SPARQLIgnore;
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
import org.opensilex.sparql.annotations.SPARQLTypeRDF;
import org.opensilex.sparql.annotations.SPARQLTypeRDFLabel;

/**
 *
 * @author vincent
 */
public final class SPARQLClassAnalyzer {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassAnalyzer.class);

    public Class<?> getObjectClass() {
        return objectClass;
    }

    private final Class<?> objectClass;

    private final SPARQLClassObjectMapperIndex mapperIndex;

    private final Resource resource;
    private final String graph;
    private final String graphPrefix;

    private Field fieldURI;
    private Field fieldType;
    private Field fieldTypeLabel;

    private final Map<String, Property> dataProperties = new HashMap<>();

    private final Map<String, Property> objectProperties = new HashMap<>();

    private final Map<String, Property> dataPropertiesLists = new HashMap<>();

    private final Map<String, Property> objectPropertiesLists = new HashMap<>();

    private final Map<String, Property> labelProperties = new HashMap<>();

    private final Map<String, Property> propertiesByField = new HashMap<>();

    private final Map<String, Field> fieldsByName = new HashMap<>();

    private final Map<Property, String> fieldsByUniqueProperty = new HashMap<>();

    private final Set<Property> managedProperties = new HashSet<>();

    private final BiMap<Method, String> fieldsByGetter;

    private final BiMap<Method, String> fieldsBySetter;

    private final Map<Class<? extends SPARQLResourceModel>, Set<String>> relatedModelsFields;

    private final Map<String, SPARQLProperty> annotationsByField = new HashMap<>();
    private final List<String> optionalFields = new ArrayList<>();

    private final List<String> defaultGraphFields = new ArrayList<>();

    private final List<String> reverseRelationFields = new ArrayList<>();

    private final Map<String, Class<? extends SPARQLResourceModel>> cascadeDeleteClassesField = new HashMap<>();

    private final List<String> autoUpdateFields = new ArrayList<>();
    private final List<String> autoUpdateListFields = new ArrayList<>();

    private final URIGenerator<? extends SPARQLResourceModel> uriGenerator;

    private final boolean ignoreValidation;

    private final boolean allowBlankNode;

    @SuppressWarnings("unchecked")
    public SPARQLClassAnalyzer(SPARQLClassObjectMapperIndex mapperIndex, Class<?> objectClass) throws SPARQLInvalidClassDefinitionException {
        LOGGER.debug("Start SPARQL model class analyze for: " + objectClass.getName());
        this.objectClass = objectClass;
        this.mapperIndex = mapperIndex;

        LOGGER.debug("Determine RDF Type for class: " + objectClass.getName());
        SPARQLResource resourceAnnotation = ClassUtils.findClassAnnotationRecursivly(objectClass, SPARQLResource.class);
        if (resourceAnnotation == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "annotation not found: " + SPARQLResource.class.getCanonicalName());
        }

        ignoreValidation = resourceAnnotation.ignoreValidation();

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
            allowBlankNode = resourceAnnotation.allowBlankNode();
            LOGGER.debug("RDF Type for class: " + objectClass.getName() + " is: " + resource.toString());
            if (!resourceAnnotation.graph().isEmpty()) {
                graph = resourceAnnotation.graph();
            } else {
                graph = null;
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
        relatedModelsFields = new HashMap<>();

        Map<String, Field> fieldMapping = new HashMap<>();
        ClassUtils.executeOnClassFieldsRecursivly(objectClass, (parentClass, field) -> {
            SPARQLIgnore ignoreProperty = field.getDeclaredAnnotation(SPARQLIgnore.class);
            if (ignoreProperty != null) {
                LOGGER.debug("Ignore field: " + field.getName());
                fieldMapping.remove(field.getName());
            } else {
                if (field.getAnnotation(SPARQLProperty.class) != null
                        || field.getAnnotation(SPARQLResourceURI.class) != null
                        || field.getAnnotation(SPARQLTypeRDF.class) != null
                        || field.getAnnotation(SPARQLTypeRDFLabel.class) != null) {
                    fieldMapping.put(field.getName(), field);
                }
            }
        }, SPARQLResourceModel.class);

        for (Field field : fieldMapping.values()) {
            field.setAccessible(true);

            SPARQLIgnore ignoreProperty = field.getAnnotation(SPARQLIgnore.class);
            if (ignoreProperty != null) {
                LOGGER.debug("Ignore field: " + field.getName());
                continue;
            }

            SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);

            if (sProperty != null) {
                LOGGER.debug("Analyse " + SPARQLProperty.class.getCanonicalName() + " annotation for field: " + field.getName());
                analyzeSPARQLPropertyField(sProperty, field);
            } else {
                SPARQLResourceURI sURI = field.getAnnotation(SPARQLResourceURI.class);
                if (sURI != null) {
                    LOGGER.debug("Analyse " + SPARQLResourceURI.class.getCanonicalName() + " annotation for field: " + field.getName());
                    analyzeSPARQLResourceURIField(field);
                } else {
                    SPARQLTypeRDF sType = field.getAnnotation(SPARQLTypeRDF.class);
                    if (sType != null) {
                        LOGGER.debug("Analyse " + SPARQLTypeRDF.class.getCanonicalName() + " annotation for field: " + field.getName());
                        analyzeSPARQLTypeField(field);
                    } else {
                        SPARQLTypeRDFLabel sTypeLabel = field.getAnnotation(SPARQLTypeRDFLabel.class);
                        if (sTypeLabel != null) {
                            LOGGER.debug("Analyse " + SPARQLTypeRDFLabel.class.getCanonicalName() + " annotation for field: " + field.getName());
                            analyzeSPARQLTypeLabelField(field);
                        }
                    }
                }
            }
        }

        LOGGER.debug("Check URI field is defined");
        if (fieldURI == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, SPARQLResourceURI.class.getCanonicalName() + " annotation not found");
        }

        LOGGER.debug("Check Type field is defined");
        if (fieldType == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, SPARQLTypeRDF.class.getCanonicalName() + " annotation not found");
        }

        LOGGER.debug("Check Type label field is defined");
        if (fieldTypeLabel == null) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, SPARQLTypeRDFLabel.class.getCanonicalName() + " annotation not found");
        }

        LOGGER.debug("Init fields accessor registry for: " + objectClass.getName());
        Method[] methods = objectClass.getMethods();
        fieldsByGetter = HashBiMap.create(fieldsByName.size());
        fieldsBySetter = HashBiMap.create(fieldsByName.size());
        for (Method method : methods) {
            if (isGetter(method)) {
                Field getter = findFieldByGetter(method);
                if (getter != null) {
                    fieldsByGetter.put(method, getter.getName());
                }
            } else if (isSetter(method)) {
                Field setter = findFieldBySetter(method);
                if (setter != null) {
                    fieldsBySetter.put(method, setter.getName());
                }
            }
        }

        propertiesByField.putAll(dataProperties);
        propertiesByField.putAll(objectProperties);
        propertiesByField.putAll(labelProperties);
        propertiesByField.putAll(dataPropertiesLists);
        propertiesByField.putAll(objectPropertiesLists);

        for (Field field : fieldMapping.values()) {

            SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);
            if (sProperty == null) {
                continue;
            }
            Method getter = getGetterFromField(field);
            if (getter == null) {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "no getter found for the field: " + field.getName());
            }
            Method setter = getSetterFromField(field);
            if (setter == null) {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "no setter found for the field: " + field.getName());
            }
        }
    }

    public boolean hasValidation() {
        return !ignoreValidation;
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

        Type fType = field.getGenericType();
        if (ClassUtils.isGenericType(fType)) {
            ParameterizedType parameterizedType = (ParameterizedType) fType;
            if (ClassUtils.isGenericList(parameterizedType)) {
                Class<?> genericParameter = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                LOGGER.debug("Field " + field.getName() + " is a list of: " + genericParameter.getName());
                if (genericParameter == SPARQLLabel.class) {
                    throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " has an unsupported type, List<SPARQLLabel> are not supported");
                } else if (SPARQLDeserializers.existsForClass(genericParameter)) {
                    LOGGER.debug("Field " + field.getName() + " is a data property list of: " + objectClass.getName());
                    dataPropertiesLists.put(field.getName(), property);
                } else if (mapperIndex.existsForClass((Class<? extends SPARQLResourceModel>) genericParameter)) {
                    LOGGER.debug("Field " + field.getName() + " is an object property list of: " + objectClass.getName());
                    objectPropertiesLists.put(field.getName(), property);
                    Class<? extends SPARQLResourceModel> fieldClass = (Class<? extends SPARQLResourceModel>) genericParameter;
                    addRelatedModelProperty(field, fieldClass);
                    if (sProperty.cascadeDelete()) {
                        cascadeDeleteClassesField.put(field.getName(), fieldClass);
                    }
                    if (sProperty.autoUpdate()) {
                        autoUpdateListFields.add(field.getName());
                    }
                } else {
                    throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " has an unsupported type, List<" + genericParameter.getCanonicalName() + "> is not supported");
                }
            } else {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " has an unsupported type, only List are allowed as generics");
            }
        } else if ((Class<?>) fType == SPARQLLabel.class) {
            LOGGER.debug("Field " + field.getName() + " is a label of: " + objectClass.getName());
            labelProperties.put(field.getName(), property);
        } else if (SPARQLDeserializers.existsForClass((Class<?>) fType)) {
            LOGGER.debug("Field " + field.getName() + " is a data property of: " + objectClass.getName());
            dataProperties.put(field.getName(), property);
        } else if (mapperIndex.existsForClass((Class<? extends SPARQLResourceModel>) fType)) {
            LOGGER.debug("Field " + field.getName() + " is an object property of: " + objectClass.getName());
            objectProperties.put(field.getName(), property);
            Class<? extends SPARQLResourceModel> fieldClass = (Class<? extends SPARQLResourceModel>) fType;
            addRelatedModelProperty(field, fieldClass);
            if (sProperty.cascadeDelete()) {
                cascadeDeleteClassesField.put(field.getName(), fieldClass);
            }
            if (sProperty.autoUpdate()) {
                autoUpdateFields.add(field.getName());
            }
        } else {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " refer to an invalid SPARQL class model: " + fType.getTypeName());
        }

        LOGGER.debug("Determine if field " + field.getName() + " is a unique property (used only once for SPARQL property " + property.getLocalName() + ")");
        if (fieldsByUniqueProperty.containsKey(property)) {
            fieldsByUniqueProperty.remove(property);
        } else {
            fieldsByUniqueProperty.put(property, field.getName());
        }

        LOGGER.debug("Determine if field " + field.getName() + " is required or optional");
        if (!sProperty.required()) {
            optionalFields.add(field.getName());
        }

        LOGGER.debug("Determine if field " + field.getName() + " is stored in default graph");
        if (sProperty.useDefaultGraph()) {
            defaultGraphFields.add(field.getName());
        }

        LOGGER.debug("Determine if field " + field.getName() + " is a reversed property or not");
        if (sProperty.inverse()) {
            checkAllowedReverseField(field);
            reverseRelationFields.add(field.getName());
        }

        annotationsByField.put(field.getName(), sProperty);

        LOGGER.debug("Store field " + field.getName() + " in global index by name");
        fieldsByName.put(field.getName(), field);

        managedProperties.add(property);
    }

    private void checkAllowedReverseField(Field field) throws SPARQLInvalidClassDefinitionException {
        Type fType = field.getGenericType();
        if (dataProperties.containsKey(field.getName()) && ((Class<?>) fType != URI.class)) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " of literal type are not allowed to be a reverse property");
        } else if (dataPropertiesLists.containsKey(field.getName())) {
            ParameterizedType genericReturnType = (ParameterizedType) fType;
            Class<?> genericParameter = (Class<?>) genericReturnType.getActualTypeArguments()[0];
            if (genericParameter != URI.class) {
                throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " List of literal type are not allowed to be a reverse property");
            }
        } else if (labelProperties.containsKey(field.getName())) {
            throw new SPARQLInvalidClassDefinitionException(objectClass, "Field " + field.getName() + " of SPARQLLabel type is not allowed to be a reverse property");
        }
    }

    private void addRelatedModelProperty(Field field, Class<? extends SPARQLResourceModel> model) {
        if (!relatedModelsFields.containsKey(model)) {
            relatedModelsFields.put(model, new HashSet<>());
        }

        relatedModelsFields.get(model).add(field.getName());
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

    private void analyzeSPARQLTypeLabelField(Field field) throws SPARQLInvalidClassDefinitionException {
        if (fieldTypeLabel == null) {
            LOGGER.debug("Field " + field.getName() + " defined as type label field for: " + objectClass.getName());
            fieldTypeLabel = field;
            LOGGER.debug("Store field " + field.getName() + " in global index by name");
            fieldsByName.put(field.getName(), field);
        } else {
            throw new SPARQLInvalidClassDefinitionException(
                    objectClass,
                    SPARQLTypeRDF.class.getCanonicalName() + " annotation must be unique "
                    + "and is defined multiple times for field " + fieldTypeLabel.getName()
                    + " and for field " + field.getName()
            );
        }
    }

    private void analyzeSPARQLTypeField(Field field) throws SPARQLInvalidClassDefinitionException {
        if (fieldType == null) {
            LOGGER.debug("Field " + field.getName() + " defined as type field for: " + objectClass.getName());
            fieldType = field;
            LOGGER.debug("Store field " + field.getName() + " in global index by name");
            fieldsByName.put(field.getName(), field);
        } else {
            throw new SPARQLInvalidClassDefinitionException(
                    objectClass,
                    SPARQLTypeRDF.class.getCanonicalName() + " annotation must be unique "
                    + "and is defined multiple times for field " + fieldType.getName()
                    + " and for field " + field.getName()
            );
        }

        managedProperties.add(RDF.type);
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

        if (candidateField != null && (getter.getReturnType().equals(candidateField.getType()) || getter.getReturnType().isAssignableFrom(candidateField.getType()))) {
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
                    && setter.getParameters()[0].getType().isAssignableFrom(field.getType())) {
                return field;
            }
        }

        return null;
    }

    public boolean isOptional(Field field) {
        return optionalFields.contains(field.getName());
    }

    public boolean isReverseRelation(Field field) {
        return reverseRelationFields.contains(field.getName());
    }

    public boolean useDefaultGraph(Field field) {
        return defaultGraphFields.contains(field.getName());
    }

    public void forEachDataProperty(BiConsumer<Field, Property> lambda) {
        dataProperties.forEach((fieldName, property) -> {
            lambda.accept(getFieldFromName(fieldName), property);
        });
    }

    public void forEachObjectProperty(BiConsumer<Field, Property> lambda) {
        objectProperties.forEach((fieldName, property) -> {
            lambda.accept(getFieldFromName(fieldName), property);
        });
    }

    public void forEachDataPropertyList(BiConsumer<Field, Property> lambda) {
        dataPropertiesLists.forEach((fieldName, property) -> {
            lambda.accept(getFieldFromName(fieldName), property);
        });
    }

    public void forEachObjectPropertyList(BiConsumer<Field, Property> lambda) {
        objectPropertiesLists.forEach((fieldName, property) -> {
            lambda.accept(getFieldFromName(fieldName), property);
        });
    }

    public void forEachLabelProperty(BiConsumer<Field, Property> lambda) {
        labelProperties.forEach((fieldName, property) -> {
            lambda.accept(getFieldFromName(fieldName), property);
        });
    }

    public boolean hasLabelProperty() {
        return labelProperties.size() > 0;
    }

    public Field getFieldFromGetter(Method method) {
        return fieldsByName.get(fieldsByGetter.get(method));
    }

    public Field getFieldFromSetter(Method method) {
        return fieldsByName.get(fieldsBySetter.get(method));
    }

    private Method getGetterFromField(Field field) {
        return fieldsByGetter.inverse().get(field.getName());
    }

    public Object getFieldValue(Field field, Object instance) {
        try {
            return instance.getClass().getMethod(fieldsByGetter.inverse().get(field.getName()).getName()).invoke(instance);
        } catch (Exception ex) {
            return null;
        }
    }

    public Method getSetterFromField(Field field) {
        return fieldsBySetter.inverse().get(field.getName());
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
        return dataProperties.containsKey(f.getName());
    }

    public boolean isObjectPropertyField(Field f) {
        return objectProperties.containsKey(f.getName());
    }

    public boolean isDataListField(Field f) {
        return dataPropertiesLists.containsKey(f.getName());
    }

    public boolean isObjectListField(Field f) {
        return objectPropertiesLists.containsKey(f.getName());
    }

    public boolean isLabelField(Field f) {
        return labelProperties.containsKey(f.getName());
    }

    public boolean isNullIgnorableUpdateField(Field f) {
        return getFieldAnnotation(f).ignoreUpdateIfNull();
    }

    public Resource getRDFType() {
        return resource;
    }

    public String getGraph() {
        return graph;
    }

    public Field getFieldFromName(String fieldName) {
        return fieldsByName.get(fieldName);
    }

    public Field getFieldFromUniqueProperty(Property property) {
        return getFieldFromName(fieldsByUniqueProperty.get(property));
    }

    public Set<Field> getObjectPropertyFields() {
        Set<Field> set = new HashSet<>();
        objectProperties.keySet().stream().forEach(fieldName -> {
            set.add(getFieldFromName(fieldName));
        });
        return set;
    }

    public Property getObjectPropertyByField(Field field) {
        return objectProperties.get(field.getName());
    }

    public Set<Field> getDataPropertyFields() {
        Set<Field> set = new HashSet<>();
        dataProperties.keySet().stream().forEach(fieldName -> {
            set.add(getFieldFromName(fieldName));
        });
        return set;
    }

    public Property getDataPropertyByField(Field field) {
        return dataProperties.get(field.getName());
    }

    public Set<Field> getObjectListPropertyFields() {
        Set<Field> set = new HashSet<>();
        objectPropertiesLists.keySet().stream().forEach(fieldName -> {
            set.add(getFieldFromName(fieldName));
        });
        return set;
    }

    public Property getObjectListPropertyByField(Field field) {
        return objectPropertiesLists.get(field.getName());
    }

    public Set<Field> getDataListPropertyFields() {
        Set<Field> set = new HashSet<>();
        dataPropertiesLists.keySet().stream().forEach(fieldName -> {
            set.add(getFieldFromName(fieldName));
        });
        return set;
    }

    public Property getDataListPropertyByField(Field field) {
        return dataPropertiesLists.get(field.getName());
    }

    public Set<Field> getLabelPropertyFields() {
        Set<Field> set = new HashSet<>();
        labelProperties.keySet().stream().forEach(fieldName -> {
            set.add(getFieldFromName(fieldName));
        });
        return set;
    }

    public Property getLabelPropertyByField(Field field) {
        return labelProperties.get(field.getName());
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

    public Set<Property> getManagedProperties() {
        return Collections.unmodifiableSet(managedProperties);
    }

    public String getResourceGraphPrefix() {
        return this.graphPrefix;
    }

    public SPARQLProperty getFieldAnnotation(Field field) {
        return annotationsByField.get(field.getName());
    }

    public String getTypeFieldName() {
        return fieldType.getName();
    }

    public String getTypeLabelFieldName() {
        return fieldTypeLabel.getName();
    }

    public Method getURIMethod() {
        return getGetterFromField(getURIField());
    }

    protected XSDDatatype getFieldDatatype(Field field) {
        try {
            return SPARQLDeserializers.getForClass(field.getType()).getDataType();
        } catch (Exception ex) {
            LOGGER.error("Error while getting datatype for field: " + field.getName(), ex);
            return null;
        }
    }

    protected Resource getFieldRDFType(Field field) {
        try {
            return mapperIndex.getForClass(field.getType()).getRDFType();
        } catch (Exception ex) {
            LOGGER.error("Error while getting rdf type for field: " + field.getName(), ex);
            return null;
        }
    }

    protected Resource getFieldListRDFType(Field field) {
        try {
            ParameterizedType genericReturnType = (ParameterizedType) field.getGenericType();
            Type genericParameter = genericReturnType.getActualTypeArguments()[0];
            return mapperIndex.getForClass((Class<?>) genericParameter).getRDFType();
        } catch (Exception ex) {
            LOGGER.error("Error while getting rdf type for list field: " + field.getName(), ex);
            return null;
        }
    }

    protected XSDDatatype getFieldListDatatype(Field field) {
        try {
            ParameterizedType genericReturnType = (ParameterizedType) field.getGenericType();
            Type genericParameter = genericReturnType.getActualTypeArguments()[0];
            return SPARQLDeserializers.getForClass((Class<?>) genericParameter).getDataType();
        } catch (Exception ex) {
            LOGGER.error("Error while getting datatype for list field: " + field.getName(), ex);
            return null;
        }
    }

    public Set<Field> getFieldsRelatedTo(Class<? extends SPARQLResourceModel> model) {
        Set<Field> relatedFields = new HashSet<>();
        if (relatedModelsFields.containsKey(model)) {
            relatedModelsFields.get(model).stream().forEach((fieldName) -> {
                relatedFields.add(getFieldFromName(fieldName));
            });
        }

        return relatedFields;
    }

    public Property getFieldProperty(Field field) {
        return propertiesByField.get(field.getName());
    }

    public Set<Class<? extends SPARQLResourceModel>> getRelatedResources() {
        return relatedModelsFields.keySet();
    }

    public Map<Field, Class<? extends SPARQLResourceModel>> getCascadeDeleteClassesField() {
        Map<Field, Class<? extends SPARQLResourceModel>> map = new HashMap<>();
        cascadeDeleteClassesField.keySet().stream().forEach((fieldName) -> {
            map.put(getFieldFromName(fieldName), cascadeDeleteClassesField.get(fieldName));
        });

        return map;
    }

    public List<Field> getAutoUpdateFields() {
        List<Field> list = new ArrayList<>();
        autoUpdateFields.forEach((fieldName) -> {
            list.add(getFieldFromName(fieldName));
        });

        return list;
    }

    public List<Field> getAutoUpdateListFields() {
        List<Field> list = new ArrayList<>();
        autoUpdateListFields.forEach((fieldName) -> {
            list.add(getFieldFromName(fieldName));
        });

        return list;
    }

    boolean allowBlankNode() {
        return allowBlankNode;
    }

}
