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

    public default URI generateURI(URI platformUri, T instance) throws Exception {
        return normalize(platformUri, instance);
    }

    ;
    
    public default URI generateURI(URI platformUri, T instance, int retryCount) throws Exception {
        return UriBuilder.fromUri(generateURI(platformUri, instance)).segment("" + retryCount).build();
    }

    ;
    
    public static String normalizePart(Object txt) {
        return txt.toString().toLowerCase().trim().replaceAll(" +", " ").replace(" ", "-");
    }

    public static URI normalize(URI base, Object... parts) {
        UriBuilder builder = UriBuilder.fromUri(base);
        List<String> normalizedString = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            normalizedString.add(normalizePart(parts[i]));
        }

        return builder.segment(normalizedString.toArray(new String[normalizedString.size()])).build();
    }
}
