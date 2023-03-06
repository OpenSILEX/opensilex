//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.unit;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
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

public class UnitDetailsDTO extends BaseVariableDetailsDTO<UnitModel> {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("alternative_symbol")
    private String alternativeSymbol;

    public UnitDetailsDTO(UnitModel model) {
        super(model);
        this.symbol = model.getSymbol();
        this.alternativeSymbol = model.getAlternativeSymbol();
    }

    public UnitDetailsDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/Centimeter")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Centimeter")
    public String getName() {
        return name;
    }

    @Override
    @ApiModelProperty(example = "A common unit for describing a length")
    public String getDescription() {
        return description;
    }

    @Override
    public UnitModel toModel() {
        UnitModel model = new UnitModel();
        setBasePropertiesToModel(model);
        model.setSymbol(this.getSymbol());
        model.setAlternativeSymbol(this.getAlternativeSymbol());
        return model;
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
