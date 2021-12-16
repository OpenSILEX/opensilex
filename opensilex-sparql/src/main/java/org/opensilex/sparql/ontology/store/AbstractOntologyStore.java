/*******************************************************************************
 *                         AbstractOntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.commons.collections4.CollectionUtils;
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
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;
import org.opensilex.sparql.ontology.dal.PropertyModel;
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

    public static final URI OWL_CLASS_URI = SPARQLDeserializers.formatURI(URI.create(OWL2.Class.getURI()));
    public final ClassModel OWL_CLASS_MODEL;

    private static final String STORE_LOADING_MSG = "{} {} loaded [OK] time: {} ms";

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

        OWL_CLASS_MODEL = new ClassModel();
        OWL_CLASS_MODEL.setUri(OWL_CLASS_URI);
        OWL_CLASS_MODEL.setType(URIDeserializer.formatURI(RDFS.Class.getURI()));
        OWL_CLASS_MODEL.setTypeLabel(new SPARQLLabel(RDF.type.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        OWL_CLASS_MODEL.setLabel(new SPARQLLabel(OWL.CLASS.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
    }

    public void load() throws SPARQLException {

        OntologyStoreLoader storeLoader = new OntologyStoreLoader(sparql, this, languages);

        // Initial classes loading
        Instant begin = Instant.now();
        List<ClassModel> classes = storeLoader.getClasses();
        addAll(classes);
        long elapsedMs = Duration.between(begin, Instant.now()).toMillis();
        LOGGER.info(STORE_LOADING_MSG, classes.size(), "classes", elapsedMs);


        // Initial properties loading
        begin = Instant.now();
        List<PropertyModel> properties = storeLoader.getProperties();
        addAll(properties);
        elapsedMs = Duration.between(begin, Instant.now()).toMillis();
        LOGGER.info(STORE_LOADING_MSG, properties.size(), "properties", elapsedMs);
    }

    public void clear() {
        modelsByUris.clear();
        Set<String> vertexesCopy = new HashSet<>(modelsGraph.vertexSet());
        modelsGraph.removeAllVertices(vertexesCopy);
    }

    private String formatURI(URI uri) {
        return SPARQLDeserializers.formatURI(uri.toString());
    }

    public <T extends VocabularyModel<T>> void addAll(List<T> classes) {

        Objects.requireNonNull(classes);

        // compute map of Class URI <-> Class
        Map<String, T> localClassesByUris = new HashMap<>();
        for (T classModel : classes) {
            String classURI = formatURI(classModel.getUri());
            if (localClassesByUris.containsKey(classURI)) {
                throw new IllegalArgumentException("Duplicate URI " + classURI);
            }
            localClassesByUris.put(classURI, classModel);
        }

        for (T classModel : classes) {
            String classURI = formatURI(classModel.getUri());
            if (modelsByUris.containsKey(classURI)) {
                throw new IllegalArgumentException("URI already exist : "+classURI);
            }

            resolveParentAndUpdateClasses(localClassesByUris, classModel, classURI);
            modelsByUris.put(classURI,classModel);
//            classesByUris.put(classURI, classModel);
//            OWL_CLASS_MODEL.getChildren().add(classModel);
        }
    }

    private <T extends VocabularyModel<T>> void resolveParentAndUpdateClasses(Map<String,T> localClassesByUris, T classModel, String classURI) {

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

                if(! modelsByUris.containsKey(parentURI)){
                    throw new IllegalArgumentException("Parent URI is unknown : "+parentURI);
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

    private ClassModel getClassModel(URI classURI) throws SPARQLInvalidURIException {

        URI formattedURI = URIDeserializer.formatURI(classURI);

        VocabularyModel<?> genericModel = modelsByUris.get(formattedURI.toString());
        if (genericModel == null) {
            throw new SPARQLInvalidURIException("owl:Class URI not found : ", formattedURI);
        }
        if(! (genericModel instanceof ClassModel)){
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

        if (ancestorURI != null) {
            addInheritedRestrictions(ancestorURI, finalModel);
        }
        if (!StringUtils.isEmpty(lang)) {
            handleLang(lang, finalModel);
            finalModel.visit(descendant -> handleLang(lang, descendant));
        }

        return finalModel;
    }

    private void addInheritedRestrictions(URI ancestorURI, ClassModel classModel) throws SPARQLInvalidURIException {

        String formattedAncestorURI = URIDeserializer.formatURI(ancestorURI).toString();
        String classURI = classModel.getUri().toString();

        if(! modelsByUris.containsKey(formattedAncestorURI)){
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
            ancestorModel.getRestrictions().values().forEach(ancestorRestriction ->
                    classModel.getRestrictions().put(ancestorRestriction.getUri(), ancestorRestriction)
            );
        }
    }

    private void handleLang(String lang, VocabularyModel<?> classModel) {

        SPARQLLabel label = classModel.getLabel();
        if (label.getTranslations().containsKey(lang)) {
            label.setDefaultLang(lang);
            label.setDefaultValue(label.getTranslations().get(lang));
        }

        SPARQLLabel comment = classModel.getComment();
        if (comment.getTranslations().containsKey(lang)) {
            comment.setDefaultLang(lang);
            comment.setDefaultValue(comment.getTranslations().get(lang));
        }

        SPARQLLabel typeLabel = classModel.getTypeLabel();
        if (typeLabel.getTranslations().containsKey(lang)) {
            typeLabel.setDefaultLang(lang);
            typeLabel.setDefaultValue(typeLabel.getTranslations().get(lang));
        }
    }


    void addEdgeBetweenParentAndClass(String parentURI, String classURI) {

        modelsGraph.addVertex(classURI);
        modelsGraph.addVertex(parentURI);

        if (!modelsGraph.containsEdge(parentURI, classURI)) {
            modelsGraph.addEdge(parentURI, classURI);
        }
    }

    @Override
    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI classURI, String stringPattern, String lang, boolean excludeRoot) throws SPARQLException {
        ClassModel classModel = getClassModel(classURI, null, lang);
        if (classModel == null) {
            return null;
        }
        return new SPARQLTreeListModel<>(classModel, excludeRoot, true);
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String lang) throws SPARQLException {
        return null;
    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String lang) throws SPARQLException {
        return null;
    }
}
