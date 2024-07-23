//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;
import org.opensilex.server.rest.serialization.uri.UriFormater;

import java.net.URI;
import java.util.Objects;

/**
 *
 * @author vincent
 */
public class URIDeserializer implements SPARQLDeserializer<URI> {

    @Override
    public URI fromString(String value) throws Exception {
        if (prefixes == null || !usePrefixes) {
            return new URI(value);
        }

        return formatURI(new URI(value));
    }

    @Override
    public Node getNodeFromString(String value) throws Exception {
        return getNode(new URI(value));
    }

    public static URI formatURI(URI uri) {
       return UriFormater.formatURI(uri);
    }

    public static URI formatURI(String uri) {
        return UriFormater.formatURI(uri);
    }

    public static String formatURIAsStr(String uri){
        return UriFormater.formatURIAsStr(uri);
    }

    public static String getShortURI(String value) {
        return UriFormater.getShortURI(value);
    }

    public static String getExpandedURI(URI value) {
        return UriFormater.getExpandedURI(value);
    }

    public static String getExpandedURI(String value) {
        return UriFormater.getExpandedURI(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        if (prefixes == null) {
            return NodeFactory.createURI(value.toString());
        }
        return NodeFactory.createURI(getExpandedURI(value.toString()));
    }

    private static PrefixMapping prefixes = null;
    private static boolean usePrefixes = false;

    public static void setPrefixes(PrefixMapping prefixesMap, boolean usePrefixes) {
        URIDeserializer.prefixes = prefixesMap;
        URIDeserializer.usePrefixes = usePrefixes;
        UriFormater.setPrefixes(prefixesMap,usePrefixes);
    }

    public static PrefixMapping getPrefixes() {
        return prefixes;
    }

    public static void clearPrefixes() {
        prefixes = null;
        usePrefixes = false;
    }

    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDanyURI;
    }

    @Override
    public boolean validate(String value) {
        return validateURI(value);
    }

    public static boolean validateURI(String value) {
        if (value == null) {
            return true;
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Check whether the given URI uses a prefix (scheme) defined in the prefix mapping, or if it can be shortened to
     * a prefixed URI using the mapping. This is done by comparing the short and the expanded forms of the URI :
     * if they are identical, the URI prefix or namespace is not known in the mapping.
     *
     * @param uri The URI to check
     * @return whether `uri` uses a known prefix or namespace
     */
    public static boolean hasKnownPrefix(URI uri) {
        return !Objects.equals(
                getExpandedURI(uri.toString()),
                getShortURI(uri.toString())
        );
    }

}
