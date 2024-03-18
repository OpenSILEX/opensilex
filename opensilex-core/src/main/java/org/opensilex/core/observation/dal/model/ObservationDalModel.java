/*******************************************************************************
 *                         ObservationDalModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.dal.model;

import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.util.UUID;

/**
 * @author Valentin Rigolle
 */
public class ObservationDalModel extends MongoModel {
    private URI variable;
    private String value;

    public URI getVariable() {
        return variable;
    }

    public void setVariable(URI variable) {
        this.variable = variable;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[] {
                UUID.randomUUID().toString()
        };
    }
}
