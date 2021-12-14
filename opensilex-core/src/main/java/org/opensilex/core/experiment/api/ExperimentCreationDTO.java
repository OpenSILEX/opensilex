//******************************************************************************
//                          ExperimentCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.project.dal.ProjectModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.dal.UserModel;

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

        List<SpeciesModel> speciesList = new ArrayList<>(species.size());
        species.forEach((URI u) -> {
            SpeciesModel species = new SpeciesModel();
            species.setUri(u);
            speciesList.add(species);
        });
        model.setSpecies(speciesList);
        
        List<InfrastructureModel> infrastructuresList = new ArrayList<>(infrastructures.size());
        infrastructures.forEach((URI u) -> {
            InfrastructureModel infrastructure = new InfrastructureModel();
            infrastructure.setUri(u);
            infrastructuresList.add(infrastructure);
        });
        model.setInfrastructures(infrastructuresList);

        List<InfrastructureFacilityModel> facilityList = new ArrayList<>(facilities.size());
        facilities.forEach((facilityUri) -> {
            InfrastructureFacilityModel facilityModel = new InfrastructureFacilityModel();
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

        List<UserModel> scientificList = new ArrayList<>(scientificSupervisors.size());
        scientificSupervisors.forEach((URI u) -> {
            UserModel user = new UserModel();
            user.setUri(u);
            scientificList.add(user);
        });
        model.setScientificSupervisors(scientificList);

        List<UserModel> technicalList = new ArrayList<>(technicalSupervisors.size());
        technicalSupervisors.forEach((URI u) -> {
            UserModel user = new UserModel();
            user.setUri(u);
            technicalList.add(user);
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
