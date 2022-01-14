//******************************************************************************
//                          ProcessDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;

/**
 * @author Fernandez Emilie 
 */

public abstract class ProcessDTO {

@JsonProperty("uri")
protected URI uri;

@JsonProperty("name")
protected String name;

@ValidURI
@JsonProperty("experiment")
protected List<URI> experiment = new ArrayList<>();

@JsonProperty("start")
protected String start;

@JsonProperty("end")
protected String end;

@JsonProperty("description")
protected String description;

@ValidURI
@JsonProperty("facilities")
protected List<URI> facilities = new ArrayList<>(); 

@ValidURI
@JsonProperty("scientific_supervisors")
protected List<URI> scientificSupervisors = new ArrayList<>();

@ValidURI
@JsonProperty("technical_supervisors")
protected List<URI> technicalSupervisors = new ArrayList<>();

@ValidURI
@JsonProperty("step")
protected List<URI> step = new ArrayList<>();

public URI getUri() {
    return uri;
}

public ProcessDTO setUri(URI uri) {
    this.uri = uri;
    return this;
}

public String getName() {
    return name;
}

public ProcessDTO setName(String name) {
    this.name = name;
    return this;
}

public List<URI> getExperiment() {
    return experiment;
}

public ProcessDTO setExperiment(List<URI> experiment) {
    this.experiment = experiment;
    return this;
}

@ValidOffsetDateTime
@ApiModelProperty(example = "2022-01-08T12:00:00+01:00")
public String getStart() {
    return start;
}

public ProcessDTO setStart(String start) {
    this.start = start;
    return this;
}

@ValidOffsetDateTime
@ApiModelProperty(example = "2022-01-08T12:00:00+01:00")
public String getEnd() {
    return end;
}

public ProcessDTO setEnd(String end) {
    this.end = end;
    return this;
}

public String getDescription() {
    return description;
}

public ProcessDTO setDescription(String description) {
    this.description = description;
    return this;
}

public List<URI> getScientificSupervisors() {
    return scientificSupervisors;
}

public ProcessDTO setScientificSupervisors(List<URI> scientificSupervisors) {
    this.scientificSupervisors = scientificSupervisors;
    return this;
}

public List<URI> getTechnicalSupervisors() {
    return technicalSupervisors;
}

public ProcessDTO setTechnicalSupervisors(List<URI> technicalSupervisors) {
    this.technicalSupervisors = technicalSupervisors;
    return this;
}

public List<URI> getFacilities() {
    return facilities;
}

public ProcessDTO setFacilities(List<URI> facilities) {
    this.facilities = facilities;
    return this;
}

public List<URI> getStep() {
    return step;
}

public ProcessDTO setStep(List<URI> step) {
    this.step = step;
    return this;
}
}
