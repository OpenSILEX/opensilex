/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.dal;

import java.time.LocalDate;
import java.util.List;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.security.dal.GroupModel;
import org.opensilex.server.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Experiment",
        graph = "experiments"
)
public class ExperimentModel extends SPARQLResourceModel implements ClassURIGenerator<ExperimentModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String alias;
    public static final String ALIAS_FIELD = "alias";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasProject"
    )
    List<ProjectModel> projects;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasStartDate",
            required = true
    )
    LocalDate startDate;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasEndDate"
    )
    LocalDate endDate;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasScientificSupervisor"
    )
    List<UserModel> scientificSupervisors;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTechnicalSupervisor"
    )
    List<UserModel> technicalSupervisors;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasGroup"
    )
    List<GroupModel> groups;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective"
    )
    String objectives;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasKeyword"
    )
    List<String> keywords;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<ProjectModel> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectModel> projects) {
        this.projects = projects;
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

    public List<UserModel> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public void setScientificSupervisors(List<UserModel> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
    }

    public List<UserModel> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public void setTechnicalSupervisors(List<UserModel> technicalSupervisors) {
        this.technicalSupervisors = technicalSupervisors;
    }

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> groups) {
        this.groups = groups;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    
    
    @Override
    public String[] getUriSegments(ExperimentModel instance) {
        return new String[]{
            "experiment"
        };
    }

}
