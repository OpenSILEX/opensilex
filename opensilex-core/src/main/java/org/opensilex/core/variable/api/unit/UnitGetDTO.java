/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.unit;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.UnitModel;


/**
 *
 * @author vidalmor
 */
public class UnitGetDTO extends SKOSReferencesDTO {

    private URI uri;

    private String name;

    private URI type;

    private String typeLabel;

    private String comment;

    private String symbol;

    private String alternativeSymbol;

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/Centimeter")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "Centimeter")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "A common unit for describing a length")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Unit")
    public URI getType() { return type; }

    public void setType(URI type) {
        this.type = type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    @ApiModelProperty(example = "Unit")
    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public static UnitGetDTO fromModel(UnitModel model) {
        UnitGetDTO dto = new UnitGetDTO();

        dto.uri = model.getUri();
        dto.name = model.getName();
        dto.typeLabel = model.getTypeLabel().getDefaultValue();
        dto.type = model.getType();
        dto.comment = model.getComment();

        dto.symbol = model.getSymbol();
        dto.alternativeSymbol = model.getAlternativeSymbol();
        dto.setSkosReferencesFromModel(model);

        return dto;
    }
}
