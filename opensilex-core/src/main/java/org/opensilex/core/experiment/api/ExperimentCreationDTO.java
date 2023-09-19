//******************************************************************************
//                          ExperimentCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.person.dal.PersonModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent MIGOT
 */
public class ExperimentCreationDTO extends ExperimentDTO {

    public ExperimentModel newModel() {

        ExperimentModel model = new ExperimentModel();
        model.setUri(getUri());
        model.setName(getName());
        model.setStartDate(startDate);
        model.setEndDate(endDate);

        model.setObjective(getObjective());
        model.setDescription(getDescription());
        model.setIsPublic(isPublic);

        // No species at experiment creation
        model.setSpecies(null);
        
        List<OrganizationModel> organizationsList = new ArrayList<>(organizations.size());
        organizations.forEach((URI u) -> {
            OrganizationModel organization = new OrganizationModel();
            organization.setUri(u);
            organizationsList.add(organization);
        });
        model.setOrganizations(organizationsList);

        List<FacilityModel> facilityList = new ArrayList<>(facilities.size());
        facilities.forEach( facilityUri -> {
            FacilityModel facilityModel = new FacilityModel();
            facilityModel.setUri(facilityUri);
            facilityList.add(facilityModel);
        });
        model.setFacilities(facilityList);

        List<ProjectModel> projectList = new ArrayList<>(projects.size());
        projects.forEach((URI u) -> {
            ProjectModel project = new ProjectModel();
            project.setUri(u);
            projectList.add(project);
        });
        model.setProjects(projectList);

        List<PersonModel> scientificList = new ArrayList<>(scientificSupervisors.size());
        scientificSupervisors.forEach((URI u) -> {
            PersonModel person = new PersonModel();
            person.setUri(u);
            scientificList.add(person);
        });
        model.setScientificSupervisors(scientificList);

        List<PersonModel> technicalList = new ArrayList<>(technicalSupervisors.size());
        technicalSupervisors.forEach((URI u) -> {
            PersonModel person = new PersonModel();
            person.setUri(u);
            technicalList.add(person);
        });
        model.setTechnicalSupervisors(technicalList);

        List<GroupModel> groupList = new ArrayList<>(groups.size());
        groups.forEach((URI u) -> {
            GroupModel group = new GroupModel();
            group.setUri(u);
            groupList.add(group);
        });
        model.setGroups(groupList);
        
        List<FactorModel> factorsList = new ArrayList<>(factors.size());
        factors.forEach((URI u) -> {
            FactorModel factor = new FactorModel();
            factor.setUri(u);
            factorsList.add(factor);
        });
        model.setFactors(factorsList);

        return model;
    }

}
