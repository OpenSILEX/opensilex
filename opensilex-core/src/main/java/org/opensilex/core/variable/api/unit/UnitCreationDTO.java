/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.unit;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.dal.UnitModel;

/**
 *
 * @author vidalmor
 */
@JsonPropertyOrder({
        "uri", "name", "description", "symbol", "alternative_symbol",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public class UnitCreationDTO extends BaseVariableCreationDTO<UnitModel> {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("alternative_symbol")
    private String alternativeSymbol;

    @Override
    protected UnitModel newModelInstance() {
        UnitModel model = new UnitModel();
        model.setSymbol(symbol);
        model.setAlternativeSymbol(alternativeSymbol);
        return model;
    }

    @ApiModelProperty(example = "Centimeter", required = true)
    public String getName() {
        return name;
    }

    @ApiModelProperty(example = "A common unit for describing a length")
    public String getDescription() {
        return description;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/Centimeter")
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(example = "cm")
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @ApiModelProperty(example = "cm")
    public String getAlternativeSymbol() {
        return alternativeSymbol;
    }

    public void setAlternativeSymbol(String alternativeSymbol) {
        this.alternativeSymbol = alternativeSymbol;
    }
    
}
