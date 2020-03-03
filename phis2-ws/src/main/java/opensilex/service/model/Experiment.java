//******************************************************************************
//                               Experiment.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import opensilex.service.dao.SpeciesDAO;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.group.dal.GroupDAO;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Experiment model.
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
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
    private ArrayList<ContactPostgreSQL> contacts = new ArrayList<>();
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

    public ArrayList<ContactPostgreSQL> getContacts() {
        return contacts;
    }

    public void addContact(ContactPostgreSQL contact) {
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
     * Compares to another experiment. Compares attributes one by one.
     *
     * @param experiment Experiment to compare
     * @return true if equal, false otherwise.
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


    public static ExperimentModel toExperimentModel(Experiment xp, SpeciesDAO speciesDAO, ProjectDAO projectDAO, UserDAO userDAO, GroupDAO groupDAO) throws Exception {

        ExperimentModel xpModel = new ExperimentModel();

        if (!StringUtils.isEmpty(xp.getUri())) {
            xpModel.setUri(new URI(xp.getUri()));
        }

        xpModel.setLabel(xp.getAlias());
        if (StringUtils.isEmpty(xp.getCampaign())) {
            xpModel.setCampaign(Integer.parseInt(xp.getCampaign()));
        }
        xpModel.setObjective(xp.getObjective());
        xpModel.setComment(xp.getComment());

        if (!StringUtils.isEmpty(xp.getKeywords())) {
            String[] keywords = xp.getKeywords().split("\\s+");
            xpModel.setKeywords(Arrays.asList(keywords));
        }

        if (xp.getStartDate() != null) {
            xpModel.setStartDate(LocalDate.parse(xp.getStartDate()));
        }
        if (xp.getEndDate() != null) {
            xpModel.setEndDate(LocalDate.parse(xp.getEndDate()));
        }

        if (! StringUtils.isEmpty(xp.getCropSpecies())) {
            try {
                xpModel.setSpecies(new URI(xp.getCropSpecies()));
            } catch (URISyntaxException e) {
                Species searchSpecies = new Species();
                searchSpecies.setLabel(xp.getCropSpecies());
                ArrayList<Species> daoSpecies = speciesDAO.searchWithFilter(searchSpecies, null);

                if (daoSpecies.isEmpty()) {
                    throw new IllegalArgumentException("Unknown species label " + xp.getCropSpecies());
                }
                xpModel.setSpecies(new URI(daoSpecies.get(0).getUri()));
            }
        }

        if (xp.getProjects() != null) {

            for (Project project : xp.getProjects()) {
                ProjectModel projectModel = projectDAO.get(new URI(project.getUri()));
                if (projectModel == null) {
                    throw new IllegalArgumentException("Unknown project URI :" + project.getUri());
                }
                xpModel.getProjects().add(projectModel);
            }
        }

        if (xp.getGroups() != null) {
            for (Group group : xp.getGroups()) {
                GroupModel groupModel = groupDAO.get(new URI(group.getUri()));
                if (groupModel == null) {
                    throw new IllegalArgumentException("Unknown group URI :" + group.getUri());
                }
                xpModel.getGroups().add(groupModel);
            }
        }

        if (xp.getContacts() != null) {
            for (ContactPostgreSQL contact : xp.getContacts()) {

                UserModel userModel = userDAO.getByEmail(new InternetAddress(contact.getEmail()));
                if (userModel == null) {
                    throw new IllegalArgumentException("Unknown User email :" + contact.getEmail());
                }

                if (contact.getType().equals(Oeso.ScientificSupervisor.getURI())) {
                    xpModel.getScientificSupervisors().add(userModel);
                } else if (contact.getType().equals(Oeso.TechnicalSupervisor.getURI())) {
                    xpModel.getTechnicalSupervisors().add(userModel);
                } else {
                    throw new IllegalArgumentException("Bad contact type : " + contact.getType());
                }
            }
        }
        // try to get the Infrastructures/field URI
//        if (! StringUtils.isEmpty(xp.getField())) {
//            try {
//                xpModel.getInfrastructures().add(new URI(xp.getField()));
//            } catch (URISyntaxException ignored) {
//            }
//        }
//
//        // try to get the devices/place URI
//        if (! StringUtils.isEmpty(xp.getPlace())) {
//            try {
//                xpModel.getDevices().add(new URI(xp.getPlace()));
//            } catch (URISyntaxException ignored) {
//            }
//        }
        return xpModel;
    }
}
