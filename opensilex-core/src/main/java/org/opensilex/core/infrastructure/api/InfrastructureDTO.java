/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vince
 */
public class InfrastructureDTO extends NamedResourceDTO<InfrastructureModel> {

    protected URI parent;

    protected List<URI> children;

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
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

        if (model.getParent() != null) {
            setParent(model.getParent().getUri());
        }

        List<URI> children = new LinkedList<>();
        model.getChildren().forEach(child -> {
            children.add(child.getUri());
        });

    }

    @Override
    public void toModel(InfrastructureModel model) {
        super.toModel(model);

        if (getParent() != null) {
            InfrastructureModel parentModel = new InfrastructureModel();
            parentModel.setUri(getParent());
            model.setParent(parentModel);
        }

        if (getChildren() != null) {
            List<InfrastructureModel> children = new LinkedList<>();
            getChildren().forEach(child -> {
                InfrastructureModel childModel = new InfrastructureModel();
                childModel.setUri(child);
                children.add(childModel);
            });
            model.setChildren(children);
        }

    }

    public static InfrastructureDTO getDTOFromModel(InfrastructureModel model) {
        InfrastructureDTO dto = new InfrastructureDTO();
        dto.fromModel(model);

        return dto;
    }
}
