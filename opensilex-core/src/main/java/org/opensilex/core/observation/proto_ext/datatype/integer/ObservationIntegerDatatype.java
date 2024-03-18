/*******************************************************************************
 *                         ObservationIntegerDatatype.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.proto_ext.datatype.integer;

import org.bson.BsonInt64;
import org.bson.BsonValue;
import org.bson.Document;
import org.opensilex.core.observation.proto_ext.datatype.ObservationDatatype;

import java.net.URI;

/**
 * @author Valentin Rigolle
 */
public class ObservationIntegerDatatype extends ObservationDatatype<ObservationIntegerValue> {
    public ObservationIntegerDatatype(URI uri) {
        super(uri);
    }

    @Override
    public BsonValue toBson(ObservationIntegerValue value) {
        return new BsonInt64(value.getIntegerValue());
    }

    @Override
    public ObservationIntegerValue fromBson(BsonValue bson) {
        var value = new ObservationIntegerValue();
        //todo handle deserialization error
        value.setIntegerValue(bson.asInt64().longValue());
        return value;
    }
}
