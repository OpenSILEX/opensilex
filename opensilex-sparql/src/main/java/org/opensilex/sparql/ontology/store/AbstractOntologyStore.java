/*******************************************************************************
 *                         AbstractOntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
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

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author rcolin
 */
public abstract class AbstractOntologyStore implements OntologyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOntologyStore.class);

    private final SPARQLService sparql;
    private final OntologyDAO ontologyDAO;
    private final Map<String, VocabularyModel<?>> modelsByUris;
    private final Graph<String, DefaultEdge> modelsGraph;

    static final int MAX_GRAPH_PATH_LENGTH = 20;

    private final List<String> languages;
    protected static final String NO_LANG = "";

    public static final URI TOP_DATA_PROPERTY_URI = URIDeserializer.formatURI(OWL2.topDataProperty.getURI());
    public static final URI TOP_OBJECT_PROPERTY_URI = URIDeserializer.formatURI(OWL2.topObjectProperty.getURI());

    public static final ClassModel OWL_CLASS_MODEL = getRootClassModel();
    public static final DatatypePropertyModel OWL_DATATYPE_PROPERTY_MODEL = getRootDataTypePropertyModel();
    public static final ObjectPropertyModel OWL_OBJECT_PROPERTY_MODEL = getRootObjectPropertyModel();
    public static final OwlRestrictionModel OWL_ROOT_RESTRICTION_MODEL = getRootRestrictionModel();

    protected AbstractOntologyStore(SPARQLService sparql, OpenSilex openSilex, Map<String, VocabularyModel<?>> modelsByUris, Graph<String, DefaultEdge> modelsGraph) throws OpenSilexModuleNotFoundException {

        Objects.requireNonNull(sparql);
        Objects.requireNonNull(openSilex);
        Objects.requireNonNull(modelsByUris);
        Objects.requireNonNull(modelsGraph);

        this.sparql = sparql;
        ontologyDAO = new OntologyDAO(sparql);

        // get languages from server config and append empty language code
        ServerModule serverModule = openSilex.getModuleByClass(ServerModule.class);
        ServerConfig serverConfig = serverModule.getConfig(ServerConfig.class);
        this.languages = serverConfig.availableLanguages();
        this.languages.add(NO_LANG);

        this.modelsByUris = modelsByUris;
        this.modelsGraph = modelsGraph;
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

            String loadingMsg = "{} {} loaded [OK] time: {} ms";

            // Initial classes loading
            Instant begin = Instant.now();
            addAll(storeLoader.getClasses());
            long elapsedMs = Duration.between(begin, Instant.now()).toMillis();

            int nbInserted = modelsByUris.size();
            LOGGER.info(loadingMsg, nbInserted, "classes", elapsedMs);

            // Initial properties loading
            begin = Instant.now();
            List<AbstractPropertyModel> properties = storeLoader.getProperties();
            addAll(properties);
            linkPropertiesWithClasses(properties);
            elapsedMs = Duration.between(begin, Instant.now()).toMillis();

            nbInserted = modelsByUris.size() - nbInserted;
            LOGGER.info(loadingMsg, nbInserted, "properties", elapsedMs);

            // Initial OWL restrictions loading
            begin = Instant.now();
            List<OwlRestrictionModel> restrictions = storeLoader.getRestrictions();
            linkRestrictions(restrictions);
            elapsedMs = Duration.between(begin, Instant.now()).toMillis();
            LOGGER.info(loadingMsg, restrictions.size(), "restrictions", elapsedMs);

        } catch (Exception e) {
            throw new SPARQLException(e);
        }

    }

    public void clear() {
        modelsByUris.clear();

        if (!modelsGraph.vertexSet().isEmpty()) {
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

            linkWithParent(localModelsByUri, model, uri);
            modelsByUris.put(uri, model);
        }
    }

    private <T extends VocabularyModel<T>> void linkWithParent(Map<String, T> localClassesByUris, T classModel, String classURI) {

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
                    throw new IllegalArgumentException("Parent URI is unknown : " + parentURI);
                }
                VocabularyModel<?> modelFromIndex = this.modelsByUris.get(parentURI);
                resolvedParent = (T) modelFromIndex;
            }

            addEdgeBetweenParentAndClass(parentURI, classURI);
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
                LOGGER.warn("NULL rdfs:domain for property {}", property.getUri());
            } else {

                ClassModel existingDomain = getClassModel(domain.getUri());
                property.setDomain(existingDomain);

                // update ClassModel data/object properties
                if (property instanceof DatatypePropertyModel) {
                    existingDomain.getDatatypeProperties().put(property.getUri(), (DatatypePropertyModel) property);

                } else if (property instanceof ObjectPropertyModel) {
                    existingDomain.getObjectProperties().put(property.getUri(), (ObjectPropertyModel) property);
                }
            }

            URI rangeURI = property.getRangeURI();
            if (rangeURI == null) {
                LOGGER.warn("NULL range for property {}", property.getUri());
            } else if (property instanceof ObjectPropertyModel) {

                // replace partial range ClassModel by full ClassModel
                ClassModel existingRange = getClassModel(rangeURI);
                ((ObjectPropertyModel) property).setRange(existingRange);
            }
        }
    }

    private void linkDataProperty(OwlRestrictionModel restriction, ClassModel restrictedClass, DatatypePropertyModel property) {

        if (restriction.getOnDataRange() == null) {
            LOGGER.warn("NULL owl:onDataRange for restriction {} on property {}", restriction.getUri(), property.getUri());
        } else {
            restrictedClass.getRestrictionsByProperties().put(restriction.getOnProperty(), restriction);
        }
    }

    private void linkObjectProperty(OwlRestrictionModel restriction, ClassModel restrictedClass, ObjectPropertyModel property)  {
        if (restriction.getOnClass() == null) {
            LOGGER.warn("NULL owl:onClass for restriction {} on property {}", restriction.getUri(), property.getUri());
        } else {
            restrictedClass.getRestrictionsByProperties().put(restriction.getOnProperty(), restriction);
        }
    }

    private void linkRestrictions(Collection<OwlRestrictionModel> restrictions) throws SPARQLException {

        for (OwlRestrictionModel restriction : restrictions) {

            Objects.requireNonNull(restriction.getDomain());

            // update domainCLass with complete ClassModel from store
            ClassModel domainClass = getClassModel(restriction.getDomain().getUri());
            restriction.setDomain(domainClass);

            // update onClass with complete ClassModel from store
            URI onClass = restriction.getOnClass();
            if (onClass != null) {
                restriction.setOnClass(onClass);
            }

            URI property = restriction.getOnProperty();
            if (property == null) {
                throw new IllegalArgumentException("Null property URI for restriction : " + restriction.getUri());
            }

            AbstractPropertyModel<?> propertyModel = getProperty(restriction.getOnProperty(), null, null,null);
            if (propertyModel instanceof DatatypePropertyModel) {
                linkDataProperty(restriction, domainClass, (DatatypePropertyModel) propertyModel);
            } else if (propertyModel instanceof ObjectPropertyModel) {
                linkObjectProperty(restriction, domainClass, (ObjectPropertyModel) propertyModel);
            }
        }

    }

    @Override
    public LinkedHashSet<String> getAncestorHierarchy(URI classURI, URI ancestorUri){
        return JgraphtUtils.getVertexesFromAncestor(modelsGraph, URIDeserializer.formatURI(ancestorUri).toString(), classURI.toString(), MAX_GRAPH_PATH_LENGTH);
    }

    private void inheritFromSuperClasses(URI ancestorURI, ClassModel classModel, boolean addRestrictions, boolean addDataProperties, boolean addObjectProperties) throws SPARQLInvalidURIException {

        if (ancestorURI == null) {
            return;
        }

        String formattedAncestorURI = URIDeserializer.formatURI(ancestorURI).toString();
        String classURI = classModel.getUri().toString();
        if (classURI.equals(formattedAncestorURI)) {
            return;
        }

        if (!modelsByUris.containsKey(formattedAncestorURI)) {
            throw new SPARQLInvalidURIException("Unknown ancestor " + ancestorURI + " for class " + classURI, ancestorURI);
        }

        // check if ancestor exist and if it's an ancestor of the given class
        Set<String> ancestors = JgraphtUtils.getVertexesFromAncestor(modelsGraph, formattedAncestorURI, classURI, MAX_GRAPH_PATH_LENGTH);
        if (ancestors.isEmpty()) {
            throw new SPARQLInvalidURIException(ancestorURI + " is not a " + classURI + " parent or ancestor . ", ancestorURI);
        }

        // append inherited OWL restrictions
        for (String ancestor : ancestors) {
            ClassModel ancestorModel = (ClassModel) modelsByUris.get(ancestor);

            // add inherited data/object properties
            if(addDataProperties){
                ancestorModel.getDatatypeProperties().values().forEach(property -> {
                    classModel.getDatatypeProperties().put(property.getUri(),property);
                });
            }
            if(addObjectProperties){
                ancestorModel.getObjectProperties().values().forEach(property -> {
                    classModel.getObjectProperties().put(property.getUri(),property);
                });
            }

            // add inherited restrictions
            if(addRestrictions){
                ancestorModel.getRestrictionsByProperties().values().forEach(ancestorRestriction ->
                        classModel.getRestrictionsByProperties().put(ancestorRestriction.getOnProperty(), ancestorRestriction)
                );
            }

        }
    }

    private void handleLang(String lang, VocabularyModel<?> model) {

        if (StringUtils.isEmpty(lang)) {
            return;
        }

        SPARQLLabel label = model.getLabel();
        if (label.getTranslations().containsKey(lang)) {
            label.setDefaultLang(lang);
            label.setDefaultValue(label.getTranslations().get(lang));
        }

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


    @Override
    public ClassModel getClassModel(URI classURI, URI ancestorURI, String lang) throws SPARQLException {

        Objects.requireNonNull(classURI);
        ClassModel model = getClassModel(classURI);

        // compute a ClassModel which take care of parent and lang
        ClassModel finalModel = new ClassModel(model);

        inheritFromSuperClasses(ancestorURI, finalModel,true,true,true);
        handleLang(lang, finalModel);
        model.visit(descendant -> handleLang(lang, descendant));
        return finalModel;
    }

    @Override
    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI classURI, String namePattern, String lang, boolean excludeRoot) throws SPARQLException {
        if(! StringUtils.isEmpty(namePattern)){
            return new NoOntologyStore(ontologyDAO).searchSubClasses(classURI, namePattern, lang, excludeRoot);
        }
        ClassModel classModel = getClassModel(classURI, null, lang);
        return new SPARQLTreeListModel<>(classModel, excludeRoot, true);
    }

    private AbstractPropertyModel<?> getProperty(URI uri,URI domain) throws SPARQLInvalidURIException {

        URI formattedURI = URIDeserializer.formatURI(uri);

        VocabularyModel<?> genericModel = modelsByUris.get(formattedURI.toString());
        if (genericModel == null) {
            throw new SPARQLInvalidURIException("owl:property URI not found : ", formattedURI);
        }

        if (!(AbstractPropertyModel.class.isAssignableFrom(genericModel.getClass()))) {
            throw new SPARQLInvalidURIException("URI is not a property URI : ", formattedURI);
        }

//        if (type != null && !SPARQLDeserializers.compareURIs(model.getType(), type)) {
//            throw new SPARQLInvalidURIException(String.format("Property (%s) type (%s) don't match with the given type : %s. ", uri, model.getType(), type), uri);
//        }

        return (AbstractPropertyModel<?>) genericModel;
    }

    private <PT extends AbstractPropertyModel<PT>> Set<PT> computeProperties(
            boolean root,
            Set<PT> accumulator,
            ClassModel classModel,
            Function<ClassModel, Stream<PT>> function,
            boolean includeSubClasses,
            BiPredicate<PT,ClassModel> filter,
            String lang) {

        if(root){
            accumulator = new HashSet<>();
        }
        if (filter == null) {
            function.apply(classModel).forEach(accumulator::add);
        } else {
            function.apply(classModel)
                    .filter(property -> filter.test(property,classModel))
                    .forEach(accumulator::add);
        }

        if (includeSubClasses) {
            for(ClassModel child : classModel.getChildren()){
                computeProperties(false, accumulator, child, function, includeSubClasses, filter, lang);
            }
        }
        if(root){
            accumulator.forEach(property -> handleLang(lang, property));
        }
        return accumulator;
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String namePattern, String lang, boolean includeSubClasses, BiPredicate<DatatypePropertyModel,ClassModel> filter) throws SPARQLException {

        if (!StringUtils.isEmpty(namePattern)) {
            return new NoOntologyStore(ontologyDAO).searchDataProperties(domain, namePattern, lang, includeSubClasses, filter);
        }

        Set<DatatypePropertyModel> properties = computeProperties(
                true,
                new HashSet<>(),
                getClassModel(domain),
                (classModel -> classModel.getDatatypeProperties().values().stream()),
                includeSubClasses,
                filter,
                lang
        );
        return new SPARQLTreeListModel<>(properties, TOP_DATA_PROPERTY_URI, true, true);

    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String namePattern, String lang, boolean includeSubClasses,  BiPredicate<ObjectPropertyModel,ClassModel> filter) throws SPARQLException {

        if (!StringUtils.isEmpty(namePattern)) {
            return new NoOntologyStore(ontologyDAO).searchObjectProperties(domain, namePattern, lang, includeSubClasses, filter);
        }

        Set<ObjectPropertyModel> properties = computeProperties(
                true,
                new HashSet<>(),
                getClassModel(domain),
                (classModel -> classModel.getObjectProperties().values().stream()),
                includeSubClasses,
                filter,
                lang
        );
        return new SPARQLTreeListModel<>(properties, TOP_OBJECT_PROPERTY_URI, true, true);
    }

    @Override
    public Set<DatatypePropertyModel> getLinkableDataProperties(URI domain, URI ancestor, String lang) throws SPARQLException {

        // compute set of inherited data properties and inherited properties
        ClassModel domainClass = new ClassModel(getClassModel(domain));
        if(ancestor != null){
            inheritFromSuperClasses(ancestor, domainClass, true, true, false);
        }

        // return properties on domain MINUS properties already associated to domain with a restriction
        BiPredicate<DatatypePropertyModel, ClassModel> filter = (property, classModel) ->
                property.getRange() != null &&
                ! classModel.getRestrictionsByProperties().containsKey(property.getUri());

        return computeProperties(
                true,
                new HashSet<>(),
                domainClass,
                (classModel -> classModel.getDatatypeProperties().values().stream()),
                false, // don't include subclasses, since theirs properties are too specific
                filter,
                lang
        );
    }

    @Override
    public Set<ObjectPropertyModel> getLinkableObjectProperties(URI domain, URI ancestor, String lang) throws SPARQLException {

        // compute set of inherited object properties
        ClassModel domainClass = new ClassModel(getClassModel(domain));
        if(ancestor != null){
            inheritFromSuperClasses(ancestor, domainClass, true, false, true);
        }

        // return properties on domain MINUS properties already associated to domain with a restriction
        BiPredicate<ObjectPropertyModel,ClassModel> filter = (property,classModel) ->
                property.getRange() != null &&
                ! classModel.getRestrictionsByProperties().containsKey(property.getUri());

        return computeProperties(
                true,
                new HashSet<>(),
                domainClass,
                (classModel -> classModel.getObjectProperties().values().stream()),
                false, // don't include subclasses, since theirs properties are too specific
                filter,
                lang
        );
    }


    @Override
    public AbstractPropertyModel<?> getProperty(URI propertyURI, URI type, URI domain, String lang) throws SPARQLException {

        AbstractPropertyModel<?> model = getProperty(propertyURI, domain);
        handleLang(lang, model);
        model.visit(descendant -> handleLang(lang, descendant));

        return model;
    }

    @Override
    public DatatypePropertyModel getDataProperty(URI property, URI domain, String lang) throws SPARQLException {
        return (DatatypePropertyModel) getProperty(property,OWL_DATATYPE_PROPERTY_MODEL.getUri(),domain,lang);
    }

    @Override
    public ObjectPropertyModel getObjectProperty(URI property, URI domain, String lang) throws SPARQLException {
        return (ObjectPropertyModel) getProperty(property,OWL_OBJECT_PROPERTY_MODEL.getUri(),domain,lang);
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
