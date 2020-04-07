//******************************************************************************
//                          ExperimentModelToExperiment.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package opensilex.service.resource.dto.experiment;

import opensilex.service.dao.SensorDAO;
import opensilex.service.dao.VariableDAO;
import opensilex.service.model.*;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.dal.UserModel;

import java.net.URI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;

/**
 * @author Renaud COLIN
 * DTO used to convert from {@link ExperimentModel} (new model) to {@link Experiment} (old model)
 */
public class ExperimentModelToExperiment {

    protected final SensorDAO sensorDAO;
    protected final VariableDAO variableDAO;

    public ExperimentModelToExperiment(SPARQLService sparql) {
        sensorDAO = new SensorDAO(sparql);
        variableDAO = new VariableDAO(sparql);
    }

    public Experiment convert(ExperimentModel xp) throws Exception {

        Experiment oldXpModel = new Experiment();
        oldXpModel.setUri(SPARQLDeserializers.getExpandedURI(xp.getUri().toString()));
        oldXpModel.setAlias(xp.getLabel());
        oldXpModel.setObjective(xp.getObjective());
        oldXpModel.setComment(xp.getComment());
        if(! xp.getKeywords().isEmpty()){
            oldXpModel.setKeywords(String.join(" ",xp.getKeywords()));
        }

        oldXpModel.setStartDate(xp.getStartDate().toString());
        if(xp.getCampaign() != null){
            oldXpModel.setCampaign(xp.getCampaign().toString());
        }
        if(xp.getEndDate() != null) {
            oldXpModel.setEndDate(xp.getEndDate().toString());
        }
        if(xp.getSpecies() != null){
            oldXpModel.setCropSpecies(xp.getSpecies().toString());
        }
        for(ProjectModel projectModel : xp.getProjects()){
            Project oldProjectMOdel = new Project();
            oldProjectMOdel.setUri(projectModel.getUri().toString());
            oldProjectMOdel.setShortname(projectModel.getShortname());
            oldProjectMOdel.setName(projectModel.getName());
            oldXpModel.addProject(oldProjectMOdel);
        }
        for(URI varURI : xp.getVariables()){
            Variable variable = variableDAO.findById(varURI.toString());
            if(variable == null){
                throw new IllegalArgumentException("Unknown variable URI "+varURI);
            }
            oldXpModel.getVariables().put(variable.getUri(),variable.getLabel());
        }


        // use the Sensor DAO since new model only store sensors URIs instead of a pair <SensorURI,sensorLabel> for the old model
        for(URI sensorUri : xp.getSensors()){
            Sensor sensor = sensorDAO.findById(sensorUri.toString());
            if(sensor == null){
                throw new IllegalArgumentException("Unknown sensor URI "+sensorUri);
            }
            oldXpModel.getSensors().put(sensor.getUri(),sensor.getLabel());
        }


        // convert infrastructure to field
        if(!xp.getInfrastructures().isEmpty()){
            oldXpModel.setField(xp.getInfrastructures().get(0).toString());
        }
        // convert devices to place
        if(!xp.getDevices().isEmpty()){
            oldXpModel.setPlace(xp.getDevices().get(0).toString());
        }

        for(UserModel scientific : xp.getScientificSupervisors()){
            ContactPostgreSQL contact = new ContactPostgreSQL();
            contact.setEmail(scientific.getEmail().toString());
            contact.setFamilyName(scientific.getLastName());
            contact.setFirstName(scientific.getFirstName());
            contact.setType(Oeso.ScientificSupervisor.getURI());
            oldXpModel.addContact(contact);
        }
        for(UserModel technical : xp.getTechnicalSupervisors()){
            ContactPostgreSQL contact = new ContactPostgreSQL();
            contact.setEmail(technical.getEmail().toString());
            contact.setFamilyName(technical.getLastName());
            contact.setFamilyName(technical.getFirstName());
            contact.setType(Oeso.TechnicalSupervisor.getURI());
            oldXpModel.addContact(contact);
        }
        for(GroupModel groupModel : xp.getGroups()){
            Group group = new Group();
            group.setName(groupModel.getName());
            group.setDescription(groupModel.getDescription());
            group.setUri(groupModel.getUri().toString());
            oldXpModel.addGroup(group);
        }
        return oldXpModel;
    }
}
