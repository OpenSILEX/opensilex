package org.opensilex.faidare.builder;

import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.faidare.model.Faidarev1TrialAdditionalInfoDTO;
import org.opensilex.faidare.model.Faidarev1TrialDTO;
import org.opensilex.security.account.dal.AccountModel;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Faidarev1TrialDTOBuilder {
    private final ExperimentDAO experimentDAO;

    public Faidarev1TrialDTOBuilder(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    public Faidarev1TrialDTO fromModel(ProjectModel projectModel, AccountModel accountModel) throws Exception {
        Faidarev1StudySummaryDTOBuilder studySummaryDTOBuilder = new Faidarev1StudySummaryDTOBuilder();
        Faidarev1ExtendedContactDTOBuilder extendedContactDTOBuilder = new Faidarev1ExtendedContactDTOBuilder();
        Faidarev1TrialDTO dto = new Faidarev1TrialDTO();
        dto.setDocumentationURL(Objects.toString(projectModel.getHomePage(), null))
                .setEndDate(Objects.toString(projectModel.getEndDate(), null))
                .setStartDate(Objects.toString(projectModel.getStartDate(), null))
                .setTrialName(Objects.toString(projectModel.getName(), null))
                .setTrialDbId(Objects.toString(projectModel.getUri(), null))
                .setTrialType(Objects.toString(projectModel.getObjective(), null))
                .setStudies(
                        this.experimentDAO.search(
                                        new ExperimentSearchFilter().setUser(accountModel)
                                                .setProjects(Collections.singletonList(projectModel.getUri()))
                                )
                                .getList()
                                .stream().map(studySummaryDTOBuilder::fromModel).collect(Collectors.toList())
                )
                .setContacts(
                        Stream.concat(
                                        projectModel.getAdministrativeContacts()
                                                .stream().map(extendedContactDTOBuilder::fromModel),
                                        projectModel.getScientificContacts()
                                                .stream().map(extendedContactDTOBuilder::fromModel))
                                .collect(Collectors.toList())
                )
                .setAdditionalInfo(
                        new Faidarev1TrialAdditionalInfoDTO(
                                projectModel.getShortname(),
                                projectModel.getDescription(),
                                projectModel.getFinancialFunding(),
                                projectModel.getRelatedProjects()
                                        .stream().map(projectModel1 -> projectModel1.getUri().toString())
                                        .collect(Collectors.toList()),
                                projectModel.getCoordinators()
                                        .stream().map(extendedContactDTOBuilder::fromModel)
                                        .collect(Collectors.toList())
                        )
                );

        return dto;
    }
}
