//******************************************************************************
//                              ExperimentDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.experiment;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DateFormat;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.ContactPostgreSQL;
import opensilex.service.model.Group;
import opensilex.service.model.Experiment;
import opensilex.service.model.Project;

/**
 * Experiment DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ExperimentDTO extends AbstractVerifiedClass {

    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentDTO.class);
    
    private String uri;
    private String startDate;
    private String endDate;
    private String field;
    private String campaign;
    private String place;
    private String alias;
    private String comment;
    private String keywords;
    private String objective;
    private String cropSpecies;
    private ArrayList<String> projectsUris;
    private ArrayList<String> groupsUris;
    private ArrayList<ContactPostgreSQL> contacts;
    
    @Override
    public Experiment createObjectFromDTO() {
        Experiment experiment = new Experiment(uri);
        experiment.setStartDate(startDate);
        experiment.setEndDate(endDate);
        experiment.setField(field);
        experiment.setCampaign(campaign);
        experiment.setPlace(place);
        experiment.setAlias(alias);
        experiment.setComment(comment);
        experiment.setKeywords(keywords);
        experiment.setObjective(objective);
        experiment.setCropSpecies(cropSpecies);
        
        if (projectsUris != null) {
            for (String projectURI : projectsUris) {
                Project project = new Project();
                project.setUri(projectURI);
                experiment.addProject(project);
            }
        }
        
        if (groupsUris != null) {
            for (String groupURI : groupsUris) {
                Group group = new Group(groupURI);
                experiment.addGroup(group);
            }
        }
        
        if (contacts != null && !contacts.isEmpty()) {
            for (ContactPostgreSQL contact : contacts) {
                experiment.addContact(contact);
            }
        }
        return experiment;
    }
    
    @URL
    @Required
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/drops")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @Date(DateFormat.YMD)
    @Required
    @ApiModelProperty(example = "2015-07-07")
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    @Date(DateFormat.YMD)
    @Required
    @ApiModelProperty(example = "2015-08-07")
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @ApiModelProperty(example = "field")
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @ApiModelProperty(example = "campaign")
    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    @ApiModelProperty(example = "place")
    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    @ApiModelProperty(example = "alias")
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @ApiModelProperty(example = "comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(example = "keywords")
    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    @ApiModelProperty(example = "objective")
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
    
    @URL
    public ArrayList<String> getProjectsUris() {
        return projectsUris;
    }

    public void setProjectsUris(ArrayList<String> projectsUris) {
        this.projectsUris = projectsUris;
    }

    @URL
    public ArrayList<String> getGroupsUris() {
        return groupsUris;
    }

    public void setGroupsUris(ArrayList<String> groupsUris) {
        this.groupsUris = groupsUris;
    }

    @ApiModelProperty(example = "maize")
    public String getCropSpecies() {
        return cropSpecies;
    }

    public void setCropSpecies(String cropSpecies) {
        this.cropSpecies = cropSpecies;
    }
    
    @Valid
    public ArrayList<ContactPostgreSQL> getContacts() {
        return contacts;
    }
    
    public void setContacts(ArrayList<ContactPostgreSQL> contacts) {
        this.contacts = contacts;
    }
}
