//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import org.apache.jena.graph.*;

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
