package org.opensilex.faidare.builder;

import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.faidare.model.Faidarev1TrialAdditionalInfoDTO;
import org.opensilex.faidare.model.Faidarev1TrialDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

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
        Faidarev1ContactDTOBuilder contactDTOBuilder = new Faidarev1ContactDTOBuilder();
        Faidarev1TrialDTO dto = new Faidarev1TrialDTO();
        dto.setDocumentationURL(Objects.toString(projectModel.getHomePage(), null))
                .setEndDate(Objects.toString(projectModel.getEndDate(), null))
                .setStartDate(Objects.toString(projectModel.getStartDate(), null))
                .setTrialName(Objects.toString(projectModel.getName(), null))
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
                                                .stream().map(person -> contactDTOBuilder.fromModel(person, null)),
                                        projectModel.getScientificContacts()
                                                .stream().map(person -> contactDTOBuilder.fromModel(person, null)))
                                .collect(Collectors.toList())
                )
                .setAdditionalInfo(
                        new Faidarev1TrialAdditionalInfoDTO(
                                projectModel.getShortname(),
                                projectModel.getDescription(),
                                projectModel.getFinancialFunding(),
                                projectModel.getRelatedProjects()
                                        .stream().map(projectModel1 -> SPARQLDeserializers.getExpandedURI(projectModel1.getUri()))
                                        .collect(Collectors.toList()),
                                projectModel.getCoordinators()
                                        .stream().map(person -> contactDTOBuilder.fromModel(person, null))
                                        .collect(Collectors.toList())
                        )
                );

        if (Objects.nonNull(projectModel.getUri())) {
            dto.setTrialDbId(SPARQLDeserializers.getExpandedURI(projectModel.getUri()));
        }

        return dto;
    }
}
