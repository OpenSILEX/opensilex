/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vmigot
 */
public class OWLClassPropertyRestrictionDTO {

    @ValidURI
    @NotNull
    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "RDF type", required = true, name = "rdf_type", example = "vocabulary:Plot")
    private URI classURI;

    private URI property;

    private boolean required;

    private boolean list;

    public URI getClassURI() {
        return classURI;
    }

    public void setClassURI(URI classURI) {
        this.classURI = classURI;
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

}
