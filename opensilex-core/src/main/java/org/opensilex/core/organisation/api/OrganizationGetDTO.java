/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceDagReferenceDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * DTO representing JSON for getting organisation
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "publisher", "publication_date", "last_updated_date", "name", "parents", "children", "groups", "facilities", "sites", "experiments"})
public class OrganizationGetDTO extends ResourceDagReferenceDTO<OrganizationModel> {
    
    protected List<NamedResourceDTO<GroupModel>> groups;

    protected List<NamedResourceDTO<FacilityModel>> facilities;

    protected List<NamedResourceDTO<SiteModel>> sites;

    protected List<NamedResourceDTO<ExperimentModel>> experiments;

    protected UserGetDTO publisher;

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public List<NamedResourceDTO<GroupModel>> getGroups() {
        return groups;
    }

    public void setGroups(List<NamedResourceDTO<GroupModel>> groups) {
        this.groups = groups;
    }

    public List<NamedResourceDTO<FacilityModel>> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<NamedResourceDTO<FacilityModel>> facilities) {
        this.facilities = facilities;
    }

    public List<NamedResourceDTO<SiteModel>> getSites() {
        return sites;
    }

    public void setSites(List<NamedResourceDTO<SiteModel>> sites) {
        this.sites = sites;
    }

    public List<NamedResourceDTO<ExperimentModel>> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<NamedResourceDTO<ExperimentModel>> experiments) {
        this.experiments = experiments;
    }

    @Override
    public OrganizationModel newModelInstance() {
        return new OrganizationModel();
    }

    @Override
    public void fromModel(OrganizationModel model) {
        super.fromModel(model);

        List<NamedResourceDTO<GroupModel>> groups;
        if (model.getGroups() != null) {
            groups = new ArrayList<>(model.getGroups().size());
            model.getGroups().forEach(group -> {
                groups.add(NamedResourceDTO.getDTOFromModel(group));
            });
        } else {
            groups = new ArrayList<>();
        }
        setGroups(groups);

        List<NamedResourceDTO<FacilityModel>> facilities;
        if (model.getFacilities() != null) {
            facilities = new ArrayList<>(model.getFacilities().size());
            model.getFacilities().forEach(facility -> {
                facilities.add(NamedResourceDTO.getDTOFromModel(facility));
            });
        } else {
            facilities = new ArrayList<>();
        }
        setFacilities(facilities);

        List<NamedResourceDTO<SiteModel>> sites;
        if (model.getSites() != null) {
            sites = new ArrayList<>(model.getSites().size());
            model.getSites().forEach(site -> {
                sites.add(NamedResourceDTO.getDTOFromModel(site));
            });
        } else {
            sites = new ArrayList<>();
        }
        setSites(sites);

        List<NamedResourceDTO<ExperimentModel>> experiments;
        if (model.getExperiments() != null) {
            experiments = new ArrayList<>(model.getExperiments().size());
            model.getExperiments().forEach(experiment -> {
                experiments.add(NamedResourceDTO.getDTOFromModel(experiment));
            });
        } else {
            experiments = new ArrayList<>();
        }
        setExperiments(experiments);
    }

    @Override
    public void toModel(OrganizationModel model) {
        super.toModel(model);

        List<GroupModel> groups;
        if (getGroups() != null) {
            groups = new ArrayList<>(getGroups().size());
            getGroups().forEach(group -> {
                groups.add(group.newModel());
            });

            model.setGroups(groups);
        }

        List<FacilityModel> facilities;
        if (getFacilities() != null) {
            facilities = new ArrayList<>(getFacilities().size());
            getFacilities().forEach(facility -> {
                facilities.add(facility.newModel());
            });

            model.setFacilities(facilities);
        }

        if (getSites() != null) {
            model.setSites(getSites().stream()
                    .map(NamedResourceDTO::newModel)
                    .collect(Collectors.toList()));
        }

        if (getExperiments() != null) {
            model.setExperiments(getExperiments().stream()
                    .map(NamedResourceDTO::newModel)
                    .collect(Collectors.toList()));
        }
    }

    public static OrganizationGetDTO getDTOFromModel(OrganizationModel model) {
        OrganizationGetDTO dto = new OrganizationGetDTO();
        dto.fromModel(model);

        return dto;
    }

    public static List<OrganizationGetDTO> getDTOListFromModel(List<OrganizationModel> modelList) {
        return modelList
                .stream().map(OrganizationGetDTO::getDTOFromModel)
                .collect(Collectors.toList());
    }
}
