/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.dal;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import javax.mail.internet.InternetAddress;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;


@SPARQLResource(
        ontology = Oeso.class,
        resource = "Project",
        graph = "set/projects"
)
public class ProjectModel extends SPARQLResourceModel implements ClassURIGenerator<ProjectModel> {

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "name",
            required = true
    )
    private String name;
    public static final String NAME_SPARQL_VAR = "name";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasShortname"
    )
    private String shortname;
    public static final String SHORTNAME_SPARQL_VAR = "shortname";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public static final String DESCRIPTION_SPARQL_VAR = "description";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective"
    )
    private String objective;
    public static final String OBJECTIVE_SPARQL_VAR = "objective";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startDate"
    )
    private LocalDate startDate;
    public static final String START_DATE_SPARQL_VAR = "startDate";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "endDate"
    )
    private LocalDate endDate;
    public static final String END_DATE_SPARQL_VAR = "endDate";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasKeyword"
    )
    private List<String> keywords;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "homepage"
    )
    private URI homePage;
    public static final String HOMEPAGE_SPARQL_VAR = "homePage";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasAdministrativeContact"
    )
    private List<UserModel> administrativeContacts;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCoordinator"
    )
    private List<UserModel> coordinators;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasScientificContact"
    )
    private List<UserModel> scientificContacts;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasRelatedProject"
    )
    private List<ProjectModel> relatedProjects;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public List<ProjectModel> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(List<ProjectModel> relatedProjects) {
        this.relatedProjects = relatedProjects;
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

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public URI getHomePage() {
        return homePage;
    }

    public void setHomePage(URI homePage) {
        this.homePage = homePage;
    }

    public List<UserModel> getAdministrativeContacts() {
        return administrativeContacts;
    }

    public void setAdministrativeContacts(List<UserModel> administrativeContacts) {
        this.administrativeContacts = administrativeContacts;
    }

    public List<UserModel> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(List<UserModel> coordinators) {
        this.coordinators = coordinators;
    }

    public List<UserModel> getScientificContacts() {
        return scientificContacts;
    }

    public void setScientificContacts(List<UserModel> scientificContacts) {
        this.scientificContacts = scientificContacts;
    }

    @Override
    public String[] getUriSegments(ProjectModel instance) {
        return new String[]{
            "project",
            instance.getName()
        };
    }

}
