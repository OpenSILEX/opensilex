/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vince
 */
public class ScientificObjectCsvExportDTO {

    @ValidURI
    @ApiModelProperty(value = "List of scientific objects URI to export")
    protected List<URI> uris;

    @ValidURI
    @ApiModelProperty(value = "Scientific object experiment URI")
    protected URI experiment;

    public List<URI> getUris() {
        return uris;
    }

    public void setUris(List<URI> uris) {
        this.uris = uris;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

}
