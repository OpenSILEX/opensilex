//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

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
        return instance.toString();
    }

    /**
     * Universal REGEX Logical OR operator
     */
    String REGEX_OR_SEPARATOR = "|";


    /**
     * list of special characters
     */
    String FORBIDDEN_CHARS_REGEX = "$&+|,/:;=?@<>#%{}()^~\\[\\]\\\\\"'`*!.";

    /**
     * non-ASCII characters
     */
    String ASCII_REGEX = "^\\p{ASCII}";


    /**
     * Compiled pattern for efficient search/replace.
     * This pattern use a logical OR on multiple regex
     */
    Pattern pattern = Pattern.compile(String.join(
            REGEX_OR_SEPARATOR,
            Arrays.asList(
                    "[" + FORBIDDEN_CHARS_REGEX + "]",
                    "[" + ASCII_REGEX + "]"
            ))
    );

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
                .replace(' ','_')
                .trim();

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
