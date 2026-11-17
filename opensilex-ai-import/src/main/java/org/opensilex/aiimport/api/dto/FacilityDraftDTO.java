//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.drafts.FacilityDraft;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * A facility a sheet of facilities describes: what the platform's facility form is opened with.
 *
 * @author Arnaud Charleroy
 */
public class FacilityDraftDTO {

    /**
     * The value of one property of the facility's type.
     */
    public static class RelationDTO {
        private String property;
        private String value;

        @ApiModelProperty(value = "The property, as the ontology names it")
        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private String name;

    @JsonProperty("rdf_type")
    private URI rdfType;

    private List<RelationDTO> relations = new ArrayList<>();

    public static FacilityDraftDTO fromModel(FacilityDraft model) {
        FacilityDraftDTO dto = new FacilityDraftDTO();
        dto.name = model.name();
        dto.rdfType = model.type();
        model.properties().forEach((property, value) -> {
            RelationDTO relation = new RelationDTO();
            relation.property = property;
            relation.value = value;
            dto.relations.add(relation);
        });
        return dto;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "The type the user chose for the sheet, or none")
    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    @ApiModelProperty(value = "The properties of the type the sheet gives a value to")
    public List<RelationDTO> getRelations() {
        return relations;
    }

    public void setRelations(List<RelationDTO> relations) {
        this.relations = relations;
    }
}
