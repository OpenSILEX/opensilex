/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
public class InfrastructureDTO extends NamedResourceDTO<InfrastructureModel> {

    
    @JsonProperty("rdf_type")
    protected URI type;
    
    @JsonProperty("rdf_type_name")
    protected String typeLabel;
    
    protected List<URI> parents;

    protected List<URI> children;

    public List<URI> getParents() {
        return parents;
    }

    public void setParents(List<URI> parents) {
        this.parents = parents;
    }

    public List<URI> getChildren() {
        return children;
    }

    public void setChildren(List<URI> children) {
        this.children = children;
    }

    @Override
    public InfrastructureModel newModelInstance() {
        return new InfrastructureModel();
    }

    @Override
    public void fromModel(InfrastructureModel model) {
        super.fromModel(model);

        List<InfrastructureModel> parents = model.getParents();
        if (parents != null) {
            setParents(parents
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        }

        List<InfrastructureModel> children = model.getChildren();
        if (children != null) {
            setChildren(children
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void toModel(InfrastructureModel model) {
        super.toModel(model);

        if (getParents() != null) {
            model.setParents(getParents().stream().map(parentUri -> {
                InfrastructureModel parentModel = new InfrastructureModel();
                parentModel.setUri(parentUri);
                return parentModel;
            }).collect(Collectors.toList()));
        }

        if (getChildren() != null) {
            model.setChildren(getChildren().stream().map(childUri -> {
                InfrastructureModel childModel = new InfrastructureModel();
                childModel.setUri(childUri);
                return childModel;
            }).collect(Collectors.toList()));
        }
    }

    public static InfrastructureDTO getDTOFromModel(InfrastructureModel model) {
        InfrastructureDTO dto = new InfrastructureDTO();
        dto.fromModel(model);

        return dto;
    }
}
