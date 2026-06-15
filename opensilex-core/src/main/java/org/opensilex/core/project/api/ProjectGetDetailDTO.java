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
public class ProjectGetDetailDTO extends ProjectDTO {

    public static ProjectGetDetailDTO fromModel(ProjectModel model) {

        ProjectGetDetailDTO dto = new ProjectGetDetailDTO();

        dto.setUri(model.getUri())
                .setName(model.getName())
                .setStartDate(model.getStartDate())
                .setEndDate(model.getEndDate())
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

        dto     .setAdministrativeContacts(SPARQLResourceModel.getUriList(model.getAdministrativeContacts()))
                .setCoordinators(SPARQLResourceModel.getUriList(model.getCoordinators()))
                .setScientificContacts(SPARQLResourceModel.getUriList(model.getScientificContacts()))
                .setRelatedProjects(SPARQLResourceModel.getUriList(model.getRelatedProjects()));

        return dto;
    }

}
