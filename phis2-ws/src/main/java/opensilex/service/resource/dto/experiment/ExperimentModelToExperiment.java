//******************************************************************************
//                          ExperimentModelToExperiment.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package opensilex.service.resource.dto.experiment;

import opensilex.service.dao.SensorDAO;
import opensilex.service.model.*;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.user.dal.UserModel;

import java.net.URI;

/**
 * @author Renaud COLIN
 * DTO used to convert from {@link ExperimentModel} (new model) to {@link Experiment} (old model)
 */
public class ExperimentModelToExperiment {

    protected final SensorDAO sensorDAO;

    /**
     *
     * @param sensorDAO Dao needed to build {@link Experiment}  sensor URI -> sensor label map from an {@link ExperimentModel}
     *
     * @see Experiment#getSensors()
     * @see ExperimentModel#getSensors()
      */
    public ExperimentModelToExperiment(SensorDAO sensorDAO) {
        this.sensorDAO = sensorDAO;
    }


    public Experiment convert(ExperimentModel xp) throws Exception {

        Experiment oldXpModel = new Experiment();
        oldXpModel.setUri(xp.getUri().toString());
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
            oldProjectMOdel.setName(projectModel.getName());
            oldXpModel.addProject(oldProjectMOdel);
        }
        for(VariableModel variable : xp.getVariables()){
            oldXpModel.getVariables().put(variable.getUri().toString(),variable.getLabel());
        }


        // use the Sensor DAO since new model only store sensors URIs instead of a pair <SensorURI,sensorLabel> for the old model
        for(URI sensorUri : xp.getSensors()){
            Sensor sensor = sensorDAO.findById(sensorUri.toString());
            if(sensor == null){
                throw new IllegalArgumentException("Unknown species URI "+sensorUri);
            }
            oldXpModel.getSensors().put(sensor.getUri(),sensor.getLabel());
        }

//        if(xp.getInfrastructures() != null){
//            InfrastructureDAO infraDao = new InfrastructureDAO();
//            infraDao.uri = xp.getInfrastructures().toString();
//            List<Infrastructure> allInfras = infraDao.allPaginate();
//
//            // the old model use the label and not the URI of an infrastructure
//            if(! allInfras.isEmpty()){
//                oldXpModel.setPlace(allInfras.get(0).getLabel());
//            }
//        }

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
