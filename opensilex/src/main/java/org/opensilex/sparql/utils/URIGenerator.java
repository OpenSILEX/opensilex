//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.UriBuilder;


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
    
    public default String getInstanceURI(T instance) {
        return instance.toString();
    }
    
    public static String normalizePart(Object txt) {
        return txt.toString().toLowerCase().trim().replaceAll(" +", " ").replace(" ", "-");
    }

    public static List<String> normalize(Object... parts) {
        List<String> normalizedString = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            normalizedString.add(normalizePart(parts[i]));
        }

        return normalizedString;
    }

    
}
