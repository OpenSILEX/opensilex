/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.api.project;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.opensilex.core.dal.project.Project;
import org.opensilex.server.security.user.User;

/**
 * DTO corresponding to the GET detail of a project
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectDetailDTO {
    private URI uri;
    private String name;
    private List<URI> relatedProjects = new ArrayList<>();
    private String shortname;
    private FinancialSupportDTO financialSupport;
    private String financialName;
    private Date dateStart;
    private Date dateEnd;
    private List<String> keywords = new ArrayList<>();
    private String description;
    private String objective;
    private URL website;
    
    private List<ContactDTO> scientificContacts = new ArrayList<>();
    private List<ContactDTO> administrativeContacts = new ArrayList<>();
    private List<ContactDTO> coordinatorContacts = new ArrayList<>();

    public ProjectDetailDTO(Project project) {
        uri = project.getUri();
        name = project.getName();
        
        for (Project relatedProject : project.getRelatedProjects()) {
            relatedProjects.add(relatedProject.getUri());
        }
        
        shortname = project.getShortname();
        financialSupport = new FinancialSupportDTO(project.getFinancialSupport());
        financialName = project.getFinancialReference();
        dateStart = project.getStartDate();
        dateEnd = project.getEndDate();
        keywords = project.getKeywords();
        description = project.getDescription();
        objective = project.getObjective();
        website = project.getHomePage();
        
//        for (User user : project.getScientificContacts()) {
//            scientificContacts.add(new ContactDTO(user));
//        }
//        
//        for (User user : project.getAdministrativeContacts()) {
//            administrativeContacts.add(new ContactDTO(user));
//        }
//        
//        for (User user : project.getCoordinators()) {
//            coordinatorContacts.add(new ContactDTO(user));
//        }
    }    
    
    public URI getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public FinancialSupportDTO getFinancialSupport() {
        return financialSupport;
    }

    public String getFinancialName() {
        return financialName;
    }

    public String getDescription() {
        return description;
    }

    public String getObjective() {
        return objective;
    }

    public List<ContactDTO> getScientificContacts() {
        return scientificContacts;
    }

    public List<ContactDTO> getAdministrativeContacts() {
        return administrativeContacts;
    }

    public List<ContactDTO> getCoordinatorContacts() {
        return coordinatorContacts;
    }

    public List<URI> getRelatedProjects() {
        return relatedProjects;
    }

    public String getShortname() {
        return shortname;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public URL getWebsite() {
        return website;
    }
}
