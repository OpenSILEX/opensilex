//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.Map;

/**
 * What the user chose for one object sheet: its type, whether it takes part, what some columns
 * become. Absent fields are left as they were.
 * <p>
 * The conversation identifier travels in the body, as everywhere else in this API.
 *
 * @author Arnaud Charleroy
 */
public class ObjectSheetPlanDTO {

    @NotEmpty
    @JsonProperty("session_id")
    private String sessionId;

    @NotEmpty
    private String sheet;

    @JsonProperty("rdf_type")
    private URI rdfType;

    private Boolean included;

    private Map<String, String> mapping;

    @ApiModelProperty(value = "Identifier of the open conversation", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "The object sheet", required = true, example = "ed_placette")
    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    @ApiModelProperty(value = "The scientific object type of the sheet's objects")
    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    @ApiModelProperty(value = "Whether the sheet takes part in the next creation")
    public Boolean getIncluded() {
        return included;
    }

    public void setIncluded(Boolean included) {
        this.included = included;
    }

    @ApiModelProperty(value = "Column to property URI; empty to leave a column unwritten, 'x' or 'y' for "
            + "a position. Columns left out keep their mapping.")
    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }
}
