//******************************************************************************
//                          ProcessCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.api;

import org.opensilex.process.process.dal.ProcessModel;
import org.opensilex.process.process.dal.StepModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.time.InstantModel;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Emilie Fernandez
 */
public class ProcessCreationDTO extends ProcessDTO {
  
    public ProcessModel newModel() {

        ProcessModel model = new ProcessModel();
        model.setUri(getUri());
        model.setName(getName());

        List<ExperimentModel> xpList = new ArrayList<>(experiment.size());
        experiment.forEach((xpUri) -> {
            ExperimentModel xpModel = new ExperimentModel();
            xpModel.setUri(xpUri);
            xpList.add(xpModel);
        });
        model.setExperiment(xpList);

        if (!StringUtils.isEmpty(start)) {
            InstantModel instant = new InstantModel();
            instant.setDateTimeStamp(OffsetDateTime.parse(start));
            model.setStart(instant);
        }
        if (!StringUtils.isEmpty(end)) {
            InstantModel endInstant = new InstantModel();
            endInstant.setDateTimeStamp(OffsetDateTime.parse(end));
            model.setEnd(endInstant);
        }
        model.setDescription(getDescription());

        List<InfrastructureFacilityModel> facilityList = new ArrayList<>(facilities.size());
        facilities.forEach((facilityUri) -> {
            InfrastructureFacilityModel facilityModel = new InfrastructureFacilityModel();
            facilityModel.setUri(facilityUri);
            facilityList.add(facilityModel);
        });
        model.setFacilities(facilityList);

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

        List<StepModel> stepList = new ArrayList<>(step.size());
        step.forEach((URI u) -> {
            StepModel stepModel = new StepModel();
            stepModel.setUri(u);
            stepList.add(stepModel);
        });
        model.setStep(stepList);
        return model;
    }

}
