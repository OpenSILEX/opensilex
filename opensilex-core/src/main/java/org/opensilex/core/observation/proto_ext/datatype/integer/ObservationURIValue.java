/*******************************************************************************
 *                         ObservationStringValue.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.proto_ext.datatype.integer;

import org.opensilex.core.observation.proto_ext.datatype.ObservationValue;

import java.net.URI;
import java.util.Objects;

/**
 * @author Valentin Rigolle
 */
public class ObservationURIValue extends ObservationValue {
    private URI uriValue;

    public URI getUriValue() {
        return uriValue;
    }

    public void setUriValue(URI uriValue) {
        this.uriValue = uriValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObservationURIValue that = (ObservationURIValue) o;

        return Objects.equals(uriValue, that.uriValue);
    }

    @Override
    public int hashCode() {
        return uriValue != null ? uriValue.hashCode() : 0;
    }
}
