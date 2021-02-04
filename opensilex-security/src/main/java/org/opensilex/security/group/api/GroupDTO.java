/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri","rdf_type","rdf_type_name", "name", "description", "user_profiles"})
public class GroupDTO extends NamedResourceDTO<GroupModel> {
    
     @JsonProperty("rdf_type")
    protected URI type;
    
    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    protected String description;

    @JsonProperty("user_profiles")
    protected List<GroupUserProfileDTO> userProfiles;
    

    @ApiModelProperty(value = "Group URI", example = "http://opensilex.dev/groups#Experiment_manager")
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(value = "Group name", example = "Experiment manager",required = true)
    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "Group description", example = "Group for all experiments managers",required = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Group user with profile")
    public List<GroupUserProfileDTO> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileDTO> userProfiles) {
        this.userProfiles = userProfiles;
    }

    @Override
    public GroupModel newModelInstance() {
        return new GroupModel();
    }

    @Override
    public void toModel(GroupModel group) {
        super.toModel(group);

        group.setDescription(getDescription());

        if (getUserProfiles() != null) {
            List<GroupUserProfileDTO> up = getUserProfiles();
            List<GroupUserProfileModel> userProfilesModel = new ArrayList<>(up.size());
            for (GroupUserProfileDTO userProfile : up) {
                GroupUserProfileModel userProfileModel = userProfile.newModel();
                userProfilesModel.add(userProfileModel);
            }
            group.setUserProfiles(userProfilesModel);
        }
    }

    @Override
    public void fromModel(GroupModel model) {
        super.fromModel(model);

        setDescription(model.getDescription());

        List<GroupUserProfileModel> up = model.getUserProfiles();
        List<GroupUserProfileDTO> userProfilesDTO = new ArrayList<>(up.size());
        for (GroupUserProfileModel userProfile : up) {
            GroupUserProfileDTO dto = newUserProfileDtoInstance();
            dto.fromModel(userProfile);
            userProfilesDTO.add(dto);
        }
        setUserProfiles(userProfilesDTO);
    }

    public static GroupDTO getDTOFromModel(GroupModel model) {
        GroupDTO dto = new GroupDTO();
        dto.fromModel(model);

        return dto;
    }

    public GroupUserProfileDTO newUserProfileDtoInstance() {
        return new GroupUserProfileDTO();
    }
}
