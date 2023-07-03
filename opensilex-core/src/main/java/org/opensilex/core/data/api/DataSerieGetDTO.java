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

import javax.validation.Valid;
import java.util.List;

/**
 * This class defines a serie of data as a list of data associated with a provenance
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "provenance", "data"
})
public class DataSerieGetDTO {

    @JsonProperty("provenance")
    private DataSimpleProvenanceGetDTO provenance;

    @Valid
    @JsonProperty("data")
    private List<DataComputedGetDTO> data;


    public DataSerieGetDTO(DataSimpleProvenanceGetDTO provenance, List<DataComputedGetDTO> data) {
        this.provenance = provenance;
        this.data = data;
    }

    public DataSimpleProvenanceGetDTO getProvenance() {
        return provenance;
    }

    public void setProvenance(DataSimpleProvenanceGetDTO provenance) {
        this.provenance = provenance;
    }

    public List<DataComputedGetDTO> getData() {
        return data;
    }

    public void setData(List<DataComputedGetDTO> data) {
        this.data = data;
    }

}
