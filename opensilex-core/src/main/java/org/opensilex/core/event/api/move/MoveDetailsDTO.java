package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.sparql.model.SPARQLModelRelation;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@JsonPropertyOrder({
        "uri", "rdf_type", "rdf_type_name", "start", "end", "is_instant","description","targets","author", "relations",
        "from","to","targets_positions"
})
public class MoveDetailsDTO extends MoveGetDTO{

    private List<RDFObjectRelationDTO> relations;

    /**
     * Public constructor necessary for deserialization (e.g. for deserializing responses in automatic API tests)
     */
    public MoveDetailsDTO() {

    }

    public MoveDetailsDTO(MoveModel model) throws URISyntaxException, JsonProcessingException {
        super(model);

        List<SPARQLModelRelation> relations = model.getRelations();

        this.relations = new ArrayList<>(relations.size());
        for (SPARQLModelRelation relation : relations) {
            this.relations.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }
    }

    public List<RDFObjectRelationDTO> getRelations() {
        return relations;
    }

    public void setRelations(List<RDFObjectRelationDTO> relations) {
        this.relations = relations;
    }


}
