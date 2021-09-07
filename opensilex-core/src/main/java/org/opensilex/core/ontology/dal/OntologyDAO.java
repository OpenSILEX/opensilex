/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import com.opencsv.CSVWriter;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.cache.OntologyCache;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLMultipleObjectException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vince
 */
public final class OntologyDAO {

    private final SPARQLService sparql;

    public OntologyDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(OntologyDAO.class);

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchSubClasses(URI parent, Class<T> clazz, String stringPattern, UserModel user, boolean excludeRoot, Consumer<T> handler) throws Exception {
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
                    if (!StringUtils.isEmpty(stringPattern)) {
                        Var parentNameField = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(ClassModel.PARENT_FIELD));

                        select.addFilter(SPARQLQueryHelper.or(
                                SPARQLQueryHelper.regexFilter(ClassModel.NAME_FIELD, stringPattern),
                                SPARQLQueryHelper.regexFilter(parentNameField.getVarName(), stringPattern)
                        ));
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

    public ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws Exception {

        WhereHandler parentHandler = null;
        if (parentClass != null) {
            // Add a WHERE with a subClassOf* path on PARENT field, instead to add it on the end of query
            parentHandler = new WhereHandler();
            parentHandler.addWhere(new TriplePath(makeVar(ClassModel.PARENT_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(parentClass)));
        }


        ClassModel model = sparql.loadByURI(
                sparql.getDefaultGraph(ClassModel.class),
                ClassModel.class,
                rdfClass,
                lang,
                null,
                parentClass != null ? Collections.singletonMap(ClassModel.PARENT_FIELD, parentHandler) : null
        );

        if (model == null) {
            throw new NotFoundURIException(rdfClass);
        }

        buildProperties(model, lang);

        return model;
    }


    /**
     * @param modelUri    URI of a model with a {@link RDF#type type}
     * @param parentClass the root class URI
     * @return the {@link ClassModel} of the given model URI {@link RDF#type type}
     */
    public ClassModel getClassModelOf(URI modelUri, URI parentClass, String lang) throws Exception {

        List<ClassModel> results = sparql.search(
                ClassModel.class,
                lang,
                (select) -> {
                    select.addWhere(SPARQLDeserializers.nodeURI(modelUri), RDF.type, makeVar(ClassModel.URI_FIELD));
                }
        );

        if (results.isEmpty()) {
            throw new NotFoundURIException("No ClassModel associated to the given URI found : ", modelUri);
        } else if (results.size() > 1) {
            throw new SPARQLMultipleObjectException(modelUri, "Multiple ClassModel associated to the given URI, only one was expected");
        }

        ClassModel model = results.get(0);
        buildProperties(model, lang);

        return model;
    }

    public CSVValidationModel validateCSV(URI graph, URI parentClass, InputStream file, int firstRow, UserModel currentUser, Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators, List<String> customColumns, URIGenerator<String> uriGenerator) throws Exception {

        Map<String, Map<String, OwlRestrictionModel>> typeRestrictions = new HashMap<>();
        Map<String, ClassModel> typeModels = new HashMap<>();
        Map<Integer, String> headerByIndex = new HashMap<>();

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import Ontology objects - CSV format => \n '" + csvReader.getDetectedFormat() + "'");

            String[] ids = csvReader.parseNext();

            int uriIndex = -1;
            int typeIndex = -1;
            int nameIndex = -1;
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];

                if (id.equalsIgnoreCase(CSV_URI_KEY)) {
                    uriIndex = i;
                } else if (id.equalsIgnoreCase(CSV_TYPE_KEY)) {
                    typeIndex = i;
                } else if (id.equalsIgnoreCase(CSV_NAME_KEY)) {
                    nameIndex = i;
                }
                headerByIndex.put(i, SPARQLDeserializers.getExpandedURI(id));
            }

            if (firstRow > 2) {
                int skipLines = 0;
                while (skipLines < (firstRow - 2)) {
                    csvReader.parseNext();
                    skipLines++;
                }
            }

            int rowIndex = 1;
            String[] values = null;

            CSVValidationModel csvValidation = new CSVValidationModel();

            List<String> missingHeaders = new ArrayList<>();
            if (uriIndex == -1) {
                missingHeaders.add(CSV_URI_KEY);
            }
            if (typeIndex == -1) {
                missingHeaders.add(CSV_TYPE_KEY);
            }
            if (nameIndex == -1) {
                missingHeaders.add(CSV_NAME_KEY);
            }

            if (missingHeaders.size() > 0) {
                csvValidation.addMissingHeaders(missingHeaders);
            } else {

                Map<URI, Map<URI, Boolean>> checkedClassObjectURIs = new HashMap<>();
                Map<URI, Integer> checkedURIs = new HashMap<>();

                while ((values = csvReader.parseNext()) != null) {
                    try {
                        URI rdfType = new URI(SPARQLDeserializers.getExpandedURI(values[typeIndex].trim()));
                        if (!typeRestrictions.containsKey(rdfType.toString())) {
                            ClassModel model = getClassModel(rdfType, parentClass, currentUser.getLanguage());

                            Map<String, OwlRestrictionModel> restrictionsByID = new HashMap<>();
                            model.getRestrictions().values().forEach(restriction -> {
                                String propertyURI = SPARQLDeserializers.getExpandedURI(restriction.getOnProperty());
                                restrictionsByID.put(propertyURI, restriction);
                            });

                            typeRestrictions.put(SPARQLDeserializers.getExpandedURI(rdfType.toString()), restrictionsByID);
                            typeModels.put(rdfType.toString(), model);
                        }

                        Map<String, OwlRestrictionModel> restrictionsByID = typeRestrictions.get(rdfType.toString());

                        validateCSVRow(graph, typeModels.get(rdfType.toString()), values, rowIndex, csvValidation, uriIndex, typeIndex, nameIndex, restrictionsByID, headerByIndex, checkedClassObjectURIs, checkedURIs, customValidators, uriGenerator);

                    } catch (Exception ex) {
                        CSVCell cell = new CSVCell(rowIndex, 0, "Unhandled error while parsing row: " + ex.getMessage(), "all");
                        csvValidation.addInvalidValueError(cell);
                    }

                    rowIndex++;
                }

            }

            return csvValidation;
        }
    }

    private void validateCSVRow(
            URI graph,
            ClassModel model,
            String[] values,
            int rowIndex,
            CSVValidationModel csvValidation,
            int uriIndex,
            int typeIndex,
            int nameIndex,
            Map<String, OwlRestrictionModel> restrictionsByID,
            Map<Integer, String> headerByIndex,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            Map<URI, Integer> checkedURIs,
            Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators,
            URIGenerator<String> uriGenerator
    ) throws Exception {
        SPARQLNamedResourceModel object = new SPARQLNamedResourceModel();
        String name = null;

        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (colIndex == typeIndex) {
                continue;
            } else if (colIndex == nameIndex) {
                name = values[colIndex];
                if (StringUtils.isEmpty(name)) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, name, CSV_NAME_KEY);
                    csvValidation.addMissingRequiredValue(cell);
                } else {
                    object.setName(name);
                }
            } else if (colIndex == uriIndex) {
                String value = values[colIndex];
                CSVCell cell = new CSVCell(rowIndex, colIndex, value, CSV_URI_KEY);
                if (!StringUtils.isEmpty(value)) {
                    if (URIDeserializer.validateURI(value)) {
                        URI objectURI = new URI(value);
                        if (checkedURIs.containsKey(objectURI)) {
                            csvValidation.addDuplicateURIError(cell, checkedURIs.get(objectURI));
                        } else if (!sparql.uriExists(SPARQLDeserializers.nodeURI(graph), objectURI)) {
                            object.setUri(objectURI);
                        } else {
                            csvValidation.addAlreadyExistingURIError(cell);
                        }
                        checkedURIs.put(objectURI, rowIndex);
                    } else {
                        csvValidation.addInvalidURIError(cell);
                    }
                }
            } else if (headerByIndex.containsKey(colIndex)) {
                String header = headerByIndex.get(colIndex);
                String value = values[colIndex];
                if (restrictionsByID.containsKey(header)) {
                    OwlRestrictionModel restriction = restrictionsByID.get(header);
                    URI propertyURI = restriction.getOnProperty();
                    BiConsumer<CSVCell, CSVValidationModel> customValidator = null;
                    if (customValidators != null) {
                        customValidator = customValidators.get(SPARQLDeserializers.getExpandedURI(propertyURI));
                    }

                    CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
                    validateCSVSingleValue(graph, model, propertyURI, cell, restriction, csvValidation, checkedClassObjectURIs, customValidator, object);

                } else {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
                    if (customValidators != null) {
                        BiConsumer<CSVCell, CSVValidationModel> customValidator = customValidators.get(header);
                        if (customValidator != null) {
                            customValidator.accept(cell, csvValidation);
                        }
                    }
                }
            }
        }

        if (!csvValidation.hasErrors()) {
            if (object.getUri() == null) {
                int retry = 0;
                URI objectURI = uriGenerator.generateURI(graph.toString(), name, retry);
                while (checkedURIs.containsKey(objectURI) || sparql.uriExists(SPARQLDeserializers.nodeURI(graph), objectURI)) {
                    retry++;
                    objectURI = uriGenerator.generateURI(graph.toString(), name, retry);
                }
                checkedURIs.put(objectURI, rowIndex);
                object.setUri(objectURI);
            }

            object.setType(model.getUri());
            csvValidation.addObject(name, object);
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

        if (restriction.isRequired() && (StringUtils.isEmpty(value))) {
            csvValidation.addMissingRequiredValue(cell);
        } else if (model.isDatatypePropertyRestriction(propertyURI)) {
            try {
                if (!StringUtils.isEmpty(value)) {
                    SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getSubjectURI());
                    if (!deserializer.validate(value)) {
                        csvValidation.addInvalidDatatypeError(cell, restriction.getSubjectURI());
                    } else if (customValidator != null) {
                        customValidator.accept(cell, csvValidation);
                    }

                    if (!csvValidation.hasErrors()) {
                        object.addRelation(graph, propertyURI, deserializer.getClassType(), value);
                    }
                }
            } catch (SPARQLDeserializerNotFoundException ex) {
                csvValidation.addInvalidDatatypeError(cell, restriction.getSubjectURI());
            }
        } else if (model.isObjectPropertyRestriction(propertyURI)) {
            try {
                if (!StringUtils.isEmpty(value)) {
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

                        if (!csvValidation.hasErrors()) {
                            object.addRelation(graph, propertyURI, URI.class, value);
                        }
                    } else {
                        if (customValidator != null) {
                            customValidator.accept(cell, csvValidation);
                        } else {
                            csvValidation.addInvalidURIError(cell);
                        }
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
        boolean nullOrEmpty = (value == null || value.isEmpty());
        if (restriction != null) {
            if (restriction.isRequired() && nullOrEmpty) {
                return false;
            } else if (model.isDatatypePropertyRestriction(propertyURI)) {
                try {
                    SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getSubjectURI());

                    if (nullOrEmpty || deserializer.validate(value)) {
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

        Map<String,WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.searchResourceTree(
                sparql.getDefaultGraph(DatatypePropertyModel.class),
                DatatypePropertyModel.class,
                user.getLanguage(),
                dataPropertiesParent,
                true,
                this::appendDomainBoundExpr,
                customHandlerByFields
        );
    }

    protected void appendDomainBoundExpr(SelectBuilder select){

        ExprFactory exprFactory = select.getExprFactory();

        // the filtering on domain subClass will apply for any bound type,
        // but if type is not bound, it match since the domain field is optional. So we must ensure that domain is bound
        select.addFilter(exprFactory.bound(makeVar(ObjectPropertyModel.DOMAIN_FIELD)));
    }

    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, UserModel user) throws Exception {

        URI objectPropertiesParent = new URI(OWL2.topObjectProperty.getURI());

        Map<String,WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.searchResourceTree(
                sparql.getDefaultGraph(ObjectPropertyModel.class),
                ObjectPropertyModel.class,
                user.getLanguage(),
                objectPropertiesParent,
                true,
                this::appendDomainBoundExpr,
                customHandlerByFields
        );
    }

    private void addDomainSubClassOfExistExpr(Map<String,WhereHandler> customHandlerByFields, URI domain) {

        if (domain != null) {
            WhereHandler handler = new WhereHandler();
            handler.addWhere(new TriplePath(makeVar(DatatypePropertyModel.DOMAIN_FIELD),Ontology.subClassAny,SPARQLDeserializers.nodeURI(domain)));
            customHandlerByFields.put(DatatypePropertyModel.DOMAIN_FIELD,handler);
        }
    }

    public void createDataProperty(Node graph, DatatypePropertyModel dataProperty) throws Exception {
        sparql.create(graph, dataProperty);
        OntologyCache.getInstance(sparql).invalidateProperties();
    }

    public void createObjectProperty(Node graph, ObjectPropertyModel objectProperty) throws Exception {
        sparql.create(graph, objectProperty);
        OntologyCache.getInstance(sparql).invalidateProperties();
    }

    public DatatypePropertyModel getDataProperty(URI propertyURI, URI domain, UserModel user) throws Exception {

        Map<String,WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.loadByURI(
                sparql.getDefaultGraph(DatatypePropertyModel.class),
                DatatypePropertyModel.class,
                propertyURI,
                user.getLanguage(),
                null,
                customHandlerByFields
        );
    }

    public ObjectPropertyModel getObjectProperty(URI propertyURI, URI domain, UserModel user) throws Exception {

        Map<String,WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.loadByURI(
                sparql.getDefaultGraph(ObjectPropertyModel.class),
                ObjectPropertyModel.class,
                propertyURI,
                user.getLanguage(),
                null,
                customHandlerByFields
        );

    }

    public void updateDataProperty(Node graph, DatatypePropertyModel dataProperty) throws Exception {
        sparql.update(graph, dataProperty);
        OntologyCache.getInstance(sparql).invalidateProperties();
    }

    public void updateObjectProperty(Node graph, ObjectPropertyModel objectProperty) throws Exception {
        sparql.update(graph, objectProperty);
        OntologyCache.getInstance(sparql).invalidateProperties();
    }

    public void deleteDataProperty(Node propertyGraph, URI propertyURI) throws Exception {
        sparql.delete(propertyGraph, DatatypePropertyModel.class, propertyURI);
        OntologyCache.getInstance(sparql).invalidateProperties();
    }

    public void deleteObjectProperty(Node propertyGraph, URI propertyURI) throws Exception {
        sparql.delete(propertyGraph, ObjectPropertyModel.class, propertyURI);
        OntologyCache.getInstance(sparql).invalidateProperties();
    }

    public boolean addClassPropertyRestriction(Node graph, URI classURI, OwlRestrictionModel restriction, String lang) throws Exception {
        List<OwlRestrictionModel> results = getClassPropertyRestriction(graph, classURI, restriction.getOnProperty(), lang);

        if (results.size() == 0) {
            sparql.create(graph, restriction, false, true, (create, node) -> {
                create.addInsert(graph, SPARQLDeserializers.nodeURI(classURI), RDFS.subClassOf, node);
            });
            return true;
        } else {
            return false;
        }
    }

    public List<OwlRestrictionModel> getClassPropertyRestriction(Node graph, URI classURI, URI propertyURI, String lang) throws Exception {

        return sparql.search(graph, OwlRestrictionModel.class, lang, (select) -> {
                    Var uriVar = makeVar(OwlRestrictionModel.URI_FIELD);
                    select.addWhere(SPARQLDeserializers.nodeURI(classURI), RDFS.subClassOf, uriVar);
                    select.addWhere(uriVar, OWL2.onProperty, SPARQLDeserializers.nodeURI(propertyURI));
                },
                null,
                null,
                null,
                0,
                1
        );
    }

    public void deleteClassPropertyRestriction(Node graph, URI classURI, URI propertyURI, String lang) throws Exception {
        List<OwlRestrictionModel> results = getClassPropertyRestriction(graph, classURI, propertyURI, lang);

        if (results.size() == 0) {
            throw new NotFoundException("Class property restriction not found for : " + classURI.toString() + " - " + propertyURI.toString());
        } else if (results.size() > 1) {
            throw new NotFoundException("Multiple class property restrictions found (should never happend) for : " + classURI.toString() + " - " + propertyURI.toString());
        } else {
            UpdateBuilder delete = new UpdateBuilder();
            delete.addDelete(graph, SPARQLDeserializers.nodeURI(classURI), RDFS.subClassOf, "?s");
            delete.addDelete(graph, "?s", "?p", "?o");
            delete.addWhere("?s", OWL2.onProperty, SPARQLDeserializers.nodeURI(propertyURI));
            delete.addWhere("?s", "?p", "?o");
            sparql.executeDeleteQuery(delete);
        }
    }

    public void updateClassPropertyRestriction(Node graph, URI classURI, OwlRestrictionModel restriction, String language) throws Exception {
        try {
            sparql.startTransaction();
            deleteClassPropertyRestriction(graph, classURI, restriction.getOnProperty(), language);
            addClassPropertyRestriction(graph, classURI, restriction, language);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            throw ex;
        }
    }

    public String getURILabel(URI uri, String language) throws SPARQLException {
        SelectBuilder select = new SelectBuilder();

        String nameField = "name";
        Var nameVar = makeVar(nameField);
        select.addVar(nameVar);
        select.addWhere(SPARQLDeserializers.nodeURI(uri), RDFS.label, nameVar);
        Locale locale = Locale.forLanguageTag(language);
        select.addFilter(SPARQLQueryHelper.langFilter(nameField, locale.getLanguage()));
        List<SPARQLResult> results = sparql.executeSelectQuery(select);
        String name;
        if (results.size() >= 1) {
            name = results.get(0).getStringValue(nameField);
        } else {
            name = SPARQLDeserializers.formatURI(uri).toString();
        }

        return name;
    }
    
    public List<SPARQLNamedResourceModel> getURILabels(Collection<URI> uris, String language, URI context) throws SPARQLException, SPARQLDeserializerNotFoundException, Exception {
        SelectBuilder select = new SelectBuilder();
        select.setDistinct(true);

        String nameField = "name";
        String namesField = "names";
        Var nameVar = makeVar(nameField);
        ExprFactory exprFactory = select.getExprFactory();
        Aggregator groupConcat = AggregatorFactory.createGroupConcat(true, exprFactory.asExpr(nameVar), " | ", null); 
        Var fieldConcatVar = makeVar(namesField); 
        select.addVar(groupConcat.toString(),fieldConcatVar);        

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        select.addVar(uriVar);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        select.addVar(typeVar);
        Var typeNameVar = makeVar(SPARQLResourceModel.TYPE_NAME_FIELD);
        select.addVar(typeNameVar);
        
        if (context != null) {
            select.addGraph(NodeFactory.createURI(SPARQLDeserializers.nodeURI(context).toString()), new Triple(uriVar, NodeFactory.createURI(RDFS.label.toString()), nameVar));
        } else {
            select.addWhere(uriVar, RDFS.label, nameVar);
        }   
        select.addWhere(uriVar, RDF.type, typeVar);
        select.addWhere(typeVar, RDFS.label, typeNameVar);
        select.addGroupBy(SPARQLResourceModel.URI_FIELD).addGroupBy(SPARQLResourceModel.TYPE_FIELD).addGroupBy(SPARQLResourceModel.TYPE_NAME_FIELD);
        Locale locale = Locale.forLanguageTag(language);
        select.addFilter(SPARQLQueryHelper.langFilter(nameField, locale.getLanguage()));
        select.addFilter(SPARQLQueryHelper.langFilter(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
        select.addFilter(SPARQLQueryHelper.inURIFilter(SPARQLResourceModel.URI_FIELD, uris));
        
        List<SPARQLResult> results = sparql.executeSelectQuery(select);
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        List<SPARQLNamedResourceModel> resultList = new ArrayList<>();
        for (SPARQLResult result : results) {
            SPARQLNamedResourceModel model = new SPARQLNamedResourceModel();
            model.setName(result.getStringValue(namesField));
            model.setUri(uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            model.setType(uriDeserializer.fromString(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));
            SPARQLLabel typeLabel = new SPARQLLabel();
            typeLabel.setDefaultLang(locale.getLanguage());
            typeLabel.setDefaultValue(result.getStringValue(SPARQLResourceModel.TYPE_NAME_FIELD));
            model.setTypeLabel(typeLabel);
            resultList.add(model);
        };

        return resultList;
    }

    private static final String CSV_URI_KEY = "uri";
    private static final String CSV_TYPE_KEY = "type";
    private static final String CSV_NAME_KEY = "name";

    public <T extends SPARQLNamedResourceModel> String exportCSV(List<T> objects, URI parentClass, String lang, BiFunction<String, T, String> customValueGenerator, List<String> customColumns, Comparator<String> columnSorter) throws Exception {
        List<String> columnsID = new ArrayList<>();

        List<Map<Integer, String>> rows = new ArrayList<>();
        Map<String, List<Integer>> propertiesIndexes = new HashMap<>();

        List<String> loadedTypes = new ArrayList<>();

        for (T object : objects) {
            Map<Integer, String> row = new HashMap<>();

            String typeURI = URIDeserializer.formatURI(object.getType()).toString();
            row.put(0, URIDeserializer.formatURI(object.getUri()).toString());
            row.put(1, typeURI);
            row.put(2, object.getName());

            int initialOffset = row.size();

            for (int i = 0; i < customColumns.size(); i++) {
                String value = customValueGenerator.apply(customColumns.get(i), object);
                row.put(initialOffset + i, value);
            }

            int rowOffset = row.size();

            for (SPARQLModelRelation relation : object.getRelations()) {
                Property property = relation.getProperty();
                String propertyURIString = property.getURI();

                int propertyIndex = columnsID.indexOf(propertyURIString);
                if (propertyIndex < 0) {
                    propertyIndex = columnsID.size();
                    columnsID.add(propertyURIString);
                }

                propertyIndex += rowOffset;

                if (!propertiesIndexes.containsKey(propertyURIString)) {
                    List<Integer> indexes = new ArrayList<>();
                    indexes.add(propertyIndex);
                    propertiesIndexes.put(propertyURIString, indexes);
                }

                List<Integer> indexes = propertiesIndexes.get(propertyURIString);

                Integer currentIndex = -1;
                for (int i = 0; i < indexes.size(); i++) {
                    if (!row.containsKey(indexes.get(i))) {
                        currentIndex = indexes.get(i);
                        break;
                    }
                }

                if (currentIndex < 0) {
                    propertyIndex = columnsID.size() + rowOffset;
                    columnsID.add(propertyURIString);
                    indexes.add(propertyIndex);
                    currentIndex = propertyIndex;
                }

                String strValue = relation.getValue();
                if (customValueGenerator != null) {
                    strValue = customValueGenerator.apply(propertyURIString, object);
                    if (strValue == null) {
                        strValue = relation.getValue();
                    }
                }
                row.put(currentIndex, strValue);
            }

            // for each type relation
            if (!loadedTypes.contains(typeURI)) {
                ClassModel model = getClassModel(object.getType(), parentClass, lang);
                model.getRestrictions().keySet().forEach(property -> {
                    String propertyURI = SPARQLDeserializers.getExpandedURI(property);
                    if (!columnsID.contains(propertyURI)
                            && !customColumns.contains(propertyURI)
                            && !SPARQLDeserializers.compareURIs(RDFS.label.getURI(), property)) {
                        columnsID.add(propertyURI);
                        List<Integer> indexes = new ArrayList<>();
                        indexes.add(columnsID.size() + rowOffset - 1);
                        propertiesIndexes.put(propertyURI, indexes);

                    }
                });

                loadedTypes.add(typeURI);
            }

            rows.add(row);
        }

        int colOffset = 3;

        for (int i = 0; i < customColumns.size(); i++) {
            String customColumn = customColumns.get(i);
            List<Integer> customColumnIndex = new ArrayList<>();
            customColumnIndex.add(i + colOffset);
            propertiesIndexes.put(customColumn, customColumnIndex);
            columnsID.add(customColumn);
        }

        columnsID.sort((id1, id2) -> {
            if (columnSorter != null) {
                return columnSorter.compare(id1, id2);
            }
            return id1.compareTo(id2);
        });

        List<URI> columnURIs = new ArrayList<>();
        columnURIs.add(new URI(RDFS.label.getURI()));
        columnsID.forEach((columID) -> {
            try {
                URI columnURI = new URI(columID);
                if (columnURI.isAbsolute()) {
                    columnURIs.add(columnURI);
                }
            } catch (Exception ex) {
                // Ignore invalid column IDs
            }
        });

        Map<String, String> columnsNames = new HashMap<>();

        SelectBuilder propertyNamesRequest = new SelectBuilder();
        Var uriVar = makeVar("uri");
        Var nameVar = makeVar("name");
        propertyNamesRequest.addVar(uriVar);
        propertyNamesRequest.addVar(nameVar);
        propertyNamesRequest.addWhere(uriVar, RDFS.label, nameVar);
        propertyNamesRequest.addFilter(SPARQLQueryHelper.langFilter(nameVar.getVarName(), lang));
        SPARQLQueryHelper.inURI(propertyNamesRequest, uriVar.getVarName(), columnURIs);
        sparql.executeSelectQuery(propertyNamesRequest, (result) -> {
            String uri = SPARQLDeserializers.getExpandedURI(result.getStringValue(uriVar.getVarName()));
            columnsNames.put(uri, result.getStringValue(nameVar.getVarName()));
        });

        StringWriter strWriter = new StringWriter();
        CSVWriter writer = new CSVWriter(strWriter,
                lang.equals("fr") ? ';' : CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        String[] headerArray = new String[colOffset + columnsID.size()];
        String[] headerNameArray = new String[colOffset + columnsID.size()];
        headerArray[0] = CSV_URI_KEY;
        headerNameArray[0] = "URI";
        headerArray[1] = CSV_TYPE_KEY;
        headerNameArray[1] = "Type";
        headerArray[2] = CSV_NAME_KEY;
        headerNameArray[2] = columnsNames.get(SPARQLDeserializers.getExpandedURI(RDFS.label.getURI()));

        for (int i = 0; i < columnsID.size(); i++) {
            headerArray[i + colOffset] = SPARQLDeserializers.formatURI(columnsID.get(i));
            String colName = columnsID.get(i);
            if (columnsNames.containsKey(colName)) {
                colName = columnsNames.get(colName);
            }
            if (!colName.isEmpty()) {
                headerNameArray[i + colOffset] = colName.substring(0, 1).toUpperCase() + colName.substring(1);
                ;
            }
        }

        writer.writeNext(headerArray);

        writer.writeNext(headerNameArray);

        for (Map<Integer, String> row : rows) {
            String[] rowArray = new String[columnsID.size() + colOffset];
            rowArray[0] = row.get(0);
            rowArray[1] = row.get(1);
            rowArray[2] = row.get(2);

            int arrayIndex = colOffset;
            String lastColumnId = "";
            List<Integer> indexes = null;
            int currentColumnIndex = 0;
            for (String columnID : columnsID) {
                if (!lastColumnId.equals(columnID)) {
                    indexes = propertiesIndexes.get(columnID);
                    currentColumnIndex = 0;
                } else {
                    currentColumnIndex++;
                }
                Integer rowIndex = indexes.get(currentColumnIndex);
                if (row.containsKey(rowIndex)) {
                    rowArray[arrayIndex] = row.get(rowIndex);
                }
                lastColumnId = columnID;
                arrayIndex++;
            }

            writer.writeNext(rowArray);

        }
        writer.close();

        strWriter.close();

        return strWriter.toString();

    }

    public SPARQLResourceModel getRdfType(URI uri, String language) throws Exception {
        return sparql.getByURI(SPARQLResourceModel.class, uri, language);
    }

//    public List<TypeModel> getRdfTypes(URI uri, String language, boolean parents) throws SPARQLException, URISyntaxException, Exception {
//        String typeField = "type";
//        String typeLabelField = "typeLabel";
//        SelectBuilder select = new SelectBuilder();
//        select.addVar(typeField);
//        select.addVar(typeLabelField);
//        select.setDistinct(true);
//        
//        if (parents) {
//            select.addWhere(NodeFactory.createURI(SPARQLDeserializers.nodeURI(uri).toString()), RDF.type, makeVar("directType"));
//            select.addWhere(makeVar("directType"), Ontology.subClassAny, makeVar(typeField));
//            select.addWhere(makeVar(typeField), RDFS.label, makeVar(typeLabelField));            
//        } else {
//            select.addWhere(NodeFactory.createURI(SPARQLDeserializers.nodeURI(uri).toString()), RDF.type, makeVar(typeField));
//            select.addWhere(makeVar(typeField), RDFS.label, makeVar(typeLabelField));
//        }
//        
//        Locale locale = Locale.forLanguageTag(language);
//        select.addFilter(SPARQLQueryHelper.langFilter(typeLabelField, locale.getLanguage()));
//
//        List<TypeModel> rdfTypes = new ArrayList<>();
//        List<SPARQLResult> results = sparql.executeSelectQuery(select);
//        for (SPARQLResult res:results) {
//            TypeModel model = new TypeModel();
//            model.setType(new URI(res.getStringValue(typeField)));
//            model.setTypeLabel(res.getStringValue(typeLabelField));
//            rdfTypes.add(model);
//        }
//
//        return rdfTypes;
//
//    }

    public List<URITypesModel> checkURIsTypes(List<URI> uris, List<URI> rdfTypes) throws SPARQLException, URISyntaxException, Exception {
        List<URITypesModel> urisTypes = new ArrayList<>();

        String uriField = "_uri";
        SelectBuilder select = new SelectBuilder();
        select.addVar(uriField);
        select.setDistinct(true);

        String dTypeField = "_dtype";
        String typeField = "_type";

        select.addWhere(select.makeTriplePath(makeVar(uriField), RDF.type, makeVar("type")));

        for (int i = 0; i < rdfTypes.size(); i++) {
            String index = String.valueOf(i);
            select.addVar(typeField + index);
            WhereBuilder optionBuiler = new WhereBuilder();
            optionBuiler.addWhere(makeVar(uriField), RDF.type, makeVar(dTypeField + index));
            optionBuiler.addWhere(makeVar(dTypeField + index), Ontology.subClassAny, makeVar(typeField + index));
            optionBuiler.addFilter(SPARQLQueryHelper.eq(typeField + index, rdfTypes.get(i)));

            select.addOptional(optionBuiler);

        }
        select.addFilter(SPARQLQueryHelper.inURIFilter(uriField, uris));

        List<SPARQLResult> results = sparql.executeSelectQuery(select);

        for (SPARQLResult res : results) {
            URI uri = new URI(res.getStringValue(uriField));
            List<URI> types = new ArrayList();
            for (int i = 0; i < rdfTypes.size(); i++) {
                String index = String.valueOf(i);
                if (res.getStringValue(typeField + String.valueOf(index)) != null) {
                    URI type = new URI(res.getStringValue(typeField + String.valueOf(index)));
                    types.add(type);
                }
            }
            URITypesModel model = new URITypesModel(uri, types);
            urisTypes.add(model);

        }
        return urisTypes;
    }
    
    public List<URI> getSubclassRdfTypes(URI rdfType, UserModel user) throws Exception {
        
        SPARQLTreeListModel treeList = searchSubClasses(
                rdfType,
                ClassModel.class,
                null,
                user,
                false,
                null
        );
        
        List<URI> rdfTypes = new ArrayList<>();        
        List<ResourceTreeDTO> trees = ResourceTreeDTO.fromResourceTree(treeList);        
        
        while (!trees.isEmpty()) {
            for (ResourceTreeDTO tree:trees) {
                rdfTypes.add(tree.getUri());
                trees = tree.getChildren();
                
            }
        }
        
        return rdfTypes;
    }  

    public List<SPARQLNamedResourceModel> getByName(String targetNameOrUri) throws SPARQLException, SPARQLDeserializerNotFoundException, Exception {
        SelectBuilder select = new SelectBuilder();
        select.setDistinct(true);
        String uriField = "uri";
        Var uriVar = makeVar(uriField);
        select.addVar(uriVar);
        select.addWhere(uriVar, RDFS.label, targetNameOrUri);  
          
        List<SPARQLResult> results = sparql.executeSelectQuery(select);
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);
        List<SPARQLNamedResourceModel> resultList = new ArrayList<>();
        for (SPARQLResult result : results) {
            SPARQLNamedResourceModel model = new SPARQLNamedResourceModel();
            model.setName(targetNameOrUri);
            model.setUri(uriDeserializer.fromString(result.getStringValue(uriField)));
            resultList.add(model);
        };

        return resultList;
    }

}
