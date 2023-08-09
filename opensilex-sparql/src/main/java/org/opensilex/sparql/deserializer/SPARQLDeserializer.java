//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.exceptions.SPARQLException;

/**
 *
 * @author vincent
 */
public interface SPARQLDeserializer<T> {

    T fromString(String value) throws SPARQLException;

    Node getNode(Object value) throws SPARQLException;

    default boolean validate(String value) {
        try {
            return this.getDataType().isValid(value);
        } catch (Exception ex) {
            return false;
        }
    }

    XSDDatatype getDataType();

    default String getNodeString(Object value) throws SPARQLException {
        return getNode(value).toString();
    }

    default Node getNodeFromString(String value) throws SPARQLException {
        return getNode(fromString(value));
    }

    default Class<T> getClassType() {
        return (Class<T>) SPARQLDeserializers.getDeserializerClass(this);
    }

}
