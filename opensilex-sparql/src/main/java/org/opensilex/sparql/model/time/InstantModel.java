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
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Renaud COLIN
 *
 * Model describing an Instant by using the {@link Time#Instant} concept
 * @see Time#Instant
 */

@SPARQLResource(
        ontology = Time.class,
        resource = "Instant",
        graph = InstantModel.GRAPH
)
public class InstantModel extends SPARQLResourceModel implements ClassURIGenerator<InstantModel> {

    public static final String GRAPH = "instant";

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
    public URI generateURI(String prefix, InstantModel instance, int retryCount) throws URISyntaxException {
        String instantPrefix = prefix.replace("event","instant");
        return ClassURIGenerator.super.generateURI(instantPrefix, instance, retryCount);
    }

    @Override
    public String[] getInstancePathSegments(InstantModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
