/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.experiment.dal.ExperimentDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * A basic GetDTO which extends the {@link ExperimentDTO} and which add the conversion from
 * an {@link ExperimentModel} to a {@link ExperimentGetDTO}
 *
 *  @author Vincent MIGOT
 *  @author Renaud COLIN
 */
public class ExperimentGetDTO extends ExperimentDTO {

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
                .setLabel(model.getLabel())
                .setStartDate(model.getStartDate().toString())
                .setCampaign(model.getCampaign())
                .setObjective(model.getObjective())
                .setComment(model.getComment())
                .setKeywords(model.getKeywords())
                .setSpecies(model.getSpecies())
                .setInfrastructures(model.getInfrastructures())
                .setDevices(model.getDevices())
                .setIsPublic(model.getIsPublic())
                .setSensors(model.getSensors());

        if (model.getEndDate() != null) {
            dto.setEndDate(model.getEndDate().toString());
        }

        dto.setScientificSupervisors(getUriList(model.getScientificSupervisors()))
                .setTechnicalSupervisors(getUriList(model.getTechnicalSupervisors()))
                .setProjects(getUriList(model.getProjects()))
                .setGroups(getUriList(model.getGroups()))
                .setVariables(getUriList(model.getVariables()));

        return dto;
    }
}
