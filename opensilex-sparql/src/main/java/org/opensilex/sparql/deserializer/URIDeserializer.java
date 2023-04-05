//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.sparql.service.SPARQLPrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class URIDeserializer implements SPARQLDeserializer<URI> {

    private static final Logger LOGGER = LoggerFactory.getLogger(URIDeserializer.class);

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
        if (uri == null || prefixes == null) {
            return uri;
        }
        try {
            if (usePrefixes) {
                return new URI(prefixes.shortForm(uri.toString()));
            } else {
                return new URI(prefixes.expandPrefix(uri.toString()));
            }
        } catch (URISyntaxException ex) {
            LOGGER.error("[URISyntaxException] Error while formatting URI : {}", ex.getMessage());
        }

        return null;
    }

    public static URI formatURI(String uri) {
        if (StringUtils.isEmpty(uri)) {
            return null;
        }
        try {
            if (usePrefixes) {
                return new URI(prefixes.shortForm(uri));
            } else {
                return new URI(prefixes.expandPrefix(uri));
            }
        } catch (URISyntaxException ex) {
            LOGGER.error("[URISyntaxException] Error while formatting URI : {}", ex.getMessage());
        }

        return null;
    }

    public static URI indexedFormatURI(String uri){
        if (StringUtils.isEmpty(uri)) {
            return null;
        }
        try {
            if (usePrefixes) {
                return new URI(prefixes.shortFormIndexed(uri));
            } else {
                return new URI(prefixes.expandedFormIndexed(uri));
            }
        } catch (URISyntaxException ex) {
            LOGGER.error("[URISyntaxException] Error while formatting URI : {}", ex.getMessage());
            return null;
        }
    }

    public static String formatURIAsStr(String uri){
        if (StringUtils.isEmpty(uri)) {
            return null;
        }

        return usePrefixes ? prefixes.shortForm(uri) : prefixes.expandPrefix(uri);
    }

    public static String getShortURI(String value) {
        if (value == null || value.isEmpty() || prefixes == null) {
            return value;
        }
        return prefixes.shortForm(value);
    }

    public static String getExpandedURI(URI value) {
        if (value == null) {
            return null;
        }
        return getExpandedURI(value.toString());
    }

    public static String getExpandedURI(String value) {
        if (prefixes == null || value == null) {
            return value;
        }
        return prefixes.expandPrefix(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        if (prefixes == null) {
            return NodeFactory.createURI(value.toString());
        }
        return NodeFactory.createURI(getExpandedURI(value.toString()));
    }

    private static SPARQLPrefixMapping prefixes = null;
    private static boolean usePrefixes = false;

    public static void setPrefixes(SPARQLPrefixMapping prefixesMap, boolean usePrefixes) {
        URIDeserializer.prefixes = prefixesMap;
        URIDeserializer.usePrefixes = usePrefixes;
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

}
