/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 *
 * @author vince
 */
public class NamedResourceDTO<T extends SPARQLNamedResourceModel> extends ResourceDTO<T> {

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void toModel(T model) {
        super.toModel(model);
        model.setName(getName());
    }

    @Override
    public void fromModel(T model) {
        super.fromModel(model);
        setName(model.getName());
    }

    @Override
    public T newModelInstance() {
        return (T) new SPARQLNamedResourceModel();
    }

    public static NamedResourceDTO getDTOFromModel(SPARQLNamedResourceModel model) {
        NamedResourceDTO dto = new NamedResourceDTO();
        dto.fromModel(model);

        return dto;
    }
}
