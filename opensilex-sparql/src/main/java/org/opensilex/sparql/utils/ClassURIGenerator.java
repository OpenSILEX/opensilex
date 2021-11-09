//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

/**
 *
 * @author vidalmor
 */
public interface ClassURIGenerator<T> extends URIGenerator<T> {

    @Override
    default String getInstanceUriPath(T instance) {
        return normalize(getInstancePathSegments(instance));
    }

    /**
     *
     * @param instance the instance used
     * @return the array of string to use for generation instance URI path
     */
    String[] getInstancePathSegments(T instance);
}
