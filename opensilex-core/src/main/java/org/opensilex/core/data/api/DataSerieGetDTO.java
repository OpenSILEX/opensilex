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

import java.util.List;

/**
 *
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "provenance", "data"
})
public class DataSerieGetDTO {

    @JsonProperty("provenance")
    private DataProvenanceModel provenance;

    @JsonProperty("data")
    private List<DataSimpleGetDTO> data;


    public DataSerieGetDTO(DataProvenanceModel provenance, List<DataSimpleGetDTO> data) {
        this.provenance = provenance;
        this.data = data;
    }

    public DataProvenanceModel getProvenance() {
        return provenance;
    }

    public void setProvenance(DataProvenanceModel provenance) {
        this.provenance = provenance;
    }

    public List<DataSimpleGetDTO> getData() {
        return data;
    }

    public void setData(List<DataSimpleGetDTO> data) {
        this.data = data;
    }

}
