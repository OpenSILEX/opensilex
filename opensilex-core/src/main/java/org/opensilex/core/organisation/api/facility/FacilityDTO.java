/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.sparql.model.SPARQLModelRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing JSON for getting facility
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name", "address"})
public class FacilityDTO extends RDFObjectDTO {

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    protected String name;

    protected FacilityAddressDTO address;

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

    public FacilityAddressDTO getAddress() {
        return address;
    }

    public void setAddress(FacilityAddressDTO address) {
        this.address = address;
    }

    public void toModel(FacilityModel model) {
        model.setUri(getUri());
        model.setType(getType());
        model.setName(getName());
        if (getAddress() != null) {
            model.setAddress(getAddress().newModel());
        }
    }

    public void fromModel(FacilityModel model) {
        setUri(model.getUri());
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());
        setName(model.getName());
        if (model.getAddress() != null) {
            FacilityAddressDTO address = new FacilityAddressDTO();
            address.fromModel(model.getAddress());
            setAddress(address);
        }
    }

    public FacilityModel newModel() {
        FacilityModel instance = new FacilityModel();
        toModel(instance);

        return instance;
    }

    public static FacilityGetDTO getDTOFromModel(FacilityModel model, boolean withDetails) {
        FacilityGetDTO dto = new FacilityGetDTO();
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
