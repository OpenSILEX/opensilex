/*******************************************************************************
 *                         ObservationDatatypeRegistry.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.proto_ext.datatype;

import org.opensilex.core.observation.proto_ext.datatype.integer.ObservationIntegerDatatype;
import org.opensilex.core.observation.proto_ext.datatype.integer.ObservationURIDatatype;

import java.net.URI;
import java.util.Map;

/**
 * @author Valentin Rigolle
 */
public class ObservationDatatypeRegistry {
    public static final ObservationIntegerDatatype OBSERVATION_INTEGER_DATATYPE =
            new ObservationIntegerDatatype(URI.create("http://example.org/datatype/integer"));
    public static final ObservationURIDatatype OBSERVATION_URI_DATATYPE =
            new ObservationURIDatatype(URI.create("http://example.org/datatype/uri"));
    private static final Map<URI, ObservationDatatype<?>> DATATYPE_MAP = Map.of(
            OBSERVATION_INTEGER_DATATYPE.getUri(), OBSERVATION_INTEGER_DATATYPE,
            OBSERVATION_URI_DATATYPE.getUri(), OBSERVATION_URI_DATATYPE
    );

    public static ObservationDatatype<ObservationValue> get(URI datatypeUri) {
        if (!DATATYPE_MAP.containsKey(datatypeUri)) {
            //todo custom exception ?
            throw new IllegalArgumentException("No datatype registered for URI " + datatypeUri);
        }
        return (ObservationDatatype<ObservationValue>) DATATYPE_MAP.get(datatypeUri);
    }
}
