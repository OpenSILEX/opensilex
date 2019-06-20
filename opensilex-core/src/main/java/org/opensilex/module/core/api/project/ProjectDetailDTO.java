/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.api.project;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectDetailDTO {
    private URI uri;
    private String name;
    private List<URI> relatedProject = new ArrayList<>();
    private String shortname;
    private String financialSupport;
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

    public List<ContactDTO> getScientificContacts() {
        return scientificContacts;
    }

    public void setScientificContacts(List<ContactDTO> scientificContacts) {
        this.scientificContacts = scientificContacts;
    }

    public List<ContactDTO> getAdministrativeContacts() {
        return administrativeContacts;
    }

    public void setAdministrativeContacts(List<ContactDTO> administrativeContacts) {
        this.administrativeContacts = administrativeContacts;
    }

    public List<ContactDTO> getCoordinatorContacts() {
        return coordinatorContacts;
    }

    public void setCoordinatorContacts(List<ContactDTO> coordinatorContacts) {
        this.coordinatorContacts = coordinatorContacts;
    }

    public List<URI> getRelatedProject() {
        return relatedProject;
    }

    public void setRelatedProject(List<URI> relatedProject) {
        this.relatedProject = relatedProject;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }
}
