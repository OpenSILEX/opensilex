/*******************************************************************************
 *                         ObservationIntegerValue.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.proto_ext.datatype.integer;

import org.opensilex.core.observation.proto_ext.datatype.ObservationValue;

/**
 * @author Valentin Rigolle
 */
public class ObservationIntegerValue extends ObservationValue {
    private long integerValue;

    public long getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(long integerValue) {
        this.integerValue = integerValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObservationIntegerValue that = (ObservationIntegerValue) o;

        return integerValue == that.integerValue;
    }

    @Override
    public int hashCode() {
        return (int) (integerValue ^ (integerValue >>> 32));
    }
}
