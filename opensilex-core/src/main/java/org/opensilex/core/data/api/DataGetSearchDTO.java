/*
 *  *************************************************************************************
 *  DataGetSearchDTO.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2024
 * Contact :  user@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.data.dal.DataModel;

import java.net.URI;
import java.util.Set;

/**
 *
 * @author rcolin
 * DTO which for data search
 */
@JsonPropertyOrder({"uri", "date","timezone", "target", "variable", "value", "confidence", "provenance",  "metadata", "publisher"})
public class DataGetSearchDTO extends DataGetDTO {

    @JsonProperty("publisher")
    private URI publisher;

    public URI getPublisher() {
        return publisher;
    }

    public void setPublisher(URI publisher) {
        this.publisher = publisher;
    }

    public static DataGetSearchDTO getDtoFromModel(DataModel model, Set<URI> dateVariables) {
        DataGetSearchDTO dto = new DataGetSearchDTO();
        dto.fromModel(model, dateVariables);
        return dto;
    }
}
