//******************************************************************************
//                          InstantModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.sparql.model.time;

import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Renaud COLIN
 */
@SPARQLResource(
        ontology = Time.class,
        resource = "Instant",
        graph = "set/events"
)
public class InstantModel extends SPARQLResourceModel implements ClassURIGenerator<InstantModel> {

    @SPARQLProperty(
        ontology = Time.class,
        property = "inXSDDateTimeStamp",
        required = true
    )
    private OffsetDateTime dateTimeStamp;

    public static final String IN_XSD_DATA_TIME_STAMP_FIELD = "dateTimeStamp";

    public OffsetDateTime getDateTimeStamp() { return dateTimeStamp; }

    public void setDateTimeStamp(OffsetDateTime dateTimeStamp) { this.dateTimeStamp = dateTimeStamp; }

    @Override
    public String[] getUriSegments(InstantModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
