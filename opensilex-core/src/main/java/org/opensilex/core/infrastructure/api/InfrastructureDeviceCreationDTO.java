/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import org.opensilex.core.infrastructure.dal.InfrastructureDeviceModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.sparql.mapping.SPARQLProxyListObject;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vince
 */
class InfrastructureDeviceCreationDTO {

    protected URI uri;

    protected URI type;

    protected String name;

    protected URI infrastructure;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    public InfrastructureDeviceModel newModel(SPARQLService sparql, String lang) throws Exception {
        InfrastructureDeviceModel model = new InfrastructureDeviceModel();
        model.setUri(getUri());
        model.setName(getName());
        if (getType() != null) {
            model.setType(getType());
        }

        model.setInfrastructure(sparql.getByURI(InfrastructureModel.class, getInfrastructure(), lang));

        return model;
    }
}
