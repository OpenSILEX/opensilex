/*******************************************************************************
 *                         ClassURIGenerator.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 24/11/2021 17:18
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.uri.generation;

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
