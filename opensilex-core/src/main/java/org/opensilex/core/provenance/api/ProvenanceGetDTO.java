//******************************************************************************
//                          ProvenanceGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.security.user.api.UserGetDTO;

/**
 * Provenance Get DTO
 * @author Alice Boizet
 */
public class ProvenanceGetDTO extends ProvenanceCreationDTO {
        
    @JsonProperty("prov_activity")
    protected List<ActivityGetDTO> activityGet;

    public List<ActivityGetDTO> getActivityGet() {
        return activityGet;
    }

    public void setActivityGet(List<ActivityGetDTO> activityGet) {
        this.activityGet = activityGet;
    }
    
    @JsonIgnore
    @Override
    public List<ActivityCreationDTO> getActivity() {
        return activity;
    }
    
    public static ProvenanceGetDTO fromModel(ProvenanceModel model){
        ProvenanceGetDTO dto = new ProvenanceGetDTO();        
        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }

        if (model.getActivity() != null) {
            List<ActivityGetDTO> activities = new ArrayList<>();
            for (ActivityModel act:model.getActivity()) {
                ActivityGetDTO actDTO = new ActivityGetDTO();
                activities.add(actDTO.fromModel(act));
            }
            dto.setActivityGet(activities);
        }
        
        dto.setAgents(model.getAgents());
        
        return dto;
    }
}
