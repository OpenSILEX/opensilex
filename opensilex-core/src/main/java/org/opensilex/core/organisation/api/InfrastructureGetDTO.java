/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.organisation.api.team.InfrastructureTeamDTO;
import org.opensilex.core.organisation.api.facitity.InfrastructureFacilityGetDTO;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.organisation.dal.InfrastructureTeamModel;


/**
 * DTO representing JSON for getting organisation
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "parent", "children","groups", "facilities"})
public class InfrastructureGetDTO extends InfrastructureDTO {

    
    protected List<InfrastructureTeamDTO> groups;

    
    protected List<InfrastructureFacilityGetDTO> facilities;

    public List<InfrastructureTeamDTO> getGroups() {
        return groups;
    }

    public void setGroups(List<InfrastructureTeamDTO> groups) {
        this.groups = groups;
    }

    public List<InfrastructureFacilityGetDTO> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<InfrastructureFacilityGetDTO> facilities) {
        this.facilities = facilities;
    }

    @Override
    public InfrastructureModel newModelInstance() {
        return new InfrastructureModel();
    }

    @Override
    public void fromModel(InfrastructureModel model) {
        super.fromModel(model);

        List<InfrastructureTeamDTO> groups;
        if (model.getGroups() != null) {
            groups = new ArrayList<>(model.getGroups().size());
            model.getGroups().forEach(group -> {
                groups.add(InfrastructureTeamDTO.getDTOFromModel(group));
            });
        } else {
            groups = new ArrayList<>();
        }
        setGroups(groups);

        List<InfrastructureFacilityGetDTO> facilities;
        if (model.getFacilities() != null) {
            facilities = new ArrayList<>(model.getFacilities().size());
            model.getFacilities().forEach(facility -> {
                facilities.add(InfrastructureFacilityGetDTO.getDTOFromModel(facility, false));
            });
        } else {
            facilities = new ArrayList<>();
        }
        setFacilities(facilities);
    }

    @Override
    public void toModel(InfrastructureModel model) {
        super.toModel(model);

        List<InfrastructureTeamModel> groups;
        if (getGroups() != null) {
            groups = new ArrayList<>(getGroups().size());
            getGroups().forEach(group -> {
                groups.add(group.newModel());
            });
        } else {
            groups = new ArrayList<>();
        }
        model.setGroups(groups);

        List<InfrastructureFacilityModel> facilities;
        if (getFacilities() != null) {
            facilities = new ArrayList<>(getFacilities().size());
            getFacilities().forEach(facility -> {
                facilities.add(facility.newModel());
            });
        } else {
            facilities = new ArrayList<>();
        }
        model.setFacilities(facilities);
    }

    public static InfrastructureGetDTO getDTOFromModel(InfrastructureModel model) {
        InfrastructureGetDTO dto = new InfrastructureGetDTO();
        dto.fromModel(model);

        return dto;
    }
}
