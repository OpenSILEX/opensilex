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
    default String getInstancePath(T instance) {
        return normalize(getUriSegments(instance));
    }

    /**
     *
     * @param instance
     * @return
     */
    String[] getUriSegments(T instance);
}
