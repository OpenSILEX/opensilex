/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.organisation.dal.InfrastructureModel;
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

        List<InfrastructureModel> mc = model.getChildren();
        List<URI> children = new ArrayList<>(mc.size());
        mc.forEach(child -> {
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

        List<URI> mc = getChildren();
        if (mc != null) {
            List<InfrastructureModel> children = new ArrayList<>(mc.size());
            mc.forEach(child -> {
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
