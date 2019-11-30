//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.net.URI;

/**
 *
 * @author vidalmor
 */
public interface ClassURIGenerator<T> extends URIGenerator<T> {

    @Override
    public default URI generateURI(URI platformUri, T instance) throws Exception {
        return URIGenerator.normalize(platformUri, (Object) getUriSegments(instance));
    }

    public String[] getUriSegments(T instance);
}
