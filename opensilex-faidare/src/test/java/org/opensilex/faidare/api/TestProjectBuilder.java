package org.opensilex.faidare.api;

import org.opensilex.core.project.api.ProjectCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestProjectBuilder {

    private static final String stringPrefix = "default project ";
    private URI uri = new URI("test:default-project-uri/");
    private String shortname = stringPrefix + "short name";
    private String financialFunding = stringPrefix + "financial funding";
    private LocalDate startDate = LocalDate.of(2001,1,1);
    private LocalDate endDate = LocalDate.of(2002,2,2);
    private String name = stringPrefix + "name";
    private String description = stringPrefix + "description";
    private String objective = stringPrefix + "objective";
    private URI homePage = new URI("test:default-project-uri/homePage/");
    private List<URI> administrativeContacts = new ArrayList<>();
    private List<URI> coordinators = new ArrayList<>();
    private List<URI> scientificContacts = new ArrayList<>();
    private List<URI> relatedProjects = new ArrayList<>();

    public TestProjectBuilder() throws URISyntaxException {}

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getFinancialFunding() {
        return financialFunding;
    }

    public void setFinancialFunding(String financialFunding) {
        this.financialFunding = financialFunding;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public URI getHomePage() {
        return homePage;
    }

    public void setHomePage(URI homePage) {
        this.homePage = homePage;
    }

    public List<URI> getAdministrativeContacts() {
        return administrativeContacts;
    }

    public void setAdministrativeContacts(List<URI> administrativeContacts) {
        this.administrativeContacts = administrativeContacts;
    }

    public List<URI> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(List<URI> coordinators) {
        this.coordinators = coordinators;
    }

    public List<URI> getScientificContacts() {
        return scientificContacts;
    }

    public void setScientificContacts(List<URI> scientificContacts) {
        this.scientificContacts = scientificContacts;
    }

    public List<URI> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(List<URI> relatedProjects) {
        this.relatedProjects = relatedProjects;
    }

    private final List<ProjectCreationDTO> dtoList = new ArrayList<>();

    public List<ProjectCreationDTO> getDTOList() {
        return dtoList;
    }

    public ProjectCreationDTO createDTO() throws URISyntaxException {
        ProjectCreationDTO dto = new ProjectCreationDTO();
        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setShortname(getShortname() + dtoList.size());
        dto.setFinancialFunding(getFinancialFunding());
        dto.setStartDate(getStartDate());
        dto.setEndDate(getEndDate());
        dto.setName(getName() + dtoList.size());
        dto.setDescription(getDescription());
        dto.setObjective(getObjective());
        dto.setHomePage(getHomePage());
        dto.setAdministrativeContacts(getAdministrativeContacts());
        dto.setCoordinators(getCoordinators());
        dto.setScientificContacts(getScientificContacts());
        dto.setRelatedProjects(getRelatedProjects());

        dtoList.add(dto);
        return dto;
    }
}
