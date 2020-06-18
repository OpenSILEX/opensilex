/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.unit;

import java.net.URI;

import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.unit.UnitModel;


/**
 *
 * @author vidalmor
 */
public class UnitGetDTO extends SKOSReferencesDTO {

    private URI uri;

    private String label;

    private String comment;

    private String symbol;

    private String alternativeSymbol;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAlternativeSymbol() {
        return alternativeSymbol;
    }

    public void setAlternativeSymbol(String alternativeSymbol) {
        this.alternativeSymbol = alternativeSymbol;
    }


    public static UnitGetDTO fromModel(UnitModel model) {
        UnitGetDTO dto = new UnitGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        dto.setComment(model.getComment());
        dto.setSymbol(model.getSymbol());
        dto.setAlternativeSymbol(model.getAlternativeSymbol());
        dto.setSkosReferencesFromModel(model);

        return dto;
    }
}
