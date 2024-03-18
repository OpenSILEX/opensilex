/*******************************************************************************
 *                         ObservationDalModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.dal.model;

import org.bson.BsonValue;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.util.UUID;

/**
 * @author Valentin Rigolle
 */
public class ObservationModel extends MongoModel {
    private URI datatype;
    private BsonValue value;

    public URI getDatatype() {
        return datatype;
    }

    public void setDatatype(URI datatype) {
        this.datatype = datatype;
    }

    public BsonValue getValue() {
        return value;
    }

    public void setValue(BsonValue value) {
        this.value = value;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[] {
                UUID.randomUUID().toString()
        };
    }
}
