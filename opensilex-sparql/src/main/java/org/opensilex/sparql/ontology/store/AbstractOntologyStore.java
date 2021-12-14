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
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
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
import java.util.function.Consumer;

/**
 * @author rcolin
 */
public abstract class AbstractOntologyStore implements OntologyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOntologyStore.class);

    static final int MAX_GRAPH_PATH_LENGTH = 20;

    private final List<String> languages;
    private final Map<String, ClassModel> classesByUris;
    private final Graph<String, DefaultEdge> classesGraph;
    private final SPARQLService sparql;

    static final URI OWL_CLASS_URI = SPARQLDeserializers.formatURI(URI.create(OWL2.Class.getURI()));

    private static final String CLASSES_LOADED_INFO_MSG = "{} classes loaded [OK] time: {} ms";

    protected AbstractOntologyStore(SPARQLService sparql, List<String> languages){

        Objects.requireNonNull(sparql);
        if(CollectionUtils.isEmpty(languages)){
            throw new IllegalArgumentException("languages is null or empty");
        }

        this.sparql = sparql;
        this.languages = languages;
        this.classesByUris = new PatriciaTrie<>();

        this.classesGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
        this.classesGraph.addVertex(OWL_CLASS_URI.toString());
    }

    public void load() throws SPARQLException {
        // Initial classes loading
        Instant begin = Instant.now();
        OntologyStoreLoader storeLoader = new OntologyStoreLoader(sparql, this, languages);
        storeLoader.load();
        long elapsedMs = Duration.between(begin,Instant.now()).toMillis();
        LOGGER.info(CLASSES_LOADED_INFO_MSG,classesByUris.size(),elapsedMs);
    }

    public void clear(){
        classesByUris.clear();
        Set<String> vertexesCopy = new HashSet<>(classesGraph.vertexSet());
        classesGraph.removeAllVertices(vertexesCopy);
    }

    private String getFormattedURI(URI uri) {
        return SPARQLDeserializers.formatURI(uri.toString());
    }

    public void addAll(List<ClassModel> classes) {

        Objects.requireNonNull(classes);

        // compute map of Class URI <-> Class
        Map<String, ClassModel> localClassesByUris = new HashMap<>();
        for (ClassModel classModel : classes) {
            String classURI = getFormattedURI(classModel.getUri());
            if (localClassesByUris.containsKey(classURI)) {
                throw new IllegalArgumentException("Duplicate URI " + classURI);
            }
            localClassesByUris.put(classURI, classModel);
        }

        for (ClassModel classModel : classes) {
            String classURI = getFormattedURI(classModel.getUri());
            if (classesByUris.containsKey(classURI)) {
                throw new IllegalArgumentException("Class already exist");
            }

            resolveParentAndUpdateClasses(localClassesByUris, classModel, classURI);
            classesByUris.put(classURI, classModel);
        }
    }

    private void resolveParentAndUpdateClasses(Map<String, ClassModel> localClassesByUris, ClassModel classModel, String classURI) {

        if (classModel.getParents() == null) {
            return;
        }

        List<ClassModel> newParents = new ArrayList<>(classModel.getParents().size());

        // iterate over incomplete parent and build a list of parent full-filled class parent
        for (ClassModel parentClass : classModel.getParents()) {
            String parentURI = getFormattedURI(parentClass.getUri());
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
    }


    @Override
    public ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException {

        Objects.requireNonNull(rdfClass);

        String classURI = getFormattedURI(rdfClass);
        ClassModel baseClassModel = classesByUris.get(classURI);
        if (baseClassModel == null) {
            return null;
        }

        // compute a ClassModel which take care of parent and lang
        ClassModel finalClassModel = new ClassModel(baseClassModel);

        if (parentClass != null) {
            addRestrictionsInheritedFromParent(classURI, parentClass, finalClassModel);
        }
        if (!StringUtils.isEmpty(lang)) {
            handleLang(lang, finalClassModel);
        }

        return finalClassModel;
    }

    private void addRestrictionsInheritedFromParent(String classURI, URI parentClass, ClassModel finalClassModel) {

        String parentURI = getFormattedURI(parentClass);
        ClassModel parentClassModel = classesByUris.get(parentURI);
        if (parentClassModel == null) {
            throw new IllegalArgumentException("Unknown parent");
        }

        Set<String> ancestors = JgraphtUtils.getVertexesFromAncestor(classesGraph, parentURI, classURI, MAX_GRAPH_PATH_LENGTH);
        if (ancestors.isEmpty()) {
            throw new IllegalArgumentException(parentURI + " is not a " + classURI + " parent or ancestor");
        }

        for (String ancestor : ancestors) {
            ClassModel ancestorModel = classesByUris.get(ancestor);
            ancestorModel.getRestrictions().values().forEach(ancestorRestriction ->
                    finalClassModel.getRestrictions().put(ancestorRestriction.getUri(), ancestorRestriction)
            );
        }
    }

    private void handleLang(String lang, ClassModel classModel) {

    }


    void addEdgeBetweenParentAndClass(String parentURI, String classURI) {

        classesGraph.addVertex(classURI);
        classesGraph.addVertex(parentURI);

        if (!classesGraph.containsEdge(parentURI, classURI)) {
            classesGraph.addEdge(parentURI, classURI);
        }
    }

    @Override
    public <T extends SPARQLTreeModel<T>> SPARQLTreeListModel<T> searchSubClasses(URI parent, Class<T> clazz, String stringPattern, String lang, boolean excludeRoot, Consumer<T> handler) throws SPARQLException {
        return null;
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
