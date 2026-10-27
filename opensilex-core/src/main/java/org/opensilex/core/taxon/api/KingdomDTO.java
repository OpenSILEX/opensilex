//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.taxon.dal.Kingdom;

import java.net.URI;

public class KingdomDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("name")
    protected String name;

    public static KingdomDTO fromModel(Kingdom kingdom) {
        KingdomDTO dto = new KingdomDTO();
        dto.uri = kingdom.uri();
        dto.name = kingdom.label();
        return dto;
    }

    public URI getUri() { return uri; }
    public String getName() { return name; }
}
