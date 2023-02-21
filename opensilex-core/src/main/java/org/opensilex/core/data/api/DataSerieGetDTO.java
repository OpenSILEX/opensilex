//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.variable.api.VariableGetDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.utils.ListWithPagination;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    private List<DataGetDTO> data;


    public DataSerieGetDTO(VariableGetDTO variable, DataProvenanceModel provenance, List<DataGetDTO> data) {
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

    public List<DataGetDTO> getData() {
        return data;
    }

    public void setData(List<DataGetDTO> data) {
        this.data = data;
    }

}
