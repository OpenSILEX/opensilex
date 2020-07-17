/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author vince
 */
public final class OntologyDAO {

    private final SPARQLService sparql;

    public OntologyDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final static Map<String, Map<URI, Map<URI, String>>> labelTranslationMap = new HashMap<>();
    private final static Map<URI, Map<URI, Integer>> classPropertiesOrderMap = new HashMap<>();
    private final static List<URI> abstractClasses = new ArrayList<>();

    public static void customizeOntology(ObjectNode ontologyDefinitionOverride) throws URISyntaxException {
        labelTranslationMap.clear();
        classPropertiesOrderMap.clear();
        abstractClasses.clear();
        Iterator<Map.Entry<String, JsonNode>> classesOverride = ontologyDefinitionOverride.fields();
        while (classesOverride.hasNext()) {
            Map.Entry<String, JsonNode> classOverride = classesOverride.next();
            URI classURI = new URI(classOverride.getKey());
            JsonNode classOverrideNode = classOverride.getValue();
            if (classOverrideNode.hasNonNull("properties")) {
                Iterator<Map.Entry<String, JsonNode>> propertiesOverride = classOverrideNode.get("properties").fields();
                while (propertiesOverride.hasNext()) {
                    Map.Entry<String, JsonNode> propertyOverride = propertiesOverride.next();
                    URI propertyURI = new URI(propertyOverride.getKey());
                    JsonNode propertyValues = propertyOverride.getValue();
                    if (propertyValues.hasNonNull("label")) {
                        Iterator<Map.Entry<String, JsonNode>> labelTs = propertyValues.get("label").fields();
                        while (labelTs.hasNext()) {
                            Map.Entry<String, JsonNode> label = labelTs.next();
                            String lang = label.getKey();
                            String ts = label.getValue().asText();
                            addOntologyTranslationLabel(classURI, propertyURI, lang, ts);
                        }
                    }
                    if (propertyValues.hasNonNull("order")) {
                        int propertyOrder = propertyValues.get("order").asInt();
                        addOntologyPropertiesOrder(classURI, propertyURI, propertyOrder);
                    }
                }
            }

            if (classOverrideNode.hasNonNull("abstract")) {
                if (classOverrideNode.get("abstract").asBoolean(false)) {
                    abstractClasses.add(classURI);
                }
            }
        }
    }

    private static String getOntologyTranslationLabel(URI classURI, URI propertyURI, String lang) {
        if (labelTranslationMap.containsKey(lang)) {
            Map<URI, Map<URI, String>> langMap = labelTranslationMap.get(lang);
            if (langMap.containsKey(classURI)) {
                Map<URI, String> classMap = langMap.get(classURI);
                if (classMap.containsKey(propertyURI)) {
                    return classMap.get(propertyURI);
                }
            }
        }

        return null;
    }

    private static void addOntologyTranslationLabel(URI classURI, URI propertyURI, String lang, String value) {
        if (!labelTranslationMap.containsKey(lang)) {
            labelTranslationMap.put(lang, new HashMap<>());
        }

        Map<URI, Map<URI, String>> langMap = labelTranslationMap.get(lang);

        if (!langMap.containsKey(classURI)) {
            langMap.put(classURI, new HashMap<>());
        }

        Map<URI, String> classMap = langMap.get(classURI);
        classMap.put(propertyURI, value);
    }

    private static Map<URI, Integer> getOntologyPropertiesOrder(URI classURI) {
        if (classPropertiesOrderMap.containsKey(classURI)) {
            return classPropertiesOrderMap.get(classURI);
        } else {
            return new HashMap<>();
        }
    }

    private static void addOntologyPropertiesOrder(URI classURI, URI propertyURI, int value) {
        if (!classPropertiesOrderMap.containsKey(classURI)) {
            classPropertiesOrderMap.put(classURI, new HashMap<>());
        }

        classPropertiesOrderMap.get(classURI).put(propertyURI, value);
    }

    private static boolean isAbstractClass(URI classURI) {
        return abstractClasses.contains(classURI);
    }

    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, UserModel user, boolean excludeRoot) throws Exception {
        SPARQLTreeListModel<ClassModel> classTree = sparql.searchResourceTree(
                ClassModel.class,
                user.getLanguage(),
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    if (parent != null) {
                        Var parentVar = makeVar(ClassModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(parent));
                        select.addWhere(makeVar(ClassModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                }
        );

        classTree.traverse((ClassModel model) -> {
            model.setAbstractClass(isAbstractClass(model.getUri()));
        });

        return classTree;
    }

    public SPARQLTreeListModel<PropertyModel> searchSubProperties(URI parent, UserModel user, boolean excludeRoot) throws Exception {
        return sparql.searchResourceTree(
                PropertyModel.class,
                user.getLanguage(),
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    Node parentNode = SPARQLDeserializers.nodeURI(parent);
                    if (parentNode
                    != null) {
                        Var parentVar = makeVar(DatatypePropertyModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, parentNode);
                        select.addWhere(makeVar(DatatypePropertyModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                }
        );
    }

    public List<OwlRestrictionModel> getOwlRestrictions(URI rdfClass, String lang) throws Exception {
        List<OrderBy> orderByList = new ArrayList<>();
        orderByList.add(new OrderBy(OwlRestrictionModel.URI_FIELD + "=ASC"));
        return sparql.search(OwlRestrictionModel.class,
                lang, (SelectBuilder select) -> {
                    Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
                    Var classUriVar = makeVar("classURI");

                    select.addWhere(classUriVar, RDFS.subClassOf, uriVar);

                    select.addWhere(SPARQLDeserializers.nodeURI(rdfClass), Ontology.subClassAny, classUriVar);
                }, orderByList);

    }

    public ClassModel getClassModel(URI rdfClass, String lang) throws Exception {
        ClassModel model = sparql.getByURI(ClassModel.class,
                rdfClass, lang);

        if (model == null) {
            throw new NotFoundURIException(rdfClass);
        }

        model.setAbstractClass(isAbstractClass(rdfClass));

        List<OwlRestrictionModel> restrictions = getOwlRestrictions(rdfClass, lang);

        Map<URI, URI> datatypePropertiesURI = new HashMap<>();
        Map<URI, URI> objectPropertiesURI = new HashMap<>();
        Map<URI, OwlRestrictionModel> mergedRestrictions = new HashMap<>();
        for (OwlRestrictionModel restriction : restrictions) {
            URI propertyURI = restriction.getOnProperty();

            if (mergedRestrictions.containsKey(propertyURI)) {
                OwlRestrictionModel mergedRestriction = mergedRestrictions.get(propertyURI);
                if (restriction.getCardinality() != null) {
                    mergedRestriction.setCardinality(restriction.getCardinality());
                }
                if (restriction.getMinCardinality() != null) {
                    mergedRestriction.setMinCardinality(restriction.getMinCardinality());
                }
                if (restriction.getMaxCardinality() != null) {
                    mergedRestriction.setMaxCardinality(restriction.getMaxCardinality());
                }
            } else {
                mergedRestrictions.put(propertyURI, restriction);
            }
            if (restriction.getOnDataRange() != null) {
                if (BuiltInDatatypes.isBuiltInDatatype(restriction.getOnDataRange())) {
                    datatypePropertiesURI.put(propertyURI, restriction.getOnDataRange());
                }
            } else if (restriction.getOnClass() != null) {
                if (sparql.uriExists(ClassModel.class,
                        restriction.getOnClass())) {
                    objectPropertiesURI.put(propertyURI, restriction.getOnClass());
                }
            } else if (restriction.getSomeValuesFrom() != null) {
                URI someValueFrom = restriction.getSomeValuesFrom();
                if (BuiltInDatatypes.isBuiltInDatatype(someValueFrom)) {
                    datatypePropertiesURI.put(propertyURI, someValueFrom);
                } else if (sparql.uriExists(ClassModel.class,
                        someValueFrom)) {
                    objectPropertiesURI.put(propertyURI, someValueFrom);
                }
            }
        }

        model.setRestrictions(mergedRestrictions);

        Map<URI, DatatypePropertyModel> dataPropertiesMap = new HashMap<>();
        List<DatatypePropertyModel> dataPropertiesList = sparql.getListByURIs(DatatypePropertyModel.class,
                datatypePropertiesURI.keySet(), lang);
        MapUtils.populateMap(dataPropertiesMap, dataPropertiesList, (pModel) -> {
            pModel.setName(getRelationLabel(model, pModel, lang));
            pModel.setDatatypeRestriction(datatypePropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setDatatypeProperties(dataPropertiesMap);

        Map<URI, ObjectPropertyModel> objectPropertiesMap = new HashMap<>();
        List<ObjectPropertyModel> objectPropertiesList = sparql.getListByURIs(ObjectPropertyModel.class,
                objectPropertiesURI.keySet(), lang);
        MapUtils.populateMap(objectPropertiesMap, objectPropertiesList, (pModel) -> {
            pModel.setName(getRelationLabel(model, pModel, lang));
            pModel.setObjectClassRestriction(objectPropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setObjectProperties(objectPropertiesMap);

        model.setPropertiesOrder(getPropertiesOrder(model));

        return model;
    }

    private String getRelationLabel(ClassModel model, PropertyModel property, String lang) {
        String ts = OntologyDAO.getOntologyTranslationLabel(model.getUri(), property.getUri(), lang);

        if (ts == null) {
            if (model.getParent() != null) {
                ts = getRelationLabel(model.getParent(), property, lang);
                OntologyDAO.addOntologyTranslationLabel(model.getType(), property.getUri(), lang, ts);
            } else {
                ts = property.getName();
                if (ts == null) {
                    ts = property.getUri().toString();
                }
            }
        }

        return ts;
    }

    private Map<URI, Integer> getPropertiesOrder(ClassModel model) {
        Map<URI, Integer> baseMap = new HashMap<>();

        if (model.getParent() != null) {
            baseMap = getPropertiesOrder(model.getParent());
        }

        baseMap.putAll(OntologyDAO.getOntologyPropertiesOrder(model.getUri()));

        return baseMap;
    }

    public CSVValidationModel validateCSV(URI rdfType, File file, UserModel currentUser, Map<Property, BiConsumer<CSVCell, CSVValidationModel>> customValidators) throws Exception {
        Map<URI, OwlRestrictionModel> restrictionsByID = new HashMap<>();

        ClassModel model = getClassModel(rdfType, currentUser.getLanguage());

        model.getOrderedRestrictions().forEach(restriction -> {
            URI propertyURI = restriction.getOnProperty();
            restrictionsByID.put(propertyURI, restriction);
        });

        Map<Integer, OwlRestrictionModel> restrictionsByIndex = new HashMap<>();
        Map<Integer, String> headerByIndex = new HashMap<>();

        CSVValidationModel csvErrors = new CSVValidationModel();

        try (CSVReader csvReader = new CSVReader(new FileReader(file));) {
            String[] ids = csvReader.readNext();

            csvReader.readNext();
            csvReader.readNext();

            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    try {
                        URI id = new URI(ids[i]);

                        if (restrictionsByID.containsKey(id)) {
                            restrictionsByIndex.put(i, restrictionsByID.get(id));

                            String header = id.toString();
                            if (model.isDatatypePropertyRestriction(id)) {
                                header = model.getDatatypeProperty(id).getName();
                            } else if (model.isObjectPropertyRestriction(id)) {
                                header = model.getObjectProperty(id).getName();
                            }
                            headerByIndex.put(i, header);

                            restrictionsByID.remove(id);
                        }
                    } catch (URISyntaxException ex) {
                        csvErrors.addInvalidHeaderURI(i, ids[i]);
                    }
                }

                if (!restrictionsByID.isEmpty()) {
                    csvErrors.addMissingHeaders(restrictionsByID.keySet());
                }

                if (csvErrors.hasErrors()) {
                    return csvErrors;
                }

                Map<URI, Map<URI, Boolean>> checkedClassObjectURIs = new HashMap<>();

                int rowIndex = 1;
                String[] values = null;
                while ((values = csvReader.readNext()) != null) {
                    validateCSVRow(model, values, rowIndex, csvErrors, restrictionsByIndex, headerByIndex, checkedClassObjectURIs, customValidators);
                    rowIndex++;
                }
            }
        }

        return csvErrors;
    }

    private void validateCSVRow(
            ClassModel model,
            String[] values,
            int rowIndex,
            CSVValidationModel csvErrors,
            Map<Integer, OwlRestrictionModel> restrictionsByIndex,
            Map<Integer, String> headerByIndex,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            Map<Property, BiConsumer<CSVCell, CSVValidationModel>> customValidators
    ) throws Exception {
        SPARQLResourceModel object = new SPARQLResourceModel();
        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (restrictionsByIndex.containsKey(colIndex)) {
                String value = values[colIndex].trim();
                OwlRestrictionModel restriction = restrictionsByIndex.get(colIndex);
                URI propertyURI = restriction.getOnProperty();

                BiConsumer<CSVCell, CSVValidationModel> customValidator = null;
                if (customValidators != null) {
                    customValidator = customValidators.get(propertyURI);
                }

                String header = headerByIndex.get(colIndex);
                validateCSVValue(model, propertyURI, value, rowIndex, colIndex, restriction, header, csvErrors, checkedClassObjectURIs, customValidator, object);
            }
        }
    }

    private void validateCSVValue(
            ClassModel model,
            URI propertyURI,
            String value,
            int rowIndex,
            int colIndex,
            OwlRestrictionModel restriction,
            String header,
            CSVValidationModel csvErrors,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            BiConsumer<CSVCell, CSVValidationModel> customValidator,
             SPARQLResourceModel object
    ) throws SPARQLException {
        if (!restriction.isList()) {
            CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
            validateCSVSingleValue(model, propertyURI, cell, restriction, csvErrors, checkedClassObjectURIs, customValidator, object);
        } else {
            String[] multipleValues = value.split("|");
            for (String singleValue : multipleValues) {
                CSVCell cell = new CSVCell(rowIndex, colIndex, singleValue, header);
                validateCSVSingleValue(model, propertyURI, cell, restriction, csvErrors, checkedClassObjectURIs, customValidator, object);
            }
        }

    }

    private void validateCSVSingleValue(
            ClassModel model,
            URI propertyURI,
            CSVCell cell,
            OwlRestrictionModel restriction,
            CSVValidationModel csvErrors,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            BiConsumer<CSVCell, CSVValidationModel> customValidator,
            SPARQLResourceModel object
    ) throws SPARQLException {
        String value = cell.getValue();

        if (restriction.isRequired() && (value == null || value.isEmpty())) {
            csvErrors.addMissingRequiredValue(cell);
        } else if (model.isDatatypePropertyRestriction(propertyURI)) {
            BuiltInDatatypes dataType = BuiltInDatatypes.getBuiltInDatatype(restriction.getSubjectURI());
            if (!dataType.validate(value)) {
                csvErrors.addInvalidDatatypeError(cell, dataType);
            } else if (customValidator != null) {
                customValidator.accept(cell, csvErrors);
            }

            if (!csvErrors.hasErrors()) {
                object.addRelation(propertyURI, dataType.getTypeClass(), value);
            }
        } else if (model.isObjectPropertyRestriction(propertyURI)) {
            try {
                URI objectURI = new URI(value);
                if (objectURI.isAbsolute()) {
                    URI classURI = restriction.getSubjectURI();
                    boolean doesClassObjectUriExist;
                    if (checkedClassObjectURIs.containsKey(classURI) && checkedClassObjectURIs.get(classURI).containsKey(objectURI)) {
                        doesClassObjectUriExist = checkedClassObjectURIs.get(classURI).get(objectURI);
                    } else {
                        doesClassObjectUriExist = sparql.uriExists(model.getUri(), objectURI);
                        if (!checkedClassObjectURIs.containsKey(classURI)) {
                            checkedClassObjectURIs.put(classURI, new HashMap<>());
                        }
                        checkedClassObjectURIs.get(classURI).put(objectURI, doesClassObjectUriExist);
                    }

                    if (!doesClassObjectUriExist) {
                        csvErrors.addURINotFoundError(cell, classURI, objectURI);
                    } else if (customValidator != null) {
                        customValidator.accept(cell, csvErrors);
                    }
                } else {
                    csvErrors.addInvalidURIError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        }
    }

}
