/*
 * *****************************************************************************
 *                         VueDataTypeDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 23/06/2025 13:13
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public class VueDataTypeDTO {

    @ValidURI
    protected URI uri;

    @ValidURI
    @JsonProperty("short_uri")
    protected URI shortUri;
    
    @JsonProperty("input_component")
    protected String inputComponent;
    
    @JsonProperty("view_component")
    protected String viewComponent;
    
    @JsonProperty("label_key")
    protected String labelKey;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getShortUri() {
        return SPARQLDeserializers.formatURI(getUri());
    }

    public String getInputComponent() {
        return inputComponent;
    }

    public void setInputComponent(String inputComponent) {
        this.inputComponent = inputComponent;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

}
