//******************************************************************************
//                          AgentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Agent model
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "rdf_type", "settings"})
public class AgentModel extends GlobalProvenanceEntity {

    @ApiModelProperty(value = "Agent type defined in the ontology", example = "oeso:Sensor")
    public URI getRdfType() {
        return rdfType;
    }

    @ApiModelProperty(value = "Agent uri", example = "http://sensor/s01")
    public URI getUri() {
        return uri;
    }

    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AgentModel))
            return false;
        if (obj == this)
            return true;

        AgentModel agent = (AgentModel) obj;
        return new EqualsBuilder().
            append(rdfType, agent.rdfType).
            append(uri, agent.uri).
            append(settings, agent.settings).
            isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(rdfType).
            append(uri).
            append(settings).
            toHashCode();       
    }

}
