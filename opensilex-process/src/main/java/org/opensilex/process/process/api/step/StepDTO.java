//******************************************************************************
//                          StepDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.step.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;

/**
 * @author Fernandez Emilie 
 */

public abstract class StepDTO {

    @JsonProperty("uri")
    protected URI uri;
    
    @JsonProperty("name")
    protected String name;
    
    @JsonProperty("start")
    protected String start;
    
    @JsonProperty("end")
    protected String end;

    @JsonProperty("after")
    protected URI after;
    
    @JsonProperty("before")
    protected URI before;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("input")
    protected List<URI> input = new ArrayList<>();

    @JsonProperty("output")
    protected List<URI> output = new ArrayList<>();
    
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

    @ValidOffsetDateTime
    @ApiModelProperty(example = "2022-01-08T12:00:00+01:00")
    public String getStart() {
        return start;
    }

    public StepDTO setStart(String start) {
        this.start = start;
        return this;
    }

    @ValidOffsetDateTime
    @ApiModelProperty(example = "2022-01-08T12:00:00+01:00")
    public String getEnd() {
        return end;
    }

    public StepDTO setEnd(String end) {
        this.end = end;
        return this;
    }

    @ApiModelProperty(example = "http://example.com")
    public URI getAfter() {
        return after;
    }

    public StepDTO setAfter(URI after) {
        this.after = after;
        return this;
    }

    @ApiModelProperty(example = "http://example.com")
    public URI getBefore() {
        return before;
    }

    public StepDTO setBefore(URI before) {
        this.before = before;
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

}
