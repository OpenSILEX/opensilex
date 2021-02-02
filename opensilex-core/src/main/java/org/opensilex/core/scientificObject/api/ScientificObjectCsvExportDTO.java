/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.Min;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vince
 */
public class ScientificObjectCsvExportDTO {

    @ValidURI
    @ApiModelProperty(value = "List of scientific objects URI to export")
    @Min(1)
    protected List<URI> objects;

    @ValidURI
    @ApiModelProperty(value = "Scientific object experiment URI")
    protected URI experiment;

    public List<URI> getObjects() {
        return objects;
    }

    public void setObjects(List<URI> objects) {
        this.objects = objects;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

}
