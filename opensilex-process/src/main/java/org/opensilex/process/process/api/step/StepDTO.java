//******************************************************************************
//                          StepDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.step.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Fernandez Emilie 
 */

public abstract class StepDTO {

    @JsonProperty("uri")
    protected URI uri;
    
    @JsonProperty("name")
    protected String name;
    
    @JsonProperty("start_date")
    protected LocalDate startDate;
    
    @JsonProperty("end_date")
    protected LocalDate endDate;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("input")
    protected List<URI> input = new ArrayList<>();

    @JsonProperty("output")
    protected List<URI> output = new ArrayList<>();

    @JsonProperty("facilities")
    protected List<URI> facilities = new ArrayList<>(); 
    
    @ApiModelProperty(example = "http://opensilex.dev/set/process#Process3456")
    public URI getUri() {
        return uri;
    }

    public StepDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    @ApiModelProperty(example = "mixing")
    public String getName() {
        return name;
    }

    public StepDTO setName(String name) {
        this.name = name;
        return this;
    }

    @ApiModelProperty(example = "2020-06-01")
    public LocalDate getStartDate() {
        return startDate;
    }

    public StepDTO setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    @ApiModelProperty(example = "2020-06-01")
    public LocalDate getEndDate() {
        return endDate;
    }

    public StepDTO setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    @ApiModelProperty(example = "description")
    public String getDescription() {
        return description;
    }

    public StepDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    @ApiModelProperty(example = "uri")
    public List<URI> getInput() {
        return input;
    }

    public StepDTO setInput(List<URI> input) {
        this.input = input;
        return this;
    }

    @ApiModelProperty(example = "uri")
    public List<URI> getOutput() {
        return output;
    }

    public StepDTO setOutput(List<URI> output) {
        this.output = output;
        return this;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public StepDTO setFacilities(List<URI> facilities) {
        this.facilities = facilities;
        return this;
    }

}
