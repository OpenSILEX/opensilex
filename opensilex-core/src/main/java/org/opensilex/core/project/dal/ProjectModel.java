/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.dal;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Julien BONNEFONT
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Project",
        graph = ProjectModel.GRAPH,
        prefix = "prj"
)
public class ProjectModel extends SPARQLNamedResourceModel<ProjectModel> implements ClassURIGenerator<ProjectModel> {

    public static final String GRAPH = "project";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasShortname"
    )
    private String shortname;
    public static final String SHORTNAME_FIELD = "shortname";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFinancialFunding"
    )
    private String financialFunding;
    public static final String FINANCIAL_FUNDING_FIELD = "financialFunding";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective"
    )
    private String objective;
    public static final String OBJECTIVE_FIELD = "objective";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startDate",
            required = true
    )
    private LocalDate startDate;
    public static final String START_DATE_FIELD = "startDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "endDate"
    )
    private LocalDate endDate;
    public static final String END_DATE_FIELD = "endDate";


    @SPARQLProperty(
            ontology = FOAF.class,
            property = "homepage"
    )
    private URI homePage;
    public static final String HOMEPAGE_FIELD = "homePage";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasAdministrativeContact"
    )
    private List<UserModel> administrativeContacts;
    public static final String ADMINISTRATIVE_CONTACTS_FIELD = "administrativeContacts";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCoordinator"
    )
    private List<UserModel> coordinators;
    public static final String COORDINATORS_FIELD = "coordinators";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasScientificContact"
    )
    private List<UserModel> scientificContacts;
    public static final String SCIENTIFIC_CONTACTS_FIELD = "scientificContacts";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasRelatedProject"
    )
    private List<ProjectModel> relatedProjects;

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
    public String[] getInstancePathSegments(ProjectModel instance) {
        String name =  instance.getShortname();
        if (name == null || name.trim().isEmpty()) {
            name = instance.getName();
        }
        return new String[]{
            name
        };
    }

}
