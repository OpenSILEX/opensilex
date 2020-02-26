//******************************************************************************
//                          PostDtoToExperimentModel.java
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
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Renaud COLIN
 * DTO used to convert from {@link ExperimentPostDTO} (old model) to {@link ExperimentModel} (new model)
 */
public class PostDtoToExperimentModel {

    protected final SpeciesDAO speciesDAO;
    protected final ProjectDAO projectDAO;
    protected final UserDAO userDAO;

    /**
     * @param speciesDAO Dao needed to resolve {@link Species} URI from {@link ExperimentPostDTO#getCropSpecies()}
     * @param projectDAO Dao needed to get {@link ProjectModel} from {@link ExperimentPostDTO#getProjectsUris()}
     * @param userDAO    Dao needed to get an {@link UserModel} from {@link ExperimentPostDTO#getContacts()}
     */
    public PostDtoToExperimentModel(SpeciesDAO speciesDAO, ProjectDAO projectDAO, UserDAO userDAO) {
        this.speciesDAO = speciesDAO;
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
    }

    public ExperimentModel convert(ExperimentPostDTO xpPostDto) throws Exception {

        ExperimentModel xpModel = new ExperimentModel();

        xpModel.setLabel(xpPostDto.getAlias());
        if (xpPostDto.getCampaign() != null) {
            xpModel.setCampaign(Integer.parseInt(xpPostDto.getCampaign()));
        }
        xpModel.setObjective(xpPostDto.getObjective());
        xpModel.setComment(xpPostDto.getComment());

        if (!StringUtils.isEmpty(xpPostDto.getKeywords())) {
            String[] keywords = xpPostDto.getKeywords().split("\\s+");
            xpModel.setKeywords(Arrays.asList(keywords));
        }

        xpModel.setStartDate(LocalDate.parse(xpPostDto.getStartDate()));
        if (xpPostDto.getEndDate() != null) {
            xpModel.setEndDate(LocalDate.parse(xpPostDto.getEndDate()));
        }

        if (xpPostDto.getCropSpecies() != null && !xpPostDto.getCropSpecies().isEmpty()) {
            Species searchSpecies = new Species();
            searchSpecies.setLabel(xpPostDto.getCropSpecies());
            ArrayList<Species> daoSpecies = speciesDAO.searchWithFilter(searchSpecies, null);

            if (daoSpecies.isEmpty()) {
                throw new IllegalArgumentException("Unknown species label " + xpPostDto.getCropSpecies());
            }
            xpModel.setSpecies(new URI(daoSpecies.get(0).getUri()));
        }

        if (xpPostDto.getProjectsUris() != null) {

            List<ProjectModel> projects = new ArrayList<>();
            for (String projectURI : xpPostDto.getProjectsUris()) {
                ProjectModel projectModel = projectDAO.get(new URI(projectURI));
                if (projectModel == null) {
                    throw new IllegalArgumentException("Unknown project URI :" + projectURI);
                }
                projects.add(projectModel);
            }
            
            xpModel.setProjects(projects);
        }

        if (xpPostDto.getContacts() != null) {
            for (ContactPostgreSQL contact : xpPostDto.getContacts()) {

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
        if (!xpPostDto.getField().isEmpty()) {
            try {
                xpModel.getInfrastructures().add(new URI(xpPostDto.getField()));
            } catch (URISyntaxException ignored) {
            }
        }

        // try to get the devices/place URI
        if (!xpPostDto.getPlace().isEmpty()) {
            try {
                xpModel.getDevices().add(new URI(xpPostDto.getPlace()));
            } catch (URISyntaxException ignored) {
            }
        }
        return xpModel;
    }
}
