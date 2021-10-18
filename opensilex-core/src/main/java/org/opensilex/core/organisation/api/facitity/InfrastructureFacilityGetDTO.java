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
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * DTO representing JSON for getting facility
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "organizations"})
public class InfrastructureFacilityGetDTO extends RDFObjectDTO {

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("organizations")
    protected List<NamedResourceDTO<InfrastructureModel>> infrastructures;

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
    public List<NamedResourceDTO<InfrastructureModel>> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<NamedResourceDTO<InfrastructureModel>> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public void toModel(InfrastructureFacilityModel model) {
        model.setUri(getUri());
        model.setType(getType());
        model.setName(getName());
        List<InfrastructureModel> infrastructureModels = new ArrayList<>();
        getInfrastructures().forEach(infrastructure -> {
            InfrastructureModel infrastructureModel = new InfrastructureModel();
            infrastructureModel.setUri(infrastructure.getUri());
            infrastructureModels.add(infrastructureModel);
        });
        model.setInfrastructures(infrastructureModels);
    }

    public void fromModel(InfrastructureFacilityModel model) {
        setUri(model.getUri());
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());
        setName(model.getName());
        if (model.getInfrastructures() != null) {
            setInfrastructures(model.getInfrastructures()
                    .stream()
                    .map(infrastructureModel ->
                            (NamedResourceDTO<InfrastructureModel>)NamedResourceDTO.getDTOFromModel(infrastructureModel))
                    .collect(Collectors.toList()));
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
