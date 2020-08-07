/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public final class OntologyDAO {

    private final SPARQLService sparql;

    public OntologyDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(OntologyDAO.class);

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchSubClasses(URI parent, Class<T> clazz, UserModel user, boolean excludeRoot, Consumer<T> handler) throws Exception {
        SPARQLTreeListModel<T> classTree = sparql.searchResourceTree(
                clazz,
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

        if (handler != null) {
            classTree.traverse(handler);
        }

        return classTree;
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

    public void buildProperties(ClassModel model, String lang) throws Exception {
        List<OwlRestrictionModel> restrictions = getOwlRestrictions(model.getUri(), lang);

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
                if (SPARQLDeserializers.existsForDatatype(restriction.getOnDataRange())) {
                    datatypePropertiesURI.put(propertyURI, restriction.getOnDataRange());
                }
            } else if (restriction.getOnClass() != null) {
                if (sparql.uriExists(ClassModel.class,
                        restriction.getOnClass())) {
                    objectPropertiesURI.put(propertyURI, restriction.getOnClass());
                }
            } else if (restriction.getSomeValuesFrom() != null) {
                URI someValueFrom = restriction.getSomeValuesFrom();
                if (SPARQLDeserializers.existsForDatatype(someValueFrom)) {
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
            pModel.setTypeRestriction(datatypePropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setDatatypeProperties(dataPropertiesMap);

        Map<URI, ObjectPropertyModel> objectPropertiesMap = new HashMap<>();
        List<ObjectPropertyModel> objectPropertiesList = sparql.getListByURIs(ObjectPropertyModel.class, objectPropertiesURI.keySet(), lang);

        MapUtils.populateMap(objectPropertiesMap, objectPropertiesList, (pModel) -> {
            pModel.setTypeRestriction(objectPropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setObjectProperties(objectPropertiesMap);
    }

    public ClassModel getClassModel(URI rdfClass, String lang) throws Exception {
        ClassModel model = sparql.getByURI(ClassModel.class,
                rdfClass, lang);

        if (model == null) {
            throw new NotFoundURIException(rdfClass);
        }

        buildProperties(model, lang);

        return model;
    }

    public CSVValidationModel validateCSV(URI graph, URI rdfType, File file, UserModel currentUser, Map<Property, BiConsumer<CSVCell, CSVValidationModel>> customValidators, URIGenerator<String> uriGenerator) throws Exception {
        Map<URI, OwlRestrictionModel> restrictionsByID = new HashMap<>();

        ClassModel model = getClassModel(rdfType, currentUser.getLanguage());

        model.getRestrictions().values().forEach(restriction -> {
            URI propertyURI = restriction.getOnProperty();
            restrictionsByID.put(propertyURI, restriction);
        });

        int uriIndex = 0;
        Map<Integer, OwlRestrictionModel> restrictionsByIndex = new HashMap<>();
        Map<Integer, String> headerByIndex = new HashMap<>();

        CSVValidationModel csvValidation = new CSVValidationModel();

        try (CSVReader csvReader = new CSVReader(new FileReader(file));) {
            String[] ids = csvReader.readNext();

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

                        } else if ("uri".equals(ids[i])) {
                            uriIndex = i;
                        }
                    } catch (URISyntaxException ex) {
                        csvValidation.addInvalidHeaderURI(i, ids[i]);
                    }
                }

                if (!restrictionsByID.isEmpty()) {
                    csvValidation.addMissingHeaders(restrictionsByID.keySet());
                }

                if (csvValidation.hasErrors()) {
                    return csvValidation;
                }

                Map<URI, Map<URI, Boolean>> checkedClassObjectURIs = new HashMap<>();
                Map<URI, Integer> checkedURIs = new HashMap<>();
                int rowIndex = 1;
                String[] values = null;

                while ((values = csvReader.readNext()) != null) {
                    validateCSVRow(graph, model, values, rowIndex, csvValidation, uriIndex, restrictionsByIndex, headerByIndex, checkedClassObjectURIs, checkedURIs, customValidators, uriGenerator);
                    rowIndex++;
                }
            }
        }

        return csvValidation;
    }

    private void validateCSVRow(
            URI graph,
            ClassModel model,
            String[] values,
            int rowIndex,
            CSVValidationModel csvValidation,
            int uriIndex,
            Map<Integer, OwlRestrictionModel> restrictionsByIndex,
            Map<Integer, String> headerByIndex,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            Map<URI, Integer> checkedURIs,
            Map<Property, BiConsumer<CSVCell, CSVValidationModel>> customValidators,
            URIGenerator<String> uriGenerator
    ) throws Exception {
        SPARQLResourceModel object = new SPARQLResourceModel();
        String name = null;
        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (restrictionsByIndex.containsKey(colIndex)) {
                String value = values[colIndex].trim();
                OwlRestrictionModel restriction = restrictionsByIndex.get(colIndex);
                URI propertyURI = restriction.getOnProperty();

                BiConsumer<CSVCell, CSVValidationModel> customValidator = null;
                if (customValidators != null) {
                    customValidator = customValidators.get(propertyURI);
                }

                if (SPARQLDeserializers.compareURIs(RDFS.label.getURI(), propertyURI.toString())) {
                    name = value;
                }
                String header = headerByIndex.get(colIndex);
                validateCSVValue(graph, model, propertyURI, value, rowIndex, colIndex, restriction, header, csvValidation, checkedClassObjectURIs, customValidator, object);
            } else if (colIndex == uriIndex) {
                String value = values[colIndex].trim();
                CSVCell cell = new CSVCell(rowIndex, colIndex, value, "uri");
                if (value != null && !value.isEmpty()) {
                    if (URIDeserializer.validateURI(value)) {
                        URI objectURI = new URI(value);
                        if (checkedURIs.containsKey(objectURI)) {
                            csvValidation.addDuplicateURIError(cell, checkedURIs.get(objectURI));
                        } else if (!sparql.uriExists(graph, objectURI)) {
                            object.setUri(objectURI);
                        } else {
                            csvValidation.addAlreadyExistingURIError(cell);
                        }
                        checkedURIs.put(objectURI, rowIndex);
                    } else {
                        csvValidation.addInvalidURIError(cell);
                    }
                }
            }
        }

        if (!csvValidation.hasErrors()) {
            if (object.getUri() == null) {
                int retry = 0;
                URI objectURI = uriGenerator.generateURI(graph.toString(), name, retry);
                while (checkedURIs.containsKey(objectURI) || sparql.uriExists(graph, objectURI)) {
                    retry++;
                    objectURI = uriGenerator.generateURI(graph.toString(), name, retry);
                }
                checkedURIs.put(objectURI, rowIndex);
                object.setUri(objectURI);
            }

            object.setType(model.getUri());
            csvValidation.addObject(object);
        }
    }

    private void validateCSVValue(
            URI graph,
            ClassModel model,
            URI propertyURI,
            String value,
            int rowIndex,
            int colIndex,
            OwlRestrictionModel restriction,
            String header,
            CSVValidationModel csvValidation,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            BiConsumer<CSVCell, CSVValidationModel> customValidator,
            SPARQLResourceModel object
    ) throws SPARQLException {
        if (!restriction.isList()) {
            CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
            validateCSVSingleValue(graph, model, propertyURI, cell, restriction, csvValidation, checkedClassObjectURIs, customValidator, object);
        } else {
            String[] multipleValues = value.split("|");
            for (String singleValue : multipleValues) {
                CSVCell cell = new CSVCell(rowIndex, colIndex, singleValue, header);
                validateCSVSingleValue(graph, model, propertyURI, cell, restriction, csvValidation, checkedClassObjectURIs, customValidator, object);
            }
        }

    }

    private void validateCSVSingleValue(
            URI graph,
            ClassModel model,
            URI propertyURI,
            CSVCell cell,
            OwlRestrictionModel restriction,
            CSVValidationModel csvValidation,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            BiConsumer<CSVCell, CSVValidationModel> customValidator,
            SPARQLResourceModel object
    ) throws SPARQLException {
        String value = cell.getValue();

        if (restriction.isRequired() && (value == null || value.isEmpty())) {
            csvValidation.addMissingRequiredValue(cell);
        } else if (model.isDatatypePropertyRestriction(propertyURI)) {
            try {
                SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getSubjectURI());
                if (!deserializer.validate(value)) {
                    csvValidation.addInvalidDatatypeError(cell, restriction.getSubjectURI());
                } else if (customValidator != null) {
                    customValidator.accept(cell, csvValidation);
                }

                if (!csvValidation.hasErrors()) {
                    object.addRelation(graph, propertyURI, deserializer.getClassType(), value);
                }
            } catch (SPARQLDeserializerNotFoundException ex) {
                csvValidation.addInvalidDatatypeError(cell, restriction.getSubjectURI());
            }
        } else if (model.isObjectPropertyRestriction(propertyURI)) {
            try {
                if (!value.isEmpty()) {
                    if (URIDeserializer.validateURI(value)) {
                        URI objectURI = new URI(value);
                        URI classURI = restriction.getSubjectURI();
                        boolean doesClassObjectUriExist;
                        if (checkedClassObjectURIs.containsKey(classURI) && checkedClassObjectURIs.get(classURI).containsKey(objectURI)) {
                            doesClassObjectUriExist = checkedClassObjectURIs.get(classURI).get(objectURI);
                        } else {
                            doesClassObjectUriExist = sparql.uriExists(classURI, objectURI);
                            if (!checkedClassObjectURIs.containsKey(classURI)) {
                                checkedClassObjectURIs.put(classURI, new HashMap<>());
                            }
                            checkedClassObjectURIs.get(classURI).put(objectURI, doesClassObjectUriExist);
                        }

                        if (!doesClassObjectUriExist) {
                            csvValidation.addURINotFoundError(cell, classURI, objectURI);
                        } else if (customValidator != null) {
                            customValidator.accept(cell, csvValidation);
                        }
                    } else {
                        csvValidation.addInvalidURIError(cell);
                    }

                    if (!csvValidation.hasErrors()) {
                        object.addRelation(graph, propertyURI, URI.class, value);
                    }
                }
            } catch (URISyntaxException ex) {
                csvValidation.addInvalidURIError(cell);
            }
        }
    }

    public boolean validateObjectValue(
            URI graph,
            ClassModel model,
            URI propertyURI,
            String value,
            SPARQLResourceModel object
    ) {

        OwlRestrictionModel restriction = model.getRestrictions().get(propertyURI);

        if (restriction != null) {
            if (restriction.isRequired() && (value == null || value.isEmpty())) {
                return false;
            } else if (model.isDatatypePropertyRestriction(propertyURI)) {
                try {
                    SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getSubjectURI());
                    if (deserializer.validate(value)) {
                        object.addRelation(graph, propertyURI, deserializer.getClassType(), value);
                        return true;
                    }
                } catch (SPARQLDeserializerNotFoundException ex) {
                    LOGGER.warn("Error while searching deserializer that should never happend for type: " + restriction.getSubjectURI(), ex);
                }
            } else if (model.isObjectPropertyRestriction(propertyURI)) {
                if (!value.isEmpty()) {
                    if (URIDeserializer.validateURI(value)) {
                        try {
                            URI objectURI = new URI(value);
                            URI classURI = restriction.getSubjectURI();
                            if (sparql.uriExists(classURI, objectURI)) {
                                object.addRelation(graph, propertyURI, URI.class, value);
                                return true;
                            }
                        } catch (Exception ex) {
                            LOGGER.warn("Error while creating or validating URI that should never happend with value: " + value, ex);
                        }
                    }
                }
            }
        }

        return false;
    }

    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, UserModel user) throws Exception {
        URI dataPropertiesParent = new URI(OWL2.topDataProperty.getURI());
        return sparql.searchResourceTree(
                DatatypePropertyModel.class,
                user.getLanguage(),
                dataPropertiesParent,
                true,
                (select) -> {
                    if (domain != null) {
                        select.addFilter(SPARQLQueryHelper.eq(DatatypePropertyModel.DOMAIN_FIELD, domain));
                    }
                }
        );
    }

    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, UserModel user) throws Exception {
        URI objectPropertiesParent = new URI(OWL2.topObjectProperty.getURI());
        return sparql.searchResourceTree(
                ObjectPropertyModel.class,
                user.getLanguage(),
                objectPropertiesParent,
                true,
                (select) -> {
                    if (domain != null) {
                        select.addFilter(SPARQLQueryHelper.eq(DatatypePropertyModel.DOMAIN_FIELD, domain));
                    }
                }
        );
    }

    public void createDataProperty(Node graph, DatatypePropertyModel dataProperty) throws Exception {
        sparql.create(graph, dataProperty);
    }

    public void createObjectProperty(Node graph, ObjectPropertyModel objectProperty) throws Exception {
        sparql.create(graph, objectProperty);
    }

    public DatatypePropertyModel getDataProperty(URI propertyURI, UserModel user) throws Exception {
        return sparql.getByURI(DatatypePropertyModel.class, propertyURI, user.getLanguage());
    }

    public ObjectPropertyModel getObjectProperty(URI propertyURI, UserModel user) throws Exception {
        return sparql.getByURI(ObjectPropertyModel.class, propertyURI, user.getLanguage());
    }

    public void updateDataProperty(Node graph, DatatypePropertyModel dataProperty) throws Exception {
        sparql.update(graph, dataProperty);
    }

    public void updateObjectProperty(Node graph, ObjectPropertyModel objectProperty) throws Exception {
        sparql.update(graph, objectProperty);
    }

}
