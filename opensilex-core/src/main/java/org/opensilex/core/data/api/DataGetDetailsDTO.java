//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.security.user.api.UserGetDTO;

import java.net.URI;
import java.util.Set;

/**
 *
 * @author rcolin
 * Specialized DTO which store detailed information about the publisher
 */
@JsonPropertyOrder({"uri", "date","timezone", "target", "variable", "value", "confidence", "provenance",  "metadata", "publisher"})

public class DataGetDetailsDTO extends DataGetDTO {

    @JsonProperty("publisher")
    private UserGetDTO publisher;

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public static DataGetDetailsDTO getDtoFromModel(DataModel model, Set<URI> dateVariables) {
        DataGetDetailsDTO dto = new DataGetDetailsDTO();
        dto.fromModel(model, dateVariables);
        return dto;
    }

}
