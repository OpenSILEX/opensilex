//******************************************************************************
//                          ProcessDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * @author Fernandez Emilie 
 */

public abstract class ProcessDTO {

@JsonProperty("uri")
protected URI uri;

@JsonProperty("name")
protected String name;

@JsonProperty("experiment")
protected URI experiment;

@JsonProperty("creation_date")
protected LocalDate creationDate;

@JsonProperty("destruction_date")
protected LocalDate destructionDate;

@JsonProperty("description")
protected String description;

@JsonProperty("organisations") 
protected List<URI> infrastructures = new ArrayList<>();

@JsonProperty("scientific_supervisors")
protected List<URI> scientificSupervisors = new ArrayList<>();

@JsonProperty("technical_supervisors")
protected List<URI> technicalSupervisors = new ArrayList<>();

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

public URI getExperiment() {
    return experiment;
}

public ProcessDTO setExperiment(URI experiment) {
    this.experiment = experiment;
    return this;
}

@NotNull
@ApiModelProperty(example = "2021-02-20")
public LocalDate getCreationDate() {
    return creationDate;
}

public ProcessDTO setCreationDate(LocalDate creationDate) {
    this.creationDate = creationDate;
    return this;
}

public LocalDate getDestructionDate() {
    return destructionDate;
}

public ProcessDTO setDestructionDate(LocalDate destructionDate) {
    this.destructionDate = destructionDate;
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

public List<URI> getInfrastructures() {
    return infrastructures;
}

public ProcessDTO setInfrastructures(List<URI> infrastructures) {
    this.infrastructures = infrastructures;
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
