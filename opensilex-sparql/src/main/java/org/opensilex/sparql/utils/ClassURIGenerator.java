//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author vidalmor
 */
public interface ClassURIGenerator<T> extends URIGenerator<T> {

    @Override
    default String getInstanceURI(T instance) throws UnsupportedEncodingException  {
        return URIGenerator.normalize(getUriSegments(instance));
    }

    String[] getUriSegments(T instance);
}
