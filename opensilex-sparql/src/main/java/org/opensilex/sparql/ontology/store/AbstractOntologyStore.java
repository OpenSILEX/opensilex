/*******************************************************************************
 *                         AbstractOntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;


import org.apache.commons.lang3.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractOntologyStore implements OntologyStore {

    static final int MAX_GRAPH_PATH_LENGTH = 20;

    private Map<String,ClassModel> classesByUris;
    private final Graph<String, DefaultEdge> classesGraph;

    protected AbstractOntologyStore(Map<String,ClassModel> classesByUris>) {
        this.classesGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
        this.classesByUris = classesByUris;
    }

    private String getFormattedURI(URI uri){
        return SPARQLDeserializers.formatURI(uri.toString());
    }

    public void addClass(ClassModel classModel){
        Objects.requireNonNull(classModel);

        String classURI = getFormattedURI(classModel.getUri());
        ClassModel oldModel = classesByUris.get(classURI);
        if(oldModel != null){
            throw new IllegalArgumentException("Class already exist");
        }

        classesByUris.put(classURI,classModel);
    }

    @Override
    public ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException {

        Objects.requireNonNull(rdfClass);

        String classURI = getFormattedURI(rdfClass);
        ClassModel baseClassModel = classesByUris.get(classURI);
        if(baseClassModel == null){
            return null;
        }

        // compute a ClassModel which take care of parentClass and lang
        ClassModel finalClassModel = new ClassModel(baseClassModel,true);

        if(parentClass != null){
            handleParentRestrictions(classURI,parentClass,finalClassModel);
        }
        if(!StringUtils.isEmpty(lang)){
            handleLang(lang,finalClassModel);
        }

        return finalClassModel;
    }

    private void handleParentRestrictions(String classURI, URI parentClass, ClassModel finalClassModel){

        String parentURI = getFormattedURI(parentClass);
        ClassModel parentClassModel = classesByUris.get(parentURI);
        if(parentClassModel == null){
            throw new IllegalArgumentException("Unknown parent");
        }

        Set<String> ancestors = getVertexesFromAncestor(classURI,parentURI);
        if(ancestors.isEmpty()){
            throw new IllegalArgumentException(parentURI+" is not a "+classURI+" parent or ancestor");
        }

        for(String ancestor : ancestors){
            ClassModel ancestorModel = classesByUris.get(ancestor);
            ancestorModel.getRestrictions().values().forEach(ancestorRestriction ->
                    finalClassModel.getRestrictions().put(ancestorRestriction.getUri(), ancestorRestriction)
            );
        }
    }

    private void handleLang(String lang, ClassModel classModel){

    }

    void addEdgeBetweenParentAndClass(String parentURI, String classURI) {
        classesGraph.addVertex(parentURI);
        classesGraph.addVertex(classURI);
        classesGraph.addEdge(parentURI, classURI);
    }

    Set<String> getVertexesFromAncestor(String classURI, String parentURI) {

        AllDirectedPaths<String, DefaultEdge> allPaths = new AllDirectedPaths<>(classesGraph);
        List<GraphPath<String, DefaultEdge>> pathList = allPaths.getAllPaths(parentURI, classURI, true, MAX_GRAPH_PATH_LENGTH);

        if (pathList.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> ancestors = new HashSet<>();
        for (GraphPath<String, DefaultEdge> path : pathList) {
            ancestors.addAll(path.getVertexList());
        }
        ancestors.remove(classURI);
        return ancestors;
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
