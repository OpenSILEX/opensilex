//******************************************************************************
//                          StepCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.step.api;

import org.opensilex.process.process.dal.StepModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import java.util.ArrayList;
import java.net.URI;
import java.util.List;

/**
 * @author Emilie Fernandez
 */
public class StepCreationDTO extends StepDTO {
  
    public StepModel newModel() {

        StepModel model = new StepModel();
        model.setUri(getUri());
        model.setName(getName());
        model.setStartDate(getStartDate());
        model.setEndDate(getEndDate());
        model.setDescription(getDescription());

        List<ScientificObjectModel> inputList = new ArrayList<>(input.size());
        input.forEach((inputUri) -> {
            ScientificObjectModel soInputModel = new ScientificObjectModel();
            soInputModel.setUri(inputUri);
            inputList.add(soInputModel);
        });
        model.setInput(inputList);

        List<ScientificObjectModel> outputList = new ArrayList<>(output.size());
        output.forEach((outputUri) -> {
            ScientificObjectModel soOutputModel = new ScientificObjectModel();
            soOutputModel.setUri(outputUri);
            outputList.add(soOutputModel);
        });
        model.setOutput(outputList);

        List<InfrastructureFacilityModel> facilityList = new ArrayList<>(facilities.size());
        facilities.forEach((facilityUri) -> {
            InfrastructureFacilityModel facilityModel = new InfrastructureFacilityModel();
            facilityModel.setUri(facilityUri);
            facilityList.add(facilityModel);
        });
        model.setFacilities(facilityList);

        return model;
    }

}
