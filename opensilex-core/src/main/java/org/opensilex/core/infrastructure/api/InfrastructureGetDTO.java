/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.util.LinkedList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureFacilityModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.core.infrastructure.dal.InfrastructureTeamModel;

/**
 *
 * @author vince
 */
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

        List<InfrastructureTeamDTO> groups = new LinkedList<>();
        if (model.getGroups() != null) {
            model.getGroups().forEach(group -> {
                groups.add(InfrastructureTeamDTO.getDTOFromModel(group));
            });

        }
        setGroups(groups);

        List<InfrastructureFacilityGetDTO> facilities = new LinkedList<>();
        if (model.getFacilities() != null) {
            model.getFacilities().forEach(device -> {
                facilities.add(InfrastructureFacilityGetDTO.getDTOFromModel(device));
            });
        }
        setFacilities(facilities);
    }

    @Override
    public void toModel(InfrastructureModel model) {
        super.toModel(model);

        List<InfrastructureTeamModel> groups = new LinkedList<>();
        if (getGroups() != null) {
            getGroups().forEach(group -> {
                groups.add(group.newModel());
            });
        }
        model.setGroups(groups);

        List<InfrastructureFacilityModel> facilities = new LinkedList<>();
        if (getFacilities() != null) {
            getFacilities().forEach(facility -> {
                facilities.add(facility.newModel());
            });
        }
        model.setFacilities(facilities);
    }

    public static InfrastructureGetDTO getDTOFromModel(InfrastructureModel model) {
        InfrastructureGetDTO dto = new InfrastructureGetDTO();
        dto.fromModel(model);

        return dto;
    }
}
