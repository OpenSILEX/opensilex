/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import org.apache.commons.lang3.tuple.Pair;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author vince
 * Class used for representing a Tree of any {@link SPARQLTreeModel} with a bidirectionnal-indexing between model and model parent.
 */
public class SPARQLTreeListModel<T extends SPARQLTreeModel<T>> {

    private final Map<Pair<URI, String>, List<T>> modelsByParent = new HashMap<>();
    private final Map<Pair<URI, String>, T> parentByModel = new HashMap<>();
    private final List<Pair<URI, String>> selectionList;
    private final URI root;
    private final boolean excludeRoot;

    public SPARQLTreeListModel() {
        this(new ArrayList<>(), null, false);
    }

    public SPARQLTreeListModel(Collection<T> selectionList, URI root, boolean excludeRoot, boolean addSelectionToTree) {
        this.selectionList = new ArrayList<>(selectionList.size());
        for (T instance : selectionList) {
            URI formattedUri = SPARQLDeserializers.formatURI(instance.getUri());
            this.selectionList.add(Pair.of(formattedUri, instance.getGraph()));
            this.parentByModel.put(Pair.of(formattedUri, instance.getGraph()), instance.getParent());
        }
        this.root = root != null ? SPARQLDeserializers.formatURI(root) : null;
        this.excludeRoot = excludeRoot;

        if (addSelectionToTree) {
            selectionList.forEach(this::addTree);
        }
    }

    public SPARQLTreeListModel(Collection<T> selectionList, URI root, boolean excludeRoot) {
        this(selectionList, root, excludeRoot, false);
    }

    public SPARQLTreeListModel(T rootModel, boolean excludeRoot, boolean addSelectionToTree) {
        this(rootModel.getNodes(false), rootModel.getUri(), excludeRoot, addSelectionToTree);

        if(! excludeRoot && getRootsCount() == 0){
            addTree(rootModel);
        }
    }

    public void listRoots(Consumer<T> handler) {
        this.listChildren(null, handler);
    }

    public int getRootsCount() {
        return this.getChildCount(null);
    }

    private List<T> getChildren(T parent) {
        URI parentURI = null;
        String parentGRAPH = null;
        if (parent != null) {
            parentURI = SPARQLDeserializers.formatURI(parent.getUri());
            parentGRAPH = parent.getGraph();
        }
        return this.modelsByParent.get(Pair.of(parentURI, parentGRAPH));
    }

    public int getChildCount(T parent) {
        List<T> children = getChildren(parent);
        return children == null ? 0 : children.size();
    }

    public void listChildren(T parent, Consumer<T> handler) {
        List<T> children = getChildren(parent);
        if (children != null) {
            children.forEach(handler);
        }
    }

    public T getParent(T instance) {
        return parentByModel.get(Pair.of(SPARQLDeserializers.formatURI(instance.getUri()), instance.getGraph()));
    }

    public void addTree(T instance) {
        T parent = getParent(instance);
        addTreeWithParent(instance, parent);
    }

    public void addTreeWithParent(T instance, T parent) {

        URI formattedUri = SPARQLDeserializers.formatURI(instance.getUri());
        if (parent == null || formattedUri.equals(root)) {
            if (parent == null || !excludeRoot) {
                //modelsByParent.computeIfAbsent(null, key -> new HashSet<>());
                modelsByParent.computeIfAbsent(Pair.of(null, null), key -> new ArrayList<>());
                addInMapIfExists(Pair.of(null, null), instance);
            }
        } else {
            URI parentURI = SPARQLDeserializers.formatURI(parent.getUri());
            if (parentURI.equals(root) && excludeRoot) {
                parentURI = null;
            }
            if (!modelsByParent.containsKey(Pair.of(parentURI, parent.getGraph()))) {
                if (parentURI != null && parent.getGraph() != null) {
                    addTree(parent);
                }
                modelsByParent.put(Pair.of(parentURI, parent.getGraph()), new ArrayList<>());
            }
            addInMapIfExists(Pair.of(parentURI, parent.getGraph()), instance);
        }

    }

    private void addInMapIfExists(Pair<URI, String> parent, T instance) {
        boolean exists = false;
        for (T item : modelsByParent.get(parent)) {
            if (SPARQLDeserializers.compareURIs(instance.getUri(), item.getUri()) && instance.getGraph().equals(item.getGraph())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            modelsByParent.get(parent).add(instance);
            parentByModel.put(Pair.of(SPARQLDeserializers.formatURI(instance.getUri()), instance.getGraph()), instance.getParent());
        }
    }

    public boolean isSelected(T instance) {
        return this.selectionList.contains(Pair.of(instance.getUri(), instance.getGraph()));
    }

    public void traverse(Consumer<T> handler) {
        listRoots(instance ->
                traverseNode(instance, handler)
        );
    }

    private void traverseNode(T instance, Consumer<T> handler) {
        handler.accept(instance);
        listChildren(instance, child ->
                traverseNode(child, handler)
        );
    }
}
