/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facitity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.sparql.model.SPARQLModelRelation;

/**
 * DTO representing JSON for getting facility
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "organisation"})
public class InfrastructureFacilityGetDTO extends RDFObjectDTO {

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("organisation")
    protected URI infrastructure;

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    @NotNull
    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    public void toModel(InfrastructureFacilityModel model) {
        model.setUri(getUri());
        model.setType(getType());
        model.setName(getName());
        InfrastructureModel infra = new InfrastructureModel();
        infra.setUri(getInfrastructure());
        model.setInfrastructure(infra);
    }

    public void fromModel(InfrastructureFacilityModel model) {
        setUri(model.getUri());
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());
        setName(model.getName());
        if (model != null && model.getInfrastructure() != null) {
            setInfrastructure(model.getInfrastructure().getUri());
        }
    }

    public InfrastructureFacilityModel newModel() {
        InfrastructureFacilityModel instance = new InfrastructureFacilityModel();
        toModel(instance);
        
        return instance;
    }

    public static InfrastructureFacilityGetDTO getDTOFromModel(InfrastructureFacilityModel model, boolean withDetails) {
        InfrastructureFacilityGetDTO dto = new InfrastructureFacilityGetDTO();
        dto.fromModel(model);

        if (withDetails) {
            List<RDFObjectRelationDTO> relationsDTO = new ArrayList<>();

            for (SPARQLModelRelation relation : model.getRelations()) {
                relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
            }

            dto.setRelations(relationsDTO);
        }

        return dto;
    }
}
