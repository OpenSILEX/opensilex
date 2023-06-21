//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasmGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Maximilian HART
 */
public class GermplasmGroupGetDTO extends GermplasmGroupDTO {

    @JsonProperty("germplasm_count")
    protected int germplasmCount;

    public int getGermplasmCount(){
        return this.germplasmCount;
    }

    public void setGermplasmCount(int germplasmCount){
        this.germplasmCount = germplasmCount;
    }

    public static GermplasmGroupGetDTO fromModel(GermplasmGroupModel model) {

        GermplasmGroupGetDTO dto = new GermplasmGroupGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());

        if (model.getPublicationDate() != null) {
            dto.setPublicationDate(model.getPublicationDate());
        }

        if (model.getLastUpdateDate() != null) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }

        return dto;
    }
}
