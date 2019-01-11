//**********************************************************************************************
//                                       Experiment.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  February, 2017
// Subject: Represents the experiment 
//***********************************************************************************************

package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import java.util.HashMap;

public class Experiment {
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
    
    private ArrayList<Project> projects = new ArrayList<>();
    private ArrayList<Group> groups = new ArrayList<>();
    private ArrayList<Contact> contacts = new ArrayList<>();
    private HashMap<String, String> variables = new HashMap<>();
    private HashMap<String, String> sensors = new HashMap<>();
    
    public Experiment() {
        
    }
    
    public Experiment(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getCropSpecies() {
        return cropSpecies;
    }

    public void setCropSpecies(String cropSpecies) {
        this.cropSpecies = cropSpecies;
    }
    
    public ArrayList<Contact> getContacts() {
        return contacts;
    }
      
    public void addContact(Contact contact) {
        this.contacts.add(contact);
    }
    
    public ArrayList<Project> getProjects() {
        return this.projects;
    }
    
    public void addProject(Project project) {
        this.projects.add(project);
    }
    
    public ArrayList<Group> getGroups() {
        return this.groups;
    }
    
    public void addGroup(Group group) {
        this.groups.add(group);
    }
    
    public void setGroupList(ArrayList<Group> groups) {
        this.groups = groups;
    }

    public HashMap<String, String> getVariables() {
        return variables;
    }

    public void setVariables(HashMap<String, String> variables) {
        this.variables = variables;
    }

    public HashMap<String, String> getSensors() {
        return sensors;
    }

    public void setSensors(HashMap<String, String> sensors) {
        this.sensors = sensors;
    }
    
    /**
     * Compare les deux experimentations (compare les attributs un à un)
     * @param experiment Experiment à comparer à this
     * @return true s'ils sont égaux, false sinon
     */
    public boolean equals(Experiment experiment) {
        return this.uri.equals(experiment.uri)
            && this.startDate.equals(experiment.startDate)
            && this.endDate.equals(experiment.endDate)
            && this.field.equals(experiment.field)
            && this.campaign.equals(experiment.campaign)
            && this.place.equals(experiment.place)
            && this.alias.equals(experiment.alias)
            && this.comment.equals(experiment.comment)
            && this.keywords.equals(experiment.keywords)
            && this.objective.equals(experiment.objective)
            && this.cropSpecies.equals(experiment.cropSpecies);
    }
}
