//******************************************************************************
//                          ProcessGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.api;

import org.opensilex.process.process.dal.ProcessModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.Collections;
import org.opensilex.sparql.model.SPARQLResourceModel;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Emilie Fernandez
 */
public class ProcessGetDTO extends ProcessDTO {

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    } 

    public static ProcessGetDTO fromModel(ProcessModel model) {

        ProcessGetDTO dto = new ProcessGetDTO();
        dto.setUri(model.getUri())
                .setName(model.getName())
                // .setExperiment(model.getExperiment().getUri())
                .setCreationDate(model.getCreationDate())
                .setDestructionDate(model.getDestructionDate())
                .setDescription(model.getDescription())
                .setInfrastructures(getUriList(model.getInfrastructures()))
                .setScientificSupervisors(getUriList(model.getScientificSupervisors()))
                .setTechnicalSupervisors(getUriList(model.getTechnicalSupervisors()))
                .setStep(getUriList(model.getStep()));
    
        return dto;
    }

}
