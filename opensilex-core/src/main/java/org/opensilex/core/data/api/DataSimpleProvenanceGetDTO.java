//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;

import java.net.URI;
import java.util.List;

/**
 * This class defines a serie of data as a list of data associated with a provenance
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "uri",
        "rdf_type",
        "name"
})
public class DataSimpleProvenanceGetDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("rdf_type")
    private URI rdfType;

    @JsonProperty("name")
    private String name;


    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public static DataSimpleProvenanceGetDTO fromModel(DataProvenanceModel model){
        DataSimpleProvenanceGetDTO dto = new DataSimpleProvenanceGetDTO();

        List<ProvEntityModel> provEntityList = model.getProvWasAssociatedWith();

        if (provEntityList != null && provEntityList.size() == 1) {
            dto.setUri(provEntityList.get(0).getUri());
            dto.setRdfType(provEntityList.get(0).getType());
            dto.setName(dto.getUri().toString());
        }
        else {
            dto.setUri(model.getUri());
            dto.setName(dto.getUri().toString());
        }

        return dto;
    }

}
