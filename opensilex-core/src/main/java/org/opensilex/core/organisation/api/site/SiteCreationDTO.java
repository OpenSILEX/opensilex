package org.opensilex.core.organisation.api.site;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.organisation.dal.SiteAddressModel;
import org.opensilex.core.organisation.dal.SiteModel;
import org.opensilex.security.group.dal.GroupModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@JsonPropertyOrder({"uri", "rdf_type", "name", "address", "organizations", "groups"})
public class SiteCreationDTO extends SiteDTO {
    protected SiteAddressDTO address;

    protected List<URI> organizations;

    protected List<URI> groups;

    public SiteAddressDTO getAddress() {
        return address;
    }

    public void setAddress(SiteAddressDTO address) {
        this.address = address;
    }

    public List<URI> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    @JsonIgnore
    @Override
    public String getTypeLabel() {
        return super.getTypeLabel();
    }

    @Override
    public void toModel(SiteModel model) {
        super.toModel(model);

        if (getAddress() != null) {
            model.setAddress(getAddress().newModel());
        }

        if (getOrganizations() != null) {
            List<InfrastructureModel> organizationModels = getOrganizations().stream().map(organizationUri -> {
                InfrastructureModel organizationModel = new InfrastructureModel();
                organizationModel.setUri(organizationUri);
                return organizationModel;
            }).collect(Collectors.toList());
            model.setOrganizations(organizationModels);
        }

        if (getGroups() != null) {
            List<GroupModel> groupModels = getGroups().stream().map(groupUri -> {
                GroupModel groupModel = new GroupModel();
                groupModel.setUri(groupUri);
                return groupModel;
            }).collect(Collectors.toList());
            model.setGroups(groupModels);
        }
    }

    @Override
    public void fromModel(SiteModel model) {
        super.fromModel(model);

        SiteAddressModel addressModel = model.getAddress();
        if (addressModel != null) {
            SiteAddressDTO addressDTO = new SiteAddressDTO();
            addressDTO.fromModel(addressModel);
            setAddress(addressDTO);
        }

        List<InfrastructureModel> organizationModels = model.getOrganizations();
        if (organizationModels != null) {
            setOrganizations(organizationModels.stream()
                    .map(InfrastructureModel::getUri)
                    .collect(Collectors.toList()));
        }

        List<GroupModel> groupModels = model.getGroups();
        if (groupModels != null) {
            setGroups(groupModels.stream()
                    .map(GroupModel::getUri)
                    .collect(Collectors.toList()));
        }
    }
}
