//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;

/**
 *
 * @author vincent
 */
public interface SPARQLDeserializer<T> {

    public T fromString(String value) throws Exception;

    public Node getNode(Object value) throws Exception;

    public default boolean validate(String value) {
        try {
            return this.getDataType().isValid(getNodeFromString(value).toString());
        } catch (Exception ex) {
            return false;
        }
    }

    public XSDDatatype getDataType();

    public default XSDDatatype[] getAlternativeDataType() {
        return new XSDDatatype[]{};
    }

    public default String getNodeString(Object value) throws Exception {
        return getNode(value).toString();
    }

    public default Node getNodeFromString(String value) throws Exception {
        return getNode(fromString(value));
    }

    public default Class<T> getClassType() {
        return (Class<T>) SPARQLDeserializers.getDeserializerClass(this);
    }

}
