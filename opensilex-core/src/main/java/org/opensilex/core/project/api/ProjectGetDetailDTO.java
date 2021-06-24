/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * A basic GetDTO which extends the {@link ProjectDTO} and which add the
 * conversion from an {@link ProjectModel} to a {@link ProjectGetDTO}
 *
 * @author vidalmor
 * @author Julien BONNEFONT
 */
public class ProjectGetDetailDTO extends ProjectDTO {

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ProjectGetDetailDTO fromModel(ProjectModel model) {

        ProjectGetDetailDTO dto = new ProjectGetDetailDTO();

        dto.setUri(model.getUri())
                .setName(model.getName())
                .setType(model.getType())
                .setStartDate(model.getStartDate())
                .setEndDate(model.getEndDate())
                .setShortname(model.getShortname())
                .setFinancialFunding(model.getFinancialFunding())
                .setDescription(model.getDescription())
                .setObjective(model.getObjective())
                .setHomePage(model.getHomePage());

        dto     .setAdministrativeContacts(getUriList(model.getAdministrativeContacts()))
                .setCoordinators(getUriList(model.getCoordinators()))
                .setScientificContacts(getUriList(model.getScientificContacts()))
                .setRelatedProjects(getUriList(model.getRelatedProjects()));

        return dto;
    }

}
