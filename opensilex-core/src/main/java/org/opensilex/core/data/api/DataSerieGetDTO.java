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
import org.opensilex.core.variable.api.VariableGetDTO;

import java.util.List;

/**
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "variable", "provenance", "data"
})
public class DataSerieGetDTO {

    @JsonProperty("variable")
    private VariableGetDTO variable;

    @JsonProperty("provenance")
    private DataProvenanceModel provenance;

    @JsonProperty("data")
    private List<DataSimpleGetDTO> data;


    public DataSerieGetDTO(VariableGetDTO variable, DataProvenanceModel provenance, List<DataSimpleGetDTO> data) {
        this.variable = variable;
        this.provenance = provenance;
        this.data = data;
    }

    public VariableGetDTO getVariable() {
        return variable;
    }

    public void setVariable(VariableGetDTO variable) {
        this.variable = variable;
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
