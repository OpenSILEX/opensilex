/*******************************************************************************
 *                         AbstractOntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.VocabularyModel;
import org.opensilex.sparql.ontology.dal.*;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.JgraphtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * @author rcolin
 */
public abstract class AbstractOntologyStore implements OntologyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOntologyStore.class);

    private final SPARQLService sparql;

    private final List<String> languages;
    private static final String NO_LANG = "";

    private final Map<String, VocabularyModel<?>> modelsByUris;
    private final Graph<String, DefaultEdge> modelsGraph;
    static final int MAX_GRAPH_PATH_LENGTH = 20;

    public static final ClassModel OWL_CLASS_MODEL = getRootClassModel();

    public static final URI TOP_DATA_PROPERTY_URI = URIDeserializer.formatURI(OWL2.topDataProperty.getURI());
    public static final DatatypePropertyModel OWL_DATATYPE_PROPERTY_MODEL = getRootDataTypePropertyModel();

    public static final URI TOP_OBJECT_PROPERTY_URI = URIDeserializer.formatURI(OWL2.topObjectProperty.getURI());
    public static final ObjectPropertyModel OWL_OBJECT_PROPERTY_MODEL = getRootObjectPropertyModel();

    public static final OwlRestrictionModel OWL_ROOT_RESTRICTION_MODEL = getRootRestrictionModel();

    private static final String STORE_LOADING_MSG = "{} {} {} loaded [OK] time: {} ms {}";
    private static final String WRONG_PROPERTY_TYPE_MSG = "Property (%s) type (%s) don't match with the given type : %s . ";
    private static final String WRONG_DOMAIN_MSG = "Property (%s) domain (%s) don't match with the given domain : %s . ";
    private static final String UNKNOWN_PARENT_MSG = "URI (%s) has an unknown parent : %s ";

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    protected AbstractOntologyStore(SPARQLService sparql, OpenSilex openSilex) throws OpenSilexModuleNotFoundException {

        Objects.requireNonNull(sparql);
        this.sparql = sparql;

        // get languages from server config and append empty language code
        ServerModule serverModule = openSilex.getModuleByClass(ServerModule.class);
        ServerConfig serverConfig = serverModule.getConfig(ServerConfig.class);
        this.languages = serverConfig.availableLanguages();
        this.languages.add(NO_LANG);

        this.modelsByUris = new PatriciaTrie<>();
        this.modelsGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
    }

    private static ClassModel getRootClassModel() {
        ClassModel model = new ClassModel();
        model.setUri(SPARQLDeserializers.formatURI(URI.create(OWL2.Class.getURI())));
        model.setType(URIDeserializer.formatURI(RDFS.Class.getURI()));
        model.setTypeLabel(new SPARQLLabel(RDF.type.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        model.setLabel(new SPARQLLabel(OWL.CLASS.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        return model;
    }

    private static DatatypePropertyModel getRootDataTypePropertyModel() {
        DatatypePropertyModel model = new DatatypePropertyModel();
        model.setUri(SPARQLDeserializers.formatURI(URI.create(OWL2.DatatypeProperty.getURI())));
        model.setType(URIDeserializer.formatURI(RDFS.Class.getURI()));
        model.setTypeLabel(new SPARQLLabel(OWL2.DatatypeProperty.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        model.setLabel(new SPARQLLabel(OWL2.DatatypeProperty.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        return model;
    }

    private static ObjectPropertyModel getRootObjectPropertyModel() {
        ObjectPropertyModel model = new ObjectPropertyModel();
        model.setUri(SPARQLDeserializers.formatURI(URI.create(OWL2.ObjectProperty.getURI())));
        model.setType(URIDeserializer.formatURI(RDFS.Class.getURI()));
        model.setTypeLabel(new SPARQLLabel(OWL2.ObjectProperty.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        model.setLabel(new SPARQLLabel(OWL2.ObjectProperty.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        return model;
    }

    private static OwlRestrictionModel getRootRestrictionModel() {
        OwlRestrictionModel model = new OwlRestrictionModel();
        model.setUri(SPARQLDeserializers.formatURI(URI.create(OWL2.Restriction.getURI())));
        model.setType(URIDeserializer.formatURI(RDFS.Class.getURI()));
        model.setTypeLabel(new SPARQLLabel(OWL2.Restriction.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        return model;
    }

    public void load() throws SPARQLException {

        try {
            OntologyStoreLoader storeLoader = new OntologyStoreLoader(sparql, languages);
            clear();

            // Initial classes loading
            Instant begin = Instant.now();
            List<ClassModel> classes = storeLoader.getClasses();
            addAll(classes);
            long elapsedMs = Duration.between(begin, Instant.now()).toMillis();
            LOGGER.info(STORE_LOADING_MSG, ANSI_GREEN, classes.size(), "classes", elapsedMs, ANSI_RESET);

            // Initial properties loading
            begin = Instant.now();
            List<AbstractPropertyModel> properties = storeLoader.getProperties();
            addAll(properties);
            linkPropertiesWithClasses(properties);
            elapsedMs = Duration.between(begin, Instant.now()).toMillis();
            LOGGER.info(STORE_LOADING_MSG, ANSI_GREEN, properties.size(), "properties", elapsedMs, ANSI_RESET);

            // Initial OWL restrictions loading
            begin = Instant.now();
            List<OwlRestrictionModel> restrictions = storeLoader.getRestrictions();
            linkRestrictions(restrictions);
            elapsedMs = Duration.between(begin, Instant.now()).toMillis();
            LOGGER.info(STORE_LOADING_MSG, ANSI_GREEN, restrictions.size(), "restrictions", elapsedMs, ANSI_RESET);


        } catch (Exception e) {
            throw new SPARQLException(e);
        }

    }

    public void clear() {
        modelsByUris.clear();

        if(! modelsGraph.vertexSet().isEmpty()){
            Set<String> vertexesCopy = new HashSet<>(modelsGraph.vertexSet());
            modelsGraph.removeAllVertices(vertexesCopy);
        }
    }

    private String formatURI(URI uri) {
        return SPARQLDeserializers.formatURI(uri.toString());
    }

    public <T extends VocabularyModel<T>> void addAll(Collection<T> models) {

        Objects.requireNonNull(models);

        // compute map of Class URI <-> Class
        Map<String, T> localModelsByUri = new HashMap<>();
        for (T model : models) {
            String uri = formatURI(model.getUri());
            if (localModelsByUri.containsKey(uri)) {
                throw new IllegalArgumentException("Duplicate URI " + uri);
            }
            localModelsByUri.put(uri, model);
        }

        for (T model : models) {
            String uri = formatURI(model.getUri());
            if (modelsByUris.containsKey(uri)) {
                throw new IllegalArgumentException("URI already exist : " + uri);
            }

            if (model.getLabel() == null || MapUtils.isEmpty(model.getLabel().getTranslations())) {
                LOGGER.warn("{} {} has no label {}", ANSI_RED, uri, ANSI_RESET);
            }
            linkWithParent(localModelsByUri, model, uri);
            modelsByUris.put(uri, model);
        }
    }

    private <T extends VocabularyModel<T>> void linkWithParent(Map<String, T> localClassesByUris, T classModel, String childURI) {

        if (CollectionUtils.isEmpty(classModel.getParents())) {
            return;
        }

        Set<T> newParents = new HashSet<>(classModel.getParents().size());

        // iterate over incomplete parent and build a list of parent full-filled class parent
        for (T parentClass : classModel.getParents()) {
            String parentURI = formatURI(parentClass.getUri());
            T resolvedParent = localClassesByUris.get(parentURI);

            // try to resolve locally or from already existing parents
            if (resolvedParent == null) {

                if (!modelsByUris.containsKey(parentURI)) {
                    throw new IllegalArgumentException(String.format(UNKNOWN_PARENT_MSG, childURI, parentURI));
                }
                VocabularyModel<?> modelFromIndex = this.modelsByUris.get(parentURI);
                resolvedParent = (T) modelFromIndex;
            }

            addEdgeBetweenParentAndClass(parentURI, childURI);
            newParents.add(resolvedParent);
            resolvedParent.getChildren().add(classModel);
        }

        classModel.setParents(newParents);
        classModel.setParent(classModel.getParents().iterator().next());
    }

    void addEdgeBetweenParentAndClass(String parentURI, String classURI) {

        modelsGraph.addVertex(classURI);
        modelsGraph.addVertex(parentURI);

        if (!modelsGraph.containsEdge(parentURI, classURI)) {
            modelsGraph.addEdge(parentURI, classURI);
        }
    }

    private void linkPropertiesWithClasses(Collection<AbstractPropertyModel> properties) throws SPARQLInvalidURIException {

        for (AbstractPropertyModel<?> property : properties) {

            // replace partial domain ClassModel by full ClassModel
            ClassModel domain = property.getDomain();
            if (domain == null || domain.getUri() == null) {
                LOGGER.warn("{} NULL rdfs:domain for property {} {}", ANSI_RED, property.getUri(), ANSI_RESET);
            } else {

                ClassModel existingDomain = getClassModel(domain.getUri());
                property.setDomain(existingDomain);

                // update ClassModel data/object properties
                if(property instanceof DatatypePropertyModel){
                    existingDomain.getDatatypeProperties().put(property.getUri(), (DatatypePropertyModel) property);

                }else if(property instanceof ObjectPropertyModel){
                    existingDomain.getObjectProperties().put(property.getUri(), (ObjectPropertyModel) property);
                }
            }

            URI rangeURI = property.getRangeURI();
            if (rangeURI == null) {
                LOGGER.warn("{} NULL rdfs:range for property {} {}", ANSI_RED, property.getUri(), ANSI_RESET);
            } else if (property instanceof ObjectPropertyModel) {

                // replace partial range ClassModel by full ClassModel
                ClassModel existingRange = getClassModel(rangeURI);
                ((ObjectPropertyModel) property).setRange(existingRange);
            }
        }
    }

    private void linkDataProperty(OwlRestrictionModel restriction, ClassModel restrictedClass, DatatypePropertyModel property) {

        if (restriction.getOnDataRange() == null) {
            LOGGER.warn("{} NULL owl:onDataRange for restriction {} on property {} {}", ANSI_RED, restriction.getUri(), property.getUri(), ANSI_RESET);
        }else{
            //            restrictedClass.getDatatypeProperties().put(property.getUri(), property);
            restrictedClass.getRestrictionsByProperties().put(restriction.getOnProperty(), restriction);
        }
    }

    private void linkObjectProperty(OwlRestrictionModel restriction, ClassModel restrictedClass, ObjectPropertyModel property) throws SPARQLInvalidURIException {
        if (restriction.getOnClass() == null) {
            LOGGER.warn("{} NULL owl:onClass for restriction {} on property {} {}", ANSI_RED, restriction.getUri(), property.getUri(), ANSI_RESET);
        }else{
            // update onClass with complete ClassModel from store

            restriction.setOnClass(getClassModel(restriction.getOnClass().getUri()));
            //            restrictedClass.getObjectProperties().put(property.getUri(), property);
            restrictedClass.getRestrictionsByProperties().put(restriction.getOnProperty(), restriction);
        }
    }

    private void linkRestrictions(Collection<OwlRestrictionModel> restrictions) throws SPARQLException {

        for (OwlRestrictionModel restriction : restrictions) {

            // check that restricted class is filled and exist
            if (restriction.getDomain() == null) {
                throw new IllegalArgumentException("Null Domain URI for restriction : " + restriction.getUri());
            }
            ClassModel restrictedClass = getClassModel(restriction.getDomain());

            // check that property is filled and exist
            if (restriction.getOnProperty() == null) {
                throw new IllegalArgumentException("Null property URI for restriction : " + restriction.getUri());
            }

            AbstractPropertyModel<?> propertyModel = getProperty(restriction.getOnProperty(), null, null);
            if (propertyModel instanceof DatatypePropertyModel) {
                linkDataProperty(restriction, restrictedClass, (DatatypePropertyModel) propertyModel);
            } else if (propertyModel instanceof ObjectPropertyModel) {
                linkObjectProperty(restriction, restrictedClass, (ObjectPropertyModel) propertyModel);
            }
        }

    }

    private void addInheritedRestrictions(URI ancestorURI, ClassModel classModel) throws SPARQLInvalidURIException {

        if (ancestorURI == null) {
            return;
        }

        String classURI = classModel.getUri().toString();
        if (classURI.equals(ancestorURI.toString())) {
            return;
        }

        if (!modelsByUris.containsKey(ancestorURI.toString())) {
            throw new SPARQLInvalidURIException("Unknown ancestor " + ancestorURI + " for class " + classURI, ancestorURI);
        }

        // check if ancestor exist and if it's an ancestor of the given class
        Set<String> ancestors = JgraphtUtils.getVertexesFromAncestor(modelsGraph, ancestorURI.toString(), classURI, MAX_GRAPH_PATH_LENGTH);
        if (ancestors.isEmpty()) {
            throw new SPARQLInvalidURIException(ancestorURI + " is not a " + classURI + " parent or ancestor . ", ancestorURI);
        }

        // append inherited OWL restrictions
        for (String ancestor : ancestors) {
            ClassModel ancestorModel = (ClassModel) modelsByUris.get(ancestor);
            ancestorModel.getRestrictionsByProperties().values().forEach(ancestorRestriction ->
                    classModel.getRestrictionsByProperties().put(ancestorRestriction.getOnProperty(), ancestorRestriction)
            );
        }
    }

    private boolean isAncestor(String ancestor, String descendant) {
        if (ancestor.equals(descendant)) {
            return true;
        }
        return !JgraphtUtils.getVertexesFromAncestor(modelsGraph, ancestor, descendant, MAX_GRAPH_PATH_LENGTH).isEmpty();
    }

    private void handleLang(String lang, VocabularyModel<?> model) {

        SPARQLLabel label = model.getLabel();
        if (label.getTranslations().containsKey(lang)) {
            label.setDefaultLang(lang);
            label.setDefaultValue(label.getTranslations().get(lang));
        }
        model.setName(label.getDefaultValue());

        SPARQLLabel comment = model.getComment();
        if (comment.getTranslations().containsKey(lang)) {
            comment.setDefaultLang(lang);
            comment.setDefaultValue(comment.getTranslations().get(lang));
        }

        SPARQLLabel typeLabel = model.getTypeLabel();
        if (typeLabel.getTranslations().containsKey(lang)) {
            typeLabel.setDefaultLang(lang);
            typeLabel.setDefaultValue(typeLabel.getTranslations().get(lang));
        }
    }

    private ClassModel getClassModel(URI uri) throws SPARQLInvalidURIException {

        URI formattedURI = URIDeserializer.formatURI(uri);

        VocabularyModel<?> genericModel = modelsByUris.get(formattedURI.toString());
        if (genericModel == null) {
            throw new SPARQLInvalidURIException("owl:Class URI not found : ", formattedURI);
        }
        if (!(genericModel instanceof ClassModel)) {
            throw new SPARQLInvalidURIException("URI is not a Class URI : ", formattedURI);
        }
        return (ClassModel) genericModel;
    }

    private AbstractPropertyModel<?> getProperty(URI property, URI type, URI domain) throws SPARQLInvalidURIException {

        property = URIDeserializer.formatURI(property);
        final VocabularyModel<?> genericModel = modelsByUris.get(property.toString());

        if (genericModel == null) {
            throw new SPARQLInvalidURIException("owl:property URI not found : ", property);
        }
        if (!(AbstractPropertyModel.class.isAssignableFrom(genericModel.getClass()))) {
            throw new SPARQLInvalidURIException("URI is not a property URI : ", property);
        }

        AbstractPropertyModel<?> propertyModel = (AbstractPropertyModel<?>) genericModel;
        if (type != null && !SPARQLDeserializers.compareURIs(propertyModel.getType(), type)) {
            throw new SPARQLInvalidURIException(String.format(WRONG_PROPERTY_TYPE_MSG, property, propertyModel.getType(), type), property);
        }

        // if no domain only return property, else return an updated property according domain
        if (domain == null) {
            return propertyModel;
        }
        domain = URIDeserializer.formatURI(domain);

        // existing domain
        if (propertyModel.getDomain() != null) {
            // check if the given domain is a superclass of the property domain
            if (!isAncestor(domain.toString(), propertyModel.getDomain().getUri().toString())) {
                throw new SPARQLInvalidURIException(String.format(WRONG_DOMAIN_MSG, property, propertyModel.getDomain().getUri(), domain), property);
            }
            return propertyModel;
        }

        // check that property has a restriction with the domain class
        final ClassModel domainCLass = getClassModel(domain);
        final OwlRestrictionModel restriction = domainCLass.getRestrictionsByProperties().get(property);
        if (restriction == null) {
            return propertyModel;
        }

        // update data/object property range according domain restriction
        AbstractPropertyModel<?> propertyCopy = null;
        if (propertyModel instanceof DatatypePropertyModel) {
            DatatypePropertyModel dataProperty = new DatatypePropertyModel((DatatypePropertyModel) propertyModel, true);
            dataProperty.setRange(restriction.getOnDataRange());
            propertyCopy = dataProperty;
        } else {
            ObjectPropertyModel objectProperty = new ObjectPropertyModel((ObjectPropertyModel) propertyModel, true);
            objectProperty.setRange(restriction.getOnClass());
            propertyCopy = objectProperty;
        }

        return propertyCopy;
    }

    @Override
    public ClassModel getClassModel(URI classURI, URI ancestorURI, String lang) throws SPARQLException {

        Objects.requireNonNull(classURI);
        ClassModel model = getClassModel(classURI);

        // compute a ClassModel which take care of parent and lang
        ClassModel finalModel = new ClassModel(model);

        if (ancestorURI != null) {
            addInheritedRestrictions(SPARQLDeserializers.formatURI(ancestorURI), finalModel);
        }

        if (!StringUtils.isEmpty(lang)) {
            handleLang(lang, finalModel);
            model.visit(descendant -> handleLang(lang, descendant));
        }

        return finalModel;
    }

    @Override
    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI classURI, String stringPattern, String lang, boolean excludeRoot) throws SPARQLException {
        ClassModel classModel = getClassModel(classURI, null, lang);
        return new SPARQLTreeListModel<>(classModel, excludeRoot, true);
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String lang, boolean includeSubClasses) throws SPARQLException {

        ClassModel classModel = getClassModel(domain);

        List<DatatypePropertyModel> properties = new ArrayList<>(classModel.getDatatypeProperties().values());
        if (includeSubClasses) {
            classModel.visit(child -> properties.addAll(child.getDatatypeProperties().values()));
        }
        properties.forEach(property -> handleLang(lang, property));

        return new SPARQLTreeListModel<>(properties, TOP_DATA_PROPERTY_URI, true, true);

    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String lang, boolean includeSubClasses) throws SPARQLException {

        ClassModel classModel = getClassModel(domain, null, lang);

        List<ObjectPropertyModel> properties = new ArrayList<>(classModel.getObjectProperties().values());
        if (includeSubClasses) {
            classModel.visit(child -> properties.addAll(child.getObjectProperties().values()));
        }
        properties.forEach(property -> handleLang(lang, property));

        return new SPARQLTreeListModel<>(properties, TOP_OBJECT_PROPERTY_URI, true, true);
    }

    @Override
    public AbstractPropertyModel<?> getProperty(URI uri, URI type, URI domain, String lang) throws SPARQLException {

        AbstractPropertyModel<?> model = getProperty(uri, type, domain);

        if (!StringUtils.isEmpty(lang)) {
            handleLang(lang, model);
            model.visit(descendant -> handleLang(lang, descendant));
        }

        return model;
    }

    @Override
    public boolean classExist(URI rdfClass, URI ancestorClass) {
        String classURI = formatURI(rdfClass);

        if (!modelsByUris.containsKey(classURI)) {
            return false;
        }
        if (ancestorClass == null) {
            return true;
        }

        String ancestorURI = formatURI(ancestorClass);

        if (ancestorURI.equals(classURI)) {
            return true;
        }
        if (!modelsByUris.containsKey(ancestorURI)) {
            return false;
        }

        Set<String> ancestors = JgraphtUtils.getVertexesFromAncestor(modelsGraph, ancestorURI, classURI, MAX_GRAPH_PATH_LENGTH);
        return !ancestors.isEmpty();
    }
}
