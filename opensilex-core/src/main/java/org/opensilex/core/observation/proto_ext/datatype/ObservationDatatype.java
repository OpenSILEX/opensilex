/*******************************************************************************
 *                         ObservationDatatype.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.proto_ext.datatype;

import org.bson.BsonValue;

import java.net.URI;

/**
 * @author Valentin Rigolle
 */
public abstract class ObservationDatatype<V extends ObservationValue> {
    private URI uri;

    public ObservationDatatype(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public abstract BsonValue toBson(V value);

    public abstract V fromBson(BsonValue bson);
}
