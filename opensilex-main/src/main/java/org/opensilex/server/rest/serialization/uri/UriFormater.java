package org.opensilex.server.rest.serialization.uri;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Formatter of {@link URI} using short or long format according a {@link PrefixMapping}
 *
 * @author rcolin
 */
public class UriFormater {

    private static final Logger LOGGER = LoggerFactory.getLogger(UriFormater.class);
    private static PrefixMapping prefixes = null;
    private static boolean usePrefixes = false;

    private UriFormater() {

    }

    public static void setPrefixes(PrefixMapping prefixesMap, boolean usePrefixes) {
        UriFormater.prefixes = prefixesMap;
        UriFormater.usePrefixes = usePrefixes;
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
            LOGGER.warn(ex.getMessage());
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
            LOGGER.warn(ex.getMessage());
        }

        return null;
    }

    public static String formatURIAsStr(String uri) {
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
}
