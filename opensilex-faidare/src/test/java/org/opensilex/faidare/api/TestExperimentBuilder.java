package org.opensilex.faidare.api;

import org.opensilex.core.experiment.api.ExperimentCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestExperimentBuilder {
    private static final String stringPrefix = "default experiment ";

    private URI uri = new URI("test:default-experiment-uri/");

    private String name = stringPrefix + "name";

    private LocalDate startDate = LocalDate.of(2001,1,1);

    private LocalDate endDate = LocalDate.of(2002,2,2);

    private String description = stringPrefix + "description";

    private String objective = stringPrefix + "objective";

    private List<URI> organizations = new ArrayList<>();

    private List<URI> facilities = new ArrayList<>();

    private List<URI> projects = new ArrayList<>();

    private List<URI> scientificSupervisors = new ArrayList<>();

    private List<URI> technicalSupervisors = new ArrayList<>();

    private List<URI> groups = new ArrayList<>();

    private List<URI> factors = new ArrayList<>();

    private Boolean isPublic = false;

    public TestExperimentBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestExperimentBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestExperimentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public TestExperimentBuilder setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public TestExperimentBuilder setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TestExperimentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getObjective() {
        return objective;
    }

    public TestExperimentBuilder setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    public List<URI> getOrganizations() {
        return organizations;
    }

    public TestExperimentBuilder setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
        return this;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public TestExperimentBuilder setFacilities(List<URI> facilities) {
        this.facilities = facilities;
        return this;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public TestExperimentBuilder setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    public List<URI> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public TestExperimentBuilder setScientificSupervisors(List<URI> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
        return this;
    }

    public List<URI> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public TestExperimentBuilder setTechnicalSupervisors(List<URI> technicalSupervisors) {
        this.technicalSupervisors = technicalSupervisors;
        return this;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public TestExperimentBuilder setGroups(List<URI> groups) {
        this.groups = groups;
        return this;
    }

    public List<URI> getFactors() {
        return factors;
    }

    public TestExperimentBuilder setFactors(List<URI> factors) {
        this.factors = factors;
        return this;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public TestExperimentBuilder setPublic(Boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    private final List<ExperimentCreationDTO> dtoList = new ArrayList<>();

    public List<ExperimentCreationDTO> getDTOList() {
        return dtoList;
    }

    public ExperimentCreationDTO createDTO() throws Exception {
        ExperimentCreationDTO dto = new ExperimentCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName() + dtoList.size());
        dto.setStartDate(getStartDate());
        dto.setEndDate(getEndDate());
        dto.setDescription(getDescription());
        dto.setObjective(getObjective());
        dto.setOrganizations(getOrganizations());
        dto.setFacilities(getFacilities());
        dto.setProjects(getProjects());
        dto.setScientificSupervisors(getScientificSupervisors());
        dto.setTechnicalSupervisors(getTechnicalSupervisors());
        dto.setGroups(getGroups());
        dto.setFactors(getFactors());
        dto.setIsPublic(getPublic());

        dtoList.add(dto);
        return dto;
    }
}
