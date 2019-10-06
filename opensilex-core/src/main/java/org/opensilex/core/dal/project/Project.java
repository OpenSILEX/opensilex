/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.dal.project;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.annotations.SPARQLResourceURI;

/**
 * The model of a project.
 *
 * @author Morgane Vidal
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "Project"
)
public class Project {

    //URI of the project
    @SPARQLResourceURI()
    private URI uri;

    //Name of the project
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "name"
    )
    private String name;

    //Projects related to the current project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasRelatedProject"
    )
    private List<Project> relatedProjects = new ArrayList<>();

    //Shortname of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasShortname"
    )
    private String shortname;

    //Financial support of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFinancialSupport"
    )
    private FinancialSupport financialSupport;

    //Financial reference of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFinancialReference"
    )
    private String financialReference;

    //Description of the project
    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;

    //Start date of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasStartDate"
    )
    private Date startDate;

    //End date of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasEndDate"
    )
    private Date endDate;

    //Keywords of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasKeyword"
    )
    private List<String> keywords = new ArrayList<>();

    //URL of the home page representing the project
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "homePage"
    )
    private URL homePage;

    //Objective of the project
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective"
    )
    private String objective;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Project> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(List<Project> relatedProjects) {
        this.relatedProjects = relatedProjects;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public FinancialSupport getFinancialSupport() {
        return financialSupport;
    }

    public void setFinancialSupport(FinancialSupport financialSupport) {
        this.financialSupport = financialSupport;
    }

    public String getFinancialReference() {
        return financialReference;
    }

    public void setFinancialReference(String financialReference) {
        this.financialReference = financialReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public URL getHomePage() {
        return homePage;
    }

    public void setHomePage(URL homePage) {
        this.homePage = homePage;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
}
