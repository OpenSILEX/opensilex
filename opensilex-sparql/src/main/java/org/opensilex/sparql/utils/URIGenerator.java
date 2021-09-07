//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vidalmor
 */
public interface URIGenerator<T> {

    public default URI generateURI(String prefix, T instance, int retryCount) throws Exception {
        if (retryCount > 0) {
            return new URI(prefix + "#" + getInstanceURI(instance) + "/" + retryCount);
        } else {
            return new URI(prefix + "#" + getInstanceURI(instance));
        }
    }
    
    public default String getInstanceURI(T instance) throws UnsupportedEncodingException {
        return instance.toString();
    }

    public static String normalize(Object txt) throws UnsupportedEncodingException {

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

        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    public static String normalize(String[] parts) throws UnsupportedEncodingException {
        List<String> normalizedString = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            normalizedString.add(normalize(parts[i]));
        }

        return String.join(".", normalizedString.toArray(parts));
    }

}
