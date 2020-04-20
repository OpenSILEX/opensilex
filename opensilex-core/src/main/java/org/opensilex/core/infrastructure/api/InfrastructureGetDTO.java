/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.util.LinkedList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureDeviceModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.core.infrastructure.dal.InfrastructureTeamModel;
import org.opensilex.security.group.api.GroupDTO;
import org.opensilex.security.group.dal.GroupModel;

/**
 *
 * @author vince
 */
public class InfrastructureGetDTO extends InfrastructureDTO {

    protected List<InfrastructureTeamDTO> groups;

    protected List<InfrastructureDeviceGetDTO> devices;

    public List<InfrastructureTeamDTO> getGroups() {
        return groups;
    }

    public void setGroups(List<InfrastructureTeamDTO> groups) {
        this.groups = groups;
    }

    public List<InfrastructureDeviceGetDTO> getDevices() {
        return devices;
    }

    public void setDevices(List<InfrastructureDeviceGetDTO> devices) {
        this.devices = devices;
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

        List<InfrastructureDeviceGetDTO> devices = new LinkedList<>();
        if (model.getDevices() != null) {
            model.getDevices().forEach(device -> {
                devices.add(InfrastructureDeviceGetDTO.getDTOFromModel(device));
            });
        }
        setDevices(devices);
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

        List<InfrastructureDeviceModel> devices = new LinkedList<>();
        if (getDevices() != null) {
            getDevices().forEach(device -> {
                devices.add(device.newModel());
            });
        }
        model.setDevices(devices);
    }

    public static InfrastructureGetDTO getDTOFromModel(InfrastructureModel model) {
        InfrastructureGetDTO dto = new InfrastructureGetDTO();
        dto.fromModel(model);

        return dto;
    }
}
