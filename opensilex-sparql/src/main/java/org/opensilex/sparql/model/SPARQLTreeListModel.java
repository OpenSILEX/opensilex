/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 * @author vince
 * Class used for representing a Tree of any {@link SPARQLTreeModel} with a bidirectionnal-indexing between model and model parent.
 */
public class SPARQLTreeListModel<T extends SPARQLTreeModel<T>> {

    private final HashMap<URI, Set<T>> modelsByParent = new HashMap<>();
    private final HashMap<URI, T> parentByModel = new HashMap<>();

    private final List<URI> selectionList;
    private final URI root;
    private final boolean excludeRoot;

    public SPARQLTreeListModel() {
        this(new ArrayList<>(), null, false);
    }

    public SPARQLTreeListModel(Collection<T> selectionList, URI root, boolean excludeRoot, boolean addSelectionToTree) {
        this.selectionList = new ArrayList<>(selectionList.size());
        for (T instance : selectionList) {
            URI formattedUri = SPARQLDeserializers.formatURI(instance.getUri());
            this.selectionList.add(formattedUri);
            this.parentByModel.put(formattedUri, instance.getParent());
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
        this(rootModel.getNodes(), rootModel.getUri(), excludeRoot, addSelectionToTree);
    }

    public void listRoots(Consumer<T> handler) {
        this.listChildren(null, handler);
    }

    public int getRootsCount() {
        return this.getChildCount(null);
    }

    private Set<T> getChildren(T parent) {
        URI parentURI = null;
        if (parent != null) {
            parentURI = SPARQLDeserializers.formatURI(parent.getUri());
        }
        return this.modelsByParent.get(parentURI);
    }

    public int getChildCount(T parent) {
        Set<T> children = getChildren(parent);
        return children == null ? 0 : children.size();
    }

    public void listChildren(T parent, Consumer<T> handler) {
        Set<T> children = getChildren(parent);
        if (children != null) {
            children.forEach(handler);
        }
    }

    public T getParent(T instance) {
        return parentByModel.get(SPARQLDeserializers.formatURI(instance.getUri()));
    }

    public void addTree(T instance) {
        T parent = getParent(instance);
        addTreeWithParent(instance, parent);
    }

    public void addTreeWithParent(T instance, T parent) {

        URI formattedUri = SPARQLDeserializers.formatURI(instance.getUri());

        if (modelsByParent.containsKey(formattedUri)) {
            return;
        }

        if (parent == null || formattedUri.equals(root)) {
            if (parent == null || !excludeRoot) {
                modelsByParent.computeIfAbsent(null, key -> new HashSet<>());
                addInMapIfExists(null, instance);
            }
        } else {
            URI parentURI = SPARQLDeserializers.formatURI(parent.getUri());
            if (parentURI.equals(root) && excludeRoot) {
                parentURI = null;
            }
            if (!modelsByParent.containsKey(parentURI)) {
                if (parentURI != null) {
                    addTree(parent);
                }
                modelsByParent.put(parentURI, new HashSet<>());
            }
            addInMapIfExists(parentURI, instance);
        }

    }

    private void addInMapIfExists(URI parentURI, T instance) {
        boolean exists = false;
        for (T item : modelsByParent.get(parentURI)) {
            if (SPARQLDeserializers.compareURIs(instance.getUri(), item.getUri())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            modelsByParent.get(parentURI).add(instance);
            parentByModel.put(SPARQLDeserializers.formatURI(instance.getUri()), instance.getParent());
        }
    }

    public boolean isSelected(T instance) {
        return this.selectionList.contains(instance.getUri());
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

    private void removeLinkWithChildren(URI instanceUri) {

        // update link between parent and old children
        if (modelsByParent.containsKey(instanceUri)) {
            Set<T> oldChildren = modelsByParent.get(instanceUri);

            // all old children become orphans :'(
            if (oldChildren != null) {

                for (T oldChild : oldChildren) {
                    // update the child node itself
                    oldChild.setParent(null);

                    // update itemParents index
                    parentByModel.put(oldChild.getUri(), null);
                }
                // the set of orphans receive new node
                modelsByParent.computeIfAbsent(null, key -> new HashSet<>()).addAll(oldChildren);
            }
        }
    }

    private void removeLinkWithParent(URI childUri) {

        T parent = parentByModel.get(childUri);

        URI parentUri;

        // find the old parent : null if the old parent is the root, parent.getUri() else
        if (parent == null) {
            parentUri = null;
        } else {
            // if the parentUri is root, then use null
            parentUri = SPARQLDeserializers.compareURIs(root, parent.getUri()) ? null : parent.getUri();
        }

        // the old parent loose a child :(
        modelsByParent.get(parentUri).removeIf(
                child -> SPARQLDeserializers.compareURIs(child.getUri().toString(), childUri)
        );

        // update the parent node itself
        if (parent != null && !CollectionUtils.isEmpty(parent.getChildren())) {
            parent.getChildren().removeIf(
                    model -> SPARQLDeserializers.compareURIs(model.getUri().toString(), childUri)
            );
        }
    }


    /**
     * Remove SPARQLTreeModel associated with the given modelUri if exists
     *
     * @param modelUri URI of a SPARQLTreeModel
     */
    public void remove(URI modelUri) {

        URI formattedUri = SPARQLDeserializers.formatURI(modelUri);

        // instance not found into tree
        if (!parentByModel.containsKey(formattedUri)) {
            return;
        }

        removeLinkWithChildren(formattedUri);
        removeLinkWithParent(formattedUri);

        // remove parent from the two indexes
        parentByModel.remove(formattedUri);
        modelsByParent.remove(formattedUri);

        selectionList.removeIf(uri -> uri.equals(modelUri));
    }
}
