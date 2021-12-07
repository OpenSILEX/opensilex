/*******************************************************************************
 *                         URIGenerator.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 24/11/2021 17:18
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.uri.generation;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author vidalmor
 * @author rcolin
 */
public interface URIGenerator<T> {

    /**
     * Default universal URI separator
     */
    String URI_SEPARATOR = "/";

    /**
     * @param prefix     the URI schema {@link URI#getScheme()}
     * @param instance   instance
     * @param retryCount the retry count, added as suffix of the generated URI if retryCount > 0
     * @return the generated URI
     * @throws URISyntaxException if some error is encountered during URI parsing
     */
    default URI generateURI(String prefix, T instance, int retryCount) throws URISyntaxException {

        String path = getInstanceUriPath(instance);
        if (retryCount == 0) {
            return new URI(prefix + URI_SEPARATOR + path);
        }
        return new URI(prefix + URI_SEPARATOR + path + retryCount);
    }

    /**
     * @param instance instance
     * @return the path used to generate instance URI {@link URI#getPath()}
     */
    default String getInstanceUriPath(T instance) {
        return normalize(instance.toString());
    }

    /**
     * Compiled pattern for efficient search/replace.
     * This regex match any non-alphanumeric character (including escape characters) except space and -
     */
    Pattern pattern = Pattern.compile("[^\\w -]+");

    /**
     * @param src the String to replace and normalize
     * @return a normalized, lower-case and trimmed String with special-characters and non-ASCII character removed.
     */
    default String normalize(String src) {

        Objects.requireNonNull(src);
        if (src.isEmpty()) {
            return src;
        }

        // search and replace pattern from src
        Matcher matcher = pattern.matcher(src);
        String formattedSrc = matcher.replaceAll("")
                .toLowerCase()
                .trim() // call trim before intermediate replace in order to remove leading and trailing space
                .replace(' ', '_'); // then replace intermediate space by _

        return Normalizer.normalize(formattedSrc, Normalizer.Form.NFD);
    }

    /**
     * The default separator to use when joining multiple normalized string for URI path building
     */
    String NORMALIZING_JOIN_CHARACTER = ".";

    /**
     * @param parts Array of part to normalize
     * @return the concatenation of all normalized String from parts.
     * Each part is separated by {@link URIGenerator#NORMALIZING_JOIN_CHARACTER}
     * @see URIGenerator#normalize(String)
     */
    default String normalize(String[] parts) {

        Objects.requireNonNull(parts);

        // build joined normalized String without intermediate list/array creation
        return Arrays.stream(parts)
                .map(this::normalize)
                .collect(Collectors.joining(NORMALIZING_JOIN_CHARACTER));
    }

}
