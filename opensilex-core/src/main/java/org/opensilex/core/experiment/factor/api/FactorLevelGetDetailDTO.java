/*
 * ******************************************************************************
 *                                     FactorLevelGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorLevelGetDetailDTO extends FactorLevelGetDTO {
    @JsonPropertyOrder({"uri", "name", "description","factor","factor_name"})

    @JsonProperty("factor")
    private URI factorURI;

    @JsonProperty("factor_name")
    private String factorName;

    @Schema(example = "http://opensilex.dev/set/factors#irrigation")
    public URI getFactorURI() {
        return factorURI;
    }

    public void setFactorURI(URI factorURI) {
        this.factorURI = factorURI;
    }

    @Schema(example = "Irrigation")
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
