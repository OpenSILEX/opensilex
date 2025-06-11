//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.agroportal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.external.agroportal.OntologyAgroportalModel;

import javax.validation.constraints.NotNull;

/**
 * Represents an ontology from AgroPortal
 * @author brice
 */
public class OntologyAgroportalDTO {

    @NotNull
    private String id;
    private String name;
    @NotNull
    private String acronym;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }


    public static OntologyAgroportalDTO fromModel(OntologyAgroportalModel model) {
        OntologyAgroportalDTO dto = new OntologyAgroportalDTO();

        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setAcronym(model.getAcronym());

        return dto;
    }

    public OntologyAgroportalModel toModel() {
        OntologyAgroportalModel model = new OntologyAgroportalModel();

        model.setId(id);
        model.setName(name);
        model.setAcronym(acronym);

        return model;
    }

}
