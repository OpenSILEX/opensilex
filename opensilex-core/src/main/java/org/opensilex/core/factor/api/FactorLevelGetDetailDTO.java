/*
 * ******************************************************************************
 *                                     FactorLevelGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import org.opensilex.core.factor.dal.FactorLevelModel;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorLevelGetDetailDTO extends FactorLevelGetDTO {
    @JsonPropertyOrder({"uri", "name", "description","factor_uri","factor_name"})

    private URI factorURI;

    private String factorName;

    public URI getFactorURI() {
        return factorURI;
    }

    public void setFactorURI(URI factorURI) {
        this.factorURI = factorURI;
    }

    public String getFactorName() {
        return factorName;
    }

    public void setFactorName(String factorName) {
        this.factorName = factorName;
    }

    public static FactorLevelGetDetailDTO fromModel(FactorLevelModel model) {
        FactorLevelGetDetailDTO dto = new FactorLevelGetDetailDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setFactorURI(model.getFactor().getUri());
        dto.setFactorName(model.getFactor().getName());
        return dto;
    }
}
