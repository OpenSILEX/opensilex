/*******************************************************************************
 *                         ObservationURIDatatype.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.proto_ext.datatype.integer;

import org.bson.BsonString;
import org.bson.BsonValue;
import org.opensilex.core.observation.proto_ext.datatype.ObservationDatatype;

import java.net.URI;

/**
 * @author Valentin Rigolle
 */
public class ObservationURIDatatype extends ObservationDatatype<ObservationURIValue> {
    public ObservationURIDatatype(URI uri) {
        super(uri);
    }

    @Override
    public BsonValue toBson(ObservationURIValue value) {
        return new BsonString(value.getUriValue().toString());
    }

    @Override
    public ObservationURIValue fromBson(BsonValue bson) {
        var value = new ObservationURIValue();
        //todo handle deserialization error
        value.setUriValue(URI.create(bson.asString().getValue()));
        return value;
    }
}
