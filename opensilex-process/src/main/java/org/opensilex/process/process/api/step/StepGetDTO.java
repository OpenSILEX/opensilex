//******************************************************************************
//                          StepGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.step.api;

import org.opensilex.process.process.dal.StepModel;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.Collections;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * @author Emilie Fernandez
 */
public class StepGetDTO extends StepDTO {

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static StepGetDTO fromModel(StepModel model) {

        StepGetDTO dto = new StepGetDTO();
        dto.setUri(model.getUri())
                .setName(model.getName())
                .setAfter(model.getAfter())
                .setBefore(model.getBefore())
                .setDescription(model.getDescription())
                .setInput(getUriList(model.getInput()))
                .setOutput(getUriList(model.getOutput()));
        if(model.getStart() != null){
            dto.setStart(model.getStart().getDateTimeStamp().toString());
        }
        if(model.getEnd() != null){
            dto.setEnd(model.getEnd().getDateTimeStamp().toString());
        }
        
        return dto;
    }
}
