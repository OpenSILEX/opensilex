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
import java.util.Objects;
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
public class ProjectGetDTO extends ProjectDTO {

    public static ProjectGetDTO fromModel(ProjectModel model) {

        ProjectGetDTO dto = new ProjectGetDTO();

        dto.setUri(model.getUri())
                .setName(model.getName())
                .setStartDate(model.getStartDate())
                .setShortname(model.getShortname())
                .setFinancialFunding(model.getFinancialFunding())
                .setDescription(model.getDescription())
                .setObjective(model.getObjective())
                .setHomePage(model.getHomePage());

        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }
        if (model.getEndDate() != null) {
            dto.setEndDate(model.getEndDate());
        }
        dto.setScientificContacts(SPARQLResourceModel.getUriList(model.getScientificContacts()))
                .setAdministrativeContacts(SPARQLResourceModel.getUriList(model.getAdministrativeContacts()))
                .setCoordinators(SPARQLResourceModel.getUriList(model.getCoordinators()))
                .setRelatedProjects(SPARQLResourceModel.getUriList(model.getRelatedProjects()));

        return dto;
    }

}
