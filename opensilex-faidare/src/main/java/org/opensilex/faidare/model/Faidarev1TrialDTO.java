//******************************************************************************
//                          Faidarev1TrialDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1TrialDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.account.dal.AccountModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1TrialDTO {
    private Boolean active;
    private HashMap<String, Object> additionalInfo;
    private String documentationURL;
    private String endDate;
    private String startDate;
    private String trialName;
    private String trialDbId;
    private String trialType;
    private Faidarev1DatasetAuthorshipDTO datasetAuthorship;
    private List<Faidarev1StudySummaryDTO> studies;
    private List<Faidarev1ExtendedContactDTO> contacts;
    private String programDbId;
    private String programName;

    public Boolean getActive() {
        return active;
    }

    public Faidarev1TrialDTO setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public HashMap<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public Faidarev1TrialDTO setAdditionalInfo(HashMap<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1TrialDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public Faidarev1TrialDTO setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public Faidarev1TrialDTO setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getTrialName() {
        return trialName;
    }

    public Faidarev1TrialDTO setTrialName(String trialName) {
        this.trialName = trialName;
        return this;
    }

    public String getTrialDbId() {
        return trialDbId;
    }

    public Faidarev1TrialDTO setTrialDbId(String trialDbId) {
        this.trialDbId = trialDbId;
        return this;
    }

    public String getTrialType() {
        return trialType;
    }

    public Faidarev1TrialDTO setTrialType(String trialType) {
        this.trialType = trialType;
        return this;
    }

    public Faidarev1DatasetAuthorshipDTO getDatasetAuthorship() {
        return datasetAuthorship;
    }

    public Faidarev1TrialDTO setDatasetAuthorship(Faidarev1DatasetAuthorshipDTO datasetAuthorship) {
        this.datasetAuthorship = datasetAuthorship;
        return this;
    }

    public List<Faidarev1StudySummaryDTO> getStudies() {
        return studies;
    }

    public Faidarev1TrialDTO setStudies(List<Faidarev1StudySummaryDTO> studies) {
        this.studies = studies;
        return this;
    }

    public List<Faidarev1ExtendedContactDTO> getContacts() {
        return contacts;
    }

    public Faidarev1TrialDTO setContacts(List<Faidarev1ExtendedContactDTO> contacts) {
        this.contacts = contacts;
        return this;
    }

    public String getProgramDbId() {
        return programDbId;
    }

    public Faidarev1TrialDTO setProgramDbId(String programDbId) {
        this.programDbId = programDbId;
        return this;
    }

    public String getProgramName() {
        return programName;
    }

    public Faidarev1TrialDTO setProgramName(String programName) {
        this.programName = programName;
        return this;
    }

    public static Faidarev1TrialDTO fromModel(ProjectModel projectModel, ExperimentDAO experimentDAO, AccountModel accountModel) throws Exception {
        Faidarev1TrialDTO dto = new Faidarev1TrialDTO();
        dto.setDocumentationURL(Objects.toString(projectModel.getHomePage(), null))
                .setEndDate(Objects.toString(projectModel.getEndDate(), null))
                .setStartDate(Objects.toString(projectModel.getStartDate(), null))
                .setTrialName(Objects.toString(projectModel.getName(), null))
                .setTrialDbId(Objects.toString(projectModel.getUri(), null))
                .setTrialType(Objects.toString(projectModel.getObjective(), null))
                .setStudies(
                        experimentDAO.search(
                                        new ExperimentSearchFilter().setUser(accountModel)
                                                .setProjects(Collections.singletonList(projectModel.getUri()))
                                )
                                .getList()
                                .stream().map(Faidarev1StudySummaryDTO::fromModel).collect(Collectors.toList())
                )
                .setContacts(
                        Stream.concat(
                                        projectModel.getAdministrativeContacts()
                                                .stream().map(Faidarev1ExtendedContactDTO::fromModel),
                                        projectModel.getScientificContacts()
                                                .stream().map(Faidarev1ExtendedContactDTO::fromModel))
                                .collect(Collectors.toList())
                )
                .setAdditionalInfo(
                        new HashMap<String, Object>() {{
                            put("shortName", projectModel.getShortname());
                            put("description", projectModel.getDescription());
                            put("financialFunding", projectModel.getFinancialFunding());
                            put("relatedProjects", projectModel.getRelatedProjects());
                            put("coordinators", projectModel.getCoordinators());
                        }}
                );

        return dto;
    }
}
