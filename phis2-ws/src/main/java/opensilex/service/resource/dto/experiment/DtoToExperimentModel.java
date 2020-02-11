//******************************************************************************
//                          DtoToExperimentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package opensilex.service.resource.dto.experiment;

import opensilex.service.dao.SpeciesDAO;
import opensilex.service.model.ContactPostgreSQL;
import opensilex.service.model.Species;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Renaud COLIN
 * DTO used to convert from {@link ExperimentDTO} (old model) to {@link ExperimentModel} (new model)
 */
public class DtoToExperimentModel {

    protected final SpeciesDAO speciesDAO;
    protected final ProjectDAO projectDAO;
    protected final UserDAO userDAO;

    /**
     *
     * @param speciesDAO Dao needed to resolve {@link Species} URI from {@link ExperimentPostDTO#getCropSpecies()}
     * @param projectDAO Dao needed to get {@link ProjectModel} from {@link ExperimentPostDTO#getProjectsUris()}
     * @param userDAO Dao needed to get an {@link UserModel} from {@link ExperimentPostDTO#getContacts()}
     */
    public DtoToExperimentModel(SpeciesDAO speciesDAO, ProjectDAO projectDAO, UserDAO userDAO) {
        this.speciesDAO = speciesDAO;
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
    }

    public ExperimentModel convert(ExperimentDTO xpDto) throws Exception {

        ExperimentModel xpModel = new ExperimentModel();

        xpModel.setLabel(xpDto.getAlias());
        if (xpDto.getCampaign() != null) {
            xpModel.setCampaign(Integer.parseInt(xpDto.getCampaign()));
        }
        xpModel.setObjective(xpDto.getObjective());
        xpModel.setComment(xpDto.getComment());

        if (!StringUtils.isEmpty(xpDto.getKeywords())) {
           String[] keywords = xpDto.getKeywords().split("\\s+");
           xpModel.setKeywords(Arrays.asList(keywords));
        }

        xpModel.setStartDate(LocalDate.parse(xpDto.getStartDate()));
        if (xpDto.getEndDate() != null) {
            xpModel.setEndDate(LocalDate.parse(xpDto.getEndDate()));
        }

        if (xpDto.getCropSpecies() != null) {
            Species searchSpecies = new Species();
            searchSpecies.setLabel(xpDto.getCropSpecies());
            ArrayList<Species> daoSpecies = speciesDAO.searchWithFilter(searchSpecies, null);

            if (daoSpecies.isEmpty()) {
                throw new IllegalArgumentException("Unknown species label " + xpDto.getCropSpecies());
            }
            xpModel.setSpecies(new URI(daoSpecies.get(0).getUri()));
        }

        if (xpDto.getProjectsUris() != null) {

            for (String projectURI : xpDto.getProjectsUris()) {
                ProjectModel projectModel = projectDAO.get(new URI(projectURI));
                if (projectModel == null) {
                    throw new IllegalArgumentException("Unknown project URI :" + projectURI);
                }
                xpModel.getProjects().add(projectModel);
            }
        }

        if (xpDto.getContacts() != null) {
            for (ContactPostgreSQL contact : xpDto.getContacts()) {

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
        return xpModel;
    }
}
