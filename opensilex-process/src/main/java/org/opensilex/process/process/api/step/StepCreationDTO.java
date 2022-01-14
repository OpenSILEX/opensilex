//******************************************************************************
//                          StepCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.step.api;

import org.opensilex.process.process.dal.StepModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.model.time.InstantModel;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
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
        model.setAfter(getAfter());
        model.setBefore(getBefore());
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

        return model;
    }

}
