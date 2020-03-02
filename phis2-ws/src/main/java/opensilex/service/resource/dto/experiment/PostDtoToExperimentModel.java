//******************************************************************************
//                          PostDtoToExperimentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package opensilex.service.resource.dto.experiment;

import java.net.MalformedURLException;
import opensilex.service.dao.SpeciesDAO;
import opensilex.service.model.ContactPostgreSQL;
import opensilex.service.model.Species;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Renaud COLIN
 * DTO used to convert from {@link ExperimentPostDTO} (old model) to {@link ExperimentModel} (new model)
 */
public class PostDtoToExperimentModel {

    protected final SpeciesDAO speciesDAO;
    protected final ProjectDAO projectDAO;
    protected final UserDAO userDAO;

    public PostDtoToExperimentModel(SPARQLService sparqlService, String lang) {
        this.speciesDAO = new SpeciesDAO();
        this.projectDAO = new ProjectDAO(sparqlService, lang);
        this.userDAO = new UserDAO(sparqlService);
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


        if (!StringUtils.isEmpty(xpPostDto.getCropSpecies())) {
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
        if (!StringUtils.isEmpty(xpPostDto.getField())) {
            try {
                xpModel.getInfrastructures().add(new URL(xpPostDto.getField()).toURI());
            } catch (URISyntaxException ignored) {
            } catch (MalformedURLException ex) {
                Logger.getLogger(PostDtoToExperimentModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // try to get the devices/place URI
        if (!StringUtils.isEmpty(xpPostDto.getPlace())) {
            try {
                xpModel.getDevices().add(new URL(xpPostDto.getPlace()).toURI());
            } catch (URISyntaxException ignored) {
            } catch (MalformedURLException ex) {
                Logger.getLogger(PostDtoToExperimentModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return xpModel;
    }
}
