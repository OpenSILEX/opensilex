/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vince
 */
public class SPARQLTreeListModel<T extends SPARQLTreeModel<T>> {

    private final HashMap<URI, List<T>> map = new HashMap<URI, List<T>>();

    private final List<URI> selectionList;
    private final URI root;
    private final boolean excludeRoot;

    private final HashMap<String, T> itemParents = new HashMap<>();

    public SPARQLTreeListModel() {
        this(new ArrayList<>(), null, false);
    }

    public SPARQLTreeListModel(List<T> selectionList, URI root, boolean excludeRoot) {
        this.selectionList = new ArrayList<>(selectionList.size());
        for (T instance : selectionList) {
            this.selectionList.add(instance.getUri());
            this.itemParents.put(SPARQLDeserializers.getExpandedURI(instance.getUri()), instance.getParent());
        }
        this.root = root;
        this.excludeRoot = excludeRoot;
    }

    public void listRoots(Consumer<T> handler) {
        this.listChildren(null, handler);
    }

    public int getRootsCount() {
        return this.getChildCount(null);
    }

    public int getChildCount(T parent) {
        URI parentURI = null;
        if (parent != null) {
            parentURI = parent.getUri();
        }
        if (this.map.containsKey(parentURI)) {
            return this.map.get(parentURI).size();
        } else {
            return 0;
        }
    }

    public void listChildren(T parent, Consumer<T> handler) {
        URI parentURI = null;
        if (parent != null) {
            parentURI = parent.getUri();
        }
        if (this.map.containsKey(parentURI)) {
            this.map.get(parentURI).forEach(handler);
        }
    }

    public T getParent(T candidate) {
        T parent = itemParents.get(SPARQLDeserializers.getExpandedURI(candidate.getUri()));
        return parent;
    }

    public void addTree(T candidate) {
        T parent = getParent(candidate);
        addTreeWithParent(candidate, parent);
    }

    public void addTreeWithParent(T candidate, T parent) {
        if (!map.containsKey(candidate.getUri())) {

            if (parent == null) {
                if (!map.containsKey(null)) {
                    map.put(null, new ArrayList<T>());
                }
                addInMapIfExists(null, candidate);
            } else if (candidate.getUri().equals(root)) {
                if (!excludeRoot) {
                    if (!map.containsKey(null)) {
                        map.put(null, new ArrayList<T>());
                    }
                    addInMapIfExists(null, candidate);
                }
            } else {
                URI parentURI = parent.getUri();
                if (parentURI.equals(root) && excludeRoot) {
                    parentURI = null;
                }
                if (!map.containsKey(parentURI)) {
                    if (parentURI != null) {
                        addTree(parent);
                    }
                    map.put(parentURI, new ArrayList<T>());
                }

                addInMapIfExists(parentURI, candidate);
            }

        }
    }

    private void addInMapIfExists(URI parentURI, T candidate) {
        boolean exists = false;
        for (T item : map.get(parentURI)) {
            if (SPARQLDeserializers.compareURIs(candidate.getUri(), item.getUri())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            map.get(parentURI).add(candidate);
        }
    }

    public boolean isSelected(T candidate) {
        return this.selectionList.contains(candidate.getUri());
    }

    public void traverse(Consumer<T> handler) {
        listRoots((instance) -> {
            traverseNode(instance, handler);
        });
    }

    private void traverseNode(T instance, Consumer<T> handler) {
        handler.accept(instance);
        listChildren(instance, (child) -> {
            traverseNode(child, handler);
        });
    }
}
