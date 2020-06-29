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
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author vince
 */
public class SPARQLPartialTreeListModel<T extends SPARQLTreeModel<T>> extends SPARQLTreeListModel<T> {

    private final Function<URI, Integer> countHandler;
    private final Function<URI, List<T>> searchHandler;

    private final Map<URI, Integer> countCache = new HashMap<>();

    public SPARQLPartialTreeListModel(URI root, Function<URI, List<T>> searchHandler, Function<URI, Integer> countHandler) {
        super(new ArrayList<>(), root, false);
        this.countHandler = countHandler;
        this.searchHandler = searchHandler;
    }

    public int getTotalChildCount(T parent) {
        if (!countCache.containsKey(parent.getUri())) {
            countCache.put(parent.getUri(), countHandler.apply(parent.getUri()));
        }
        return countCache.get(parent.getUri());
    }
    
    public void loadChildren(T candidate, int maxDepth) {
        if (maxDepth <= 0) {
            return;
        }
        this.addTree(candidate);
        List<T> children = searchHandler.apply(candidate.getUri());
        for (T child : children) {
            this.loadChildren(child, maxDepth - 1);
        }
    }
}
