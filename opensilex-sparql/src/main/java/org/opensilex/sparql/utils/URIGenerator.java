//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * @author vidalmor
 */
public interface URIGenerator<T> {

    String NORMALIZING_JOIN_CHARACTER = ".";

    default URI generateURI(String prefix, T instance, int retryCount) throws UnsupportedEncodingException, URISyntaxException {
        if (retryCount > 0) {
            return URI.create(prefix + "/" + getInstanceURI(instance) + "/" + retryCount);
        } else {
            return URI.create(prefix + "/" + getInstanceURI(instance));
        }
    }
    
    default String getInstanceURI(T instance) throws UnsupportedEncodingException {
        return instance.toString();
    }

    static String normalize(Object txt)  {

        String value = txt.toString()
                .toLowerCase()
                .trim()
                .replaceAll(" +", " ")
                .replace(" ", "-")
                .replaceAll("\\$|&|\\+|\\||,|/|:|;|=|\\?|@|<|>|#|%|\\{|\\}|\\(|\\)|\\^|~|\\[|\\]|\\\\|\"|'|`|\\*|\\!|\\.", "")
                .replaceAll("%", "");

        value = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll(" ", "")
                .replaceAll("[^\\p{ASCII}]", "");

        try{
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        }catch (UnsupportedEncodingException e){
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    static String normalize(String[] parts) {

        // build joined String in O(1) space complexity (no intermediate list/array)
        return Arrays.stream(parts)
                .map(URIGenerator::normalize)
                .collect(Collectors.joining(NORMALIZING_JOIN_CHARACTER));
    }

}
