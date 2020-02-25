//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.net.URI;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;

/**
 *
 * @author vincent
 */
public class URIDeserializer implements SPARQLDeserializer<URI> {

    @Override
    public URI fromString(String value) throws Exception {
        if (prefixes == null) {
            return new URI(value);
        }

        return new URI(prefixes.shortForm(value));
    }

    @Override
    public Node getNodeFromString(String value) throws Exception {
        if (prefixes == null) {
            return getNode(new URI(value));
        }

        return getNode(new URI(prefixes.expandPrefix(value)));
    }

    public static String getExpandedURI(String value) {
         if (prefixes == null || value == null) {
            return value;
        }
        return prefixes.expandPrefix(value);
    }
    
    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createURI(value.toString());
    }

    private static PrefixMapping prefixes = null;

    public static void setPrefixes(PrefixMapping prefixesMap) {
        prefixes = prefixesMap;
    }

}
