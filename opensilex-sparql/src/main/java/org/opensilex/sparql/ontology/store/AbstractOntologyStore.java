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
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;
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

    private final Map<String, ClassModel> classesByUris;

    private final Graph<String, DefaultEdge> classesGraph;
    static final int MAX_GRAPH_PATH_LENGTH = 20;

    public static final URI OWL_CLASS_URI = SPARQLDeserializers.formatURI(URI.create(OWL2.Class.getURI()));
    public final ClassModel OWL_CLASS_MODEL;
    private static final String CLASSES_LOADED_INFO_MSG = "{} classes loaded [OK] time: {} ms";

    protected AbstractOntologyStore(SPARQLService sparql, OpenSilex openSilex) throws OpenSilexModuleNotFoundException {

        Objects.requireNonNull(sparql);
        this.sparql = sparql;

        // get languages from server config and append empty language code
        ServerModule serverModule = openSilex.getModuleByClass(ServerModule.class);
        ServerConfig serverConfig = serverModule.getConfig(ServerConfig.class);
        this.languages = serverConfig.availableLanguages();
        this.languages.add(NO_LANG);

        this.classesByUris = new PatriciaTrie<>();
        this.classesGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

        OWL_CLASS_MODEL = new ClassModel();
        OWL_CLASS_MODEL.setUri(OWL_CLASS_URI);
        OWL_CLASS_MODEL.setType(URIDeserializer.formatURI(RDFS.Class.getURI()));
        OWL_CLASS_MODEL.setTypeLabel(new SPARQLLabel(RDF.type.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
        OWL_CLASS_MODEL.setLabel(new SPARQLLabel(OWL.CLASS.getLocalName(), OpenSilex.DEFAULT_LANGUAGE));
//
//        this.classesByUris.put(OWL_CLASS_URI.toString(), OWL_CLASS_MODEL);
//        this.classesGraph.addVertex(OWL_CLASS_URI.toString());

    }

    public void load() throws SPARQLException {

        // Initial classes loading
        Instant begin = Instant.now();
        OntologyStoreLoader storeLoader = new OntologyStoreLoader(sparql, this, languages);
        storeLoader.loadClasses();
        long elapsedMs = Duration.between(begin, Instant.now()).toMillis();
        LOGGER.info(CLASSES_LOADED_INFO_MSG, classesByUris.size(), elapsedMs);

        storeLoader.loadProperties();
    }

    public void clear() {
        classesByUris.clear();
        Set<String> vertexesCopy = new HashSet<>(classesGraph.vertexSet());
        classesGraph.removeAllVertices(vertexesCopy);
    }

    private String formatURI(URI uri) {
        return SPARQLDeserializers.formatURI(uri.toString());
    }

    public void addAll(List<ClassModel> classes) {

        Objects.requireNonNull(classes);

        // compute map of Class URI <-> Class
        Map<String, ClassModel> localClassesByUris = new HashMap<>();
        for (ClassModel classModel : classes) {
            String classURI = formatURI(classModel.getUri());
            if (localClassesByUris.containsKey(classURI)) {
                throw new IllegalArgumentException("Duplicate URI " + classURI);
            }
            localClassesByUris.put(classURI, classModel);
        }

        for (ClassModel classModel : classes) {
            String classURI = formatURI(classModel.getUri());
            if (classesByUris.containsKey(classURI)) {
                throw new IllegalArgumentException("Class already exist");
            }

            resolveParentAndUpdateClasses(localClassesByUris, classModel, classURI);
            classesByUris.put(classURI, classModel);
//            OWL_CLASS_MODEL.getChildren().add(classModel);
        }
    }

    private void resolveParentAndUpdateClasses(Map<String, ClassModel> localClassesByUris, ClassModel classModel, String classURI) {

        if (CollectionUtils.isEmpty(classModel.getParents())) {
            return;
        }

        Set<ClassModel> newParents = new HashSet<>(classModel.getParents().size());

        // iterate over incomplete parent and build a list of parent full-filled class parent
        for (ClassModel parentClass : classModel.getParents()) {
            String parentURI = formatURI(parentClass.getUri());
            ClassModel resolvedParent = localClassesByUris.get(parentURI);

            // try to resolve locally or from already existing parents
            if (resolvedParent == null) {
                resolvedParent = classesByUris.get(parentURI);
                if (resolvedParent == null) {
                    throw new IllegalArgumentException("Parent URI is unknown");
                }
            }

            addEdgeBetweenParentAndClass(parentURI, classURI);
            newParents.add(resolvedParent);
            resolvedParent.getChildren().add(classModel);
        }

        classModel.setParents(newParents);
        classModel.setParent(classModel.getParents().iterator().next());
    }


    @Override
    public ClassModel getClassModel(URI classURI, URI ancestorURI, String lang) throws SPARQLException {

        Objects.requireNonNull(classURI);
        classURI = URIDeserializer.formatURI(classURI);

        ClassModel model = classesByUris.get(classURI.toString());
        if (model == null) {
            throw new SPARQLInvalidURIException("owl:Class URI not found : ", classURI);
        }

        // compute a ClassModel which take care of parent and lang
        ClassModel finalModel = new ClassModel(model);

        if(ancestorURI != null){
            handleAncestor(ancestorURI, finalModel);
        }
        if (! StringUtils.isEmpty(lang)) {
            handleLang(lang, finalModel);
            finalModel.visit(descendant -> handleLang(lang,descendant));
        }

        return finalModel;
    }

    private void handleAncestor(URI ancestorURI, ClassModel classModel) throws SPARQLInvalidURIException {

        ancestorURI = URIDeserializer.formatURI(ancestorURI);
        String classURI = classModel.getUri().toString();

        // check if ancestor exist and if it's an ancestor of the given class
        ClassModel ancestorClass = classesByUris.get(ancestorURI.toString());
        if (ancestorClass == null) {
            throw new SPARQLInvalidURIException("Unknown ancestor " + ancestorURI + " for class " + classURI, ancestorURI);
        }
        Set<String> ancestors = JgraphtUtils.getVertexesFromAncestor(classesGraph, ancestorURI.toString(), classURI, MAX_GRAPH_PATH_LENGTH);
        if (ancestors.isEmpty()) {
            throw new SPARQLInvalidURIException(ancestorURI.toString() + " is not a " + classURI + " parent or ancestor . ", ancestorURI);
        }

        // append inherited OWL restrictions
        for (String ancestor : ancestors) {
            ClassModel ancestorModel = classesByUris.get(ancestor);
            ancestorModel.getRestrictions().values().forEach(ancestorRestriction ->
                    classModel.getRestrictions().put(ancestorRestriction.getUri(), ancestorRestriction)
            );
        }

    }

    private void handleLang(String lang, ClassModel classModel) {

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

        classesGraph.addVertex(classURI);
        classesGraph.addVertex(parentURI);

        if (!classesGraph.containsEdge(parentURI, classURI)) {
            classesGraph.addEdge(parentURI, classURI);
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
