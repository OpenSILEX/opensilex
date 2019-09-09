//******************************************************************************
//                                Project.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 juil. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Project {

    //URI of the project
    private String uri;
    //Foaf:name of the project
    private String name;
    //Shortname of the project
    private String shortname;
    //Projects related to the current project
    private List<Project> relatedProjects = new ArrayList<>();
    //Financial support of the project
    private FinancialFunding financialFunding;
    //Financial reference of the project
    private String financialReference;
    //dcterms:description of the project
    private String description;
    //Start date of the project
    private String startDate;
    //End date of the project
    private String endDate;
    //Keywords of the project
    private List<String> keywords = new ArrayList<>();
    //Foaf:homePage of the project
    private String homePage;
    //Administrative contacts of the project
    private List<Contact> administrativeContacts = new ArrayList<>();
    //Coordinators of the project
    private List<Contact> coordinators = new ArrayList<>();
    //Scientific contacts of the project
    private List<Contact> scientificContacts = new ArrayList<>();
    //Objective of the project
    private String objective;

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

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public List<Project> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(List<Project> relatedProjects) {
        this.relatedProjects = relatedProjects;
    }

    public void addRelatedProject(Project relatedProject) {
        relatedProjects.add(relatedProject);
    }

    /**
     * Check if the given uri is a project related to the current project.
     *
     * @param projectUri
     * @return true if related projects contains the given uri, false if the
     * given project uri is not in relatedProject list.
     */
    public boolean containsRelatedProject(String projectUri) {
        for (Project project : relatedProjects) {
            if (project.getUri().equals(projectUri)) {
                return true;
            }
        }

        return false;
    }

    public FinancialFunding getFinancialFunding() {
        return financialFunding;
    }

    public void setFinancialFunding(FinancialFunding financialFunding) {
        this.financialFunding = financialFunding;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void addKeyword(String keyword) {
        keywords.add(keyword);
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public List<Contact> getAdministrativeContacts() {
        return administrativeContacts;
    }

    /**
     * Check if the given uri is in the administrative contact list.
     *
     * @param contactUri
     * @return true if administrative contact contains the given uri, false if
     * the given uri is not in administrative contact.
     */
    public boolean containsAdministrativeContact(String contactUri) {
        for (Contact contact : administrativeContacts) {
            if (contact.getUri().equals(contactUri)) {
                return true;
            }
        }

        return false;
    }

    public void setAdministrativeContacts(List<Contact> administrativeContacts) {
        this.administrativeContacts = administrativeContacts;
    }

    public void addAdministrativeContact(Contact admisitrativeContact) {
        administrativeContacts.add(admisitrativeContact);
    }

    public List<Contact> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(List<Contact> coordinators) {
        this.coordinators = coordinators;
    }

    public void addCoordinator(Contact coordinator) {
        coordinators.add(coordinator);
    }

    /**
     * Check if the given uri is in the coordinators list.
     *
     * @param contactUri
     * @return true if coordinators contains the given uri, false if the given
     * uri is not in coordinators list.
     */
    public boolean containsCoordinator(String contactUri) {
        for (Contact contact : coordinators) {
            if (contact.getUri().equals(contactUri)) {
                return true;
            }
        }

        return false;
    }

    public List<Contact> getScientificContacts() {
        return scientificContacts;
    }

    public void setScientificContacts(List<Contact> scientificContacts) {
        this.scientificContacts = scientificContacts;
    }

    public void addScientificContact(Contact scientificContact) {
        scientificContacts.add(scientificContact);
    }

    /**
     * Check if the given uri is in the scientific contact list.
     *
     * @param contactUri
     * @return true if scientific contacts contains the given uri, false if the
     * given uri is not in scientific contacts list.
     */
    public boolean containsScientificContact(String contactUri) {
        for (Contact contact : scientificContacts) {
            if (contact.getUri().equals(contactUri)) {
                return true;
            }
        }

        return false;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
}
