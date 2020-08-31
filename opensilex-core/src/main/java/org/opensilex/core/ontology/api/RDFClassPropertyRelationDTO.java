/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class RDFClassPropertyRelationDTO {

    private URI domain;
    
    private URI property;

    private boolean required;

    private boolean list;

    private boolean isObjectProperty;

    public URI getDomain() {
        return domain;
    }

    public void setDomain(URI domain) {
        this.domain = domain;
    }

    public URI getProperty() {
        return property;
    }

    public void setProperty(URI property) {
        this.property = property;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public boolean getIsObjectProperty() {
        return isObjectProperty;
    }

    public void setIsObjectProperty(boolean isObjectProperty) {
        this.isObjectProperty = isObjectProperty;
    }

}
