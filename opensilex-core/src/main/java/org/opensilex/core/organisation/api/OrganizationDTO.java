/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceDagDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author vince
 */
public class OrganizationDTO extends ResourceDagDTO<OrganizationModel> {
    @Override
    public OrganizationModel newModelInstance() {
        return new OrganizationModel();
    }

    @Override
    public void fromModel(OrganizationModel model) {
        super.fromModel(model);

        List<OrganizationModel> parents = model.getParents();
        if (parents != null) {
            setParents(parents
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        }

        List<OrganizationModel> children = model.getChildren();
        if (children != null) {
            setChildren(children
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void toModel(OrganizationModel model) {
        super.toModel(model);

        if (getParents() != null) {
            model.setParents(getParents().stream().map(parentUri -> {
                OrganizationModel parentModel = new OrganizationModel();
                parentModel.setUri(parentUri);
                return parentModel;
            }).collect(Collectors.toList()));
        }

        if (getChildren() != null) {
            model.setChildren(getChildren().stream().map(childUri -> {
                OrganizationModel childModel = new OrganizationModel();
                childModel.setUri(childUri);
                return childModel;
            }).collect(Collectors.toList()));
        }
    }

    public static OrganizationDTO getDTOFromModel(OrganizationModel model) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.fromModel(model);

        return dto;
    }
}
