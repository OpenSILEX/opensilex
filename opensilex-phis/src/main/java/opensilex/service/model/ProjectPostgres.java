//******************************************************************************
//                                    Project.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: March 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Project model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectPostgres {
    private String uri;
    private String name;
    private String acronyme;
    private String subprojectType;
    private String financialSupport;
    private String financialName;
    private String dateStart;
    private String dateEnd;
    private String keywords;
    private String description;
    private String objective;
    private String parentProject;
    private String website;
    
    private ArrayList<ContactPostgreSQL> contacts = new ArrayList<>();
    
    public ProjectPostgres() {
    }
    
    public ProjectPostgres(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronyme() {
        return acronyme;
    }

    public void setAcronyme(String acronyme) {
        this.acronyme = acronyme;
    }

    public String getSubprojectType() {
        return subprojectType;
    }

    public void setSubprojectType(String subprojectType) {
        this.subprojectType = subprojectType;
    }

    public String getFinancialSupport() {
        return financialSupport;
    }

    public void setFinancialSupport(String financialSupport) {
        this.financialSupport = financialSupport;
    }

    public String getFinancialName() {
        return financialName;
    }

    public void setFinancialName(String financialName) {
        this.financialName = financialName;
    }

    public String getDateStart() {
        return dateStart;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
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

    public String getParentProject() {
        return parentProject;
    }

    public void setParentProject(String parentProject) {
        this.parentProject = parentProject;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public ArrayList<ContactPostgreSQL> getContacts() {
        return contacts;
    }
    
    public void addContact(ContactPostgreSQL contact) {
        this.contacts.add(contact);
    }
    
    /**
     * Compares to another project. Compares attributes one by one.
     * @param project Project to compare with
     * @return true if equal, false otherwise
     */
    public boolean equals(ProjectPostgres project) {
        return this.uri.equals(project.uri)
            && this.name.equals(project.name)
            && this.acronyme.equals(project.acronyme)
            && this.subprojectType.equals(project.subprojectType)
            && this.financialSupport.equals(project.financialSupport)
            && this.financialName.equals(project.financialName)
            && this.dateStart.equals(project.dateStart)
            && this.dateEnd.equals(project.dateEnd)
            && this.keywords.equals(project.keywords)
            && this.description.equals(project.description)
            && this.objective.equals(project.objective)
            && this.parentProject.equals(project.parentProject)
            && this.website.equals(project.website);
    }
}
