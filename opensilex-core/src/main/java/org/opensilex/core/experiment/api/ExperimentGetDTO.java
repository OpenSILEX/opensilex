/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import java.net.URI;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 *
 * @author vidalmor
 */
public class ExperimentGetDTO {
    
    private URI uri;

    private String alias;

    private List<URI> projects;

    private String startDate;

    private String endDate;

    private String objectives;

    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public void setProjects(List<URI> projects) {
        this.projects = projects;
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

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public static ExperimentGetDTO fromModel(ExperimentModel model) {
        ExperimentGetDTO dto = new ExperimentGetDTO();

        dto.setUri(model.getUri());
        dto.setAlias(model.getAlias());

        dto.setObjectives(model.getObjectives());
        dto.setComment(model.getComment());
        
        return dto;
    }
}
