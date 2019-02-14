//******************************************************************************
//                                       ProjectDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 14 févr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.projects;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.Contact;
import phis2ws.service.view.model.phis.Project;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectPostDTO extends AbstractVerifiedClass {

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
    private ArrayList<Contact> contacts;

    @Override
    public Project createObjectFromDTO() {
        Project project = new Project();
        project.setName(name);
        project.setAcronyme(acronyme);
        project.setSubprojectType(subprojectType);
        project.setFinancialSupport(financialSupport);
        project.setFinancialName(financialName);
        project.setDateStart(dateStart);
        project.setDateEnd(dateEnd);
        project.setKeywords(keywords);
        project.setDescription(description);
        project.setObjective(objective);
        project.setParentProject(parentProject);
        project.setWebsite(website);

        if (contacts != null && !contacts.isEmpty()) {
            for (Contact contact : contacts) {
                project.addContact(contact);
            }
        }

        return project;
    }

    @Required
    @ApiModelProperty(example = "projectTest")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "P T")
    public String getAcronyme() {
        return acronyme;
    }

    public void setAcronyme(String acronyme) {
        this.acronyme = acronyme;
    }

    @ApiModelProperty(example = "subproject type")
    public String getSubprojectType() {
        return subprojectType;
    }

    public void setSubprojectType(String subprojectType) {
        this.subprojectType = subprojectType;
    }

    @ApiModelProperty(example = "financial support")
    public String getFinancialSupport() {
        return financialSupport;
    }

    public void setFinancialSupport(String financialSupport) {
        this.financialSupport = financialSupport;
    }

    @ApiModelProperty(example = "financial name")
    public String getFinancialName() {
        return financialName;
    }

    public void setFinancialName(String financialName) {
        this.financialName = financialName;
    }

    @Date(DateFormat.YMD)
    @Required
    @ApiModelProperty(example = "2015-07-07")
    public String getDateStart() {
        return dateStart;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    @Date(DateFormat.YMD)
    @ApiModelProperty(example = "2016-07-07")
    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    @ApiModelProperty(example = "keywords")
    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    @ApiModelProperty(example = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(example = "objective")
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    @ApiModelProperty(example = "parent project")
    public String getParentProject() {
        return parentProject;
    }

    public void setParentProject(String parentProject) {
        this.parentProject = parentProject;
    }

    @URL
    @ApiModelProperty(example = "http://example.com")
    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    @Valid
    public ArrayList<Contact> getContacts() {
        return contacts;
    }
    
    public void setContacts(ArrayList<Contact> contacts) {
        this.contacts = contacts;
    }
}
