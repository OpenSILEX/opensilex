//******************************************************************************
//                          ExperimentGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * A basic GetDTO which add the conversion from an {@link ExperimentModel} to a {@link ExperimentGetDTO}
 *
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentGetDTO {

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ExperimentGetDTO fromModel(ExperimentModel model) {

        ExperimentGetDTO dto = new ExperimentGetDTO();

        dto.setUri(model.getUri())
                dto.setLabel(model.getLabel())
                dto.setStartDate(model.getStartDate())
                .setCampaign(model.getCampaign())
                .setObjective(model.getObjective())
                .setComment(model.getComment())
                .setKeywords(model.getKeywords())
                .setIsPublic(model.getIsPublic())
                .setSensors(model.getSensors())
                .setVariables(model.getVariables())
                .setFactors(model.getFactors());

        if (model.getEndDate() != null) {
            dto.setEndDate(model.getEndDate());
        }

        dto.setScientificSupervisors(getUriList(model.getScientificSupervisors()))
                .setTechnicalSupervisors(getUriList(model.getTechnicalSupervisors()))
                .setProjects(getUriList(model.getProjects()))
                .setGroups(getUriList(model.getGroups()))
                .setSpecies(getUriList(model.getSpecies()))
                .setInfrastructures(getUriList(model.getInfrastructures()));

        return dto;
    }
}
