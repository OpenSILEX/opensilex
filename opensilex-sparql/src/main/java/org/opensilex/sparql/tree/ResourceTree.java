/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.tree;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vince
 */
public class ResourceTree<T extends SPARQLTreeModel> {

    private HashMap<URI, Set<T>> map = new HashMap<URI, Set<T>>();

    public void listRoots(Consumer<T> handler) {
        this.listChildren(null, handler);
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

    public void addTree(T candidate) {
        if (!map.containsKey(candidate.getUri())) {

            T parent = (T) candidate.getParent();
            
            if (parent == null) {
                if (!map.containsKey(null)) {
                    map.put(null, new HashSet<T>());
                }
                map.get(null).add(candidate);
            } else {
                URI parentURI = parent.getUri();
                if (!map.containsKey(parentURI)) {
                    map.put(parentURI, new HashSet<T>());
                }
                
                 map.get(parentURI).add(candidate);
            }

           
        }
    }
}
