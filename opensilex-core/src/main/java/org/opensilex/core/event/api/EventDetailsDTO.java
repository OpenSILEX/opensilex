//******************************************************************************
//                          EventDetailsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************


package org.opensilex.core.event.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.sparql.model.SPARQLModelRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@JsonPropertyOrder({
        "uri", "rdf_type", "rdf_type_name", "start", "end", "is_instant","description","targets","author","relations"
})
public class EventDetailsDTO extends EventGetDTO{

    private List<RDFObjectRelationDTO> relations;

    public List<RDFObjectRelationDTO> getRelations() {
        return relations;
    }

    public void setRelations(List<RDFObjectRelationDTO> relations) {
        this.relations = relations;
    }

    @Override
    public void fromModel(EventModel model) {

        super.fromModel(model);

        List<SPARQLModelRelation> relations = model.getRelations();

        this.relations = new ArrayList<>(relations.size());
        for (SPARQLModelRelation relation : relations) {
            this.relations.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }
    }
}
