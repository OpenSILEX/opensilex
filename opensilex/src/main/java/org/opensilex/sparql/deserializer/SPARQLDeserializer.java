/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.deserializer;

import org.apache.jena.graph.Node;

/**
 *
 * @author vincent
 */
public interface SPARQLDeserializer<T> {

    public T fromString(String value) throws Exception;

    public Node getNode(Object value) throws Exception;
    
    public default String getNodeString(Object value) throws Exception {
        return getNode(value).toString();
    }
}
