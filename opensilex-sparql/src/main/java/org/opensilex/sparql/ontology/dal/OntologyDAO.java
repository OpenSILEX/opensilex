/*******************************************************************************
 *                         OntologyDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.ontology.dal;

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
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.vocabulary.*;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.*;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vince
 */
public final class OntologyDAO {

    private final SPARQLService sparql;
    private final URI topDataPropertyUri = URI.create(OWL2.topDataProperty.getURI());
    private final URI topObjectPropertyUri = URI.create(OWL2.topObjectProperty.getURI());

    /**
     * Custom classes and properties are stored inside 'set/properties' graph. Read-Write <br><br>
     * Classes and properties embedded from OpenSILEX ontologies are stored inside their ontology graph. Read-Only
     */
    public static final String CUSTOM_TYPES_AND_PROPERTIES_GRAPH = "properties";

    public static final String NO_LABEL_FOR_URI_MESSAGE = "No label found for URI <%s>";

    private final Node customGraph;

    public OntologyDAO(SPARQLService sparql) {
        this.sparql = sparql;

        String customGraphURI = UriBuilder.fromUri(sparql.getBaseURI())
                .path(SPARQLClassObjectMapper.DEFAULT_GRAPH_KEYWORD)
                .path(CUSTOM_TYPES_AND_PROPERTIES_GRAPH)
                .toString();

        customGraph = NodeFactory.createURI(customGraphURI);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyDAO.class);

    public URI getTopDataPropertyUri() {
        return topDataPropertyUri;
    }

    public URI getTopObjectPropertyUri() {
        return topObjectPropertyUri;
    }

    public Node getCustomGraph() {
        return customGraph;
    }

    public void create(ClassModel model) throws Exception {
        sparql.create(customGraph, model);
    }

    public void update(ClassModel model) throws Exception {
        sparql.update(customGraph, model);
    }

    public static final String CLASS_DELETION_KEY_PARAMETER = "class";
    public static final String CLASS_DELETION_ERROR_KEY = "Class can't be deleted";

    /**
     *
     * @param classURI URI of the class to delete
     * @throws Exception If some unexpected error occurs during class deletion
     * @throws DisplayableBadRequestException if class can't be deleted
     *
     * @apiNote There are several case when the class can't be deleted :
     * <ul>
     *     <li>If class has some instance</li>
     *     <li>If class has some child class</li>
     *     <li>If some data or object property have the class as domain</li>
     * </ul>
     */
    public void deleteClass(URI classURI) throws Exception, DisplayableBadRequestException {

        // check that no instance is associated to class
        if (sparql.existInstanceOf(classURI)) {
            throw new DisplayableBadRequestException(CLASS_DELETION_ERROR_KEY,
                    "component.ontology.class.exception.delete.instance-exists",
                    Collections.singletonMap(CLASS_DELETION_KEY_PARAMETER,classURI.toString())
            );
        }
        ClassModel model = getClassModel(classURI,null,null);
        if(! model.getChildren().isEmpty()){
            throw new DisplayableBadRequestException(CLASS_DELETION_ERROR_KEY,
                    "component.ontology.class.exception.delete.has-children",
                    Collections.singletonMap(CLASS_DELETION_KEY_PARAMETER,classURI.toString())
            );
        }

        boolean hasDataPropertiesOnClass = searchDataProperties(classURI,null,null).getRootsCount() > 0;
        if (hasDataPropertiesOnClass) {
            throw new DisplayableBadRequestException(CLASS_DELETION_ERROR_KEY,
                    "component.ontology.class.exception.delete.has-data-properties",
                    Collections.singletonMap(CLASS_DELETION_KEY_PARAMETER,classURI.toString())
            );
        }

        boolean hasObjectPropertiesOnClass = searchObjectProperties(classURI,null,null).getRootsCount() > 0;
        if(hasObjectPropertiesOnClass) {
            throw new DisplayableBadRequestException(CLASS_DELETION_ERROR_KEY,
                    "component.ontology.class.exception.delete.has-object-properties",
                    Collections.singletonMap(CLASS_DELETION_KEY_PARAMETER,classURI.toString())
            );
        }

        UpdateBuilder deleteRestrictionOnClass = getDeleteRestrictionUpdate(classURI,null);
        sparql.startTransaction();

        try{
            // Delete restriction first because deleting the ClassModel imply a deletion of all rdfs:subClassOf relations
            // Since a ClassModel can have an outgoing <rdfs:subClassOf> relation with a parent ClassModel, if the Class is deleted first,
            // there is no way to retrieve and delete OWL restriction which have a <rdfs:subClassOf> relation with the Class
            sparql.executeUpdateQuery(deleteRestrictionOnClass);

            // then delete class model it-self
            sparql.delete(null, ClassModel.class, classURI);

            sparql.commitTransaction();
        }catch (Exception e){
            sparql.rollbackTransaction();
        }
    }

    /**
     *
     * @param classURI URI of the class for which we remove associated OWL restriction (optional) If set, the query removal all restriction associated to this class with
     * the class with a  <b>?class rdfs:subClassOf ?restriction</b> triple
     * @param propertyURI  URI of the property for which we remove associated OWL restriction (optional) If set, the query remove all restriction associated
     * to the property with a  <b>?restriction owl:onProperty ?property</b> triple
     *
     * @return a SPARQL UPDATE query which allow to delete all restriction associated to a class and/or a property
     * @throws IllegalArgumentException if classURI and propertyURI are both null

     * @apiNote The following UPDATE is returned, depending on the nullity of classURI and propertyURI
     * <pre>
     *
     * {@code
     * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     * PREFIX owl: <http://www.w3.org/2002/07/owl#>
     *
     * DELETE {
     *      :restrictedClass rdfs:subClassOf ?restriction . # case 1 : delete link between restriction and the class
     *      ?restrictedClass rdfs:subClassOf ?restriction . # or case 2 : delete link between restrictions and associated classes
     *
     *      ?restriction ?p ?o
     *      WHERE {
     *           ?restriction a owl:Restriction .
     *           ?restriction ?p ?o
     *
     *           # case 1 : delete restriction linked to a particular class
     *           ?restriction owl:onProperty ?restrictedProperty .
     *
     *           # or case 2 : delete restriction linked to a particular property
     *           ?restrictedClass rdfs:subClassOf ?restriction .
     *           ?restriction owl:onProperty :restrictedProperty .
     *      }
     * }
     *
     * }
     * </pre>
     */
    private UpdateBuilder getDeleteRestrictionUpdate(URI classURI, URI propertyURI) {

        if(classURI == null && propertyURI == null){
            throw new IllegalArgumentException("classURI and propertyURI are both null");
        }

        Node restriction = makeVar("restriction");
        Node restrictionProperty = makeVar("p");
        Node restrictionValue = makeVar("o");

        // if class is not specified use a variable, else use the given class URI
        // if no class is provided, then delete all relation between a class and a restriction on the given property
        Node restrictedClass = classURI == null ? makeVar("?restrictedClass") : SPARQLDeserializers.nodeURI(classURI);

        // if a class is provided, then delete all link between the clas
        // if a class is provided, then delete all relation between a property and restriction on the given class
        Node restrictedProperty = propertyURI == null ? makeVar("?restrictedProperty") : SPARQLDeserializers.nodeURI(propertyURI);

        return new UpdateBuilder()
                .addDelete(customGraph, new WhereBuilder()
                        .addWhere(restrictedClass, RDFS.subClassOf, restriction)
                        .addWhere(restriction, restrictionProperty, restrictionValue)
                ).addGraph(customGraph, new WhereBuilder()
                        .addWhere(restrictedClass, RDFS.subClassOf, restriction)
                        .addWhere(restriction, OWL.onProperty, restrictedProperty)
                        .addWhere(restriction, RDF.type, OWL2.Restriction)
                        .addWhere(restriction, restrictionProperty, restrictionValue));
    }

    private UpdateBuilder getLastUpdatedDateRestrictionUpdate(URI classURI) {
        Var objectVar = makeVar("?o");
        Node nodeURI = SPARQLDeserializers.nodeURI(classURI);
        return new UpdateBuilder().addDelete(customGraph, nodeURI, DCTerms.modified, objectVar)
                .addWhere(new WhereBuilder().addGraph(customGraph, nodeURI, DCTerms.modified, objectVar))
                .addInsert(customGraph, nodeURI, DCTerms.modified, SPARQLDeserializers.nodeOffsetDateTime(OffsetDateTime.now()));
    }

    public ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException {

        WhereHandler parentHandler = null;
        if (parentClass != null) {
            // Add a WHERE with a subClassOf* path on PARENT field, instead to add it on the end of query
            parentHandler = new WhereHandler();
            parentHandler.addWhere(new TriplePath(makeVar(SPARQLTreeModel.PARENT_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(parentClass)));
        }
        ClassModel model;
        try {
            model = sparql.loadByURI(
                    null, // don't specify a graph, since multiple graph can contain a class definition
                    ClassModel.class,
                    rdfClass,
                    lang,
                    null,
                    parentClass != null ? Collections.singletonMap(SPARQLTreeModel.PARENT_FIELD, parentHandler) : null
            );
        } catch (Exception e) {
            throw new SPARQLException(e);
        }

        if (model == null) {
            throw new SPARQLInvalidUriListException("URI not found ", Collections.singletonList(rdfClass));
        }

        try {
            buildProperties(model, lang);
            return model;
        } catch (Exception e) {
            throw new SPARQLException(e);
        }

    }

    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchSubClasses(URI parent, Class<T> clazz, String stringPattern, String lang, boolean excludeRoot, Consumer<T> handler) throws Exception {
        SPARQLTreeListModel<T> classTree = sparql.searchResourceTree(
                null, // don't specify a graph, since multiple graph can contain a class definition
                clazz,
                lang,
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    if (parent != null) {
                        Var parentVar = makeVar(SPARQLTreeModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(parent));
                        select.addWhere(makeVar(SPARQLResourceModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                    if (!StringUtils.isEmpty(stringPattern)) {
                        Var parentNameField = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(SPARQLTreeModel.PARENT_FIELD));

                        select.addFilter(SPARQLQueryHelper.or(
                                SPARQLQueryHelper.regexFilter(VocabularyModel.LABEL_FIELD, stringPattern),
                                SPARQLQueryHelper.regexFilter(parentNameField.getVarName(), stringPattern)
                        ));
                    }
                },
                Collections.emptyMap()
        );

        if (handler != null) {
            classTree.traverse(handler);
        }

        return classTree;
    }


    public List<OwlRestrictionModel> getOwlRestrictions(URI rdfClass, String lang) throws Exception {
        return sparql.search(
                null, // don't specify a graph, since multiple graph can contain a restriction definition
                OwlRestrictionModel.class,
                lang,
                (SelectBuilder select) -> {
                    Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
                    Var classUriVar = makeVar("classURI");
                    select.addWhere(classUriVar, RDFS.subClassOf, uriVar);
                    select.addWhere(SPARQLDeserializers.nodeURI(rdfClass), Ontology.subClassAny, classUriVar);
                }
        );
    }

    private void buildDataAndObjectProperties(ClassModel model,
                                              String lang,
                                              Map<URI, URI> datatypePropertiesURI,
                                              Map<URI, URI> objectPropertiesURI) throws Exception {

        Map<URI, DatatypePropertyModel> dataPropertiesMap = new HashMap<>();
        List<DatatypePropertyModel> dataPropertiesList = sparql.getListByURIs(DatatypePropertyModel.class,
                datatypePropertiesURI.keySet(), lang);

        MapUtils.populateMap(dataPropertiesMap, dataPropertiesList, pModel -> {
            // don't set parent if parent is TopObjectProperty
            if (pModel.getParent() != null && SPARQLDeserializers.compareURIs(topDataPropertyUri, pModel.getParent().getUri())) {
                pModel.setParent(null);
            }
            pModel.setTypeRestriction(datatypePropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setDatatypeProperties(dataPropertiesMap);

        Map<URI, ObjectPropertyModel> objectPropertiesMap = new HashMap<>();
        List<ObjectPropertyModel> objectPropertiesList = sparql.getListByURIs(ObjectPropertyModel.class, objectPropertiesURI.keySet(), lang);

        MapUtils.populateMap(objectPropertiesMap, objectPropertiesList, pModel -> {
            pModel.setTypeRestriction(objectPropertiesURI.get(pModel.getUri()));

            // don't set parent if parent is TopObjectProperty
            if (pModel.getParent() != null && SPARQLDeserializers.compareURIs(topObjectPropertyUri, pModel.getParent().getUri())) {
                pModel.setParent(null);
            }
            return pModel.getUri();
        });
        model.setObjectProperties(objectPropertiesMap);
    }

    private void addRestriction(OwlRestrictionModel restriction,
                                Map<URI, OwlRestrictionModel> mergedRestrictions,
                                Map<URI, URI> datatypePropertiesURI,
                                Map<URI, URI> objectPropertiesURI) throws SPARQLException {

        URI propertyURI = restriction.getOnProperty();

        if (mergedRestrictions.containsKey(propertyURI)) {
            OwlRestrictionModel mergedRestriction = mergedRestrictions.get(propertyURI);
            mergedRestriction.setQualifiedCardinality(restriction.getQualifiedCardinality());
            mergedRestriction.setMinQualifiedCardinality(restriction.getMinQualifiedCardinality());
            mergedRestriction.setMaxQualifiedCardinality(restriction.getMaxQualifiedCardinality());
        } else {
            mergedRestrictions.put(propertyURI, restriction);
        }

        if (restriction.getOnDataRange() != null) {
            if (SPARQLDeserializers.existsForDatatype(restriction.getOnDataRange())) {
                datatypePropertiesURI.put(propertyURI, restriction.getOnDataRange());
            }
        } else if (restriction.getOnClass() != null) {
            if (sparql.uriExists(ClassModel.class, restriction.getOnClass())) {
                objectPropertiesURI.put(propertyURI, restriction.getOnClass());
            }
        } else if (restriction.getSomeValuesFrom() != null) {
            URI someValueFrom = restriction.getSomeValuesFrom();
            if (SPARQLDeserializers.existsForDatatype(someValueFrom)) {
                datatypePropertiesURI.put(propertyURI, someValueFrom);
            } else if (sparql.uriExists(ClassModel.class, someValueFrom)) {
                objectPropertiesURI.put(propertyURI, someValueFrom);
            }
        }

    }

    public void buildProperties(ClassModel model, String lang) throws Exception {

        List<OwlRestrictionModel> restrictions = getOwlRestrictions(model.getUri(), lang);
        if (restrictions.isEmpty()) {
            return;
        }

        Map<URI, URI> datatypePropertiesURI = new HashMap<>();
        Map<URI, URI> objectPropertiesURI = new HashMap<>();
        Map<URI, OwlRestrictionModel> mergedRestrictions = new HashMap<>();

        for (OwlRestrictionModel restriction : restrictions) {
            addRestriction(restriction, mergedRestrictions, datatypePropertiesURI, objectPropertiesURI);
        }
        model.setRestrictionsByProperties(mergedRestrictions);

        buildDataAndObjectProperties(model, lang, datatypePropertiesURI, objectPropertiesURI);
    }


    public boolean validateObjectValue(
            URI graph,
            ClassModel model,
            URI propertyURI,
            String value,
            SPARQLResourceModel object
    ) {

        OwlRestrictionModel restriction = model.getRestrictionsByProperties().get(propertyURI);
        boolean nullOrEmpty = (value == null || value.isEmpty());
        if (restriction != null) {
            if (restriction.isRequired() && nullOrEmpty) {
                return false;
            }else if(nullOrEmpty && ! restriction.isRequired()){
                return true;
            }
            else if (model.isDatatypePropertyRestriction(propertyURI)) {
                try {
                    SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getSubjectURI());

                    if (nullOrEmpty || deserializer.validate(value)) {
                        object.addRelation(graph, propertyURI, deserializer.getClassType(), value);
                        return true;
                    }
                } catch (SPARQLDeserializerNotFoundException ex) {
                    LOGGER.warn("Error while searching deserializer that should never happend for type: " + restriction.getSubjectURI(), ex);
                    return false;
                }
            } else if (model.isObjectPropertyRestriction(propertyURI) && URIDeserializer.validateURI(value)) {
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

        return false;
    }



    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String pattern, String lang) throws Exception {
        return searchProperties(DatatypePropertyModel.class, topDataPropertyUri, domain, pattern, lang);
    }

    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String pattern, String lang) throws Exception {
        return searchProperties(ObjectPropertyModel.class, topObjectPropertyUri, domain, pattern, lang);
    }

    private <PT extends AbstractPropertyModel<PT>> SPARQLTreeListModel<PT> searchProperties(Class<PT> propertyClazz, URI topPropertyUri, URI domain, String pattern, String lang) throws Exception {

        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.searchResourceTree(
                null, // don't specify a graph, since multiple graph can contain a property definition
                propertyClazz,
                lang,
                topPropertyUri,
                true,
                select -> {
                    appendDomainBoundExpr(select);
                    if (!StringUtils.isEmpty(pattern)) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(VocabularyModel.LABEL_FIELD, pattern));
                    }
                },
                customHandlerByFields
        );
    }


    protected void appendDomainBoundExpr(SelectBuilder select) {

        ExprFactory exprFactory = select.getExprFactory();

        // the filtering on domain subClass will apply for any bound type,
        // but if type is not bound, it match since the domain field is optional. So we must ensure that domain is bound
        select.addFilter(exprFactory.bound(makeVar(AbstractPropertyModel.DOMAIN_FIELD)));
    }

    private void addDomainSubClassOfExistExpr(Map<String, WhereHandler> customHandlerByFields, URI domain) {

        if (domain != null) {
            WhereHandler handler = new WhereHandler();
            handler.addWhere(new TriplePath(makeVar(AbstractPropertyModel.DOMAIN_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(domain)));
            customHandlerByFields.put(AbstractPropertyModel.DOMAIN_FIELD, handler);
        }
    }

    private void addGetLinkablePropertyHandler(Map<String, WhereHandler> customHandlerByFields, URI domain, URI ancestor) {

        Var domainVar = makeVar(AbstractPropertyModel.DOMAIN_FIELD);

        if (domain != null) {
            WhereHandler handler = new WhereHandler();
            customHandlerByFields.put(AbstractPropertyModel.DOMAIN_FIELD, handler);

            // add :domain_uri rdfs:subClassOf* ?domain
            handler.addWhere(new TriplePath(SPARQLDeserializers.nodeURI(domain),Ontology.subClassAny,domainVar));

            if(ancestor != null){
                // add ?domain rdfs:subClassOf* :ancestor_uri
                handler.addWhere(new TriplePath(domainVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(ancestor)));
            }
        }
    }

    protected void appendPropertyNotRestrictedFilter(SelectBuilder select, URI domain){

        appendDomainBoundExpr(select);

        Var restrictionVar = makeVar("restriction");
        Var restrictedClassVar = makeVar("restricted_class");

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);

        // add ( MINUS { properties which are already linked to a restriction } )
        select.addMinus(new WhereBuilder()
                .addWhere(restrictionVar, OWL2.onProperty, uriVar)
                .addWhere(restrictedClassVar,RDFS.subClassOf,restrictionVar)
                .addWhere(SPARQLDeserializers.nodeURI(domain), Ontology.subClassAny, restrictedClassVar)
        );
    }

    private <PT extends AbstractPropertyModel<PT>> Set<PT> getLinkableProperties(Class<PT> propertyClazz, URI topPropertyUri, URI domain, URI ancestor, String lang) throws SPARQLException {

        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        addGetLinkablePropertyHandler(customHandlerByFields, domain, ancestor);

        Set<PT> modelSet = new HashSet<>();

        try {
            sparql.searchResourceTree(
                    null, // don't specify a graph, since multiple graph can contain a property definition
                    propertyClazz,
                    lang,
                    topPropertyUri,
                    true,
                    select -> {
                        appendPropertyNotRestrictedFilter(select, domain);
                    },
                    customHandlerByFields
            ).traverse(modelSet::add);

        } catch (Exception e) {
            throw new SPARQLException(e);
        }

        return modelSet;
    }

    public Set<DatatypePropertyModel> getLinkableDataProperties(URI domain, URI ancestor, String lang) throws SPARQLException {
        return getLinkableProperties(DatatypePropertyModel.class, topDataPropertyUri, domain, ancestor, lang);
    }

    public Set<ObjectPropertyModel> getLinkableObjectProperties(URI domain, URI ancestor, String lang) throws SPARQLException {
        return getLinkableProperties(ObjectPropertyModel.class, topObjectPropertyUri, domain, ancestor, lang);
    }


    public void createDataProperty(DatatypePropertyModel dataProperty) throws Exception {
        sparql.create(customGraph, dataProperty);
    }

    public void createObjectProperty(ObjectPropertyModel objectProperty) throws Exception {
        sparql.create(customGraph, objectProperty);
    }

    public DatatypePropertyModel getDataProperty(URI propertyURI, URI domain, String lang) throws Exception {

        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.loadByURI(
                null, // don't specify a graph, since multiple graph can contain a property definition
                DatatypePropertyModel.class,
                propertyURI,
                lang,
                null,
                customHandlerByFields
        );
    }

    public ObjectPropertyModel getObjectProperty(URI propertyURI, URI domain, String lang) throws Exception {

        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        addDomainSubClassOfExistExpr(customHandlerByFields, domain);

        return sparql.loadByURI(
                null, // don't specify a graph, since multiple graph can contain a property definition
                ObjectPropertyModel.class,
                propertyURI,
                lang,
                null,
                customHandlerByFields
        );

    }

    private void updateRestrictionRangeOnProperty(URI propertyUri, URI newRange, boolean isDataProperty) throws SPARQLException {

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Node propertyNode = SPARQLDeserializers.nodeURI(propertyUri);
        Var rangeVar = makeVar("range");
        Node newRangeNode = SPARQLDeserializers.nodeURI(newRange);

        Triple oldRangeTriple = isDataProperty ?
                new Triple(uriVar, OWL2.onDataRange.asNode(), rangeVar) :
                new Triple(uriVar, OWL2.onClass.asNode(), rangeVar);

        Triple newRangeTriple = isDataProperty ?
                new Triple(uriVar, OWL2.onDataRange.asNode(), newRangeNode) :
                new Triple(uriVar, OWL2.onClass.asNode(), newRangeNode);

        UpdateBuilder update = new UpdateBuilder()
                .addDelete(customGraph, oldRangeTriple)
                .addInsert(customGraph, newRangeTriple)
                .addGraph(customGraph, new WhereBuilder().
                        addWhere(uriVar, OWL2.onProperty, propertyNode)
                        .addWhere(oldRangeTriple));


        sparql.executeUpdateQuery(update);
    }

    public void updateDataProperty(DatatypePropertyModel property) throws Exception {

        try {
            sparql.startTransaction();
            sparql.update(customGraph, property);
            updateRestrictionRangeOnProperty(property.getUri(), property.getRange(), true);
            sparql.commitTransaction();
        } catch (Exception e) {
            sparql.rollbackTransaction(e);
        }
    }

    public void updateObjectProperty(ObjectPropertyModel property) throws Exception {

        try {
            sparql.startTransaction();
            sparql.update(customGraph, property);
            updateRestrictionRangeOnProperty(property.getUri(), property.getRange().getUri(), false);
            sparql.commitTransaction();
        } catch (Exception e) {
            sparql.rollbackTransaction(e);
        }
    }

    public static final String PROPERTY_DELETION_ERROR_KEY = "Property can't be deleted";
    public static final String PROPERTY_DELETION_KEY_PARAMETER = "property";

    /**
     *
     * @param uri URI of the property to delete (required)
     * @param dataProperty indicate if the property is a data-property (true) or not (false)
     * @throws DisplayableBadRequestException if
     * <ul>
     *      <li>The property is used by some resource</li>
     *      <li>The property as sub-property</li>
     * </ul>
     * @throws IllegalArgumentException if no property were found with the given {@code uri} is the database
     * @throws Exception if some error is encountered during SPARQL query evaluation
     * 
     * @see #getDeleteRestrictionUpdate(URI, URI) 
     */
    public void deleteProperty(URI uri, boolean dataProperty) throws DisplayableBadRequestException, IllegalArgumentException, Exception {

        Objects.requireNonNull(uri);

        SPARQLTreeModel<?> propertyModel = dataProperty ?
                getDataProperty(uri,null, null) :
                getObjectProperty(uri, null, null);

        if(propertyModel == null){
            throw new IllegalArgumentException("Unknown property URI "+uri);
        }

        // ensure that the property has no children
        if (! propertyModel.getChildren().isEmpty()){
            throw new DisplayableBadRequestException(
                    PROPERTY_DELETION_ERROR_KEY,
                    "component.ontology.property.exception.delete.has-children",
                    Collections.singletonMap(PROPERTY_DELETION_KEY_PARAMETER,uri.toString())
            );
        }

        // check that no instance is associated to class
        if (sparql.anyPropertyValue(uri)) {
            throw new DisplayableBadRequestException(
                    PROPERTY_DELETION_ERROR_KEY,
                    "component.ontology.property.exception.delete.instance-exists",
                    Collections.singletonMap(PROPERTY_DELETION_KEY_PARAMETER,uri.toString())
            );
        }

        UpdateBuilder deleteRestrictionOnProperty = getDeleteRestrictionUpdate(null,uri);
        sparql.startTransaction();

        try{
            // delete associated restriction
            sparql.executeUpdateQuery(deleteRestrictionOnProperty);

            // delete property it-self
            if(dataProperty){
                sparql.delete(customGraph,DatatypePropertyModel.class,uri);
            }else {
                sparql.delete(customGraph,ObjectPropertyModel.class,uri);
            }
            sparql.commitTransaction();
        }catch (Exception e){
            sparql.rollbackTransaction(e);
        }

    }

    public boolean addClassPropertyRestriction(URI classURI, OwlRestrictionModel restriction, String lang) throws Exception {
        List<OwlRestrictionModel> results = getClassPropertyRestriction(null, classURI, restriction.getOnProperty(), lang);

        if (results.isEmpty()) {
            sparql.create(customGraph, restriction, false, false, true, (create, node) -> {
                create.addInsert(customGraph, SPARQLDeserializers.nodeURI(classURI), RDFS.subClassOf, node);
            });

            UpdateBuilder modified = getLastUpdatedDateRestrictionUpdate(classURI);
            sparql.executeUpdateQuery(modified);
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

    public void deleteClassPropertyRestriction(URI classURI, URI propertyURI, String lang) throws Exception {
        List<OwlRestrictionModel> results = getClassPropertyRestriction(customGraph, classURI, propertyURI, lang);

        if (results.isEmpty()) {
            throw new NotFoundException("Class property restriction not found for : " + classURI.toString() + " - " + propertyURI.toString());
        } else if (results.size() > 1) {
            throw new NotFoundException("Multiple class property restrictions found (should never happened) for : " + classURI.toString() + " - " + propertyURI.toString());
        } else {
            UpdateBuilder modified = getLastUpdatedDateRestrictionUpdate(classURI);
            UpdateBuilder delete = getDeleteRestrictionUpdate(classURI, propertyURI);

            sparql.executeUpdateQuery(modified);
            sparql.executeDeleteQuery(delete);
        }
    }

    public void updateClassPropertyRestriction(URI classURI, OwlRestrictionModel restriction, String language) throws Exception {
        try {
            sparql.startTransaction();
            deleteClassPropertyRestriction(classURI, restriction.getOnProperty(), language);
            addClassPropertyRestriction(classURI, restriction, language);
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
        select.addFilter(SPARQLQueryHelper.langFilterWithDefault(nameField, locale.getLanguage()));
        List<SPARQLResult> results = sparql.executeSelectQuery(select);

        if (results.size() >= 1) {
            return results.get(0).getStringValue(nameField);
        } else {
            throw new NotFoundException(String.format(NO_LABEL_FOR_URI_MESSAGE, uri));
        }
    }

    /**
     * Get all parent types for each given uris.
     *
     * For an uri of type X, it finds all types Y where "X subClassOf Y",
     * regardless of the number of steps between X and Y.
     *
     * @param uris list of uris
     * @return list of all parent types for each given uris
     * @throws Exception
     */
    public List<URITypesModel> getSuperClassesByURI(List<URI> uris) throws Exception {
        List<URITypesModel> resultList = new ArrayList<>();

        if (uris.isEmpty()) {
            return resultList;
        }

        SelectBuilder select = new SelectBuilder();
        select.setDistinct(true);

        String uriField = "uri";
        String typeField = "type";
        String superClassField = "superclass";

        Var uriVar = makeVar(uriField);
        Var typeVar = makeVar(typeField);
        Var superClassVar = makeVar(superClassField);
        select.addVar(uriVar);
        select.addVar(typeVar);
        select.addVar(superClassVar);

        select.addWhere(typeVar, Ontology.subClassAny, superClassVar);
        select.addWhere(uriVar, RDF.type, typeVar);
        select.addFilter(SPARQLQueryHelper.inURIFilter(SPARQLResourceModel.URI_FIELD, uris));

        List<SPARQLResult> results = sparql.executeSelectQuery(select);
        SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);

        HashMap<URI, ArrayList<URI>> uriTypes = new HashMap<>();
        for (SPARQLResult res : results) {
            URI uri = uriDeserializer.fromString(res.getStringValue(uriField));
            URI superClassUri = uriDeserializer.fromString(res.getStringValue(superClassField));

            uriTypes.computeIfAbsent(uri,newURI -> new ArrayList<>()).add(superClassUri);
        }

        // transform (uri,types) map to URITypesModel list
        return uriTypes.entrySet()
                .stream().map(entry -> new URITypesModel(entry.getKey(),entry.getValue()))
                .collect(Collectors.toList());

    }

    public List<SPARQLNamedResourceModel> getURILabels(Collection<URI> uris, String language, URI context) throws Exception {
        List<SPARQLNamedResourceModel> resultList = new ArrayList<>();

        if (uris.size() > 0) {
            // Gracefully handle empty uris.
            // SHOULD be backward compatible, since prvious behaviour in said situation was crash
            SelectBuilder select = new SelectBuilder();
            select.setDistinct(true);

            String nameField = "name";
            String namesField = "names";
            Var nameVar = makeVar(nameField);
            ExprFactory exprFactory = select.getExprFactory();
            Aggregator groupConcat = AggregatorFactory.createGroupConcat(true, exprFactory.asExpr(nameVar), " | ", null);
            Var fieldConcatVar = makeVar(namesField);
            select.addVar(groupConcat.toString(), fieldConcatVar);

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
            select.addFilter(SPARQLQueryHelper.langFilterWithDefault(nameField, locale.getLanguage()));
            select.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
            select.addFilter(SPARQLQueryHelper.inURIFilter(SPARQLResourceModel.URI_FIELD, uris));


            List<SPARQLResult> results = sparql.executeSelectQuery(select);
            SPARQLDeserializer<URI> uriDeserializer = SPARQLDeserializers.getForClass(URI.class);

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
            }
        }
        return resultList;
    }

    public SPARQLResourceModel getRdfType(URI uri, String language) throws Exception {
        return sparql.getByURI(SPARQLResourceModel.class, uri, language);
    }

    public List<URITypesModel> checkURIsTypes(List<URI> uris, List<URI> rdfTypes) throws Exception {
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
            List<URI> types = new ArrayList<>();
            for (int i = 0; i < rdfTypes.size(); i++) {
                String index = String.valueOf(i);
                if (res.getStringValue(typeField + index) != null) {
                    URI type = new URI(res.getStringValue(typeField + index));
                    types.add(type);
                }
            }
            URITypesModel model = new URITypesModel(uri, types);
            urisTypes.add(model);

        }
        return urisTypes;
    }

    public List<URI> getSubclassRdfTypes(URI rdfType, String lang) throws Exception {

        SPARQLTreeListModel treeList = searchSubClasses(
                rdfType,
                ClassModel.class,
                null,
                lang,
                false,
                null
        );

        List<URI> rdfTypes = new ArrayList<>();
        List<ResourceTreeDTO> trees = ResourceTreeDTO.fromResourceTree(treeList);

        while (!trees.isEmpty()) {
            for (ResourceTreeDTO tree : trees) {
                rdfTypes.add(tree.getUri());
                trees = tree.getChildren();

            }
        }

        return rdfTypes;
    }

    public List<SPARQLNamedResourceModel> getByName(String targetNameOrUri) throws Exception {
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
        }

        return resultList;
    }


}
